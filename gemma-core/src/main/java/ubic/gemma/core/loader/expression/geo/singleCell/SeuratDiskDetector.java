package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;

public class SeuratDiskDetector extends AbstractSingleH5FileInSeriesSingleCellDetector {

    protected SeuratDiskDetector() {
        super( "Seurat Disk", ".h5Seurat" );
    }

    @Override
    protected boolean accepts( String supplementaryFile ) {
        return super.accepts( supplementaryFile ) || supplementaryFile.endsWith( ".h5Seurat.h5" ) || supplementaryFile.endsWith( ".h5Seurat.h5.gz" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series, SingleCellDataLoaderConfig config ) {
        throw new UnsupportedOperationException( "Loading Seurat Disk data is not supported." );
    }
}
