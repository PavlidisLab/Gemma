package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;

/**
 * Detects Loom files in series and samples.
 * <p>
 * Downloading files in sample and loading is not supported yet.
 * @author poirigui
 */
@CommonsLog
public class LoomSingleCellDetector extends AbstractSingleFileInSeriesSingleCellDetector {

    public LoomSingleCellDetector() {
        super( "Loom", ".loom" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataLoaderConfig config ) {
        throw new UnsupportedOperationException( "Loading Loom is not supported." );
    }

    @Override
    public boolean hasSingleCellData( GeoSample sample ) {
        boolean found = false;
        for ( String file : sample.getSupplementaryFiles() ) {
            if ( accepts( file ) ) {
                log.info( String.format( "%s: Found Loom in supplementary materials:\n\t%s", sample.getGeoAccession(), file ) );
                found = true;
            }
        }
        return found;
    }
}
