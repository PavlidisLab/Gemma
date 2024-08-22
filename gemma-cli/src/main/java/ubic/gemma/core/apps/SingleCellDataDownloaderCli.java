package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.ProgressInputStream;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;

@Component
public class SingleCellDataDownloaderCli extends AbstractCLI {

    private static final String
            ACCESSIONS_FILE_OPTION = "f",
            ACCESSIONS_OPTION = "e",
            SUMMARY_OUTPUT_FILE_OPTION = "s",
            RESUME_OPTION = "r",
            RETRY_OPTION = "retry",
            FETCH_THREADS_OPTION = "fetchThreads";

    private static final String
            DATA_TYPE_OPTION = "dataType",
            SUPPLEMENTARY_FILE_OPTION = "supplementaryFile";

    private static final String[] SUMMARY_HEADER = new String[] { "geo_accession", "data_type", "number_of_samples", "number_of_cells", "number_of_genes", "additional_supplementary_files", "comment" };

    private static final String
            UNKNOWN_INDICATOR = "UNKNOWN",
            UNSUPPORTED_INDICATOR = "UNSUPPORTED",
            FAILED_INDICATOR = "FAILED";

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Value("${geo.local.datafile.basepath}")
    private File geoSeriesDownloadPath;

    @Value("${geo.local.singleCellData.basepath}")
    private File singleCellDataBasePath;

    private final Set<String> accessions = new HashSet<>();
    @Nullable
    private Path summaryOutputFile;
    private boolean resume;
    private String[] retry;
    @Nullable
    private Number fetchThreads;

    // single-accession options
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String supplementaryFile;

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
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
        // options are consistent with those of LoadExpressionDataCli
        options.addOption( Option.builder( ACCESSIONS_FILE_OPTION ).longOpt( "file" ).type( File.class ).hasArg().desc( "File containing accessions to download" ).build() );
        options.addOption( Option.builder( ACCESSIONS_OPTION ).longOpt( "acc" ).hasArg().desc( "Comma-delimited list of accessions to download" ).build() );
        options.addOption( Option.builder( SUMMARY_OUTPUT_FILE_OPTION ).longOpt( "summary-output-file" ).type( File.class ).hasArg().desc( "File to write the summary output to. This is used to keep track of progress and resume download with -r/--resume." ).build() );
        options.addOption( Option.builder( RESUME_OPTION ).longOpt( "resume" ).desc( "Resume download from a previous invocation of this command. Requires -s/--summary-output-file to be set and refer to an existing file." ).build() );
        options.addOption( Option.builder( RETRY_OPTION ).longOpt( "retry" ).hasArg().desc( "Retry problematic datasets. Possible values are: '" + UNSUPPORTED_INDICATOR + "', '" + UNKNOWN_INDICATOR + "' or '" + FAILED_INDICATOR + "', or any combination delimited by ','. Requires -r/--resume option to be set." ).build() );
        options.addOption( Option.builder( FETCH_THREADS_OPTION ).longOpt( "fetch-threads" ).hasArg().type( Number.class ).desc( "Number of threads to use for downloading files. Default is " + GeoSingleCellDetector.DEFAULT_NUMBER_OF_FETCH_THREADS + ". Use -threads/--threads for processing series in parallel." ).build() );
        options.addOption( Option.builder( DATA_TYPE_OPTION ).longOpt( "data-type" ).hasArg().desc( "Data type. Possible values are: " + Arrays.stream( SingleCellDataType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + ". Only works if a single accession is passed to -e/--acc." ).build() );
        options.addOption( Option.builder( SUPPLEMENTARY_FILE_OPTION ).longOpt( "supplementary-file" ).hasArgs().desc( "Supplementary file to download. Only works if a single accession is passed to -e/--acc and -dataType is specified." ).build() );
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
        if ( resume ) {
            if ( singleAccessionMode ) {
                throw new IllegalArgumentException( "The -" + RESUME_OPTION + " option cannot be used in single accession mode." );
            }
            if ( summaryOutputFile == null ) {
                throw new IllegalArgumentException( "The -" + RESUME_OPTION + " option requires the -" + SUMMARY_OUTPUT_FILE_OPTION + " option to be provided." );
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
                    throw new RuntimeException( String.format( "Some of the accessions from %s were not found as input, are you sure this is the right summary file?.", summaryOutputFile ) );
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
        if ( commandLine.hasOption( FETCH_THREADS_OPTION ) ) {
            fetchThreads = commandLine.getParsedOptionValue( FETCH_THREADS_OPTION );
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            if ( !singleAccessionMode ) {
                throw new IllegalArgumentException( "The -dataType option requires that only one accession be supplied via -e/--acc." );
            }
            dataType = SingleCellDataType.valueOf( commandLine.getOptionValue( DATA_TYPE_OPTION ).toUpperCase() );
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
            detector.setDownloadDirectory( singleCellDataBasePath.toPath() );
            if ( fetchThreads != null ) {
                // ensure that each thread can utilize a FTP connection
                ftpClientFactory.setMaxTotalConnections( fetchThreads.intValue() );
                detector.setNumberOfFetchThreads( fetchThreads.intValue() );
            }
            log.info( "Downloading single cell data to " + singleCellDataBasePath + "..." );
            for ( String geoAccession : accessions ) {
                getBatchTaskExecutor().submit( () -> {
                    String detectedDataType = UNKNOWN_INDICATOR;
                    int numberOfSamples = 0, numberOfCells = 0, numberOfGenes = 0;
                    List<String> additionalSupplementaryFiles = new ArrayList<>();
                    String comment = "";
                    try {
                        log.info( geoAccession + ": Parsing GEO series metadata..." );
                        GeoSeries series = readSeriesFromGeo( geoAccession );
                        if ( series == null ) {
                            addErrorObject( geoAccession, "The SOFT file does not contain an entry for the series." );
                            comment = "The SOFT file does not contain an entry for the series.";
                            return;
                        }
                        if ( detector.hasSingleCellData( series ) ) {
                            if ( dataType != null && supplementaryFile != null ) {
                                detectedDataType = dataType.name();
                                detector.downloadSingleCellData( series, dataType, supplementaryFile );
                            } else if ( dataType != null ) {
                                detectedDataType = dataType.name();
                                detector.downloadSingleCellData( series, dataType );
                            } else {
                                detectedDataType = detector.getSingleCellDataType( series ).name();
                                detector.downloadSingleCellData( series );
                            }
                            List<String> samples = series.getSamples().stream().map( GeoSample::getGeoAccession ).collect( Collectors.toList() );
                            ArrayDesign platform = new ArrayDesign();
                            List<BioAssay> bas = samples.stream().map( s -> BioAssay.Factory.newInstance( s, platform, BioMaterial.Factory.newInstance( s ) ) ).collect( Collectors.toList() );
                            SingleCellDataLoader loader = detector.getSingleCellDataLoader( series );
                            numberOfSamples = loader.getSampleNames().size();
                            SingleCellDimension scd = loader.getSingleCellDimension( bas );
                            numberOfCells = scd.getNumberOfCells();
                            numberOfGenes = loader.getGenes().size();
                            additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series ) );
                            for ( GeoSample sample : series.getSamples() ) {
                                additionalSupplementaryFiles.addAll( detector.getAdditionalSupplementaryFiles( series, sample ) );
                            }
                            addSuccessObject( geoAccession );
                        } else {
                            detectedDataType = UNSUPPORTED_INDICATOR;
                        }
                    } catch ( Exception e ) {
                        addErrorObject( geoAccession, e );
                        comment = StringUtils.trim( ExceptionUtils.getRootCauseMessage( e ) );
                        if ( !detectedDataType.equals( UNKNOWN_INDICATOR ) ) {
                            comment += " (detected data type: " + detectedDataType + ")";
                        }
                        detectedDataType = FAILED_INDICATOR;
                    } finally {
                        if ( writer != null ) {
                            try {
                                writer.printRecord(
                                        geoAccession, detectedDataType, numberOfSamples, numberOfCells, numberOfGenes,
                                        additionalSupplementaryFiles.stream().map( this::getFilename ).collect( Collectors.joining( ";" ) ),
                                        comment );
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
     * Exclamation marks are used to refer to files within TAR archives (i.e. GSM000012_bundle.tar!cellids.csv).
     */
    private String getFilename( String fullPath ) {
        int afterExclamationMark = fullPath.indexOf( "'!" );
        if ( afterExclamationMark > 0 ) {
            return FilenameUtils.getName( fullPath.substring( afterExclamationMark ) ) + "!" + fullPath.substring( afterExclamationMark );
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
        if ( !resume ) {
            csvFormatBuilder.setHeader( SUMMARY_HEADER );
        }
        return csvFormatBuilder.build().print( Files.newBufferedWriter( summaryOutputFile, resume ? StandardOpenOption.APPEND : StandardOpenOption.CREATE ) );
    }

    @Nullable
    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        String remoteFile = String.format( "geo/series/%snnn/%s/soft/%s_family.soft.gz",
                accession.substring( 0, accession.length() - 3 ), accession, accession );
        URL softFileUrl = new URL( "ftp://ftp.ncbi.nlm.nih.gov/" + remoteFile );
        Path dest = geoSeriesDownloadPath.toPath().resolve( accession ).resolve( accession + ".soft.gz" );
        boolean download = true;
        if ( Files.exists( dest ) ) {
            FTPClient client = ftpClientFactory.getFtpClient( softFileUrl );
            try {
                FTPFile res = client.mlistFile( remoteFile );
                long expectedLength = res != null ? res.getSize() : -1;
                if ( expectedLength != -1 && dest.toFile().length() == expectedLength ) {
                    log.info( accession + ": Using existing SOFT file " + dest + "." );
                    download = false;
                }
                ftpClientFactory.recycleClient( softFileUrl, client );
            } catch ( Exception e ) {
                ftpClientFactory.destroyClient( softFileUrl, client );
                throw e;
            }
        }
        if ( download ) {
            log.info( accession + ": Downloading SOFT file to " + dest + "..." );
            PathUtils.createParentDirectories( dest );
            StopWatch timer = StopWatch.createStarted();
            try ( InputStream in = new ProgressInputStream( ftpClientFactory.openStream( softFileUrl ), accession + ".soft.gz", SingleCellDataDownloaderCli.class.getName() ); OutputStream out = Files.newOutputStream( dest ) ) {
                int downloadedBytes = IOUtils.copy( in, out );
                if ( downloadedBytes > 0 ) {
                    log.info( String.format( "%s: Done downloading SOFT file (%s in %s @ %.3f MB/s).", accession,
                            FileUtils.byteCountToDisplaySize( downloadedBytes ), timer,
                            ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( downloadedBytes / timer.getTime() ) ) );
                }
            } catch ( IOException e ) {
                if ( Files.exists( dest ) ) {
                    log.warn( accession + ": An I/O error occurred while downloading the SOFT file, removing " + dest + "...", e );
                    PathUtils.deleteDirectory( dest.getParent() );
                }
                throw e;
            }
        }
        try ( InputStream is = new GZIPInputStream( Files.newInputStream( dest ) ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession );
        }
    }
}