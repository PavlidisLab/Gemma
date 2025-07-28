package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.fetcher2.GeoFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.expression.sra.SraFetcher;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPClientFactoryImpl;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class SingleCellDataDownloaderCli extends AbstractCLI {

    private static final String
            ACCESSIONS_FILE_OPTION = "f",
            ACCESSIONS_OPTION = "e",
            SUMMARY_OUTPUT_FILE_OPTION = "s",
            RESUME_OPTION = "r",
            RESUME_IGNORE_UNKNOWN_DATASETS = "resumeIgnoreUnknownDatasets",
            RETRY_OPTION = "retry",
            RETRY_COUNT_OPTION = "retryCount",
            FETCH_THREADS_OPTION = "fetchThreads",
            SKIP_DOWNLOAD_OPTION = "skipDownload";

    private static final String
            SAMPLE_ACCESSIONS_OPTION = "sampleAccessions",
            DATA_TYPE_OPTION = "dataType",
            SUPPLEMENTARY_FILE_OPTION = "supplementaryFile";

    /**
     * Only applicable if dataType is set to MEX.
     */
    private static final String
            MEX_BARCODES_FILE_SUFFIX = "mexBarcodesFile",
            MEX_FEATURES_FILE_SUFFIX = "mexFeaturesFile",
            MEX_MATRIX_FILE_SUFFIX = "mexMatrixFile";

    private static final String[] SUMMARY_HEADER = new String[] { "geo_accession", "data_type", "number_of_samples", "number_of_cells", "number_of_genes", "additional_supplementary_files", "data_in_sra", "comment" };

    private static final String
            UNKNOWN_INDICATOR = "UNKNOWN",
            UNSUPPORTED_INDICATOR = "UNSUPPORTED",
            FAILED_INDICATOR = "FAILED";

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${geo.local.datafile.basepath}")
    private Path geoSeriesDownloadPath;

    @Value("${geo.local.singleCellData.basepath}")
    private Path singleCellDataBasePath;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private final Set<String> accessions = new HashSet<>();
    @Nullable
    private Path summaryOutputFile;
    private boolean resume;
    private String[] retry;
    @Nullable
    private Number fetchThreads;
    private boolean skipDownload;

    // single-accession options
    @Nullable
    private Set<String> sampleAccessions;
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String supplementaryFile;

    // MEX options
    @Nullable
    private String barcodesFileSuffix;
    @Nullable
    private String featuresFileSuffix;
    @Nullable
    private String matrixFileSuffix;

    private SimpleRetryPolicy retryPolicy;

    @Nullable
    @Override
    public String getCommandName() {
        return "downloadSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Download single cell data from GEO.\nFor the moment, only GEO series accessions are supported.";
    }

    @Override
    protected void buildOptions( Options options ) {
        // options are consistent with those of LoadExpressionDataCli
        options.addOption( Option.builder( ACCESSIONS_FILE_OPTION ).longOpt( "file" ).type( File.class ).hasArg().desc( "File containing accessions to download." ).build() );
        options.addOption( Option.builder( ACCESSIONS_OPTION ).longOpt( "acc" ).hasArg().desc( "Comma-delimited list of accessions to download." ).build() );
        options.addOption( Option.builder( SUMMARY_OUTPUT_FILE_OPTION ).longOpt( "summary-output-file" ).type( File.class ).hasArg().desc( "File to write the summary output to. This is used to keep track of progress and resume download with -r/--resume." ).build() );
        options.addOption( Option.builder( RESUME_OPTION ).longOpt( "resume" ).desc( "Resume download from a previous invocation of this command. Requires -s/--summary-output-file to be set and refer to an existing file." ).build() );
        options.addOption( Option.builder( RESUME_IGNORE_UNKNOWN_DATASETS ).longOpt( "resume-ignore-unknown-datasets" ).desc( "Ignore unknown datasets when resuming." ).build() );
        options.addOption( Option.builder( RETRY_OPTION ).longOpt( "retry" ).hasArg().desc( "Retry problematic datasets. Possible values are: '" + UNSUPPORTED_INDICATOR + "', '" + UNKNOWN_INDICATOR + "' or '" + FAILED_INDICATOR + "', or any combination delimited by ','. Requires -r/--resume option to be set." ).build() );
        options.addOption( Option.builder( RETRY_COUNT_OPTION ).longOpt( "retry-count" ).hasArg().type( Integer.class ).desc( "Number of times to retry a download operation." ).build() );
        options.addOption( SKIP_DOWNLOAD_OPTION, "skip-download", false, "Skip download of single-cell data." );
        options.addOption( Option.builder( FETCH_THREADS_OPTION ).longOpt( "fetch-threads" ).hasArg().type( Number.class ).desc( "Number of threads to use for downloading files. Default is " + GeoSingleCellDetector.DEFAULT_NUMBER_OF_FETCH_THREADS + ". Use -threads/--threads for processing series in parallel." ).build() );
        options.addOption( Option.builder( SAMPLE_ACCESSIONS_OPTION ).longOpt( "sample-accessions" ).hasArg().desc( "Comma-delimited list of sample accessions to download." ).build() );
        options.addOption( Option.builder( DATA_TYPE_OPTION ).longOpt( "data-type" ).hasArg().desc( "Data type. Possible values are: " + Arrays.stream( SingleCellDataType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + ". Only works if a single accession is passed to -e/--acc." ).build() );
        options.addOption( Option.builder( SUPPLEMENTARY_FILE_OPTION ).longOpt( "supplementary-file" ).hasArgs().desc( "Supplementary file to download. Only works if a single accession is passed to -e/--acc and -dataType is specified." ).build() );
        options.addOption( Option.builder( MEX_BARCODES_FILE_SUFFIX ).longOpt( "mex-barcodes-file" ).hasArg().desc( "Suffix to use to detect MEX barcodes file. Only works if -dataType/--data-type is set to MEX." ).build() );
        options.addOption( Option.builder( MEX_FEATURES_FILE_SUFFIX ).longOpt( "mex-features-file" ).hasArg().desc( "Suffix to use to detect MEX features file. Only works if -dataType/--data-type is set to MEX." ).build() );
        options.addOption( Option.builder( MEX_MATRIX_FILE_SUFFIX ).longOpt( "mex-matrix-file" ).hasArg().desc( "Suffix to use to detect MEX matrix file. Only works if -dataType/--data-type is set to MEX." ).build() );
        addBatchOption( options );
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        boolean singleAccessionMode = commandLine.hasOption( ACCESSIONS_OPTION )
                && !commandLine.getOptionValue( ACCESSIONS_OPTION ).contains( "," )
                && !commandLine.hasOption( ACCESSIONS_FILE_OPTION );
        if ( commandLine.hasOption( ACCESSIONS_OPTION ) ) {
            Arrays.stream( StringUtils.split( commandLine.getOptionValue( ACCESSIONS_OPTION ), ',' ) )
                    .filter( geoAccession -> {
                        if ( !geoAccession.startsWith( "GSE" ) ) {
                            log.warn( "Unsupported accession " + geoAccession );
                            return false;
                        }
                        return true;
                    } ).forEach( accessions::add );
            if ( singleAccessionMode && accessions.size() != 1 ) {
                throw new IllegalStateException( "In single accession mode, exactly one supported accession must be supplied." );
            }
        }
        if ( commandLine.hasOption( ACCESSIONS_FILE_OPTION ) ) {
            if ( singleAccessionMode ) {
                throw new IllegalStateException( "The -" + ACCESSIONS_FILE_OPTION + " option cannot be used in single accession mode." );
            }
            Path inputFile = ( ( File ) commandLine.getParsedOptionValue( ACCESSIONS_FILE_OPTION ) ).toPath();
            try ( Stream<String> lines = Files.lines( inputFile ) ) {
                lines.skip( 1 )
                        .filter( StringUtils::isNotBlank )
                        .map( line -> line.split( "\t", 2 )[0] )
                        .filter( geoAccession -> {
                            if ( !geoAccession.startsWith( "GSE" ) ) {
                                log.warn( "Unsupported accession " + geoAccession );
                                return false;
                            }
                            return true;
                        } )
                        .forEach( accessions::add );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        if ( commandLine.hasOption( SUMMARY_OUTPUT_FILE_OPTION ) ) {
            if ( singleAccessionMode ) {
                throw new IllegalStateException( "The -" + SUMMARY_OUTPUT_FILE_OPTION + " option cannot be used in single accession mode." );
            }
            summaryOutputFile = ( ( File ) commandLine.getParsedOptionValue( SUMMARY_OUTPUT_FILE_OPTION ) ).toPath();
        } else {
            summaryOutputFile = null;
        }
        resume = commandLine.hasOption( RESUME_OPTION );
        if ( commandLine.hasOption( RETRY_OPTION ) ) {
            retry = StringUtils.split( commandLine.getOptionValue( RETRY_OPTION ), "," );
            for ( String r : retry ) {
                if ( !r.equals( UNKNOWN_INDICATOR ) && !r.equals( UNSUPPORTED_INDICATOR ) && !r.equals( FAILED_INDICATOR ) ) {
                    throw new IllegalArgumentException( String.format( "Value for the %s option must be one of: %s, %s or %s.",
                            RETRY_OPTION, UNKNOWN_INDICATOR, UNSUPPORTED_INDICATOR, FAILED_INDICATOR ) );
                }
            }
        } else {
            retry = null;
        }
        Integer retryCount = commandLine.getParsedOptionValue( RETRY_COUNT_OPTION );
        if ( retryCount != null ) {
            retryPolicy = new SimpleRetryPolicy( retryCount, 1000, 1.5 );
        } else {
            retryPolicy = new SimpleRetryPolicy( 3, 1000, 1.5 );
        }
        if ( resume ) {
            if ( singleAccessionMode ) {
                throw new IllegalArgumentException( "The -" + RESUME_OPTION + " option cannot be used in single accession mode." );
            }
            if ( summaryOutputFile == null ) {
                throw new IllegalArgumentException( "The -" + RESUME_OPTION + " option requires the -" + SUMMARY_OUTPUT_FILE_OPTION + " option to be provided." );
            }
            if ( !Files.exists( summaryOutputFile ) ) {
                throw new IllegalStateException( "The summary output file " + summaryOutputFile + " does not exist." );
            }
            AtomicInteger accessionsToRetry = new AtomicInteger( 0 );
            try ( Stream<String> lines = Files.lines( summaryOutputFile ) ) {
                Set<String> accessionsToRemove = lines.skip( 1 )
                        .filter( line -> {
                            if ( retry != null ) {
                                String dataType = line.split( "\t", 3 )[1];
                                if ( ArrayUtils.contains( retry, dataType ) ) {
                                    accessionsToRetry.incrementAndGet();
                                    return false;
                                }
                            }
                            return true;
                        } )
                        .map( line -> line.split( "\t", 2 )[0] )
                        .collect( Collectors.toSet() );
                if ( accessionsToRemove.isEmpty() ) {
                    throw new RuntimeException( String.format( "No accessions were found in %s, is the file empty?", summaryOutputFile ) );
                }
                if ( !accessions.containsAll( accessionsToRemove ) ) {
                    Set<String> missingAccessions = new HashSet<>( accessionsToRemove );
                    missingAccessions.removeAll( accessions );
                    String message = String.format( "Some of the accessions from %s were not found as input, are you sure this is the right summary file?. Examples: %s.",
                            summaryOutputFile, missingAccessions.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) );
                    if ( commandLine.hasOption( RESUME_IGNORE_UNKNOWN_DATASETS ) ) {
                        log.warn( message );
                    } else {
                        throw new RuntimeException( message );
                    }
                }
                accessions.removeAll( accessionsToRemove );
                log.info( String.format( "Resuming download, %d accessions were already processed%s...",
                        accessionsToRemove.size(),
                        accessionsToRetry.get() > 0 ? " and " + accessionsToRetry.get() + " will be retried" : "" ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( retry != null ) {
            throw new IllegalArgumentException( "The -" + RETRY_OPTION + " option requires the -" + RESUME_OPTION + " option to be provided." );
        }
        if ( commandLine.hasOption( SKIP_DOWNLOAD_OPTION ) ) {
            log.info( "Download of single cell data will be skipped." );
            skipDownload = true;
        } else {
            skipDownload = false;
        }
        if ( commandLine.hasOption( FETCH_THREADS_OPTION ) ) {
            fetchThreads = commandLine.getParsedOptionValue( FETCH_THREADS_OPTION );
        }
        if ( commandLine.hasOption( SAMPLE_ACCESSIONS_OPTION ) ) {
            if ( !singleAccessionMode ) {
                throw new IllegalArgumentException( "The -sampleAccessions/--sample-accessions option requires that only one accession be supplied via -e/--acc." );
            }
            sampleAccessions = new HashSet<>( Arrays.asList( StringUtils.split( commandLine.getOptionValue( SAMPLE_ACCESSIONS_OPTION ), ',' ) ) );
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            if ( !singleAccessionMode ) {
                throw new IllegalArgumentException( "The -dataType/--data-type option requires that only one accession be supplied via -e/--acc." );
            }
            dataType = SingleCellDataType.valueOf( commandLine.getOptionValue( DATA_TYPE_OPTION ).toUpperCase() );
        }
        if ( commandLine.hasOption( MEX_BARCODES_FILE_SUFFIX ) || commandLine.hasOption( MEX_FEATURES_FILE_SUFFIX ) || commandLine.hasOption( MEX_MATRIX_FILE_SUFFIX ) ) {
            if ( dataType != SingleCellDataType.MEX ) {
                throw new IllegalArgumentException( "The -mexBarcodes, -mexFeatures and -mexMatrix options are only available if -dataType is set to MEX." );
            }
            barcodesFileSuffix = commandLine.getOptionValue( MEX_BARCODES_FILE_SUFFIX, "barcodes.tsv" );
            featuresFileSuffix = commandLine.getOptionValue( MEX_FEATURES_FILE_SUFFIX, "features.tsv" );
            matrixFileSuffix = commandLine.getOptionValue( MEX_MATRIX_FILE_SUFFIX, "matrix.mtx" );
        }
        if ( commandLine.hasOption( SUPPLEMENTARY_FILE_OPTION ) ) {
            if ( !singleAccessionMode ) {
                throw new IllegalArgumentException( "The -supplementaryFile option requires that only one accession be supplied via -e/--acc." );
            }
            if ( dataType == null ) {
                throw new IllegalArgumentException( "The -supplementaryFile option requires the -dataType option to be provided." );
            }
            supplementaryFile = commandLine.getOptionValue( SUPPLEMENTARY_FILE_OPTION );
        }
    }

    @Override
    protected void doWork() throws Exception {
        if ( retry != null ) {
            log.info( String.format( "Removing accessions marked as %s from %s since they will be reattempted...", String.join( ", ", retry ), summaryOutputFile ) );
            // rewrite the summary output file to remove the accessions that will be retried since those will be appended
            assert summaryOutputFile != null;
            List<String> linesToKeep = new ArrayList<>();
            linesToKeep.add( String.join( "\t", SUMMARY_HEADER ) );
            try ( Stream<String> lines = Files.lines( summaryOutputFile ) ) {
                lines.skip( 1 )
                        .filter( line -> !accessions.contains( line.split( "\t", 2 )[0] ) )
                        .forEach( linesToKeep::add );
            }
            Files.write( summaryOutputFile, linesToKeep );
        }

        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector();
                CSVPrinter writer = getSummaryOutputFilePrinter() ) {
            detector.setFTPClientFactory( ftpClientFactory );
            detector.setDownloadDirectory( singleCellDataBasePath );
            detector.setRetryPolicy( retryPolicy );
            SraFetcher sraFetcher = new SraFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), ncbiApiKey );
            detector.setSraFetcher( sraFetcher );
            if ( barcodesFileSuffix != null && featuresFileSuffix != null && matrixFileSuffix != null ) {
                detector.setMexFileSuffixes( barcodesFileSuffix, featuresFileSuffix, matrixFileSuffix );
            }
            if ( fetchThreads != null ) {
                // ensure that each thread can utilize a FTP connection
                if ( ftpClientFactory instanceof FTPClientFactoryImpl ) {
                    ( ( FTPClientFactoryImpl ) ftpClientFactory ).setMaxTotalConnections( fetchThreads.intValue() );
                }
                detector.setNumberOfFetchThreads( fetchThreads.intValue() );
            }
            log.info( "Downloading single cell data to " + singleCellDataBasePath + "..." );
            for ( String geoAccession : accessions ) {
                getBatchTaskExecutor().submit( () -> {
                    String detectedDataType = UNKNOWN_INDICATOR;
                    Integer numberOfSamples = null, numberOfCells = null, numberOfGenes = null;
                    List<String> additionalSupplementaryFiles = new ArrayList<>();
                    String dataInSra = null;
                    String comment = "";
                    try {
                        log.info( geoAccession + ": Parsing GEO series metadata..." );
                        GeoSeries series = readSeriesFromGeo( geoAccession );
                        if ( series == null ) {
                            addErrorObject( geoAccession, "The SOFT file does not contain an entry for the series." );
                            comment = "The SOFT file does not contain an entry for the series.";
                            return;
                        }
                        if ( sampleAccessions != null ) {
                            log.info( "Only retaining the following samples from " + geoAccession + ": " + String.join( ", ", sampleAccessions ) );
                            Set<GeoSample> samplesToKeep = series.getSamples().stream()
                                    .filter( s -> sampleAccessions.contains( s.getGeoAccession() ) )
                                    .collect( Collectors.toSet() );
                            if ( samplesToKeep.size() != sampleAccessions.size() ) {
                                Set<String> availableSamples = series.getSamples().stream().map( GeoSample::getGeoAccession )
                                        .filter( Objects::nonNull ).collect( Collectors.toCollection( LinkedHashSet::new ) );
                                String missingSamples = sampleAccessions.stream()
                                        .filter( sa -> !availableSamples.contains( sa ) )
                                        .collect( Collectors.joining( ", " ) );
                                throw new IllegalArgumentException( String.format( "Not all desired samples were found in %s, the following were missing: %s. The following are available: %s.",
                                        geoAccession, missingSamples, String.join( ", ", availableSamples ) ) );
                            }
                            series.keepSamples( samplesToKeep );
                        }
                        if ( detector.hasSingleCellData( series ) ) {
                            if ( dataType != null && supplementaryFile != null ) {
                                detectedDataType = dataType.name();
                            } else {
                                detectedDataType = detector.getSingleCellDataType( series ).name();
                            }
                            additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series ) );
                            for ( GeoSample sample : series.getSamples() ) {
                                additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series, sample ) );
                            }
                            if ( skipDownload ) {
                                // emulate the behavior of the MEX downloader, which is to raise an unsupported
                                // exception if MEX data is found at the series-level
                                if ( detectedDataType.equalsIgnoreCase( "MEX" ) ) {
                                    if ( detector.hasSingleCellDataInSeries( series, SingleCellDataType.MEX ) ) {
                                        throw new UnsupportedOperationException( "MEX files were found, but single-cell data is not supported at the series level." );
                                    }
                                }
                                addSuccessObject( geoAccession, "Download was skipped." );
                            } else {
                                if ( dataType != null && supplementaryFile != null ) {
                                    detector.downloadSingleCellData( series, dataType,
                                            matchSupplementaryFile( series.getSupplementaryFiles(), supplementaryFile ) );
                                } else if ( dataType != null ) {
                                    detector.downloadSingleCellData( series, dataType );
                                } else {
                                    detector.downloadSingleCellData( series );
                                }
                                // create a dummy platform, we just need to retrieve basic metadata from the loader
                                ArrayDesign platform = new ArrayDesign();
                                List<BioAssay> bas = series.getSamples().stream()
                                        .map( GeoSample::getGeoAccession )
                                        .map( s -> BioAssay.Factory.newInstance( s, platform, BioMaterial.Factory.newInstance( s ) ) )
                                        .collect( Collectors.toList() );
                                try ( SingleCellDataLoader loader = detector.getSingleCellDataLoader( series, SingleCellDataLoaderConfig.builder().ignoreSamplesLackingData( true ).build() ) ) {
                                    numberOfSamples = loader.getSampleNames().size();
                                    SingleCellDimension scd = loader.getSingleCellDimension( bas );
                                    numberOfCells = scd.getNumberOfCells();
                                    numberOfGenes = loader.getGenes().size();
                                    addSuccessObject( geoAccession );
                                }
                            }
                        } else {
                            detectedDataType = UNSUPPORTED_INDICATOR;
                            // consider all supplementary materials as additional
                            additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series ) );
                            for ( GeoSample sample : series.getSamples() ) {
                                additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series, sample ) );
                            }
                        }
                        Collection<String> sraAccessions = new ArrayList<>();
                        Collection<String> otherDataInSra = new ArrayList<>();
                        if ( detector.hasSingleCellDataInSra( series, sraAccessions, otherDataInSra ) ) {
                            dataInSra = String.join( "|", sraAccessions );
                        } else if ( !otherDataInSra.isEmpty() ) {
                            dataInSra = String.join( "|", otherDataInSra );
                            comment = "Data found in SRA might not be single-cell data.";
                        } else {
                            log.warn( "No data found in SRA for " + geoAccession + "." );
                        }
                    } catch ( Exception e ) {
                        addErrorObject( geoAccession, e );
                        comment = StringUtils.strip( ExceptionUtils.getRootCauseMessage( e ) );
                        if ( !detectedDataType.equals( UNKNOWN_INDICATOR ) ) {
                            comment += " (detected data type: " + detectedDataType + ")";
                        }
                        if ( e instanceof UnsupportedOperationException ) {
                            // this might be caused by downloadSingleCellData() or getSingleCellDataLoader()
                            detectedDataType = UNSUPPORTED_INDICATOR;
                        } else {
                            detectedDataType = FAILED_INDICATOR;
                        }
                    } finally {
                        if ( writer != null ) {
                            try {
                                writer.printRecord(
                                        geoAccession, detectedDataType, numberOfSamples, numberOfCells, numberOfGenes,
                                        additionalSupplementaryFiles.stream().map( this::formatFilename ).collect( Collectors.joining( ";" ) ),
                                        dataInSra, comment );
                                writer.flush(); // for convenience, so that results appear immediately with tail -f
                            } catch ( IOException e ) {
                                log.error( "Failed to append to the summary output file.", e );
                            }
                        }
                    }
                } );
            }
            awaitBatchExecutorService();
        }
    }

    /**
     * Pick a supplementary file from a user-supplied string.
     */
    private String matchSupplementaryFile( Collection<String> supplementaryFiles, String supplementaryFile ) {
        // 1. check for a complete match
        for ( String f : supplementaryFiles ) {
            if ( f.equals( supplementaryFile ) ) {
                return f;
            }
        }

        // 2. check for the last component
        for ( String f : supplementaryFiles ) {
            if ( FilenameUtils.getName( f ).equals( supplementaryFile ) ) {
                return f;
            }
        }

        throw new IllegalStateException( "No supplementary file matching " + supplementaryFile + " found in: " + StringUtils.join( ", ", supplementaryFiles ) + "." );
    }

    /**
     * Format a filename for the summary output file.
     * <p>
     * Exclamation marks are used to refer to files within archives (i.e. {@code GSM000012_bundle.tar!/cellids.csv}).
     */
    private String formatFilename( String fullPath ) {
        int afterExclamationMark = fullPath.indexOf( "!" );
        if ( afterExclamationMark > 0 ) {
            return FilenameUtils.getName( fullPath.substring( 0, afterExclamationMark ) ) + fullPath.substring( afterExclamationMark );
        } else {
            return FilenameUtils.getName( fullPath );
        }
    }

    @Nullable
    private CSVPrinter getSummaryOutputFilePrinter() throws IOException {
        if ( summaryOutputFile == null ) {
            return null;
        }
        CSVFormat.Builder csvFormatBuilder = CSVFormat.TDF.builder();
        if ( resume ) {
            return csvFormatBuilder.get()
                    .print( Files.newBufferedWriter( summaryOutputFile, StandardOpenOption.APPEND ) );
        } else {
            return csvFormatBuilder.setHeader( SUMMARY_HEADER )
                    .get()
                    .print( Files.newBufferedWriter( summaryOutputFile ) );
        }
    }

    @Nullable
    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        GeoFetcher geoFetcher = new GeoFetcher( retryPolicy, geoSeriesDownloadPath );
        geoFetcher.setFtpClientFactory( ftpClientFactory );
        geoFetcher.setFileLockManager( fileLockManager );
        Path dest = geoFetcher.fetchSeriesFamilySoftFile( accession );
        try ( InputStream is = FileUtils.openCompressedFile( dest ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession );
        }
    }
}