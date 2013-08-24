/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

public class OutlierDetectionTestDetails {

    private String experiment;

    /* The threshold value at which no (more) outliers were found */
    private double lastThreshold;

    /* The number of factors 'regressed out' */
    private int numSigFactors;

    private int numExpFactors;

    /* The total number of outliers */
    private int numOutliers;

    private int numSamples;

    /* The number of samples that have already been removed as outliers */
    private int numRemoved;

    private int numOutliersByBasicAlgorithm;

    private int numOutliersByMedian;

    private Collection<ExperimentalFactor> significantFactors;

    private Collection<OutlierDetails> outliers;

    public OutlierDetectionTestDetails( String experiment ) {
        this.experiment = experiment;
        this.numExpFactors = 0; // should I change these to e.g. -1?
        this.numSigFactors = 0;
        this.numSamples = 0;
        this.numOutliers = 0;
        this.numOutliersByBasicAlgorithm = 0;
        this.numOutliersByMedian = 0;
        this.numRemoved = 0;
        this.significantFactors = new HashSet<ExperimentalFactor>();
        this.outliers = new ArrayList<OutlierDetails>();
    }

    public String getExperimentName() {
        return this.experiment;
    }

    public void setLastThreshold( double threshold ) {
        this.lastThreshold = threshold;
    }

    public double getLastThreshold() {
        return lastThreshold;
    }

    public void setSignificantFactors( Collection<ExperimentalFactor> significantFactors ) {
        this.significantFactors.addAll( significantFactors );
    }

    public Collection<ExperimentalFactor> getSignificantFactors() {
        return significantFactors;
    }

    public void setNumSigFactors( int n ) {
        this.numSigFactors = n;
    }

    public int getNumSigFactors() {
        return numSigFactors;
    }

    public void setNumExpFactors( int n ) {
        this.numExpFactors = n;
    }

    public int getNumExpFactors() {
        return this.numExpFactors;
    }

    public void setNumOutliers( int n ) {
        this.numOutliers = n;
    }

    public int getNumOutliers() {
        return numOutliers;
    }

    public void setNumSamples( int n ) {
        this.numSamples = n;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public void setNumRemoved( int n ) {
        this.numRemoved = n;
    }

    public int getNumRemoved() {
        return numRemoved;
    }

    public void setNumOutliersByBasicAlgorithm( int n ) {
        this.numOutliersByBasicAlgorithm = n;
    }

    public int getNumOutliersBasicAlgorithm() {
        return numOutliersByBasicAlgorithm;
    }

    public void setNumOutliersByMedian( int n ) {
        this.numOutliersByMedian = n;
    }

    public int getNumOutliersByMedian() {
        return numOutliersByMedian;
    }

    public void setOutliers( Collection<OutlierDetails> outliers ) {
        this.outliers.addAll( outliers );
    }

    public Collection<OutlierDetails> getOutliers() {
        return this.outliers;
    }

}
