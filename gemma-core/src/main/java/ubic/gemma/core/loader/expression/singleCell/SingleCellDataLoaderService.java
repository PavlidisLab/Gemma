package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface SingleCellDataLoaderService {

    /**
     * Load single-cell data, the data type is automatically detected.
     * @see #load(ExpressionExperiment, ArrayDesign, SingleCellDataType, SingleCellDataLoaderConfig)
     */
    void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config );

    /**
     * Load a specific single-cell data type.
     * @param ee       experiment to load data into
     * @param platform platform to use to associate data vectors to
     * @param dataType data type to detect
     * @param config   a configuration
     */
    void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config );
}