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
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.SimpleThreadFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * This is the main single-cell data detector that delegates to other more specific detectors.
 * <p>
 * Samples can be loaded in parallel when retrieving a GEO series with {@link #downloadSingleCellData(GeoSeries)}. The
 * number of threads used is controlled by {@link #setNumberOfFetchThreads(int)} and defaults to 4.
 * @author poirigui
 */
@CommonsLog
public class GeoSingleCellDetector implements SingleCellDetector, AutoCloseable {

    /**
     * Default number of threads to use for fetching data.
     */
    public static final int DEFAULT_NUMBER_OF_FETCH_THREADS = 4;

    /**
     * Keywords to look for to determine if a sample is single-cell.
     */
    private static final String[] SINGLE_CELL_KEYWORDS = { "single-cell", "single cell", "scRNA" };

    private final AnnDataDetector annDataDetector = new AnnDataDetector();
    private final SeuratDiskDetector seuratDiskDetector = new SeuratDiskDetector();
    private final MexDetector mexDetector = new MexDetector();
    private final SingleCellDetector[] detectors = new SingleCellDetector[] { annDataDetector, seuratDiskDetector, mexDetector };

    @Nullable
    private ExecutorService executor;

    private int numberOfFetchThreads = DEFAULT_NUMBER_OF_FETCH_THREADS;

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
     * Set the maximum number of retries to attempt when retrieving supplementary materials.
     */
    public void setMaxRetries( int maxRetries ) {
        annDataDetector.setMaxRetries( maxRetries );
        seuratDiskDetector.setMaxRetries( maxRetries );
        mexDetector.setMaxRetries( maxRetries );
    }

    /**
     * Set the {@link org.apache.commons.net.ftp.FTPClient} factory used to create FTP connection to retrieve
     * supplementary materials.
     */
    public void setFTPClientFactory( FTPClientFactory factory ) {
        annDataDetector.setFTPClientFactory( factory );
        seuratDiskDetector.setFTPClientFactory( factory );
        mexDetector.setFTPClientFactory( factory );
    }

    /**
     * Directory where single-cell data is downloaded.
     * <p>
     * Data are organized by GEO series or GEO series accessions.
     * <p>
     * For AnnData and Seurat Disk:
     * <ul>
     * <li>{downloadDir}/{geoSeriesAccession}.h5ad</li>
     * <li>{downloadDir}/{geoSeriesAccession}.h5Seurat</li>
     * </ul>
     * MEX data is organized in a subdirectory named after the GEO series accession:
     * <ul>
     * <li>{downloadDir}/{geoSampleAccession}/barcodes.tsv.gz</li>
     * <li>{downloadDir}/{geoSampleAccession}/features.tsv.gz</li>
     * <li>{downloadDir}/{geoSampleAccession}/matrix.mtx.gz</li>
     * </ul>
     */
    @Override
    public void setDownloadDirectory( Path dir ) {
        for ( SingleCellDetector detector : detectors ) {
            detector.setDownloadDirectory( dir );
        }
    }

    /**
     * Detects if a GEO series has single-cell data either at the series-level or in individual samples.
     */
    @Override
    public boolean hasSingleCellData( GeoSeries series ) {
        boolean hasSingleCellDataInSeries = annDataDetector.hasSingleCellData( series ) || seuratDiskDetector.hasSingleCellData( series );

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
                    if ( hasSingleCellData( sample ) ) {
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

    /**
     * Determine the type of single-cell data a GEO series contains.
     */
    public SingleCellDataType getSingleCellDataType( GeoSeries series ) throws NoSingleCellDataFoundException {
        if ( annDataDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.ANNDATA;
        } else if ( seuratDiskDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.SEURAT_DISK;
        } else if ( mexDetector.hasSingleCellData( series ) ) {
            return SingleCellDataType.MEX;
        } else {
            for ( GeoSample sample : series.getSamples() ) {
                if ( mexDetector.hasSingleCellData( sample ) ) {
                    return SingleCellDataType.MEX;
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
        } else {
            for ( GeoSample sample : series.getSamples() ) {
                if ( mexDetector.hasSingleCellData( sample ) ) {
                    result.add( SingleCellDataType.MEX );
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Download single-cell data from a GEO series to disk.
     * <p>
     * This has to be done prior to {@link #getSingleCellDataLoader(GeoSeries)}.
     * @throws NoSingleCellDataFoundException if no single-cell data is found either at the series level or in individual samples
     */
    @Override
    public void downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException, IOException {
        boolean hasSingleCellDataInSeries = annDataDetector.hasSingleCellData( series ) || seuratDiskDetector.hasSingleCellData( series );
        Assert.isTrue( series.getSamples().stream().anyMatch( s -> isSingleCell( s, hasSingleCellDataInSeries ) ),
                series.getGeoAccession() + " does not have any single-cell series." );
        for ( SingleCellDetector detector : detectors ) {
            try {
                detector.downloadSingleCellData( series );
                return;
            } catch ( NoSingleCellDataFoundException e ) {
                // ignored, we'll try the next detector
            }
        }

        // retry MEX at the series-level
        if ( mexDetector.hasSingleCellData( series ) ) {
            // this will produce a NoSingleCellDataFoundException
            mexDetector.downloadSingleCellData( series );
            return;
        }

        // data is stored at the sample-level
        downloadSamplesInParallel( series, SingleCellDataType.MEX );
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
     */
    public void downloadSingleCellData( GeoSeries series, GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        downloadSingleCellData( series, sample, SingleCellDataType.MEX );
    }

    public void downloadSingleCellData( GeoSeries series, GeoSample sample, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ), "Only MEX data can be retrieved at the sample-level." );
        Assert.isTrue( isSingleCell( sample, false ), sample.getGeoAccession() + " is not a single-cell." );
        download( () -> mexDetector.downloadSingleCellData( series, sample ) );
    }

    @Override
    public void downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException, IOException {
        downloadSingleCellData( sample, SingleCellDataType.MEX );
    }

    public void downloadSingleCellData( GeoSample sample, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ), "Only 10X data can be retrieved at the sample-level." );
        Assert.isTrue( isSingleCell( sample, false ), sample.getGeoAccession() + " is not a single-cell." );
        download( () -> mexDetector.downloadSingleCellData( sample ) );
    }

    private void downloadSamplesInParallel( GeoSeries series, SingleCellDataType dataType ) throws NoSingleCellDataFoundException, IOException {
        Assert.isTrue( dataType.equals( SingleCellDataType.MEX ), "Only MEX data can be downloaded at the sample-level." );
        ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>( getExecutor() );

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
    }

    @FunctionalInterface
    private interface DownloadFunction {
        void download() throws NoSingleCellDataFoundException, IOException;
    }

    private void download( DownloadFunction function ) throws NoSingleCellDataFoundException, IOException {
        resolveDownloadFuture( getExecutor().submit( () -> {
            try {
                function.download();
            } catch ( NoSingleCellDataFoundException | IOException ex ) {
                throw new RuntimeException( ex );
            }
        } ) );
    }

    private synchronized ExecutorService getExecutor() {
        if ( executor == null ) {
            log.info( "Created executor with " + numberOfFetchThreads + " threads" );
            executor = Executors.newFixedThreadPool( numberOfFetchThreads, new SimpleThreadFactory( "gemma-geo-single-cell-fetch-thread-" ) );
        }
        return executor;
    }

    private <T> T resolveDownloadFuture( Future<T> future ) throws NoSingleCellDataFoundException, IOException {
        try {
            return future.get();
        } catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( ex );
        } catch ( ExecutionException ex ) {
            Throwable t = ex.getCause();
            if ( t instanceof RuntimeException ) {
                // checked exceptions are wrapped in a RuntimeException
                if ( t.getCause() instanceof NoSingleCellDataFoundException ) {
                    throw ( NoSingleCellDataFoundException ) t.getCause();
                } else if ( t.getCause() instanceof IOException ) {
                    throw ( IOException ) t.getCause();
                } else {
                    throw ( RuntimeException ) t;
                }
            } else {
                throw new RuntimeException( t );
            }
        }
    }

    /**
     * Obtain a single-cell data loader.
     * <p>
     * Only local files previously retrieved with {@link #downloadSingleCellData(GeoSeries)} are inspected.
     * @throws NoSingleCellDataFoundException if no single-cell data was found on-disk
     */
    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        for ( SingleCellDetector detector : detectors ) {
            try {
                return detector.getSingleCellDataLoader( series );
            } catch ( NoSingleCellDataFoundException e ) {
                // ignored, we'll try the next detector
            }
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
     * Obtain a single-cell data loader for a specific data type.
     */
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataType dataType ) throws NoSingleCellDataFoundException {
        switch ( dataType ) {
            case ANNDATA:
                return annDataDetector.getSingleCellDataLoader( series );
            case SEURAT_DISK:
                return seuratDiskDetector.getSingleCellDataLoader( series );
            case MEX:
                return mexDetector.getSingleCellDataLoader( series );
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type " + dataType );
        }
    }

    /**
     * Check if a GEO sample is single-cell by looking up its metadata.
     * @param hasSingleCellDataInSeries indicate if the series has single-cell data, this is used as a last resort to
     *                                  determine if a given sample is single-cell
     */
    private boolean isSingleCell( GeoSample sample, boolean hasSingleCellDataInSeries ) {
        if ( Objects.equals( sample.getLibSource(), GeoLibrarySource.SINGLE_CELL_TRANSCRIPTOMIC )
                && Objects.equals( sample.getLibStrategy(), GeoLibraryStrategy.RNA_SEQ ) ) {
            return true;
        }
        // older datasets do not use the 'single cell transcriptomic' library source, rely on some heuristics
        if ( Objects.equals( sample.getLibSource(), GeoLibrarySource.TRANSCRIPTOMIC )
                && Objects.requireNonNull( sample.getLibStrategy() ).equals( GeoLibraryStrategy.RNA_SEQ ) ) {
            if ( StringUtils.containsAnyIgnoreCase( sample.getTitle(), SINGLE_CELL_KEYWORDS )
                    || StringUtils.containsAnyIgnoreCase( sample.getDescription(), SINGLE_CELL_KEYWORDS ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but keywords indicate it is single-cell." );
                return true;
            }
            if ( mexDetector.hasSingleCellData( sample, false ) ) {
                log.warn( sample.getGeoAccession() + ": does not use the 'single cell transcriptomics' library source, but has MEX data in its supplementary material." );
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
