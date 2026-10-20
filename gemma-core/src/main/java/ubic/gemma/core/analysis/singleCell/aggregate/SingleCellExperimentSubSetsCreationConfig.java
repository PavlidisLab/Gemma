package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for subsetting single-cell expression data in pseudo-bulks.
 * @author poirigui
 */
@Getter
@Builder
public class SingleCellExperimentSubSetsCreationConfig {
    /**
     * Whether to allow unmapped characteristics.
     */
    private boolean ignoreUnmatchedCharacteristics;
    /**
     * Whether to allow unmapped factor values.
     */
    private boolean ignoreUnmatchedFactorValues;
}
