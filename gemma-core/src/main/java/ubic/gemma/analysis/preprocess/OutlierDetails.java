/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Container for details about a proposed outlier
 * 
 * @author paul
 * @version $Id$
 */
public class OutlierDetails {

    private BioAssay bioAssay;

    private double score = 0.0;

    private double thresholdCorrelation;

    /**
     * @param bioAssay
     * @param score fraction of correlations this bioAssay has that are lower than the threshold
     * @param thresholdCorrelation correlation at the quantile that was set.
     */
    public OutlierDetails( BioAssay bioAssay, double score, double thresholdCorrelation ) {
        super();
        this.bioAssay = bioAssay;
        this.score = score;
        this.thresholdCorrelation = thresholdCorrelation;
    }

    public double getThresholdCorrelation() {
        return thresholdCorrelation;
    }

    public void setThresholdCorrelation( double thresholdCorrelation ) {
        this.thresholdCorrelation = thresholdCorrelation;
    }

    public BioAssay getBioAssay() {
        return bioAssay;
    }

    public double getScore() {
        return score;
    }

    public void setOutlierScore( double score ) {
        this.score = score;
    }

}
