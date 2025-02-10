package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.DataLoaderConfigurer;

/**
 * Interface for configuring a single-cell data loader.
 * <p>
 * The role of a configurer is to apply a {@link SingleCellDataLoaderConfig} to a {@link SingleCellDataLoader}.
 * <p>
 * This exists because we need to configure the loader in two different scenarios:
 * <ul>
 * <li>When we have a {@link ubic.gemma.core.loader.expression.geo.model.GeoSeries} and we need to inspect the data
 * prior to loading the dataset</li>
 * <li>When we have an existing {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and we need to load
 * single-cell data.</li>
 * </ul>
 * @param <T> the type of loader this configurer produces
 * @author poirigui
 */
public interface SingleCellDataLoaderConfigurer<T extends SingleCellDataLoader> extends DataLoaderConfigurer<T, SingleCellDataLoaderConfig> {

}
