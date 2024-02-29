package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;

import java.io.IOException;
import java.nio.file.Path;

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
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given series
     */
    void downloadSingleCellData( GeoSeries series ) throws NoSingleCellDataFoundException, IOException;

    /**
     * Download single-cell data for the given GEO sample.
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given sample
     */
    void downloadSingleCellData( GeoSample sample ) throws NoSingleCellDataFoundException, IOException;

    /**
     * Obtain a single cell data loader for the given GEO series based on previously downloading data.
     * @throws NoSingleCellDataFoundException if there is no single cell data for the given series
     */
    SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException;
}
