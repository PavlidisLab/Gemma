/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess.filter;

import cern.colt.list.DoubleArrayList;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.basecode.math.Constants;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Stats;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filter data at the row-level.
 * <p>
 * This is a low-level filter utility meant to be used by other filters.
 * @author pavlidis
 */
@Setter
class RowLevelFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static final Log log = LogFactory.getLog( RowLevelFilter.class.getName() );

    private final Method method;
    @Nullable
    private Map<CompositeSequence, Double> ranks = null;
    /**
     * Low threshold for removal, exclusive.
     * <p>
     * Rows equal or below this will be removed.
     */
    private double lowCut = Double.NEGATIVE_INFINITY;
    /**
     * High threshold for removal, inclusive.
     * <p>
     * Rows strictly higher than this will be removed.
     */
    private double highCut = Double.POSITIVE_INFINITY;
    private boolean useLowAsFraction = false;
    private boolean useHighAsFraction = false;
    /**
     * Set the filter to remove all rows that have only negative values. This is applied BEFORE applying fraction-based
     * criteria. In other words, if you request filtering 0.5 of the values, and 0.5 have all negative values, you will
     * get 0.25 of the data back. Default = false.
     */
    private boolean removeAllNegative = false;
    /**
     * Value considered to be an insignificant difference between two numbers. Default is {@link Constants#SMALL}. Used
     * by the {@link Method#DISTINCTVALUES} method.
     * <p>
     * Changed to ignore NAs in distinct value counting mode. All the other methods already did that.
     */
    private double tolerance = Constants.SMALL;

    /**
     * Create a filter that will use the specified method to compute criteria for each row.
     * @param method the method that will be used for filtering. Those rows with the lowest values are removed during
     *              'low' filtering.
     */
    public RowLevelFilter( Method method ) {
        this.method = method;
        if ( method == Method.RANK ) {
            this.lowCut = 0.0;
            this.useLowAsFraction = true;
            this.highCut = 1.0;
            this.useHighAsFraction = true;
        }
    }

    /**
     * @param ranks Map of rank values in range 0...1
     */
    public RowLevelFilter( Map<CompositeSequence, Double> ranks ) {
        this( Method.RANK );
        this.ranks = ranks;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix data ) {
        if ( lowCut == Double.NEGATIVE_INFINITY && highCut == Double.POSITIVE_INFINITY ) {
            RowLevelFilter.log.info( "No filtering requested" );
            return data;
        }

        int numRows = data.rows();
        int numAllNeg = computeAllNegatives( data );
        DoubleArrayList criteria = this.computeCriteria( data );
        DoubleArrayList sortedCriteria = criteria.copy();
        sortedCriteria.sort();

        int consideredRows = numRows;
        int startIndex = 0;
        if ( removeAllNegative ) {
            RowLevelFilter.log.info( "Rows with all negative values will be removed PRIOR TO applying fraction-based criteria" );
            consideredRows = numRows - numAllNeg;
            startIndex = numAllNeg;
        }

        double realHighCut = this.getHighThreshold( sortedCriteria, consideredRows );
        double realLowCut = this.getLowThreshold( numRows, sortedCriteria, consideredRows, startIndex );

        if ( Double.isNaN( realHighCut ) ) {
            throw new IllegalStateException( "High threshold cut is NaN" );
        }

        RowLevelFilter.log.debug( "Low cut = " + realLowCut );
        RowLevelFilter.log.debug( "High cut = " + realHighCut );

        if ( realHighCut <= realLowCut ) {
            throw new RuntimeException( "High cut " + realHighCut + " is lower or same as low cut " + realLowCut );
        }

        List<CompositeSequence> kept = new ArrayList<>();

        for ( int i = 0; i < numRows; i++ ) {
            // greater than but not equal to realLowCut to account for case when realLowCut = 0 with many ties in
            // values, zeros should always be removed
            if ( criteria.get( i ) > realLowCut && criteria.get( i ) <= realHighCut ) {
                kept.add( data.getDesignElementForRow( i ) );
            }
        }

        if ( RowLevelFilter.log.isDebugEnabled() ) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits( 2 );
            double fracFiltered = ( double ) ( numRows - kept.size() ) / numRows;
            RowLevelFilter.log.debug( String.format( "There are %d rows left after %s filtering. Filtered out %d rows %s%%",
                    kept.size(), this.method, numRows - kept.size(), nf.format( 100 * fracFiltered ) ) );
        }

        return data.sliceRows( kept );
    }

    public void setRanks( @Nullable Map<CompositeSequence, Double> ranks ) {
        Assert.state( ranks == null || this.method == Method.RANK );
        this.ranks = ranks;
    }

    /**
     * Set the high threshold for removal and whether it should be interpreted as a fraction of the rows.
     *
     * @param highCut    the threshold, inclusive
     * @param isFraction indicate if the threshold applies to a faction of the rows or an actual value
     */
    public void setHighCut( double highCut, boolean isFraction ) {
        Assert.isTrue( !isFraction || Stats.isValidFraction( highCut ) );
        setHighCut( highCut );
        setUseHighCutAsFraction( isFraction );
    }

    /**
     * Set the low threshold for removal and whether it should be interpreted as a fraction of the rows.
     * @param lowCut     the threshold, exclusive
     * @param isFraction indicate if the threshold applies to a faction of the rows or an actual value
     */
    public void setLowCut( double lowCut, boolean isFraction ) {
        Assert.isTrue( !isFraction || Stats.isValidFraction( lowCut ) );
        setLowCut( lowCut );
        setUseLowCutAsFraction( isFraction );
    }

    /**
     * Set the filter to interpret the low and high cuts as fractions; that is, if true, lowcut 0.1 means remove 0.1 of
     * the rows with the lowest values. Otherwise the cuts are interpeted as actual values. Default = false.
     *
     * @param isFraction boolean
     */
    public void setUseAsFraction( boolean isFraction ) {
        this.setUseHighCutAsFraction( isFraction );
        this.setUseLowCutAsFraction( isFraction );
    }

    public void setUseHighCutAsFraction( boolean isFraction ) {
        if ( isFraction && !Stats.isValidFraction( highCut ) ) {
            highCut = 0.0; // temporary, user sets this later, we hope.
        }
        useHighAsFraction = isFraction;
    }

    public void setUseLowCutAsFraction( boolean isFraction ) {
        if ( isFraction && !Stats.isValidFraction( lowCut ) ) {
            lowCut = 1.0; // temporary, use sets this later, we hope.
        }
        useLowAsFraction = isFraction;
    }

    public void setTolerance( double tolerance ) {
        this.tolerance = Math.abs( tolerance );
    }

    @Override
    public String toString() {
        return String.format( "%s Method=%s Low=%f%s High=%f%s Tolerance=%f%s", "RowLevelFilter", method, useLowAsFraction ? 100 * lowCut : lowCut, useLowAsFraction ? "%" : "", useLowAsFraction ? 100 * highCut : highCut, useHighAsFraction ? "%" : "", tolerance, removeAllNegative ? " [Drop Negatives]" : "" );
    }

    /**
     * Compute the number of rows that have all negative values.
     * <p>
     * Missing values are ignored.
     */
    private int computeAllNegatives( ExpressionDataDoubleMatrix data ) {
        int numRows = data.rows();
        int numAllNeg = 0;
        for ( int i = 0; i < numRows; i++ ) {
            double[] row = data.getRowAsDoubles( i );
            if ( allNegative( row ) ) {
                numAllNeg++;
            }
        }
        return numAllNeg;
    }

    private boolean allNegative( double[] row ) {
        for ( double v : row ) {
            if ( v > 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute the value of the criteria for each row.
     */
    private DoubleArrayList computeCriteria( ExpressionDataDoubleMatrix data ) {
        int numRows = data.rows();
        DoubleArrayList criteria = new DoubleArrayList( numRows );
        DoubleArrayList rowAsList = null;
        for ( int i = 0; i < numRows; i++ ) {
            if ( rowAsList == null ) {
                rowAsList = new DoubleArrayList( data.getRowAsDoubles( i ) );
            } else {
                rowAsList.elements( data.getRowAsDoubles( i ) );
            }
            this.addCriterion( criteria, rowAsList, data.getDesignElementForRow( i ) );
        }
        return criteria;
    }

    private void addCriterion( DoubleArrayList criteria, DoubleArrayList rowAsList, CompositeSequence designElement ) {
        switch ( method ) {
            case RANK:
                assert ranks != null;
                if ( ranks.containsKey( designElement ) ) {
                    criteria.add( ranks.get( designElement ) );
                } else {
                    throw new IllegalStateException( "No rank was provided for " + designElement + "; only ProcessedDataVectors have ranks, are you sure you are using the right data?" );
                }
                break;
            case MIN:
                criteria.add( DescriptiveWithMissing.min( rowAsList ) );
                break;
            case MAX:
                criteria.add( DescriptiveWithMissing.max( rowAsList ) );
                break;
            case MEAN:
                criteria.add( DescriptiveWithMissing.mean( rowAsList ) );
                break;
            case MEDIAN:
                criteria.add( DescriptiveWithMissing.median( rowAsList ) );
                break;
            case RANGE:
                criteria.add( Stats.range( rowAsList ) );
                break;
            case CV:
                criteria.add( Stats.cv( rowAsList ) );
                break;
            case VAR:
                criteria.add( DescriptiveWithMissing.variance( rowAsList ) );
                break;
            case DISTINCTVALUES:
                criteria.add( Stats.fractionDistinctValuesNonNA( rowAsList, this.tolerance ) );
                break;
            default:
                throw new UnsupportedOperationException( "Unknown method: " + method );
        }
    }

    private double getHighThreshold( DoubleArrayList sortedCriteria, int consideredRows ) {
        double realHighCut;
        if ( useHighAsFraction ) {
            if ( !Stats.isValidFraction( highCut ) ) {
                throw new IllegalStateException( "High level cut must be a fraction between 0 and 1" );
            }
            int thresholdIndex;
            thresholdIndex = ( int ) Math.ceil( consideredRows * ( 1.0 - highCut ) ) - 1;

            thresholdIndex = Math.max( 0, thresholdIndex );
            realHighCut = sortedCriteria.get( thresholdIndex );
        } else {
            realHighCut = highCut;
        }
        return realHighCut;
    }

    private double getLowThreshold( int numRows, DoubleArrayList sortedCriteria, int consideredRows, int startIndex ) {
        double realLowCut;
        if ( useLowAsFraction ) {
            if ( !Stats.isValidFraction( lowCut ) ) {
                throw new IllegalStateException( "Low level cut must be a fraction between 0 and 1" );
            }

            int thresholdIndex;
            thresholdIndex = startIndex + ( int ) Math.floor( consideredRows * lowCut );
            thresholdIndex = Math.min( numRows - 1, thresholdIndex );
            realLowCut = sortedCriteria.get( thresholdIndex );
        } else {
            realLowCut = lowCut;
        }
        return realLowCut;
    }

    public enum Method {
        RANK, MIN, MAX, MEDIAN, MEAN, RANGE, CV, VAR, DISTINCTVALUES
    }
}