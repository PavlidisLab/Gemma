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

    private double upperTailCut;
    private double lowerTailCut;

    private String metric = "pearson";
    private boolean absoluteValue = false;
    private double fwe = 0.01;
    private double cdfCut = 0.01; // 1.0 means, keep everything.

    private boolean useDb;

    private double correlationCacheThreshold;

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
        this.metric = metric;
    }

}
