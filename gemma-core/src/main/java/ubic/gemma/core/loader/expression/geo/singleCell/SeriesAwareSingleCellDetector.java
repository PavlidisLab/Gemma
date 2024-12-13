package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface implemented by single-cell detector that can contextualize a sample within a series.
 * @author poirigui
 */
public interface SeriesAwareSingleCellDetector extends SingleCellDetector {

    boolean hasSingleCellData( GeoSeries series, GeoSample sample );

    Path downloadSingleCellData( GeoSeries series, GeoSample sample ) throws UnsupportedOperationException, NoSingleCellDataFoundException, IOException;

    List<String> getAdditionalSupplementaryFiles( GeoSeries series, GeoSample sample );
}
