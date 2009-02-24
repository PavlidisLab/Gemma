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

import java.io.Serializable;

import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;

/**
 * Holds parameters needed for LinkAnalysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisConfig implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public enum NormalizationMethod {
        none, SPELL, SVD
    }

    private double upperTailCut = 0.01;
    private double lowerTailCut = 0.01;

    private String metric = "pearson"; // spearman
    private boolean absoluteValue = false;
    private double fwe = 0.01;
    private double cdfCut = 0.01; // 1.0 means, keep everything.
    private double subsetSize = 0.0;

    private boolean subset = false;
    private boolean subsetUsed = false;
    private boolean knownGenesOnly = false;
    private boolean useDb = true;
    private boolean makeSampleCorrMatImages = true;

    private NormalizationMethod normalizationMethod = NormalizationMethod.none;

    /*
     * Remove negative correlated values at the end.
     */
    private boolean omitNegLinks = false;

    private double correlationCacheThreshold = 0.8;
    private boolean textOut;

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

    public double getUpperTailCut() {
        return upperTailCut;
    }

    public boolean isAbsoluteValue() {
        return absoluteValue;
    }

    public boolean isKnownGenesOnly() {
        return knownGenesOnly;
    }

    public boolean isTextOut() {
        return textOut;
    }

    public boolean isUseDb() {
        return useDb;
    }

    public void setAbsoluteValue( boolean absoluteValue ) {
        this.absoluteValue = absoluteValue;
    }

    public void setCdfCut( double cdfCut ) {
        this.cdfCut = cdfCut;
    }

    public void setCorrelationCacheThreshold( double correlationCacheThreshold ) {
        this.correlationCacheThreshold = correlationCacheThreshold;
    }

    public void setFwe( double fwe ) {
        this.fwe = fwe;
    }

    public void setKnownGenesOnly( boolean knownGenesOnly ) {
        this.knownGenesOnly = knownGenesOnly;
    }

    public void setLowerTailCut( double lowerTailCut ) {
        this.lowerTailCut = lowerTailCut;
    }

    public void setMetric( String metric ) {
        checkValidMetric( metric );
        this.metric = metric;
    }

    public void setTextOut( boolean b ) {
        this.textOut = b;
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
    public ProbeCoexpressionAnalysis toAnalysis() {
        ProbeCoexpressionAnalysis analysis = ProbeCoexpressionAnalysis.Factory.newInstance();
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
        buf.append( "# knownGenesOnly:" + this.isKnownGenesOnly() + "\n" );
        buf.append( "# normalizationMethod:" + this.getNormalizationMethod() + "\n" );
        buf.append( "# omitNegLinks:" + this.isOmitNegLinks() + "\n" );
        if(this.isSubsetUsed()){
            buf.append( "# subset:" + this.subsetSize + "\n" );
        }
        
        return buf.toString();
    }

    /**
     * @return
     */
    public boolean useKnownGenesOnly() {
        return knownGenesOnly;
    }

    private void checkValidMetric( String m ) {
        if ( m.equalsIgnoreCase( "pearson" ) ) return;
        if ( m.equalsIgnoreCase( "spearman" ) ) return;
        throw new IllegalArgumentException( "Unrecognized metric: " + m
                + ", valid options are 'pearson' and 'spearman'" );
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

    public double getSubsetSize() {
        return subsetSize;
    }

    public void setSubsetSize( double subsetSize ) {
        this.subsetSize = subsetSize;
    }
    
    public void setSubset( boolean subset ) {
        this.subset = subset;
    }

    public boolean isSubset(){
        return subset;
    }

    public boolean isSubsetUsed() {
        return subsetUsed;
    }

    public void setSubsetUsed( boolean subsetUsed ) {
        this.subsetUsed = subsetUsed;
    }
}
