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
package ubic.gemma.analysis.expression.coexpression.links;

import java.io.File;
import java.io.Serializable;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;

/**
 * Holds parameters needed for LinkAnalysis. Note that many of these settings are not typically changed from the
 * defaults; they are there for experimentation.
 * 
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisConfig implements Serializable {

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

    // probes with more links than this are ignored. Zero means this doesn't do anything.
    public static final Integer DEFAULT_PROBE_DEGREE_THRESHOLD = 0;

    private static final long serialVersionUID = 1L;

    private boolean absoluteValue = false;

    private String arrayName = null;

    // what proportion of links to keep (possibly subject to FWE statistical significance threshold). 1.0 means keep
    // everything. 0.01 means 1%.
    private double cdfCut = 0.01;

    private boolean checkCorrelationDistribution = true;

    // only used for internal cache during calculations.
    private double correlationCacheThreshold = 0.8;

    // family-wise error rate threshold we use to select links
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

    public String getArrayName() {
        return arrayName;
    }

    public double getCdfCut() {
        return cdfCut;
    }

    public double getCorrelationCacheThreshold() {
        return correlationCacheThreshold;
    }

    public double getFwe() {
        return fwe;
    }

    public double getLowerTailCut() {
        return lowerTailCut;
    }

    public String getMetric() {
        return metric;
    }

    public int getMinNumPresent() {
        return minNumPresent;
    }

    /**
     * @return the normalizationMethod
     */
    public NormalizationMethod getNormalizationMethod() {
        return normalizationMethod;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public Integer getProbeDegreeThreshold() {
        return this.probeDegreeThreshold;
    }

    /**
     * @return the singularThreshold
     */
    public SingularThreshold getSingularThreshold() {
        return singularThreshold;
    }

    public double getSubsetSize() {
        return subsetSize;
    }

    public double getUpperTailCut() {
        return upperTailCut;
    }

    public boolean isAbsoluteValue() {
        return absoluteValue;
    }

    public boolean isCheckCorrelationDistribution() {
        return this.checkCorrelationDistribution;
    }

    /**
     * @return the lowerCdfCutUsed
     */
    public boolean isLowerCdfCutUsed() {
        return lowerCdfCutUsed;
    }

    public boolean isMakeSampleCorrMatImages() {
        return makeSampleCorrMatImages;
    }

    /**
     * @return the omitNegLinks
     */
    public boolean isOmitNegLinks() {
        return omitNegLinks;
    }

    public boolean isSubset() {
        return subset;
    }

    public boolean isSubsetUsed() {
        return subsetUsed;
    }

    public boolean isTextOut() {
        return textOut;
    }

    /**
     * @return the upperCdfCutUsed
     */
    public boolean isUpperCdfCutUsed() {
        return upperCdfCutUsed;
    }

    public boolean isUseDb() {
        return useDb;
    }

    public void setAbsoluteValue( boolean absoluteValue ) {
        this.absoluteValue = absoluteValue;
    }

    public void setArrayName( String arrayName ) {
        this.arrayName = arrayName;
    }

    public void setCdfCut( double cdfCut ) {
        this.cdfCut = cdfCut;
    }

    public void setCheckCorrelationDistribution( boolean checkCorrelationDistribution ) {
        this.checkCorrelationDistribution = checkCorrelationDistribution;
    }

    public void setCorrelationCacheThreshold( double correlationCacheThreshold ) {
        this.correlationCacheThreshold = correlationCacheThreshold;
    }

    public void setFwe( double fwe ) {
        this.fwe = fwe;
    }

    /**
     * @param lowerCdfCutUsed the lowerCdfCutUsed to set
     */
    public void setLowerCdfCutUsed( boolean lowerCdfCutUsed ) {
        this.lowerCdfCutUsed = lowerCdfCutUsed;
    }

    public void setLowerTailCut( double lowerTailCut ) {
        this.lowerTailCut = lowerTailCut;
    }

    public void setMakeSampleCorrMatImages( boolean makeSampleCorrMatImages ) {
        this.makeSampleCorrMatImages = makeSampleCorrMatImages;
    }

    public void setMetric( String metric ) {
        checkValidMetric( metric );
        this.metric = metric;
    }

    public void setMinNumPresent( int minNumPresent ) {
        this.minNumPresent = minNumPresent;
    }

    /**
     * @param normalizationMethod the normalizationMethod to set
     */
    public void setNormalizationMethod( NormalizationMethod normalizationMethod ) {
        this.normalizationMethod = normalizationMethod;
    }

    /**
     * @param omitNegLinks the omitNegLinks to set
     */
    public void setOmitNegLinks( boolean omitNegLinks ) {
        this.omitNegLinks = omitNegLinks;
    }

    public void setOutputFile( File outputFile ) {
        this.outputFile = outputFile;
    }

    /**
     * Probe degree threshold: Probes with more than this number of links are ignored. If set to <= 0, this setting is
     * ignored.
     * 
     * @param probeDegreeThreshold the probeDegreeThreshold to set
     */
    public void setProbeDegreeThreshold( int probeDegreeThreshold ) {
        this.probeDegreeThreshold = probeDegreeThreshold;
    }

    /**
     * Set to modify threshold behaviour: enforce the choice of only one of the two standard thresholds.
     * 
     * @param singularThreshold the singularThreshold to set. Default is 'none'.
     */
    public void setSingularThreshold( SingularThreshold singularThreshold ) {
        this.singularThreshold = singularThreshold;
    }

    public void setSubset( boolean subset ) {
        this.subset = subset;
    }

    public void setSubsetSize( double subsetSize ) {
        this.subsetSize = subsetSize;
    }

    public void setSubsetUsed( boolean subsetUsed ) {
        this.subsetUsed = subsetUsed;
    }

    public void setTextOut( boolean b ) {
        this.textOut = b;
    }

    /**
     * @param upperCdfCutUsed the upperCdfCutUsed to set
     */
    public void setUpperCdfCutUsed( boolean upperCdfCutUsed ) {
        this.upperCdfCutUsed = upperCdfCutUsed;
    }

    public void setUpperTailCut( double upperTailCut ) {
        this.upperTailCut = upperTailCut;
    }

    public void setUseDb( boolean useDb ) {
        this.useDb = useDb;
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
        buf.append( "# absoluteValue:" + this.isAbsoluteValue() + "\n" );
        buf.append( "# metric:" + this.getMetric() + "\n" );
        buf.append( "# cdfCut:" + this.getCdfCut() + "\n" );
        buf.append( "# cacheCut:" + this.getCorrelationCacheThreshold() + "\n" );
        buf.append( "# fwe:" + this.getFwe() + "\n" );
        buf.append( "# uppercut:" + this.getUpperTailCut() + "\n" );
        buf.append( "# lowercut:" + this.getLowerTailCut() + "\n" );
        buf.append( "# useDB:" + this.isUseDb() + "\n" );
        buf.append( "# normalizationMethod:" + this.getNormalizationMethod() + "\n" );
        buf.append( "# omitNegLinks:" + this.isOmitNegLinks() + "\n" );
        buf.append( "# probeDegreeThreshold:" + this.getProbeDegreeThreshold() + "\n" );
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
        buf.append( "# singularThreshold:" + this.getSingularThreshold() + "\n" );
        return buf.toString();
    }

    private void checkValidMetric( String m ) {
        if ( m.equalsIgnoreCase( "pearson" ) ) return;
        if ( m.equalsIgnoreCase( "spearman" ) ) return;
        throw new IllegalArgumentException( "Unrecognized metric: " + m
                + ", valid options are 'pearson' and 'spearman'" );
    }
}
