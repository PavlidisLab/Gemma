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

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "# highExpressionCut " + this.getHighExpressionCut() + "\n" );
        buf.append( "# lowExpressionCut " + this.getLowExpressionCut() + "\n" );
        buf.append( "# minPresentFraction " + this.getMinPresentFraction() + "\n" );
        buf.append( "# lowVarianceCut " + this.getLowVarianceCut() + "\n" );
        //buf.append( "# knownGenesOnly " + this.isKnownGenesOnly() + "\n" );
        return buf.toString();
    }

    /**
     * How many samples a dataset has to have before we consider analyzing it.
     * 
     * @see ExpressionExperimentFilter.MIN_NUMBER_OF_SAMPLES_PRESENT for a related setting.
     */
    public final static int MINIMUM_SAMPLE = 7;

    /**
     * Fewer rows than this, and we bail.
     */
    public static final int MINIMUM_ROWS_TO_BOTHER = 100;

    public static final double DEFAULT_HIGHEXPRESSION_CUT = 0.0;

    public static final double DEFAULT_LOWEXPRESSIONCUT = 0.3;

    public static final double DEFAULT_LOWVARIANCECUT = 0.05;

    public static final double DEFAULT_TOOSMALLTOKEEP = 0.5;

    public static final double DEFAULT_MINPRESENT_FRACTION = 0.3;

    private boolean minPresentFractionIsSet = true;
    private boolean lowExpressionCutIsSet = true;
    private boolean lowVarianceCutIsSet = true;
    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;
    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;
    private double lowVarianceCut = DEFAULT_LOWVARIANCECUT;

    private boolean knownGenesOnly = false;

    /**
     * If true, MINIMUM_SAMPLE is ignored.
     */
    private boolean ignoreMinimumSampleThreshold = false;

    /**
     * If true, the MINIMUM_ROWS_TO_BOTHER is ignored.
     */
    private boolean ignoreMinimumRowsThreshold = false;

    public boolean isKnownGenesOnly() {
        return knownGenesOnly;
    }

    public void setKnownGenesOnly( boolean knownGenesOnly ) {
        this.knownGenesOnly = knownGenesOnly;
    }

    public double getHighExpressionCut() {
        return highExpressionCut;
    }

    public boolean isLowVarianceCutIsSet() {
        return lowVarianceCutIsSet;
    }

    public void setHighExpressionCut( double highExpressionCut ) {
        this.highExpressionCut = highExpressionCut;
    }

    public double getLowExpressionCut() {
        return lowExpressionCut;
    }

    public void setLowExpressionCut( double lowExpressionCut ) {
        this.lowExpressionCutIsSet = true;
        this.lowExpressionCut = lowExpressionCut;
    }

    public boolean isLowExpressionCutIsSet() {
        return lowExpressionCutIsSet;
    }

    public double getMinPresentFraction() {
        return minPresentFraction;
    }

    public void setMinPresentFraction( double minPresentFraction ) {
        this.minPresentFractionIsSet = true;
        this.minPresentFraction = minPresentFraction;
    }

    public boolean isMinPresentFractionIsSet() {
        return minPresentFractionIsSet;
    }

    public double getLowVarianceCut() {
        return lowVarianceCut;
    }

    public void setLowVarianceCut( double lowVarianceCut ) {
        this.lowVarianceCutIsSet = true;
        this.lowVarianceCut = lowVarianceCut;
    }

    public boolean isIgnoreMinimumSampleThreshold() {
        return ignoreMinimumSampleThreshold;
    }

    public void setIgnoreMinimumSampleThreshold( boolean ignoreMinimumSampleThreshold ) {
        this.ignoreMinimumSampleThreshold = ignoreMinimumSampleThreshold;
    }

    /**
     * @return the ignoreMinimumRowThreshold
     */
    public boolean isIgnoreMinimumRowsThreshold() {
        return ignoreMinimumRowsThreshold;
    }

    /**
     * @param ignoreMinimumRowThreshold the ignoreMinimumRowThreshold to set
     */
    public void setIgnoreMinimumRowsThreshold( boolean ignoreMinimumRowsThreshold ) {
        this.ignoreMinimumRowsThreshold = ignoreMinimumRowsThreshold;
    }

}
