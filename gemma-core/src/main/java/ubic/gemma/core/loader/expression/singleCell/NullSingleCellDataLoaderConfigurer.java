package ubic.gemma.core.loader.expression.singleCell;

public class NullSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<NullSingleCellDataLoader> {

    public NullSingleCellDataLoaderConfigurer() {
    }

    @Override
    public NullSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        return new NullSingleCellDataLoader();
    }
}
