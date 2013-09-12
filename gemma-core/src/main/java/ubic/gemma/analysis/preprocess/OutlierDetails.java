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

    private BioAssay bioAssay;

    private double score = Double.MIN_VALUE;

    private double thresholdCorrelation = Double.MIN_VALUE;

    private double median = Double.MIN_VALUE;

    private double firstQuartile = Double.MIN_VALUE;

    private double thirdQuartile = Double.MIN_VALUE;

    public OutlierDetails( BioAssay bioAssay ) {
        super();
        this.bioAssay = bioAssay;
    }

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

    /**
     * Alternative constructor to be used when detecting outliers by median correlation value
     * 
     * @param bioAssay
     * @param medianCorrelation the median correlation value
     */
    public OutlierDetails( BioAssay bioAssay, double medianCorrelation ) {
        super();
        this.bioAssay = bioAssay;
        this.median = medianCorrelation;
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

    public double getMedianCorrelation() {
        return median;
    }

    public void setMedianCorrelation( double medianCorrelation ) {
        this.median = medianCorrelation;
    }

    public double getFirstQuartile() {
        return firstQuartile;
    }

    public void setFirstQuartile( double quartile ) {
        firstQuartile = quartile;
    }

    public double getThirdQuartile() {
        return thirdQuartile;
    }

    public void setThirdQuartile( double quartile ) {
        thirdQuartile = quartile;
    }

    /**
     * Compare outliers by median correlation Note: this comparator imposes orderings that are inconsistent with equals
     */
    @SuppressWarnings("rawtypes")
    public static Comparator MedianComparator = new Comparator() {

        @Override
        public int compare( Object o1, Object o2 ) {
            OutlierDetails outlier1 = ( OutlierDetails ) o1;
            OutlierDetails outlier2 = ( OutlierDetails ) o2;

            return Double.compare( outlier1.getMedianCorrelation(), outlier2.getMedianCorrelation() );
        }
    };

    /**
     * Compare outliers by first quartile Note: this comparator imposes orderings that are inconsistent with equals
     */
    @SuppressWarnings("rawtypes")
    public static Comparator FirstQuartileComparator = new Comparator() {

        @Override
        public int compare( Object o1, Object o2 ) {
            OutlierDetails outlier1 = ( OutlierDetails ) o1;
            OutlierDetails outlier2 = ( OutlierDetails ) o2;

            return Double.compare( outlier1.getFirstQuartile(), outlier2.getFirstQuartile() );
        }
    };

    /**
     * Compare outliers by third quartile Note: this comparator imposes orderings that are inconsistent with equals
     */
    @SuppressWarnings("rawtypes")
    public static Comparator ThirdQuartileComparator = new Comparator() {

        @Override
        public int compare( Object o1, Object o2 ) {
            OutlierDetails outlier1 = ( OutlierDetails ) o1;
            OutlierDetails outlier2 = ( OutlierDetails ) o2;

            return Double.compare( outlier1.getThirdQuartile(), outlier2.getThirdQuartile() );
        }
    };

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 31 ).append( bioAssay ).toHashCode();
    }

    @Override
    public boolean equals( Object obj ) {

        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( !( obj instanceof OutlierDetails ) ) return false;

        OutlierDetails outlier = ( OutlierDetails ) obj;
        return new EqualsBuilder().append( bioAssay, outlier.bioAssay ).isEquals();

    }

}
