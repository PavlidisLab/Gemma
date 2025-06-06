/*
 * The Gemma project
 *
 * Copyright (c) 2007 Columbia University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.preprocess.filter;

import java.io.Serializable;

/**
 * Holds settings for filtering with {@link ExpressionExperimentFilter}.
 *
 * @author Paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Constants should have same access level, possible external use
public class FilterConfig implements Serializable {

    public static final double DEFAULT_DISTINCTVALUE_FRACTION = 0.5;
    public static final double DEFAULT_HIGHEXPRESSION_CUT = 0.0;
    // changed from 0.3, because now we remove the "no-gene" probes by default.
    public static final double DEFAULT_LOWEXPRESSIONCUT = 0.2;
    public static final double DEFAULT_LOWVARIANCECUT = 0.05;
    public static final double DEFAULT_MINPRESENT_FRACTION = 0.3;
    public static final double DEFAULT_TOOSMALLTOKEEP = 0.5;
    /**
     * Fewer rows than this, and we bail.
     */
    public static final int MINIMUM_ROWS_TO_BOTHER = 50;
    /**
     * How many samples a dataset has to have before we consider analyzing it.
     *
     * @see ExpressionExperimentFilter#MIN_NUMBER_OF_SAMPLES_PRESENT for a related setting.
     */
    public final static int MINIMUM_SAMPLE = 20;

    private static final long serialVersionUID = 1L;
    private int afterDistinctValueCut = 0;
    private int afterInitialFilter = 0;
    private int afterLowExpressionCut = 0;
    private int afterLowVarianceCut = 0;
    private int afterMinPresentFilter = 0;
    private int afterZeroVarianceCut = 0;

    private double highExpressionCut = FilterConfig.DEFAULT_HIGHEXPRESSION_CUT;

    /**
     * If true, the MINIMUM_ROWS_TO_BOTHER is ignored.
     */
    private boolean ignoreMinimumRowsThreshold = false;

    /**
     * If true, MINIMUM_SAMPLE is ignored.
     */
    private boolean ignoreMinimumSampleThreshold = false;
    private boolean logTransform = false;
    private double lowDistinctValueCut = FilterConfig.DEFAULT_DISTINCTVALUE_FRACTION;
    private double lowExpressionCut = FilterConfig.DEFAULT_LOWEXPRESSIONCUT;
    private boolean lowExpressionCutIsSet = true;
    private double lowVarianceCut = FilterConfig.DEFAULT_LOWVARIANCECUT;
    private boolean lowVarianceCutIsSet = true;
    private double minPresentFraction = FilterConfig.DEFAULT_MINPRESENT_FRACTION;
    private boolean minPresentFractionIsSet = true;
    private boolean requireSequences = true;
    private int startingRows = 0;
    private boolean lowDistinctValueIsSet = false;

    public boolean isRequireSequences() {
        return requireSequences;
    }

    /**
     * Set to true if rows lacking associated BioSequences for the element should be removed.
     *
     * @param requireSequences new value
     */
    public void setRequireSequences( boolean requireSequences ) {
        this.requireSequences = requireSequences;
    }

    public boolean isLowDistinctValueIsSet() {
        return lowDistinctValueIsSet;
    }

    public int getAfterDistinctValueCut() {
        return afterDistinctValueCut;
    }

    public void setAfterDistinctValueCut( int afterDistinctValueCut ) {
        this.afterDistinctValueCut = afterDistinctValueCut;
    }

    /**
     * @return the afterInitialFilter
     */
    public int getAfterInitialFilter() {
        return afterInitialFilter;
    }

    /**
     * @param afterInitialFilter the afterInitialFilter to set
     */
    public void setAfterInitialFilter( int afterInitialFilter ) {
        this.afterInitialFilter = afterInitialFilter;
    }

    /**
     * @return the afterLowExpressionCut
     */
    public int getAfterLowExpressionCut() {
        return afterLowExpressionCut;
    }

    /**
     * @param afterLowExpressionCut the afterLowExpressionCut to set
     */
    public void setAfterLowExpressionCut( int afterLowExpressionCut ) {
        this.afterLowExpressionCut = afterLowExpressionCut;
    }

    /**
     * @return the afterLowVarianceCut
     */
    public int getAfterLowVarianceCut() {
        return afterLowVarianceCut;
    }

    /**
     * @param afterLowVarianceCut the afterLowVarianceCut to set
     */
    public void setAfterLowVarianceCut( int afterLowVarianceCut ) {
        this.afterLowVarianceCut = afterLowVarianceCut;
    }

    /**
     * @return the afterMinPresentFilter
     */
    public int getAfterMinPresentFilter() {
        return afterMinPresentFilter;
    }

    /**
     * @param afterMinPresentFilter the afterMinPresentFilter to set
     */
    public void setAfterMinPresentFilter( int afterMinPresentFilter ) {
        this.afterMinPresentFilter = afterMinPresentFilter;
    }

    public int getAfterZeroVarianceCut() {
        return afterZeroVarianceCut;
    }

    public void setAfterZeroVarianceCut( int afterZeroVarianceCut ) {
        this.afterZeroVarianceCut = afterZeroVarianceCut;
    }

    public double getHighExpressionCut() {
        return highExpressionCut;
    }

    public void setHighExpressionCut( double highExpressionCut ) {
        this.highExpressionCut = highExpressionCut;
    }

    public double getLowDistinctValueCut() {
        return lowDistinctValueCut;
    }

    public void setLowDistinctValueCut( double lowDistinctValueCut ) {
        this.lowDistinctValueCut = lowDistinctValueCut;
        this.lowDistinctValueIsSet = true;
    }

    public double getLowExpressionCut() {
        return lowExpressionCut;
    }

    public void setLowExpressionCut( double lowExpressionCut ) {
        this.lowExpressionCutIsSet = true;
        this.lowExpressionCut = lowExpressionCut;
    }

    public double getLowVarianceCut() {
        return lowVarianceCut;
    }

    public void setLowVarianceCut( double lowVarianceCut ) {
        this.lowVarianceCutIsSet = true;
        this.lowVarianceCut = lowVarianceCut;
    }

    public double getMinPresentFraction() {
        return minPresentFraction;
    }

    public void setMinPresentFraction( double minPresentFraction ) {
        this.minPresentFractionIsSet = true;
        this.minPresentFraction = minPresentFraction;
    }

    /**
     * @return the startingRows
     */
    public int getStartingRows() {
        return startingRows;
    }

    /**
     * @param startingRows the startingRows to set
     */
    public void setStartingRows( int startingRows ) {
        this.startingRows = startingRows;
    }

    /**
     * @return the ignoreMinimumRowThreshold
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    public boolean isIgnoreMinimumRowsThreshold() {
        return ignoreMinimumRowsThreshold;
    }

    /**
     * @param ignoreMinimumRowsThreshold the ignoreMinimumRowThreshold to set
     */
    public void setIgnoreMinimumRowsThreshold( boolean ignoreMinimumRowsThreshold ) {
        this.ignoreMinimumRowsThreshold = ignoreMinimumRowsThreshold;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Better semantics
    public boolean isIgnoreMinimumSampleThreshold() {
        return ignoreMinimumSampleThreshold;
    }

    public void setIgnoreMinimumSampleThreshold( boolean ignoreMinimumSampleThreshold ) {
        this.ignoreMinimumSampleThreshold = ignoreMinimumSampleThreshold;
    }

    /**
     * @return the logTransform
     */
    public boolean isLogTransform() {
        return logTransform;
    }

    /**
     * @param logTransform the logTransform to set
     */
    public void setLogTransform( boolean logTransform ) {
        this.logTransform = logTransform;
    }

    public boolean isLowExpressionCutIsSet() {
        return lowExpressionCutIsSet;
    }

    public boolean isLowVarianceCutIsSet() {
        return lowVarianceCutIsSet;
    }

    public boolean isMinPresentFractionIsSet() {
        return minPresentFractionIsSet;
    }

    @Override
    public String toString() {
        return ( "# highExpressionCut:" + this.getHighExpressionCut() + "\n" ) + "# lowExpressionCut:" + this
                .getLowExpressionCut() + "\n" + "# minPresentFraction:" + this.getMinPresentFraction() + "\n"
                + "# lowVarianceCut:" + this.getLowVarianceCut() + "\n" + "# distinctValuecut:"
                + this.lowDistinctValueCut + "\n" + "# startingProbes:" + this.getStartingRows() + "\n"
                + "# afterInitialFilter:" + this.getAfterInitialFilter() + "\n" + "# afterMinPresentFilter:" + this
                .getAfterMinPresentFilter() + "\n" + "# afterLowVarianceCut:" + this.getAfterLowVarianceCut() + "\n"
                + "# afterLowExpressionCut:" + this.getAfterLowExpressionCut() + "\n" + "# logTransform:" + this
                .isLogTransform() + "\n";
    }

}
