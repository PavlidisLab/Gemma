package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.model.GeoChannel;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.TenXCellRangerUtils;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Find single-cell datasets whose 10x MEX data was filtered with the wrong Cell Ranger cell calling parameters.
 * <p>
 * Until <a href="https://github.com/PavlidisLab/Gemma/issues/1700">#1700</a>, {@code filter-10x-mex.py} passed the
 * chemistry key (e.g. {@code SC3Pv3-polyA}) where Cell Ranger expects its description, so cell calling fell back to
 * the defaults (EmptyDrops FDR of 0.01 and a 45000-90000 ambient range).
 * <p>
 * Neither the filter nor the chemistry is recorded in the database, so this reconstructs both from what the load
 * left behind, without any network access:
 * <ul>
 * <li>the filter ran if a sample has fewer stored cells than barcodes in its raw download;</li>
 * <li>the chemistry is the one the loader would have detected from the cached GEO SOFT file.</li>
 * </ul>
 * A chemistry passed by hand with {@code -mexChemistry} cannot be recovered, so the detected chemistry is only what
 * the loader would have used by default.
 * <p>
 * This command is read-only.
 */
public class SingleCell10xFilterAuditCli extends ExpressionExperimentManipulatingCLI {

    private static final String OUTPUT_OPTION = "o";
    private static final String GEO_LIST_OPTION = "geoList";

    /**
     * Chemistries whose description changes the cell calling parameters relative to the key, checked against Cell
     * Ranger 9.0.1. These are all the chemistries the loader can detect except 3' v3 OCM, whose description falls back
     * to the same parameters.
     */
    private static final Set<String> AFFECTED_CHEMISTRIES = Set.of(
            "SC3Pv3-polyA", "SC3Pv3-CS1", "SC3Pv3HT-polyA", "SC3Pv3HT-CS1",
            "SC3Pv4-polyA", "SC3Pv4-CS1", "SC3Pv4-polyA-OCM", "SC3Pv4-CS1-OCM" );

    private static final Set<String> UNAFFECTED_CHEMISTRIES = Set.of( "SC3Pv3-polyA-OCM", "SC3Pv3-CS1-OCM" );

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BuildInfo buildInfo;

    @Value("${gemma.download.path}/singleCellData")
    private Path singleCellDataBasePath;

    @Value("${geo.local.datafile.basepath}")
    private Path geoSeriesDownloadPath;

    private Path outputFile;
    @Nullable
    private Path geoListFile;
    @Nullable
    private PrintWriter output;

    public SingleCell10xFilterAuditCli() {
        // troubled datasets were filtered too, and the audit only reads
        setForce();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "auditSingleCell10xFilter";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Report single-cell datasets filtered with the wrong 10x chemistry parameters (#1700); read-only";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addRequiredOption( OUTPUT_OPTION, "output", true,
                "TSV to write, one row per sample. A SHA-256 checksum is written next to it." );
        options.addOption( GEO_LIST_OPTION, "geo-accessions-file", true,
                "File of GEO series accessions (one per line) to audit instead of the usual dataset selection. Every"
                        + " dataset with that accession is audited, split parts included, and accessions that match no"
                        + " dataset are listed in a .not-loaded.txt file next to the output instead of failing the run." );
    }

    @Override
    protected boolean selectsOwnExperiments( CommandLine commandLine ) {
        return commandLine.hasOption( GEO_LIST_OPTION );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        outputFile = Paths.get( commandLine.getOptionValue( OUTPUT_OPTION ) );
        if ( commandLine.hasOption( GEO_LIST_OPTION ) ) {
            geoListFile = Paths.get( commandLine.getOptionValue( GEO_LIST_OPTION ) );
        }
        if ( Files.exists( outputFile ) ) {
            throw new ParseException( outputFile + " already exists, refusing to overwrite it." );
        }
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( geoListFile == null ) {
            super.doAuthenticatedWork();
            return;
        }
        List<String> accessions = ubic.gemma.cli.util.FileUtils.readListFileToStrings( geoListFile );
        Collection<ExpressionExperiment> ees = new TreeSet<>( Comparator.comparing( ExpressionExperiment::getId ) );
        List<String> notLoaded = new ArrayList<>();
        for ( String accession : accessions ) {
            Collection<ExpressionExperiment> found = eeService.findByAccession( accession );
            if ( found.isEmpty() ) {
                notLoaded.add( accession );
            } else {
                ees.addAll( found );
            }
        }
        Files.write( Paths.get( outputFile + ".not-loaded.txt" ), notLoaded, StandardCharsets.UTF_8 );
        log.info( String.format( "%d of %d accessions in %s matched %d datasets; the other %d are listed in %s.not-loaded.txt.",
                accessions.size() - notLoaded.size(), accessions.size(), geoListFile, ees.size(), notLoaded.size(), outputFile ) );
        setEstimatedMaxTasks( ees.size() );
        processExpressionExperiments( ees );
        postprocessExpressionExperiments( ees );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) throws Exception {
        if ( output == null ) {
            output = new PrintWriter( Files.newBufferedWriter( outputFile, StandardCharsets.UTF_8 ) );
            output.println( "# command: " + getCommandName() );
            output.println( "# gemma: " + buildInfo );
            output.println( "# started: " + Instant.now() );
            output.println( "# single_cell_data: " + singleCellDataBasePath );
            output.println( "# geo_soft_files: " + geoSeriesDownloadPath );
            output.println( "ee_id\tshort_name\tgeo_series\tquantitation_type\tsample\traw_barcodes\tstored_cells"
                    + "\tdetected_chemistry\tverdict\tnote" );
        }

        ee = eeService.thawLite( ee );
        Optional<QuantitationType> qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee );
        if ( qt.isEmpty() ) {
            addWarningObject( ee, "No preferred single-cell quantitation type." );
            return;
        }
        if ( !StringUtils.startsWith( qt.get().getDescription(), "10x MEX data loaded from" ) ) {
            addSuccessObject( ee, "Preferred single-cell data was not loaded from 10x MEX, not affected." );
            return;
        }
        if ( ee.getAccession() == null || !ExternalDatabases.GEO.equals( ee.getAccession().getExternalDatabase().getName() ) ) {
            // only GEO datasets have a cached SOFT file and a raw download under a known path
            record( ee, null, qt.get(), "", null, null, null, "REVIEW", "not from GEO" );
            addWarningObject( ee, "Not from GEO, cannot be audited." );
            return;
        }
        String gse = ee.getAccession().getAccession();

        GeoSeries series = readCachedGeoSeries( gse );
        Map<Long, BioAssay> bioAssaysById = ee.getBioAssays().stream()
                .collect( Collectors.toMap( BioAssay::getId, Function.identity() ) );
        SingleCellDimension dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt.get() );
        int affected = 0;
        for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
            BioAssay ba = Objects.requireNonNull( bioAssaysById.get( dimension.getBioAssays().get( i ).getId() ) );
            String sampleName = ba.getAccession() != null ? ba.getAccession().getAccession() : ba.getName();
            int storedCells = dimension.getNumberOfCellIdsBySample( i );
            Long rawBarcodes = countRawBarcodes( singleCellDataBasePath.resolve( "GEO" ).resolve( gse ).resolve( sampleName ) );
            String chemistry = series != null ? detectChemistry( series, sampleName ) : null;
            String verdict;
            String note = "";
            if ( rawBarcodes == null ) {
                verdict = "REVIEW";
                note = "raw barcodes.tsv.gz not found";
            } else if ( rawBarcodes == storedCells ) {
                verdict = "NOT_FILTERED";
            } else if ( rawBarcodes < storedCells ) {
                verdict = "REVIEW";
                note = "more stored cells than raw barcodes";
            } else if ( series == null ) {
                verdict = "REVIEW";
                note = "filtered, but no cached SOFT file to detect the chemistry";
            } else if ( chemistry == null ) {
                verdict = "FILTERED_DEFAULTS";
                note = "no chemistry detected, Cell Ranger defaults were correct unless one was passed by hand";
            } else if ( AFFECTED_CHEMISTRIES.contains( chemistry ) ) {
                verdict = "AFFECTED";
                affected++;
            } else if ( UNAFFECTED_CHEMISTRIES.contains( chemistry ) ) {
                verdict = "FILTERED_UNCHANGED";
                note = "the description gives the same parameters as the key";
            } else {
                verdict = "REVIEW";
                note = "chemistry not assessed for #1700";
            }
            record( ee, gse, qt.get(), sampleName, rawBarcodes, storedCells, chemistry, verdict, note );
        }
        if ( affected > 0 ) {
            addWarningObject( ee, affected + " sample(s) filtered with the wrong chemistry parameters." );
        } else {
            addSuccessObject( ee, "No affected samples." );
        }
    }

    @Override
    protected void postprocessExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( output == null ) {
            return;
        }
        output.close();
        try ( InputStream is = Files.newInputStream( outputFile ) ) {
            Files.writeString( Paths.get( outputFile + ".sha256" ),
                    DigestUtils.sha256Hex( is ) + "  " + outputFile.getFileName() + "\n" );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to checksum " + outputFile + ".", e );
        }
        log.info( "Wrote audit to " + outputFile + "." );
    }

    private void record( ExpressionExperiment ee, @Nullable String gse, QuantitationType qt, String sample,
            @Nullable Long rawBarcodes, @Nullable Integer storedCells, @Nullable String chemistry, String verdict, String note ) {
        Objects.requireNonNull( output ).println( ee.getId() + "\t" + ee.getShortName() + "\t" + StringUtils.defaultString( gse )
                + "\t" + qt.getName() + "\t" + sample
                + "\t" + Objects.toString( rawBarcodes, "" ) + "\t" + Objects.toString( storedCells, "" )
                + "\t" + StringUtils.defaultString( chemistry ) + "\t" + verdict + "\t" + note );
        output.flush();
    }

    /**
     * Read the GEO series from the SOFT file cached by the loader, never from GEO.
     */
    @Nullable
    private GeoSeries readCachedGeoSeries( String gse ) throws IOException {
        Path softFile = geoSeriesDownloadPath.resolve( gse ).resolve( gse + ".soft.gz" );
        if ( !Files.exists( softFile ) ) {
            return null;
        }
        try ( InputStream is = FileUtils.openCompressedFile( softFile ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return Objects.requireNonNull( parser.getUniqueResult() ).getSeries().get( gse );
        }
    }

    /**
     * Same detection as {@code MexSingleCellDataLoaderConfigurer#detect10xChemistry}.
     */
    @Nullable
    private String detectChemistry( GeoSeries series, String gsm ) {
        return series.getSamples().stream()
                .filter( s -> gsm.equals( s.getGeoAccession() ) )
                .findFirst()
                .map( GeoSample::getChannels )
                .flatMap( channels -> channels.stream()
                        .map( GeoChannel::getExtractProtocol )
                        .map( TenXCellRangerUtils::detect10xChemistry )
                        .filter( Objects::nonNull )
                        .findFirst() )
                .orElse( null );
    }

    @Nullable
    private Long countRawBarcodes( Path sampleDir ) throws IOException {
        Path barcodes = sampleDir.resolve( "barcodes.tsv.gz" );
        if ( !Files.exists( barcodes ) ) {
            return null;
        }
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileUtils.openCompressedFile( barcodes ), StandardCharsets.UTF_8 ) ) ) {
            return reader.lines().count();
        }
    }
}
