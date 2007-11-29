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
package ubic.gemma.analysis.linkAnalysis;

/**
 * Holds parameters needed for LinkAnalysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class LinkAnalysisConfig {

    private double upperTailCut = 0.01;
    private double lowerTailCut = 0.01;

    private String metric = "pearson"; // spearman
    private boolean absoluteValue = false;
    private double fwe = 0.01;
    private double cdfCut = 0.01; // 1.0 means, keep everything.

    private boolean useDb = true;

    private double correlationCacheThreshold = 0.8;
    private boolean textOut;

    public double getCorrelationCacheThreshold() {
        return correlationCacheThreshold;
    }

    public void setCorrelationCacheThreshold( double correlationCacheThreshold ) {
        this.correlationCacheThreshold = correlationCacheThreshold;
    }

    public boolean isUseDb() {
        return useDb;
    }

    public void setUseDb( boolean useDb ) {
        this.useDb = useDb;
    }

    public double getLowerTailCut() {
        return lowerTailCut;
    }

    public void setLowerTailCut( double lowerTailCut ) {
        this.lowerTailCut = lowerTailCut;
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

    public double getCdfCut() {
        return cdfCut;
    }

    public void setCdfCut( double cdfCut ) {
        this.cdfCut = cdfCut;
    }

    public double getFwe() {
        return fwe;
    }

    public void setFwe( double fwe ) {
        this.fwe = fwe;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric( String metric ) {
        checkValidMetric( metric );
        this.metric = metric;
    }

    private void checkValidMetric( String m ) {
        if ( m.equalsIgnoreCase( "pearson" ) ) return;
        if ( m.equalsIgnoreCase( "spearman" ) ) return;
        throw new IllegalArgumentException( "Unrecognized metric: " + m
                + ", valid options are 'pearson' and 'spearman'" );
    }

    public void setTextOut( boolean b ) {
        this.textOut = b;
    }

    public boolean isTextOut() {
        return textOut;
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
        return buf.toString();
    }
}
