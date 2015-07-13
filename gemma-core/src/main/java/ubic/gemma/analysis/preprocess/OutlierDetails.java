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

import java.util.Comparator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Container for details about a proposed outlier
 * 
 * @author paul
 * @version $Id$
 */
public class OutlierDetails {

    /**
     * Compare outliers by first quartile Note: this comparator imposes orderings that are inconsistent with equals
     */
    public static Comparator<OutlierDetails> FirstQuartileComparator = new Comparator<OutlierDetails>() {

        @Override
        public int compare( OutlierDetails o1, OutlierDetails o2 ) {
            return Double.compare( o2.getFirstQuartile(), o2.getFirstQuartile() );
        }
    };

    /**
     * Compare outliers by median correlation Note: this comparator imposes orderings that are inconsistent with equals
     */
    public static Comparator<OutlierDetails> MedianComparator = new Comparator<OutlierDetails>() {

        @Override
        public int compare( OutlierDetails o1, OutlierDetails o2 ) {

            return Double.compare( o1.getMedianCorrelation(), o2.getMedianCorrelation() );
        }
    };

    /**
     * Compare outliers by third quartile Note: this comparator imposes orderings that are inconsistent with equals
     */
    public static Comparator<OutlierDetails> ThirdQuartileComparator = new Comparator<OutlierDetails>() {
        @Override
        public int compare( OutlierDetails o1, OutlierDetails o2 ) {
            return Double.compare( o1.getThirdQuartile(), o2.getThirdQuartile() );
        }
    };
    private BioAssay bioAssay;
    private Long bioAssayId;

    private double firstQuartile = Double.MIN_VALUE;

    private double median = Double.MIN_VALUE;

    private double score = Double.MIN_VALUE;

    private double thirdQuartile = Double.MIN_VALUE;

    private double thresholdCorrelation = Double.MIN_VALUE;

    public OutlierDetails( BioAssay bioAssay ) {
        super();
        this.bioAssay = bioAssay;
        this.bioAssayId = bioAssay.getId();
    }

    /**
     * Alternative constructor to be used when detecting outliers by median correlation value
     * 
     * @param bioAssay
     * @param medianCorrelation the median correlation value
     */
    public OutlierDetails( BioAssay bioAssay, double medianCorrelation ) {
        super();
        this.bioAssay = bioAssay;
        this.bioAssayId = bioAssay.getId();

        this.median = medianCorrelation;
    }

    /**
     * @param bioAssay
     * @param score fraction of correlations this bioAssay has that are lower than the threshold
     * @param thresholdCorrelation correlation at the quantile that was set.
     */
    public OutlierDetails( BioAssay bioAssay, double score, double thresholdCorrelation ) {
        super();
        this.bioAssay = bioAssay;
        this.bioAssayId = bioAssay.getId();

        this.score = score;
        this.thresholdCorrelation = thresholdCorrelation;
    }

    @Override
    public boolean equals( Object obj ) {

        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( !( obj instanceof OutlierDetails ) ) return false;

        OutlierDetails outlier = ( OutlierDetails ) obj;
        return new EqualsBuilder().append( bioAssay, outlier.bioAssay ).isEquals();

    }

    public BioAssay getBioAssay() {
        return bioAssay;
    }

    public Long getBioAssayId() {
        return bioAssayId;
    }

    public double getFirstQuartile() {
        return firstQuartile;
    }

    public double getMedianCorrelation() {
        return median;
    }

    public double getScore() {
        return score;
    }

    public double getThirdQuartile() {
        return thirdQuartile;
    }

    public double getThresholdCorrelation() {
        return thresholdCorrelation;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 31 ).append( bioAssay ).toHashCode();
    }

    public void setFirstQuartile( double quartile ) {
        firstQuartile = quartile;
    }

    public void setMedianCorrelation( double medianCorrelation ) {
        this.median = medianCorrelation;
    }

    public void setOutlierScore( double score ) {
        this.score = score;
    }

    public void setThirdQuartile( double quartile ) {
        thirdQuartile = quartile;
    }

    public void setThresholdCorrelation( double thresholdCorrelation ) {
        this.thresholdCorrelation = thresholdCorrelation;
    }

}
