package ubic.gemma.core.analysis.preprocess.filter;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds settings for filtering with {@link ExpressionExperimentFilter}.
 *
 * @author Paul
 */
@Data
public class FilterConfig implements Serializable {

    public static final double DEFAULT_DISTINCTVALUE_FRACTION = 0.5;
    public static final double DEFAULT_HIGHEXPRESSION_CUT = 1.0;
    // changed from 0.3, because now we remove the "no-gene" probes by default.
    public static final double DEFAULT_LOWEXPRESSIONCUT = 0.2;
    public static final double DEFAULT_LOWVARIANCECUT = 0.05;
    public static final double DEFAULT_MINPRESENT_FRACTION = 0.3;

    /**
     * Low cut for expression.
     * <p>
     * Design elements expressed below the cut will be filtered out.
     * <p>
     * This threshold applies on the rank. A value of 0.9 will drop the top 10% of the data.
     * <p>
     * Set this to one to disable.
     */
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;

    /**
     * High cut for expression.
     * <p>
     * Design elements expressed above the cut will be filtered out.
     * <p>
     * This threshold applies on the rank of gene expression. A value of 0.1 will drop the bottom 10% of the data.
     * <p>
     * Set this to zero to disable.
     */
    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;

    /**
     * This threshold applies on the variance of the row.
     * <p>
     * Set this to zero disable.
     */
    private double lowVarianceCut = DEFAULT_LOWVARIANCECUT;

    private double lowDistinctValueCut = DEFAULT_DISTINCTVALUE_FRACTION;

    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;

    /**
     * If true, the MINIMUM_ROWS_TO_BOTHER is ignored.
     */
    private boolean ignoreMinimumRowsThreshold = false;

    /**
     * If true, MINIMUM_SAMPLE is ignored.
     */
    private boolean ignoreMinimumSampleThreshold = false;
    /**
     * Set to true if rows lacking associated BioSequences for the element should be removed.
     */
    private boolean requireSequences = true;
    private boolean lowDistinctValueIsSet = false;

    @Override
    public String toString() {
        return "# highExpressionCut:" + highExpressionCut + "\n" +
                "# lowExpressionCut:" + lowExpressionCut + "\n" +
                "# minPresentFraction:" + minPresentFraction + "\n" +
                "# lowVarianceCut:" + lowVarianceCut + "\n" +
                "# distinctValueCut:" + lowDistinctValueCut;
    }
}
