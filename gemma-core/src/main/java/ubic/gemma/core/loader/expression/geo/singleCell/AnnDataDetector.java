package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects AnnData in GEO series.
 * <p>
 * This detector has additional heuristics for detecting sample names.
 * @author poirigui
 */
@CommonsLog
public class AnnDataDetector extends AbstractSingleH5FileInSeriesSingleCellDetector implements SingleCellDetector {

    public AnnDataDetector() {
        super( "AnnData", ".h5ad" );
    }

    @Override
    protected boolean accepts( String supplementaryFile ) {
        return super.accepts( supplementaryFile ) || supplementaryFile.endsWith( ".h5ad.h5" ) || supplementaryFile.endsWith( ".h5ad.h5.gz" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Path annDataFile = getDest( series );
        if ( Files.exists( annDataFile ) ) {
            return new GeoAnnDataSingleCellDataLoaderConfigurer( annDataFile, series )
                    .configureLoader( SingleCellDataLoaderConfig.builder().build() );
        }
        throw new NoSingleCellDataFoundException( "Could not find " + annDataFile + " for " + series.getGeoAccession() );
    }
}
