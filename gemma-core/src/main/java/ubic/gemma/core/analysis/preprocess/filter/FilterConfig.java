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
    public static final double DEFAULT_HIGHEXPRESSION_CUT = 0.0;
    // changed from 0.3, because now we remove the "no-gene" probes by default.
    public static final double DEFAULT_LOWEXPRESSIONCUT = 0.2;
    public static final double DEFAULT_LOWVARIANCECUT = 0.05;
    public static final double DEFAULT_MINPRESENT_FRACTION = 0.3;

    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;

    /**
     * If true, the MINIMUM_ROWS_TO_BOTHER is ignored.
     */
    private boolean ignoreMinimumRowsThreshold = false;

    /**
     * If true, MINIMUM_SAMPLE is ignored.
     */
    private boolean ignoreMinimumSampleThreshold = false;
    private double lowDistinctValueCut = DEFAULT_DISTINCTVALUE_FRACTION;
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;
    private boolean lowExpressionCutIsSet = true;
    private double lowVarianceCut = DEFAULT_LOWVARIANCECUT;
    private boolean lowVarianceCutIsSet = true;
    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;
    private boolean minPresentFractionIsSet = true;
    /**
     * Set to true if rows lacking associated BioSequences for the element should be removed.
     */
    private boolean requireSequences = true;
    private boolean lowDistinctValueIsSet = false;

    public void setLowDistinctValueCut( double lowDistinctValueCut ) {
        this.lowDistinctValueCut = lowDistinctValueCut;
        this.lowDistinctValueIsSet = true;
    }

    public void setLowExpressionCut( double lowExpressionCut ) {
        this.lowExpressionCut = lowExpressionCut;
    }

    public void setLowVarianceCut( double lowVarianceCut ) {
        this.lowVarianceCut = lowVarianceCut;
        this.lowVarianceCutIsSet = true;
    }

    public void setMinPresentFraction( double minPresentFraction ) {
        this.minPresentFraction = minPresentFraction;
        this.minPresentFractionIsSet = true;
    }

    @Override
    public String toString() {
        return "# highExpressionCut:" + highExpressionCut + "\n" +
                "# lowExpressionCut:" + lowExpressionCut + "\n" +
                "# minPresentFraction:" + minPresentFraction + "\n" +
                "# lowVarianceCut:" + lowVarianceCut + "\n" +
                "# distinctValueCut:" + lowDistinctValueCut;
    }
}
