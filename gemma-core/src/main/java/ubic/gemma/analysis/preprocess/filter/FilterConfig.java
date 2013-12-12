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
package ubic.gemma.analysis.preprocess.filter;

import java.io.Serializable;

/**
 * Holds settings for filtering.
 * 
 * @author Paul
 * @version $Id$
 */
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
     * @see ExpressionExperimentFilter.MIN_NUMBER_OF_SAMPLES_PRESENT for a related setting.
     */
    public final static int MINIMUM_SAMPLE = 20;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private int afterDistinctValueCut = 0;

    private int afterInitialFilter = 0;
    private int afterLowExpressionCut = 0;
    private int afterLowVarianceCut = 0;
    private int afterMinPresentFilter = 0;
    private int afterZeroVarianceCut = 0;

    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;

    /**
     * If true, the MINIMUM_ROWS_TO_BOTHER is ignored.
     */
    private boolean ignoreMinimumRowsThreshold = false;

    /**
     * If true, MINIMUM_SAMPLE is ignored.
     */
    private boolean ignoreMinimumSampleThreshold = false;
    private boolean logTransform = false;
    private double lowDistinctValueCut = DEFAULT_DISTINCTVALUE_FRACTION;
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;
    private boolean lowExpressionCutIsSet = true;
    private double lowVarianceCut = DEFAULT_LOWVARIANCECUT;
    private boolean lowVarianceCutIsSet = true;
    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;
    private boolean minPresentFractionIsSet = true;
    private boolean requireSequences = true;

    public boolean isRequireSequences() {
        return requireSequences;
    }

    /**
     * Set to true if rows lacking associated BioSequences for the element should be removed.
     * 
     * @param requireSequences
     */
    public void setRequireSequences( boolean requireSequences ) {
        this.requireSequences = requireSequences;
    }

    private int startingRows = 0;

    private boolean lowDistinctValueIsSet = false;;

    public boolean isLowDistinctValueIsSet() {
        return lowDistinctValueIsSet;
    }

    public int getAfterDistinctValueCut() {
        return afterDistinctValueCut;
    }

    /**
     * @return the afterInitialFilter
     */
    public int getAfterInitialFilter() {
        return afterInitialFilter;
    }

    /**
     * @return the afterLowExpressionCut
     */
    public int getAfterLowExpressionCut() {
        return afterLowExpressionCut;
    }

    /**
     * @return the afterLowVarianceCut
     */
    public int getAfterLowVarianceCut() {
        return afterLowVarianceCut;
    }

    /**
     * @return the afterMinPresentFilter
     */
    public int getAfterMinPresentFilter() {
        return afterMinPresentFilter;
    }

    public int getAfterZeroVarianceCut() {
        return afterZeroVarianceCut;
    }

    public double getHighExpressionCut() {
        return highExpressionCut;
    }

    public double getLowDistinctValueCut() {
        return lowDistinctValueCut;
    }

    public double getLowExpressionCut() {
        return lowExpressionCut;
    }

    public double getLowVarianceCut() {
        return lowVarianceCut;
    }

    public double getMinPresentFraction() {
        return minPresentFraction;
    }

    /**
     * @return the startingRows
     */
    public int getStartingRows() {
        return startingRows;
    }

    /**
     * @return the ignoreMinimumRowThreshold
     */
    public boolean isIgnoreMinimumRowsThreshold() {
        return ignoreMinimumRowsThreshold;
    }

    public boolean isIgnoreMinimumSampleThreshold() {
        return ignoreMinimumSampleThreshold;
    }

    /**
     * @return the logTransform
     */
    public boolean isLogTransform() {
        return logTransform;
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

    public void setAfterDistinctValueCut( int afterDistinctValueCut ) {
        this.afterDistinctValueCut = afterDistinctValueCut;
    }

    /**
     * @param afterInitialFilter the afterInitialFilter to set
     */
    public void setAfterInitialFilter( int afterInitialFilter ) {
        this.afterInitialFilter = afterInitialFilter;
    }

    /**
     * @param afterLowExpressionCut the afterLowExpressionCut to set
     */
    public void setAfterLowExpressionCut( int afterLowExpressionCut ) {
        this.afterLowExpressionCut = afterLowExpressionCut;
    }

    /**
     * @param afterLowVarianceCut the afterLowVarianceCut to set
     */
    public void setAfterLowVarianceCut( int afterLowVarianceCut ) {
        this.afterLowVarianceCut = afterLowVarianceCut;
    }

    /**
     * @param afterMinPresentFilter the afterMinPresentFilter to set
     */
    public void setAfterMinPresentFilter( int afterMinPresentFilter ) {
        this.afterMinPresentFilter = afterMinPresentFilter;
    }

    public void setAfterZeroVarianceCut( int afterZeroVarianceCut ) {
        this.afterZeroVarianceCut = afterZeroVarianceCut;
    }

    public void setHighExpressionCut( double highExpressionCut ) {
        this.highExpressionCut = highExpressionCut;
    }

    /**
     * @param ignoreMinimumRowThreshold the ignoreMinimumRowThreshold to set
     */
    public void setIgnoreMinimumRowsThreshold( boolean ignoreMinimumRowsThreshold ) {
        this.ignoreMinimumRowsThreshold = ignoreMinimumRowsThreshold;
    }

    public void setIgnoreMinimumSampleThreshold( boolean ignoreMinimumSampleThreshold ) {
        this.ignoreMinimumSampleThreshold = ignoreMinimumSampleThreshold;
    }

    /**
     * @param logTransform the logTransform to set
     */
    public void setLogTransform( boolean logTransform ) {
        this.logTransform = logTransform;
    }

    public void setLowDistinctValueCut( double lowDistinctValueCut ) {
        this.lowDistinctValueCut = lowDistinctValueCut;
        this.lowDistinctValueIsSet = true;
    }

    public void setLowExpressionCut( double lowExpressionCut ) {
        this.lowExpressionCutIsSet = true;
        this.lowExpressionCut = lowExpressionCut;
    }

    public void setLowVarianceCut( double lowVarianceCut ) {
        this.lowVarianceCutIsSet = true;
        this.lowVarianceCut = lowVarianceCut;
    }

    public void setMinPresentFraction( double minPresentFraction ) {
        this.minPresentFractionIsSet = true;
        this.minPresentFraction = minPresentFraction;
    }

    /**
     * @param startingRows the startingRows to set
     */
    public void setStartingRows( int startingRows ) {
        this.startingRows = startingRows;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "# highExpressionCut:" + this.getHighExpressionCut() + "\n" );
        buf.append( "# lowExpressionCut:" + this.getLowExpressionCut() + "\n" );
        buf.append( "# minPresentFraction:" + this.getMinPresentFraction() + "\n" );
        buf.append( "# lowVarianceCut:" + this.getLowVarianceCut() + "\n" );
        buf.append( "# distinctValuecut:" + this.lowDistinctValueCut + "\n" );
        buf.append( "# startingProbes:" + this.getStartingRows() + "\n" );
        buf.append( "# afterInitialFilter:" + this.getAfterInitialFilter() + "\n" );
        buf.append( "# afterMinPresentFilter:" + this.getAfterMinPresentFilter() + "\n" );
        buf.append( "# afterLowVarianceCut:" + this.getAfterLowVarianceCut() + "\n" );
        buf.append( "# afterLowExpressionCut:" + this.getAfterLowExpressionCut() + "\n" );
        buf.append( "# logTransform:" + this.isLogTransform() + "\n" );
        // buf.append( "# knownGenesOnly " + this.isKnownGenesOnly() + "\n" );
        return buf.toString();
    }

    public boolean isDistinctValueThresholdSet() {
        // TODO Auto-generated method stub
        return false;
    }

}
