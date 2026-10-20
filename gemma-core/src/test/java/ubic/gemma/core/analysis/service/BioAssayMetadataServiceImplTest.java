package ubic.gemma.core.analysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ubic.gemma.core.security.audit.payload.SampleMetadataPayload;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.ExtractedMolecule;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>What this pins</h2>
 * The three columns this service writes are otherwise importer-only and unvalidated, so the tests that
 * matter are the ones about refusing a bad value and about not manufacturing audit events.
 * <p>
 * Constructed directly rather than through {@code @ContextConfiguration}: there is no Spring wiring worth
 * exercising here, and a context would make a fast test slow for nothing.
 */
public class BioAssayMetadataServiceImplTest {

    private BioAssayService bioAssayService;
    private BioAssayMetadataAuditService auditService;
    private BioAssayMetadataServiceImpl service;
    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() throws Exception {
        bioAssayService = mock( BioAssayService.class );
        auditService = mock( BioAssayMetadataAuditService.class );
        service = new BioAssayMetadataServiceImpl();
        inject( "bioAssayService", bioAssayService );
        inject( "bioAssayMetadataAuditService", auditService );
        ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE1" );
    }

    private void inject( String field, Object value ) throws Exception {
        java.lang.reflect.Field f = BioAssayMetadataServiceImpl.class.getDeclaredField( field );
        f.setAccessible( true );
        f.set( service, value );
    }

    private BioAssay assay( Long id, String strategy ) {
        BioAssay ba = BioAssay.Factory.newInstance( "BA" + id );
        ba.setId( id );
        ba.setLibraryStrategy( strategy );
        return ba;
    }

    @Test
    public void testSetLibraryStrategyWritesAndAuditsOnce() {
        BioAssay a = assay( 1L, "RNA_SEQ" ), b = assay( 2L, "RNA_SEQ" );
        Collection<BioAssay> changed = service.setLibraryStrategy( ee, Arrays.asList( a, b ), "RIBO_SEQ" );

        assertThat( changed ).containsExactly( a, b );
        assertThat( a.getLibraryStrategy() ).isEqualTo( "RIBO_SEQ" );
        assertThat( b.getLibraryStrategy() ).isEqualTo( "RIBO_SEQ" );
        verify( bioAssayService, times( 2 ) ).update( any( BioAssay.class ) );

        // one event for the whole call, not one per sample
        ArgumentCaptor<SampleMetadataPayload> payload = ArgumentCaptor.forClass( SampleMetadataPayload.class );
        verify( auditService, times( 1 ) ).recordSampleMetadataChange( eq( ee ), anyString(), payload.capture() );
        assertThat( payload.getValue().field() ).isEqualTo( "libraryStrategy" );
        assertThat( payload.getValue().newValue() ).isEqualTo( "RIBO_SEQ" );
        assertThat( payload.getValue().bioAssays() ).hasSize( 2 );
    }

    /**
     * A sample already holding the value is skipped. Without this a re-run would write an audit event
     * recording a change that did not happen, which is worse than no record at all.
     */
    @Test
    public void testAlreadyHoldingTheValueIsNotWrittenAndNotAudited() {
        BioAssay already = assay( 1L, "RIBO_SEQ" );
        Collection<BioAssay> changed = service.setLibraryStrategy( ee, Collections.singletonList( already ), "RIBO_SEQ" );

        assertThat( changed ).isEmpty();
        verify( bioAssayService, never() ).update( any( BioAssay.class ) );
        verify( auditService, never() ).recordSampleMetadataChange( any(), anyString(), any() );
    }

    @Test
    public void testUnknownLibraryStrategyIsRefusedAndNothingIsWritten() {
        BioAssay a = assay( 1L, "RNA_SEQ" );
        assertThatThrownBy( () -> service.setLibraryStrategy( ee, Collections.singletonList( a ), "RIBOSEQ" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "RIBOSEQ" )
                .hasMessageContaining( "RIBO_SEQ" ); // the 400 tells the caller what IS accepted
        assertThat( a.getLibraryStrategy() ).isEqualTo( "RNA_SEQ" );
        verify( bioAssayService, never() ).update( any( BioAssay.class ) );
        verify( auditService, never() ).recordSampleMetadataChange( any(), anyString(), any() );
    }

    /** Gemma's own microarray values are not in GeoLibraryStrategy and must still be accepted. */
    @Test
    public void testMicroarrayStrategiesAreAccepted() {
        BioAssay a = assay( 1L, null );
        service.setLibraryStrategy( ee, Collections.singletonList( a ), BioAssay.LIBRARY_STRATEGY_MICROARRAY_TWO_COLOR );
        assertThat( a.getLibraryStrategy() ).isEqualTo( "MICROARRAY_TWO_COLOR" );
    }

    /** Clearing is a real curation act — a wrong strategy has to be removable, not just replaceable. */
    @Test
    public void testNullClearsTheColumn() {
        BioAssay a = assay( 1L, "RNA_SEQ" );
        Collection<BioAssay> changed = service.setLibraryStrategy( ee, Collections.singletonList( a ), null );
        assertThat( changed ).containsExactly( a );
        assertThat( a.getLibraryStrategy() ).isNull();
        verify( auditService ).recordSampleMetadataChange( eq( ee ), contains( "Cleared libraryStrategy" ), any() );
    }

    @Test
    public void testExtractedMoleculeIsValidatedAgainstTheEnum() {
        BioAssay a = assay( 1L, null );
        assertThatThrownBy( () -> service.setExtractedMolecule( ee, Collections.singletonList( a ), "totalRna" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "totalRNA" );
        assertThat( a.getExtractedMolecule() ).isNull();

        service.setExtractedMolecule( ee, Collections.singletonList( a ), "totalRNA" );
        assertThat( a.getExtractedMolecule() ).isEqualTo( ExtractedMolecule.totalRNA );
    }

    /**
     * 🛑 librarySelection is deliberately NOT checked against a vocabulary — GeoSample keeps it as the
     * submitter's raw string so that normalizing cannot drop a value we do not know. This test exists to
     * make that a decision someone has to override on purpose rather than a gap they patch by accident.
     */
    @Test
    public void testLibrarySelectionAcceptsFreeTextButEnforcesTheColumnLength() {
        BioAssay a = assay( 1L, null );
        service.setLibrarySelection( ee, Collections.singletonList( a ), "size fractionation" );
        assertThat( a.getLibrarySelection() ).isEqualTo( "size fractionation" );

        String tooLong = new String( new char[256] ).replace( '\0', 'x' );
        assertThatThrownBy( () -> service.setLibrarySelection( ee, Collections.singletonList( a ), tooLong ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "255" );
        assertThat( a.getLibrarySelection() ).isEqualTo( "size fractionation" );
    }

    @Test
    public void testValidLibraryStrategiesCoversBothVocabularies() {
        assertThat( service.getValidLibraryStrategies() )
                .contains( "RNA_SEQ", "RIBO_SEQ", "ATAC_SEQ", "MICROARRAY_ONE_COLOR", "MICROARRAY_TWO_COLOR" )
                .doesNotContain( "RNA-Seq", "Ribo-Seq" ); // constant names, not GEO's spelling
    }
}
