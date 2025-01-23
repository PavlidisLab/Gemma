package ubic.gemma.core.loader.expression.singleCell;

/**
 * Interface for configuring a single-cell data loader.
 * <p>
 * This exists because we need to configure the loader in two different scenarios:
 * <ul>
 * <li>When we have a {@link ubic.gemma.core.loader.expression.geo.model.GeoSeries} and we need to inspect the data
 * prior to loading the dataset</li>
 * <li>When we have an existing {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and we need to load
 * single-cell data.</li>
 * </ul>
 * @param <T>
 * @author poirigui
 */
public interface SingleCellDataLoaderConfigurer<T extends SingleCellDataLoader> {

    /**
     * Pre-configure a single-cell data loader.
     */
    T configureLoader();
}
