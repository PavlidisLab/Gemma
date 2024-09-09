package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;

public class SeuratDiskDetector extends AbstractSingleFileInSeriesSingleCellDetector {

    protected SeuratDiskDetector() {
        super( "Seurat Disk", ".h5Seurat" );
    }

    @Override
    protected boolean accepts( String supplementaryFile ) {
        return super.accepts( supplementaryFile ) || supplementaryFile.endsWith( ".h5Seurat.h5" ) || supplementaryFile.endsWith( ".h5Seurat.h5.gz" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) {
        throw new UnsupportedOperationException( "Loading Seurat Disk data is not supported." );
    }
}
