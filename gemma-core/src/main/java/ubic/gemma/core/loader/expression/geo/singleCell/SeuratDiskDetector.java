package ubic.gemma.core.loader.expression.geo.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.SeuratDiskSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class SeuratDiskDetector extends AbstractSingleFileInSeriesSingleCellDetector {

    protected SeuratDiskDetector() {
        super( "Seurat Disk", ".h5Seurat" );
    }

    @Override
    protected boolean accepts( String supplementaryFile ) {
        return super.accepts( supplementaryFile ) || supplementaryFile.endsWith( ".h5Seurat.h5" ) || supplementaryFile.endsWith( ".h5Seurat.h5.gz" );
    }

    @Override
    public SingleCellDataLoader getSingleCellDataLoader( GeoSeries series ) throws NoSingleCellDataFoundException {
        Path seuratFile = getDest( series );
        if ( Files.exists( seuratFile ) ) {
            SeuratDiskSingleCellDataLoader loader = new SeuratDiskSingleCellDataLoader( seuratFile );
            loader.setBioAssayToSampleNameMatcher( ( bm, n ) -> n.equals( bm.getName() ) );
            return loader;
        }
        throw new NoSingleCellDataFoundException( "Could not find " + seuratFile + " for " + series.getGeoAccession() );
    }
}
