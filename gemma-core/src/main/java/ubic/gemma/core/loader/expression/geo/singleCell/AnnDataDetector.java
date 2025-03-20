package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.util.anndata.AnnDataException;

import java.io.IOException;
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
    protected boolean isTruncated( Path dest ) throws IOException {
        try {
            return super.isTruncated( dest );
        } catch ( AnnDataException e ) {
            log.warn( "AnnData file " + dest + " is likely invalid, however this method is only checking if the file is truncated.", e );
            return false;
        }
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataLoaderConfig config ) throws NoSingleCellDataFoundException {
        Path annDataFile = getDest( series );
        if ( Files.exists( annDataFile ) ) {
            return new GeoAnnDataSingleCellDataLoaderConfigurer( annDataFile, series )
                    .configureLoader( config );
        }
        throw new NoSingleCellDataFoundException( "Could not find " + annDataFile + " for " + series.getGeoAccession() );
    }
}
