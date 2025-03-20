package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for single-cell data detectors from GEO.
 * @author poirigui
 */
public interface SingleCellDetector {

    /**
     * Set the download directory single cell data.
     */
    void setDownloadDirectory( Path dir );

    /**
     * Indicate if the given GEO series has single cell data.
     */
    boolean hasSingleCellData( GeoSeries series );

    /**
     * Indicate if the given GEO sample has single cell data.
     */
    boolean hasSingleCellData( GeoSample sample );

    /**
     * Download single-cell data for the given GEO series.
     * @return a directory or file containing the downloaded series data
     * @throws UnsupportedOperationException  if downloading single cell for a given series is not supported
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given series
     */
    Path downloadSingleCellData( GeoSeries series ) throws UnsupportedOperationException, NoSingleCellDataFoundException, IOException;

    /**
     * Download single-cell data for the given GEO sample.
     * @return a directory or file containing the downloaded sample data
     * @throws UnsupportedOperationException  if downloading single cell for a given sample is not supported
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given sample
     */
    Path downloadSingleCellData( GeoSample sample ) throws UnsupportedOperationException, NoSingleCellDataFoundException, IOException;

    /**
     * Obtain a list of all additional supplementary files.
     */
    List<String> getAdditionalSupplementaryFiles( GeoSeries series );

    /**
     * Obtain a list of all additional supplementary files.
     */
    List<String> getAdditionalSupplementaryFiles( GeoSample sample );

    /**
     * Obtain a single cell data loader for the given GEO series based on previously downloading data.
     * @throws UnsupportedOperationException if loading single cell data is not supported
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given series
     */
    SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataLoaderConfig config ) throws UnsupportedOperationException, NoSingleCellDataFoundException;
}
