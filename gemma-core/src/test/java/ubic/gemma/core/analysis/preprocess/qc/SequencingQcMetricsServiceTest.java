package ubic.gemma.core.analysis.preprocess.qc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SequencingQcMetricsServiceImpl}, driven off two MultiQC fixtures: a trimmed
 * copy of a real production report (the array-shaped general-stats block, every row keyed by a bare
 * GSM), and a hand-built one in the object shape MultiQC 1.21 emits, whose rows mix bare accessions,
 * run and mate suffixes, and a bare SRA run accession.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
class SequencingQcMetricsServiceTest {

    @Mock
    private ExpressionDataFileService expressionDataFileService;

    @InjectMocks
    private SequencingQcMetricsServiceImpl service;

    @Test
    void readsSampleLevelMetricsFromARealReport() throws Exception {
        ExpressionExperiment ee = experiment( assay( 1L, "GSM5029427" ), assay( 2L, "GSM5029428" ),
                assay( 3L, "GSM5029429" ) );
        givenReport( "GSE165287.multiqc_data.json" );

        SequencingQcMetrics metrics = service.getSequencingQcMetrics( ee ).orElseThrow( AssertionError::new );

        assertThat( metrics.isReportPresent() ).isTrue();
        assertThat( metrics.getUnmatchedKeys() ).isEmpty();
        assertThat( metrics.getSamples() ).hasSize( 3 );

        SequencingQcMetrics.SampleMetrics first = metrics.getSamples().get( 0 );
        assertThat( first.getBioAssayId() ).isEqualTo( 1L );
        assertThat( first.getAccession() ).isEqualTo( "GSM5029427" );
        assertThat( first.getRuns() ).isEmpty();
        // the three modules of the general-stats block merge into one row
        assertThat( first.getValues() )
                .containsKeys( "alignable_percent", "uniquely_mapped_percent", "percent_duplicates", "percent_gc" );
        assertThat( first.getValues().get( "uniquely_mapped_percent" ) ).isEqualTo( 51.07, within( 1e-9 ) );
        // read depth comes from the report when the report has a sample-level row
        assertThat( first.getReadCount() ).isEqualTo( 3998179L );
        assertThat( first.getReadCountSource() ).isEqualTo( "report" );

        // metric definitions carry MultiQC's own header text where it has one
        SequencingQcMetrics.MetricDefinition aligned = definition( metrics, "uniquely_mapped_percent" );
        assertThat( aligned.getTitle() ).isEqualTo( "% Aligned" );
        assertThat( aligned.getNamespace() ).isEqualTo( "STAR" );
        assertThat( aligned.getSuffix() ).isEqualTo( "%" );
        assertThat( aligned.getMax() ).isEqualTo( 100.0 );
        // and a metric MultiQC does not display gets an entry with just its name
        assertThat( definition( metrics, "mismatch_rate" ).getTitle() ).isNull();
    }

    /**
     * A row keyed by a run or a mate must NOT be merged into the sample's own values — the two are
     * at different levels and summarizing one into the other would need a per-metric rule. A row
     * keyed by an SRA run accession alone joins to nothing and has to be reported, not dropped.
     */
    @Test
    void keepsRunLevelRowsSeparateAndReportsUnjoinableOnes() throws Exception {
        ExpressionExperiment ee = experiment( assay( 10L, "GSM8030687" ), assay( 11L, "GSM8030688" ) );
        givenReport( "mixed-keys.multiqc_data.json" );

        SequencingQcMetrics metrics = service.getSequencingQcMetrics( ee ).orElseThrow( AssertionError::new );

        SequencingQcMetrics.SampleMetrics first = metrics.getSamples().get( 0 );
        assertThat( first.getValues() ).containsOnlyKeys( "total_reads", "uniquely_mapped_percent", "mismatch_rate",
                "alignable_percent", "bp_processed" );
        // the FastQC rows are per-mate, so they stay out of the sample row entirely
        assertThat( first.getRuns() ).extracting( SequencingQcMetrics.RunMetrics::getKey )
                .containsExactlyInAnyOrder( "GSM8030687_1", "GSM8030687_2" );
        assertThat( first.getRuns().get( 0 ).getValues() ).containsKeys( "percent_gc", "percent_duplicates" );
        // STAR is listed before RSEM, so its uniquely_mapped_percent is the one kept
        assertThat( first.getValues().get( "uniquely_mapped_percent" ) ).isEqualTo( 49.82, within( 1e-9 ) );

        // a key of the form <accession>_<run>_<mate> still resolves back to the accession
        SequencingQcMetrics.SampleMetrics second = metrics.getSamples().get( 1 );
        assertThat( second.getRuns() ).extracting( SequencingQcMetrics.RunMetrics::getKey )
                .containsExactly( "GSM8030688_SRR13191146_1" );

        // the bare SRA run accession matches no assay
        assertThat( metrics.getUnmatchedKeys() ).containsExactly( "SRR13191147" );

        // run-level metric names are described too, so a caller can label the runs
        assertThat( metrics.getMetrics() ).extracting( SequencingQcMetrics.MetricDefinition::getName )
                .contains( "percent_duplicates" );
        assertThat( definition( metrics, "percent_duplicates" ).isHidden() ).isTrue();
    }

    /**
     * Most RNA-seq assays carry a read count in the database, and only a minority of datasets have a
     * MultiQC report at all, so read depth alone still has to answer.
     */
    @Test
    void fallsBackToTheDatabaseReadCountWithoutAReport() throws Exception {
        BioAssay withCount = assay( 20L, "GSM1" );
        withCount.setSequenceReadCount( 12345678L );
        ExpressionExperiment ee = experiment( withCount, assay( 21L, "GSM2" ) );
        givenNoReport();

        SequencingQcMetrics metrics = service.getSequencingQcMetrics( ee ).orElseThrow( AssertionError::new );

        assertThat( metrics.isReportPresent() ).isFalse();
        assertThat( metrics.getMetrics() ).isEmpty();
        assertThat( metrics.getSamples() ).hasSize( 2 );
        assertThat( metrics.getSamples().get( 0 ).getReadCount() ).isEqualTo( 12345678L );
        assertThat( metrics.getSamples().get( 0 ).getReadCountSource() ).isEqualTo( "bioAssay" );
        assertThat( metrics.getSamples().get( 1 ).getReadCount() ).isNull();
        assertThat( metrics.getSamples().get( 1 ).getReadCountSource() ).isNull();
    }

    @Test
    void isEmptyWhenThereIsNeitherAReportNorAReadCount() throws Exception {
        ExpressionExperiment ee = experiment( assay( 30L, "GSM1" ) );
        givenNoReport();

        assertThat( service.getSequencingQcMetrics( ee ) ).isEmpty();
    }

    private void givenReport( String fixture ) throws Exception {
        URL url = Objects.requireNonNull( getClass().getResource( "/data/analysis/preprocess/qc/" + fixture ),
                "missing fixture " + fixture );
        Path path = Paths.get( url.toURI() );
        LockedPath locked = mock( LockedPath.class );
        when( locked.getPath() ).thenReturn( path );
        when( expressionDataFileService.getMetadataFile( any( ExpressionExperiment.class ),
                any( ExpressionExperimentMetaFileType.class ), anyBoolean() ) ).thenReturn( Optional.of( locked ) );
    }

    private void givenNoReport() throws IOException {
        when( expressionDataFileService.getMetadataFile( any( ExpressionExperiment.class ),
                any( ExpressionExperimentMetaFileType.class ), anyBoolean() ) ).thenReturn( Optional.empty() );
    }

    private static SequencingQcMetrics.MetricDefinition definition( SequencingQcMetrics metrics, String name ) {
        return metrics.getMetrics().stream()
                .filter( d -> d.getName().equals( name ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no definition for " + name ) );
    }

    private static BioAssay assay( Long id, @Nullable String accession ) {
        BioAssay ba = BioAssay.Factory.newInstance( accession );
        ba.setId( id );
        if ( accession != null ) {
            ba.setAccession( DatabaseEntry.Factory.newInstance( accession, null ) );
        }
        return ba;
    }

    private static ExpressionExperiment experiment( BioAssay... assays ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE1" );
        ee.setBioAssays( new LinkedHashSet<>( Arrays.asList( assays ) ) );
        return ee;
    }
}
