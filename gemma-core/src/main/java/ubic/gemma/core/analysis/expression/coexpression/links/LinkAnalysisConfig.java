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
package ubic.gemma.core.analysis.expression.coexpression.links;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;

import java.io.File;
import java.io.Serializable;

/**
 * Holds parameters needed for LinkAnalysis. Note that many of these settings are not typically changed from the
 * defaults; they are there for experimentation.
 *
 * @author Paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible exernal use
public class LinkAnalysisConfig implements Serializable {

    /**
     * probes with more links than this are ignored. Zero means this doesn't do anything.
     */

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final Integer DEFAULT_PROBE_DEGREE_THRESHOLD = 0;
    private static final long serialVersionUID = 1L;
    private boolean absoluteValue = false;
    private String arrayName = null;
    /**
     * what proportion of links to keep (possibly subject to FWE statistical significance threshold). 1.0 means keep
     * everything. 0.01 means 1%.
     */
    private double cdfCut = 0.01;
    private boolean checkCorrelationDistribution = true;
    /**
     * only used for internal cache during calculations.
     */
    private double correlationCacheThreshold = 0.8;
    private boolean checkForBatchEffect = true;
    private boolean checkForOutliers = true;
    /**
     * family-wise error rate threshold we use to select links
     */
    private double fwe = 0.01;
    private boolean lowerCdfCutUsed = false;
    private double lowerTailCut = 0.01;
    private boolean makeSampleCorrMatImages = true;
    private String metric = "pearson"; // spearman
    /**
     * How many samples must be present in a correlation pair to keep the data, taking into account missing values.
     */
    private int minNumPresent = AbstractMatrixRowPairAnalysis.HARD_LIMIT_MIN_NUM_USED;
    private NormalizationMethod normalizationMethod = NormalizationMethod.none;
    /**
     * Remove negative correlated values at the end.
     */
    private boolean omitNegLinks = false;
    /**
     * Only used if textOut = true; if null, just write to stdout.
     */
    private File outputFile = null;
    /**
     * Probes with more than this many links are removed. Zero means no action is taken.
     */
    private int probeDegreeThreshold = DEFAULT_PROBE_DEGREE_THRESHOLD;
    private SingularThreshold singularThreshold = SingularThreshold.none; // fwe|cdfCut
    private boolean subset = false;
    private double subsetSize = 0.0;
    private boolean subsetUsed = false;
    private boolean textOut;
    private boolean upperCdfCutUsed = false;
    private double upperTailCut = 0.01;
    private boolean useDb = true;
    /**
     * If true, perform a log-transformation of the data prior to analysis. The default is to use the data as-is.
     * <p>
     * If the data is already on a log scale, nothing is done.
     */
    private boolean logTransform = false;

    public boolean isCheckForBatchEffect() {
        return checkForBatchEffect;
    }

    public void setCheckForBatchEffect( boolean rejectForBatchEffect ) {
        this.checkForBatchEffect = rejectForBatchEffect;
    }

    public boolean isCheckForOutliers() {
        return checkForOutliers;
    }

    public void setCheckForOutliers( boolean rejectForOutliers ) {
        this.checkForOutliers = rejectForOutliers;
    }

    public String getArrayName() {
        return arrayName;
    }

    public void setArrayName( String arrayName ) {
        this.arrayName = arrayName;
    }

    public double getCdfCut() {
        return cdfCut;
    }

    public void setCdfCut( double cdfCut ) {
        this.cdfCut = cdfCut;
    }

    public double getCorrelationCacheThreshold() {
        return correlationCacheThreshold;
    }

    public void setCorrelationCacheThreshold( double correlationCacheThreshold ) {
        this.correlationCacheThreshold = correlationCacheThreshold;
    }

    public double getFwe() {
        return fwe;
    }

    public void setFwe( double fwe ) {
        this.fwe = fwe;
    }

    public double getLowerTailCut() {
        return lowerTailCut;
    }

    public void setLowerTailCut( double lowerTailCut ) {
        this.lowerTailCut = lowerTailCut;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric( String metric ) {
        checkValidMetric( metric );
        this.metric = metric;
    }

    public int getMinNumPresent() {
        return minNumPresent;
    }

    public void setMinNumPresent( int minNumPresent ) {
        this.minNumPresent = minNumPresent;
    }

    /**
     * @return the normalizationMethod
     */
    public NormalizationMethod getNormalizationMethod() {
        return normalizationMethod;
    }

    /**
     * @param normalizationMethod the normalizationMethod to set
     */
    public void setNormalizationMethod( NormalizationMethod normalizationMethod ) {
        this.normalizationMethod = normalizationMethod;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile( File outputFile ) {
        this.outputFile = outputFile;
    }

    public Integer getProbeDegreeThreshold() {
        return this.probeDegreeThreshold;
    }

    /**
     * Probe degree threshold: Probes with more than this number of links are ignored. If set to &lt;= 0, this setting is
     * ignored.
     *
     * @param probeDegreeThreshold the probeDegreeThreshold to set
     */
    public void setProbeDegreeThreshold( int probeDegreeThreshold ) {
        this.probeDegreeThreshold = probeDegreeThreshold;
    }

    /**
     * @return the singularThreshold
     */
    public SingularThreshold getSingularThreshold() {
        return singularThreshold;
    }

    /**
     * Set to modify threshold behaviour: enforce the choice of only one of the two standard thresholds.
     *
     * @param singularThreshold the singularThreshold to set. Default is 'none'.
     */
    public void setSingularThreshold( SingularThreshold singularThreshold ) {
        this.singularThreshold = singularThreshold;
    }

    public double getSubsetSize() {
        return subsetSize;
    }

    public void setSubsetSize( double subsetSize ) {
        this.subsetSize = subsetSize;
    }

    public double getUpperTailCut() {
        return upperTailCut;
    }

    public void setUpperTailCut( double upperTailCut ) {
        this.upperTailCut = upperTailCut;
    }

    public boolean isAbsoluteValue() {
        return absoluteValue;
    }

    public void setAbsoluteValue( boolean absoluteValue ) {
        this.absoluteValue = absoluteValue;
    }

    public boolean isCheckCorrelationDistribution() {
        return this.checkCorrelationDistribution;
    }

    public void setCheckCorrelationDistribution( boolean checkCorrelationDistribution ) {
        this.checkCorrelationDistribution = checkCorrelationDistribution;
    }

    /**
     * @return the lowerCdfCutUsed
     */
    public boolean isLowerCdfCutUsed() {
        return lowerCdfCutUsed;
    }

    /**
     * @param lowerCdfCutUsed the lowerCdfCutUsed to set
     */
    public void setLowerCdfCutUsed( boolean lowerCdfCutUsed ) {
        this.lowerCdfCutUsed = lowerCdfCutUsed;
    }

    public boolean isMakeSampleCorrMatImages() {
        return makeSampleCorrMatImages;
    }

    public void setMakeSampleCorrMatImages( boolean makeSampleCorrMatImages ) {
        this.makeSampleCorrMatImages = makeSampleCorrMatImages;
    }

    /**
     * @return the omitNegLinks
     */
    public boolean isOmitNegLinks() {
        return omitNegLinks;
    }

    /**
     * @param omitNegLinks the omitNegLinks to set
     */
    public void setOmitNegLinks( boolean omitNegLinks ) {
        this.omitNegLinks = omitNegLinks;
    }

    public boolean isSubset() {
        return subset;
    }

    public void setSubset( boolean subset ) {
        this.subset = subset;
    }

    public boolean isSubsetUsed() {
        return subsetUsed;
    }

    public void setSubsetUsed( boolean subsetUsed ) {
        this.subsetUsed = subsetUsed;
    }

    public boolean isTextOut() {
        return textOut;
    }

    public void setTextOut( boolean b ) {
        this.textOut = b;
    }

    /**
     * @return the upperCdfCutUsed
     */
    public boolean isUpperCdfCutUsed() {
        return upperCdfCutUsed;
    }

    /**
     * @param upperCdfCutUsed the upperCdfCutUsed to set
     */
    public void setUpperCdfCutUsed( boolean upperCdfCutUsed ) {
        this.upperCdfCutUsed = upperCdfCutUsed;
    }

    public boolean isUseDb() {
        return useDb;
    }

    public void setUseDb( boolean useDb ) {
        this.useDb = useDb;
    }

    public boolean isLogTransform() {
        return logTransform;
    }

    public void setLogTransform( boolean logTransform ) {
        this.logTransform = logTransform;
    }

    /**
     * @return representation of this analysis (not completely filled in - only the basic parameters)
     */
    public CoexpressionAnalysis toAnalysis() {
        CoexpressionAnalysis analysis = CoexpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Link analysis settings" );
        protocol.setDescription( this.toString() );
        analysis.setProtocol( protocol );
        return analysis;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "# absoluteValue:" ).append( this.isAbsoluteValue() ).append( "\n" );
        buf.append( "# metric:" ).append( this.getMetric() ).append( "\n" );
        buf.append( "# cdfCut:" ).append( this.getCdfCut() ).append( "\n" );
        buf.append( "# cacheCut:" ).append( this.getCorrelationCacheThreshold() ).append( "\n" );
        buf.append( "# fwe:" ).append( this.getFwe() ).append( "\n" );
        buf.append( "# uppercut:" ).append( this.getUpperTailCut() ).append( "\n" );
        buf.append( "# lowercut:" ).append( this.getLowerTailCut() ).append( "\n" );
        buf.append( "# useDB:" ).append( this.isUseDb() ).append( "\n" );
        buf.append( "# normalizationMethod:" ).append( this.getNormalizationMethod() ).append( "\n" );
        buf.append( "# omitNegLinks:" ).append( this.isOmitNegLinks() ).append( "\n" );
        buf.append( "# probeDegreeThreshold:" ).append( this.getProbeDegreeThreshold() ).append( "\n" );
        /*
         * if ( this.isSubsetUsed() ) { buf.append( "# subset:" + this.subsetSize + "\n" ); }
         */
        if ( this.isUpperCdfCutUsed() ) {
            buf.append( "# upperCutUsed:cdfCut\n" );
        } else {
            buf.append( "# upperCutUsed:fwe\n" );
        }
        if ( this.isLowerCdfCutUsed() ) {
            buf.append( "# lowerCutUsed:cdfCut\n" );
        } else {
            buf.append( "# lowerCutUsed:fwe\n" );
        }
        buf.append( "# singularThreshold:" ).append( this.getSingularThreshold() ).append( "\n" );
        return buf.toString();
    }

    private void checkValidMetric( String m ) {
        if ( m.equalsIgnoreCase( "pearson" ) )
            return;
        if ( m.equalsIgnoreCase( "spearman" ) )
            return;
        throw new IllegalArgumentException(
                "Unrecognized metric: " + m + ", valid options are 'pearson' and 'spearman'" );
    }

    public enum NormalizationMethod {
        BALANCE, none, SPELL, SVD
    }

    /**
     * Configures whether only one of the two thresholds should be used. Set to 'none' to use the standard
     * dual-threshold, or choose 'fwe' or 'cdfcut' to use only one of those.
     */
    public enum SingularThreshold {
        cdfcut, fwe, none
    }
}
