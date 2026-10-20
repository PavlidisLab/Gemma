package ubic.gemma.core.analysis.expression.diff;

import lombok.Data;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Set;

/**
 * Holds the result of data filtering for differential expression analysis.
 *
 * @author poirigui
 */
@Data
public class DifferentialExpressionAnalysisFilterResult {
    /**
     * Starting number of design elements.
     */
    private int startingDesignElements;
    /**
     * Starting set of samples prior to filtering.
     */
    private Set<BioMaterial> startingSamples;
    private boolean outliersFilterApplied;
    private int designElementsAfterOutliers;
    private Set<BioMaterial> samplesAfterOutliers;
    /**
     * Indicate if the minimum cells filter was applied.
     * <p>
     * This filter is applied for non-single-cell data.
     */
    private boolean minimumCellsFilterApplied;
    /**
     * Number of design elements left after filtering for minimum number of cells.
     */
    private int designElementsAfterMinimumCells;
    /**
     * Number of samples left after filtering for minimum number of cells.
     */
    private Set<BioMaterial> samplesAfterMinimumCells;
    /**
     * Indicate if the repetitive values filter was applied.
     * <p>
     * This filter is usually applied when there are too few samples (columns) to apply it reliably.
     */
    private boolean repetitiveValuesFilterApplied;
    /**
     * Number of design elements left after filtering for repetitive values.
     */
    private int designElementsAfterRepetitiveValues;
    /**
     * Indicate if the low variance filter was applied.
     * <p>
     * This is usually the case when the data is not a log2cpm scale.
     */
    private boolean lowVarianceFilterApplied;
    /**
     * Number of design elements left after filtering for low variance.
     */
    private int designElementsAfterLowVariance;
    /**
     * Final number of design elements.
     */
    private int finalDesignElements;
    /**
     * Final set of samples.
     */
    private Set<BioMaterial> finalSamples;
}
