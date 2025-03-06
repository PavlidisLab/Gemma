package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.GeoLibrarySource;
import ubic.gemma.core.loader.expression.geo.model.GeoLibraryStrategy;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.util.SimpleThreadFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.loader.expression.geo.singleCell.MexDetector.*;

/**
 * This is the main single-cell data detector that delegates to other more specific detectors.
 * <p>
 * Samples can be loaded in parallel when retrieving a GEO series with {@link #downloadSingleCellData(GeoSeries)}. The
 * number of threads used is controlled by {@link #setNumberOfFetchThreads(int)} and defaults to 4.
 * @author poirigui
 */
@CommonsLog
public class GeoSingleCellDetector implements SingleCellDetector, ArchiveBasedSingleCellDetector, SeriesAwareSingleCellDetector, AutoCloseable {


    /**
     * Default number of threads to use for fetching data.
     */
    public static final int DEFAULT_NUMBER_OF_FETCH_THREADS = 4;

    /**
     * Keywords to look for to determine if a sample is single-cell.
     */
    private static final String[] SINGLE_CELL_KEYWORDS = { "single-cell", "single cell", "scRNA" };

    private static final String[] SINGLE_CELL_DATA_PROCESSING_KEYWORDS = { "cellranger" };

    private static final BioAssayMapper GEO_BIO_ASSAY_TO_SAMPLE_NAME_MATCHER = new GeoBioAssayMapper();

    private final AnnDataDetector annDataDetector = new AnnDataDetector();
    private final SeuratDiskDetector seuratDiskDetector = new SeuratDiskDetector();
    private final LoomSingleCellDetector loomDetector = new LoomSingleCellDetector();
    private final MexDetector mexDetector = new MexDetector();
    private final SingleCellDetector[] detectors = new SingleCellDetector[] { annDataDetector, seuratDiskDetector, mexDetector, loomDetector };

    @Nullable
    private ExecutorService executor;

    private int numberOfFetchThreads = DEFAULT_NUMBER_OF_FETCH_THREADS;
    private Path downloadDirectory;

    @Override
    public void close() {
        if ( executor != null ) {
            executor.shutdown();
        }
    }

    /**
     * Number of threads to use for downloading single-cell data.
     */
    public void setNumberOfFetchThreads( int numberOfFetchThreads ) {
        Assert.isNull( executor, "The fetch thread pool is already initialized, it's too late to change the number of threads." );
        this.numberOfFetchThreads = numberOfFetchThreads;
    }

    /**
     * Set the {@link org.apache.commons.net.ftp.FTPClient} factory used to create FTP connection to retrieve
     * supplementary materials.
     */
    public void setFTPClientFactory( FTPClientFactory factory ) {
        for ( SingleCellDetector detector : detectors ) {
            if ( detector instanceof AbstractSingleCellDetector ) {
                ( ( AbstractSingleCellDetector ) detector ).setFTPClientFactory( factory );
            }
        }
    }

    /**
     * Directory where single-cell data is downloaded.
     * <p>
     * Data are organized by GEO series or GEO series accessions.
     * <p>
     * For AnnData, Seurat Disk and Loom:
     * <ul>
     * <li>{downloadDir}/{geoSeriesAccession}.h5ad</li>
     * <li>{downloadDir}/{geoSeriesAccession}.h5Seurat</li>
     * <li>{downloadDir}/{geoSeriesAccession}.loom</li>
     * </ul>
     * MEX data is organized in a subdirectory named after the GEO series accession:
     * <ul>
     * <li>{downloadDir}/{geoSampleAccession}/barcodes.tsv.gz</li>
     * <li>{downloadDir}/{geoSampleAccession}/features.tsv.gz</li>
     * <li>{downloadDir}/{geoSampleAccession}/matrix.mtx.gz</li>
     * </ul>
     * Downloading Loom at the sample-level is not supported yet.
     */
    @Override
    public void setDownloadDirectory( Path dir ) {
        this.downloadDirectory = dir;
        for ( SingleCellDetector detector : detectors ) {
            detector.setDownloadDirectory( dir );
        }
    }

    /**
     * Set the suffixes to use to detect MEX metadata.
     */
    public void setMexFileSuffixes( String barcodes, String features, String matrix ) {
        mexDetector.setBarcodesFileSuffix( barcodes );
        if ( barcodes.equals( DEFAULT_BARCODES_FILE_SUFFIX ) ) {
            mexDetector.setBarcodeMetadataFileSuffix( DEFAULT_BARCODE_METADATA_FILE_SUFFIX );
        } else {
            log.warn( "Disabling detection of barcode_metadata.tsv since custom suffixes are used for detecting MEX barcodes files." );
            mexDetector.setBarcodeMetadataFileSuffix( null );
        }
        mexDetector.setFeaturesFileSuffix( features );
        if ( features.equals( DEFAULT_FEATURES_FILE_SUFFIX ) ) {
            mexDetector.setGenesFileSuffix( DEFAULT_GENES_FILE_SUFFIX );
        } else {
            log.warn( "Disabling detection of old-style genes.tsv since nce custom suffixes are used for detecting MEX features files." );
            mexDetector.setGenesFileSuffix( null );
        }
        mexDetector.setMatrixFileSuffix( matrix );
    }

    public void resetMexFileSuffixes() {
        mexDetector.setBarcodesFileSuffix( DEFAULT_BARCODES_FILE_SUFFIX );
        mexDetector.setBarcodeMetadataFileSuffix( DEFAULT_BARCODE_METADATA_FILE_SUFFIX );
        mexDetector.setFeaturesFileSuffix( DEFAULT_FEATURES_FILE_SUFFIX );
        mexDetector.setGenesFileSuffix( DEFAULT_GENES_FILE_SUFFIX );
        mexDetector.setMatrixFileSuffix( DEFAULT_MATRIX_FILE_SUFFIX );
    }

    @Override
    public void setMaxEntrySizeInArchiveToSkip( long maxNumberOfEntriesToSkip ) {
        for ( SingleCellDetector detector : detectors ) {
            if ( detector instanceof ArchiveBasedSingleCellDetector ) {
                ( ( ArchiveBasedSingleCellDetector ) detector ).setMaxEntrySizeInArchiveToSkip( maxNumberOfEntriesToSkip );
            }
        }
    }

    @Override
    public void setMaxNumberOfEntriesToSkip( long maxNumberOfEntriesToSkip ) {
        for ( SingleCellDetector detector : detectors ) {
            if ( detector instanceof ArchiveBasedSingleCellDetector ) {
                ( ( ArchiveBasedSingleCellDetector ) detector ).setMaxNumberOfEntriesToSkip( maxNumberOfEntriesToSkip );
            }
        }
    }

    /**
     * Detects if a GEO series has single-cell data either at the series-level or in individual samples.
     */
    @Override
    public boolean hasSingleCellData( GeoSeries series ) {
        boolean hasSingleCellDataInSeries = hasSingleCellDataInSeries( series );

        // don't bother checking if none of the sample is single-cell
        if ( series.getSamples().stream().noneMatch( s -> isSingleCell( s, hasSingleCellDataInSeries ) ) ) {
            Map<String, Long> samplesBreakdown = series.getSamples().stream()
                    .collect( Collectors.groupingBy( s -> s.getLibSource() + " " + s.getLibStrategy(), Collectors.counting() ) );
            log.warn( series.getGeoAccession() + ": None of the samples are single-cell transcriptomics: " + samplesBreakdown );
            return false;
        }

        // check for single-cell data at the series-level
        for ( SingleCellDetector detector : detectors ) {
            if ( detector.hasSingleCellData( series ) ) {
                return true;
            }
        }

        // check for single-cell data at sample-level
        Set<String> singleCellSamplesWithoutData = new HashSet<>();
        try {
            for ( GeoSample sample : series.getSamples() ) {
                if ( isSingleCell( sample, hasSingleCellDataInSeries ) ) {
                    if ( mexDetector.hasSingleCellData( series, sample ) || loomDetector.hasSingleCellData( sample ) ) {
                        return true;
                    } else {
                        singleCellSamplesWithoutData.add( sample.getGeoAccession() );
                    }
                }
            }
        } finally {
            if ( !singleCellSamplesWithoutData.isEmpty() ) {
                log.warn( String.format( "%s: The following samples are single-cell but do not have any supported single-cell data: %s",
                        series.getGeoAccession(), singleCellSamplesWithoutData.stream().sorted().collect( Collectors.joining( ", " ) ) ) );
            }
        }

        return false;
    }

    @Override
    public boolean hasSingleCellData( GeoSample sample ) {
        if ( !isSingleCell( sample, false ) )
            return false; // don't bother checking
        for ( SingleCellDetector detector : detectors ) {
            if ( detector.hasSingleCellData( sample ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSingleCellData( GeoSeries series, GeoSample sample ) {
        boolean hasSingleCellDataInSeries = hasSingleCellDataInSeries( series );
        if ( !isSingleCell( sample, hasSingleCellDataInSeries ) )
            return false; // don't bother checking
        for ( SingleCellDetector detector : detectors ) {
            if ( detector instanceof SeriesAwareSingleCellDetector ) {
                if ( ( ( SeriesAwareSingleCellDetector ) detector ).hasSingleCellData( series, sample ) ) {
                    return true;
                }
            } else {
                if ( detector.hasSingleCellData( sample ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine the type of single-cell data a GEO series contains.
     */
    public SingleCellDataType getSingleCellDataType( GeoSeries series ) throws NoSingleCellDataFoundException {
        if ( annDataDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.ANNDATA;
        } else if ( seuratDiskDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.SEURAT_DISK;
        } else if ( loomDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.LOOM;
        } else if ( mexDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.MEX;
        } else {
            for ( GeoSample sample : series.getSamples() ) {
                // at this point, we already rejected the presence of single-cell data in the series
                if ( isSingleCell( sample, false ) ) {
                    if ( mexDetector.hasSingleCellData( series, sample ) ) {
                        return SingleCellDataType.MEX;
                    } else if ( loomDetector.hasSingleCellData( sample ) ) {
                        return SingleCellDataType.LOOM;
                    }
                }
            }
        }
        throw new NoSingleCellDataFoundException( "No single-cell data was found for " + series.getGeoAccession() + "." );
    }

    /**
     * Obtain all single-cell data types a GEO series contains.
     */
    public Set<SingleCellDataType> getAllSingleCellDataTypes( GeoSeries series ) {
        Set<SingleCellDataType> result = new HashSet<>();
        if ( annDataDetector.hasSingleCellData( series ) ) {
            result.add( SingleCellDataType.ANNDATA );
        }
        if ( seuratDiskDetector.hasSingleCellData( series ) ) {
            result.add( SingleCellDataType.SEURAT_DISK );
        }
        if ( loomDetector.hasSingleCellData( series ) ) {
            result.add( SingleCellDataType.LOOM );
        }
        if ( mexDetector.hasSingleCellData( series ) ) {
            result.add( SingleCellDataType.MEX );
        }
        boolean hasSingleCellDataInSeries = !result.isEmpty();
        for ( GeoSample sample : series.getSamples() ) {
            if ( isSingleCell( sample, hasSingleCellDataInSeries ) ) {
                if ( loomDetector.hasSingleCellData( sample ) ) {
                    result.add( SingleCellDataType.LOOM );
                }
                if ( mexDetector.hasSingleCellData( sample ) ) {
                    result.add( SingleCellDataType.MEX );
                }
                break;
            }
        }
        return result;
    }

    /**
     * Download single-cell data from a GEO series to disk.
     * <p>
     * This has to be done prior to {@link #getSingleCellDataLoader(GeoSeries)}.
     * @throws NoSingleCellDataFoundException if no single-cell data is found either at the series level or in individual samples
     * @throws UnsupportedOperationException  if single-cell data is found at the series level
     */
    @Override
    public Path downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( series.getSamples().stream().anyMatch( s -> isSingleCell( s, hasSingleCellDataInSeries( series ) ) ),
                series.getGeoAccession() + " does not have any single-cell sample." );
        for ( SingleCellDetector detector : detectors ) {
            try {
                return detector.downloadSingleCellData( series );
            } catch ( NoSingleCellDataFoundException e ) {
                // ignored, we'll try the next detector
            }
        }

        // data is stored at the sample-level
        // TODO: include Loom files stored at sample-level
        return downloadSamplesInParallel( series, SingleCellDataType.MEX );
    }

    // this exception cannot be raised since we're downloading a specific file
    @SneakyThrows(NoSingleCellDataFoundException.class)
    public void downloadSingleCellData( GeoSeries series, SingleCellDataType dataType, String supplementaryFile ) throws IOException {
        switch ( dataType ) {
            case ANNDATA:
                download( () -> annDataDetector.downloadSingleCellData( series, supplementaryFile ) );
                break;
            case SEURAT_DISK:
                download( () -> seuratDiskDetector.downloadSingleCellData( series, supplementaryFile ) );
                break;
            case LOOM:
                download( () -> loomDetector.downloadSingleCellData( series, supplementaryFile ) );
                break;
            case MEX:
                throw new UnsupportedOperationException( "Downloading a specific supplementary file for " + dataType + " is not supported." );
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type " + dataType );
        }
    }

    public void downloadSingleCellData( GeoSeries series, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        switch ( dataType ) {
            case ANNDATA:
                download( () -> annDataDetector.downloadSingleCellData( series ) );
                break;
            case SEURAT_DISK:
                download( () -> seuratDiskDetector.downloadSingleCellData( series ) );
                break;
            case LOOM:
                download( () -> loomDetector.downloadSingleCellData( series ) );
                break;
            case MEX:
                if ( mexDetector.hasSingleCellData( series ) ) {
                    mexDetector.downloadSingleCellData( series );
                } else {
                    downloadSamplesInParallel( series, dataType );
                }
                break;
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type " + dataType );
        }
    }

    /**
     * Download a sample in the context of a series.
     * <p>
     * This is only applicable to MEX and Loom.
     */
    @Override
    public Path downloadSingleCellData( GeoSeries series, GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        if ( mexDetector.hasSingleCellData( series, sample ) ) {
            return downloadSingleCellData( series, sample, SingleCellDataType.MEX );
        } else if ( loomDetector.hasSingleCellData( sample ) ) {
            return downloadSingleCellData( series, sample, SingleCellDataType.LOOM );
        } else {
            throw new NoSingleCellDataFoundException( series.getGeoAccession() + ": No single cell data found for " + sample.getGeoAccession() + " at the sample-level." );
        }
    }

    public Path downloadSingleCellData( GeoSeries series, GeoSample sample, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ) || dataType.equals( SingleCellDataType.LOOM ),
                "Only MEX and Loom data can be retrieved at the sample-level." );
        Assert.isTrue( isSingleCell( sample, hasSingleCellDataInSeries( series ) ), sample.getGeoAccession() + " is not a single-cell sample." );
        if ( dataType == SingleCellDataType.MEX ) {
            return download( () -> mexDetector.downloadSingleCellData( series, sample ) );
        } else {
            return downloadSingleCellData( sample, dataType );
        }
    }

    @Override
    public Path downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        if ( mexDetector.hasSingleCellData( sample ) ) {
            return downloadSingleCellData( sample, SingleCellDataType.MEX );
        } else if ( loomDetector.hasSingleCellData( sample ) ) {
            return downloadSingleCellData( sample, SingleCellDataType.LOOM );
        } else {
            throw new NoSingleCellDataFoundException( "No single cell data found for " + sample.getGeoAccession() + " at the sample-level." );
        }
    }

    public Path downloadSingleCellData( GeoSample sample, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ) || dataType.equals( SingleCellDataType.LOOM ),
                "Only MEX or Loom data can be retrieved at the sample-level." );
        Assert.isTrue( isSingleCell( sample, false ), sample.getGeoAccession() + " is not a single-cell sample." );
        if ( dataType == SingleCellDataType.MEX ) {
            return download( () -> mexDetector.downloadSingleCellData( sample ) );
        } else {
            return download( () -> loomDetector.downloadSingleCellData( sample ) );
        }
    }

    private Path downloadSamplesInParallel( GeoSeries series, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.notNull( series.getGeoAccession() );
        Assert.notNull( downloadDirectory, "A downlodad directory must be set." );
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ), "Only MEX data can be downloaded at the sample-level." );
        ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>( getExecutor() );

        // directory where everything is downloaded for the series
        Path dest = downloadDirectory.resolve( series.getGeoAccession() );

        // attempt to fetch at series-level
        List<Future<Boolean>> futures = new ArrayList<>( series.getSamples().size() );
        for ( GeoSample sample : series.getSamples() ) {
            // we are downloading at the sample-level, so the hint that the series contains single-cell data is irrelevant
            // only MEX is supported at the sample-level
            if ( isSingleCell( sample, false ) ) {
                futures.add( completionService.submit( () -> {
                    try {
                        mexDetector.downloadSingleCellData( series, sample );
                        return true;
                    } catch ( UnsupportedOperationException e ) {
                        // downloading at sample-level is not supported
                        log.warn( series.getGeoAccession() + ": Downloading sample data for " + sample.getGeoAccession() + " is not supported, it will be ignored.", e );
                        return false;
                    } catch ( NoSingleCellDataFoundException ex ) {
                        // This is only problematic if the dataset has single-cell data, then we essentially failed to
                        // retrieve it. We could check if beforehand, but that would be inefficient.
                        if ( mexDetector.hasSingleCellData( sample ) ) {
                            throw new RuntimeException( ex );
                        }
                        return false;
                    } catch ( IOException ex ) {
                        throw new RuntimeException( ex );
                    }
                } ) );
            }
        }

        if ( futures.isEmpty() ) {
            throw new NoSingleCellDataFoundException( "No data was downloaded for " + series.getGeoAccession() );
        }

        boolean anySampleDownloaded = false;
        log.info( String.format( "%s: Waiting for single-cell data download to complete for %d samples...",
                series.getGeoAccession(), futures.size() ) );
        try {
            for ( Future<?> ignore : futures ) {
                anySampleDownloaded |= resolveDownloadFuture( completionService.take() );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        } finally {
            // cancel any remaining download jobs
            for ( Future<?> future : futures ) {
                if ( !future.isDone() ) {
                    future.cancel( true );
                }
            }
        }

        if ( !anySampleDownloaded ) {
            throw new NoSingleCellDataFoundException( "No data was downloaded for " + series.getGeoAccession() );
        }

        return dest;
    }

    @FunctionalInterface
    private interface DownloadFunction {
        Path download() throws NoSingleCellDataFoundException, IOException;
    }

    private Path download( DownloadFunction function ) throws NoSingleCellDataFoundException, IOException {
        return resolveDownloadFuture( getExecutor().submit( function::download ) );
    }

    private <T> T resolveDownloadFuture( Future<T> future ) throws NoSingleCellDataFoundException, IOException {
        try {
            return future.get();
        } catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( ex );
        } catch ( ExecutionException ex ) {
            // checked exceptions are wrapped in a RuntimeException
            if ( ex.getCause() instanceof NoSingleCellDataFoundException ) {
                throw ( NoSingleCellDataFoundException ) ex.getCause();
            } else if ( ex.getCause() instanceof IOException ) {
                throw ( IOException ) ex.getCause();
            } else if ( ex.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) ex.getCause();
            } else {
                throw new RuntimeException( ex.getCause() );
            }
        }
    }

    private synchronized ExecutorService getExecutor() {
        if ( executor == null ) {
            log.info( "Created executor with " + numberOfFetchThreads + " threads" );
            executor = Executors.newFixedThreadPool( numberOfFetchThreads, new SimpleThreadFactory( "gemma-geo-single-cell-fetch-thread-" ) );
        }
        return executor;
    }

    /**
     * Obtain a single-cell data loader.
     * <p>
     * Only local files previously retrieved with {@link #downloadSingleCellData(GeoSeries)} are inspected.
     * @throws NoSingleCellDataFoundException if no single-cell data was found on-disk
     * @throws UnsupportedOperationException if single-cell data was found, but cannot be loaded
     */
    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataLoaderConfig config ) throws NoSingleCellDataFoundException {
        UnsupportedOperationException firstUnsupported = null;
        for ( SingleCellDetector detector : detectors ) {
            try {
                SingleCellDataLoader loader = detector.getSingleCellDataLoader( series, config );
                loader.setBioAssayToSampleNameMapper( GEO_BIO_ASSAY_TO_SAMPLE_NAME_MATCHER );
                return loader;
            } catch ( UnsupportedOperationException e ) {
                if ( firstUnsupported == null ) {
                    firstUnsupported = e;
                }
            } catch ( NoSingleCellDataFoundException e ) {
                // ignored, we'll try the next detector
            }
        }

        // if there's at least one unsupported error at this point, raise it, it will be more informative than a
        // NoSingleCellDataFoundException
        if ( firstUnsupported != null ) {
            throw firstUnsupported;
        }

        throw new NoSingleCellDataFoundException( "No single-cell data was found for " + series.getGeoAccession() + "." );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series ) {
        for ( SingleCellDetector detector : detectors ) {
            if ( detector.hasSingleCellData( series ) ) {
                return detector.getAdditionalSupplementaryFiles( series );
            }
        }
        // if no detector can detect single-cell data, consider it all additional
        return series.getSupplementaryFiles()
                .stream()
                // this is just an aggregate of sample-level supplementary files
                .filter( f -> !f.endsWith( "_RAW.tar" ) )
                .collect( Collectors.toList() );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSeries series, GeoSample sample ) {
        if ( !isSingleCell( sample, hasSingleCellDataInSeries( series ) ) ) {
            log.warn( sample.getGeoAccession() + " is not a single-cell sample, ignoring its supplementary materials." );
            return Collections.emptyList();
        }
        for ( SingleCellDetector detector : detectors ) {
            if ( detector instanceof SeriesAwareSingleCellDetector ) {
                if ( ( ( SeriesAwareSingleCellDetector ) detector ).hasSingleCellData( series, sample ) ) {
                    return ( ( SeriesAwareSingleCellDetector ) detector ).getAdditionalSupplementaryFiles( series, sample );
                }
            } else if ( detector.hasSingleCellData( sample ) ) {
                return detector.getAdditionalSupplementaryFiles( sample );
            }
        }
        // if no detector can detect single-cell data, consider it all additional
        return new ArrayList<>( sample.getSupplementaryFiles() );
    }

    @Override
    public List<String> getAdditionalSupplementaryFiles( GeoSample sample ) {
        if ( !isSingleCell( sample, false ) ) {
            log.warn( sample.getGeoAccession() + " is not a single-cell sample, ignoring its supplementary materials." );
            return Collections.emptyList();
        }
        for ( SingleCellDetector detector : detectors ) {
            if ( detector.hasSingleCellData( sample ) ) {
                return detector.getAdditionalSupplementaryFiles( sample );
            }
        }
        // if no detector can detect single-cell data, consider it all additional
        return new ArrayList<>( sample.getSupplementaryFiles() );
    }

    /**
     * Check if a GEO series has single-cell data at the series-level.
     */
    public boolean hasSingleCellDataInSeries( GeoSeries series ) {
        return Arrays.stream( detectors ).anyMatch( detector -> detector.hasSingleCellData( series ) );
    }

    /**
     * Check if a GEO sample is single-cell by looking up its metadata.
     * @param hasSingleCellDataInSeries indicate if the series has single-cell data, this is used as a last resort to
     *                                  determine if a given sample is single-cell, use {@link #hasSingleCellDataInSeries(GeoSeries)}
     *                                  to compute and reuse this value.
     */
    public boolean isSingleCell( GeoSample sample, boolean hasSingleCellDataInSeries ) {
        if ( Objects.equals( sample.getLibSource(), GeoLibrarySource.SINGLE_CELL_TRANSCRIPTOMIC )
                && Objects.equals( sample.getLibStrategy(), GeoLibraryStrategy.RNA_SEQ ) ) {
            return true;
        }
        // older datasets do not use the 'single cell transcriptomic' library source, rely on some heuristics
        if ( Objects.equals( sample.getLibSource(), GeoLibrarySource.TRANSCRIPTOMIC )
                && Objects.requireNonNull( sample.getLibStrategy() ).equals( GeoLibraryStrategy.RNA_SEQ ) ) {
            if ( StringUtils.containsAnyIgnoreCase( sample.getTitle(), SINGLE_CELL_KEYWORDS )
                    || StringUtils.containsAnyIgnoreCase( sample.getDescription(), SINGLE_CELL_KEYWORDS ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but keywords in its description indicate it is single-cell." );
                return true;
            }
            if ( StringUtils.containsAnyIgnoreCase( sample.getDataProcessing(), SINGLE_CELL_DATA_PROCESSING_KEYWORDS ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but keywords in its data processing section indicate it is single-cell." );
                return true;
            }
            // don't allow archive lookups because that would be too slow
            if ( mexDetector.hasSingleCellData( sample, false ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but has MEX data in its supplementary material." );
                return true;
            }
            if ( loomDetector.hasSingleCellData( sample ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but has Loom data in its supplementary material." );
                return true;
            }
            if ( hasSingleCellDataInSeries ) {
                // FIXME: there's no guarantee that this particular sample has data in the series supplementary material
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but has single-cell data in the supplementary files of its series." );
                return true;
            }
        }
        return false;
    }
}
