package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Map;

/**
 * High-level service for loading single-cell data.
 * @author poirigui
 */
public interface SingleCellDataLoaderService {

    /**
     * Load single-cell data, the data type is automatically detected.
     * @see #load(ExpressionExperiment, ArrayDesign, SingleCellDataType, SingleCellDataLoaderConfig)
     */
    QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config );

    /**
     * Load a specific single-cell data type.
     *
     * @param ee       experiment to load data into
     * @param platform platform to use to associate data vectors to
     * @param dataType data type to detect
     * @param config   a configuration
     */
    QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config );

    /**
     * Load sequencing metadata.
     */
    Map<BioAssay, SequencingMetadata> loadSequencingMetadata( ExpressionExperiment ee, SingleCellDataLoaderConfig config );

    /**
     * Load the cell type assignments, the data type is automatically detected.
     * @see #loadOtherCellLevelCharacteristics(ExpressionExperiment, SingleCellDataType, SingleCellDataLoaderConfig)
     */
    Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataLoaderConfig config );

    /**
     * Load the cell type assignments.
     * <p>
     * The dataset must already have a set of preferred single-cell vectors loaded or one identified by {@link SingleCellDataLoaderConfig#getQuantitationTypeName()}.
     */
    Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config );

    /**
     * Load other cell-level characteristics (i.e. anything that is not a cell type assignment), the data type is
     * automatically detected.
     * @see #loadOtherCellLevelCharacteristics(ExpressionExperiment, SingleCellDataType, SingleCellDataLoaderConfig)
     */
    Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataLoaderConfig config );

    /**
     * Load other cell-level characteristics (i.e. anything that is not a cell type assignment).
     * <p>
     * The dataset must already have a set of preferred single-cell vectors loaded or one identified by {@link SingleCellDataLoaderConfig#getQuantitationTypeName()}.
     */
    Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config );
}