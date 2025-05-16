package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for splitting single-cell expression data in pseudo-bulks.
 */
@Getter
@Builder
public class SplitConfig {
    /**
     * Whether to allow unmapped characteristics.
     */
    private boolean ignoreUnmatchedCharacteristics;
    /**
     * Whether to allow unmapped factor values.
     */
    private boolean ignoreUnmatchedFactorValues;
}
