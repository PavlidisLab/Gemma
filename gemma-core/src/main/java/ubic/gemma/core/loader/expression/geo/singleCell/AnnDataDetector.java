package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class AnnDataDetector extends AbstractSingleFileInSeriesSingleCellDetector implements SingleCellDetector {

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
            AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( annDataFile );
            // TODO: detect the sample factor name ae
            // loader.setSampleFactorName();
            // loader.setCellTypeFactorName();
            // loader.setUnknownCellTypeIndicator();
            return loader;
        }
        throw new NoSingleCellDataFoundException( "Could not find " + annDataFile + " for " + series.getGeoAccession() );
    }
}
