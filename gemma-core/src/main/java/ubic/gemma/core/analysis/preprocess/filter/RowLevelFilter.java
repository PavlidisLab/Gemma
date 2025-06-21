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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Filter data at the row-level.
 * <p>
 * This is a low-level filter utility meant to be used by other filters.
 * @author pavlidis
 */
class RowLevelFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static final Log log = LogFactory.getLog( RowLevelFilter.class.getName() );

    private final Method method;
    /**
     * Mapping of custom values for each design elements.
     */
    @Nullable
    private Function<CompositeSequence, Double> customMethod = null;
    /**
     * Low threshold for removal, inclusive.
     * <p>
     * Rows strictly below this will be removed.
     */
    private double lowCut;
    private boolean useLowAsFraction;
    /**
     * High threshold for removal, inclusive.
     * <p>
     * Rows strictly higher than this will be removed.
     */
    private double highCut;
    private boolean useHighAsFraction;
    /**
     * Value considered to be an insignificant difference between two numbers. Default is {@link Constants#SMALL}. Used
     * by the {@link Method#DISTINCT_VALUES} method.
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
        this.lowCut = method.minimumLowCut;
        this.highCut = method.maximumHighCut;
    }

    /**
     * @param customMethod a custom method
     */
    public RowLevelFilter( Function<CompositeSequence, Double> customMethod ) {
        this( Method.CUSTOM_METHOD );
        this.customMethod = customMethod;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix data ) {
        if ( useHighAsFraction && useLowAsFraction ) {
            Assert.state( highCut + lowCut < 1.0, "High cut and low cut must sum to less than 1.0." );
        } else if ( !useHighAsFraction && !useLowAsFraction ) {
            // when equal, we're basically filtering a single value up to the tolerance
            Assert.state( highCut >= lowCut, "High cut must be strictly greater or equal to the low cut." );
        }

        int numRows = data.rows();

        if ( numRows == 0 ) {
            log.info( "No rows to filter, returning the original matrix." );
            return data;
        }

        DoubleArrayList criteria = this.computeCriteria( data );
        DoubleArrayList sortedCriteria = criteria.copy();
        sortedCriteria.sort();
        int numValues = sortedCriteria.size();
        // NaNs values are sorted last
        for ( int i = 0; i < sortedCriteria.size(); i++ ) {
            if ( Double.isNaN( sortedCriteria.get( i ) ) ) {
                numValues = i;
                break;
            }
        }
        double realHighCut = getHighThreshold( sortedCriteria, numValues );
        double realLowCut = getLowThreshold( sortedCriteria, numValues );

        if ( Double.isNaN( realLowCut ) ) {
            throw new IllegalStateException( "Low threshold cut is NaN" );
        }

        if ( Double.isNaN( realHighCut ) ) {
            throw new IllegalStateException( "High threshold cut is NaN" );
        }

        RowLevelFilter.log.debug( "Number of values = " + numValues );
        RowLevelFilter.log.debug( "Low cut = " + realLowCut );
        RowLevelFilter.log.debug( "High cut = " + realHighCut );

        if ( realHighCut < realLowCut ) {
            // this can happen with we mix an absolute low cut with a fraction high cut or vice versa
            log.warn( String.format( "High cut (%f) is below the low cut (%f), no rows will be kept.", realHighCut, realLowCut ) );
            return data.sliceRows( Collections.emptyList() );
        }

        // quickly check if all rows are to be kept
        // note: we're not checking up to numValues, because we want to filter out rows with NaN values
        if ( sortedCriteria.isEmpty() || ( sortedCriteria.get( 0 ) >= realLowCut && sortedCriteria.get( sortedCriteria.size() - 1 ) <= realHighCut ) ) {
            log.info( "All rows are within the low/high threshold, returning the original matrix." );
            return data;
        }

        List<CompositeSequence> kept = new ArrayList<>();

        for ( int i = 0; i < numRows; i++ ) {
            // greater than but not equal to realLowCut to account for case when realLowCut = 0 with many ties in
            // values, zeros should always be removed
            if ( criteria.get( i ) >= realLowCut && criteria.get( i ) <= realHighCut ) {
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

    public void setCustomMethod( @Nullable Function<CompositeSequence, Double> customMethod ) {
        Assert.state( customMethod == null || this.method == Method.CUSTOM_METHOD,
                "A custom method can only be set for the " + Method.CUSTOM_METHOD + " filtering method." );
        this.customMethod = customMethod;
    }

    public void setHighCut( double highCut ) {
        Assert.isTrue( highCut <= method.maximumHighCut, "The high cut must be less or equal to " + method.maximumHighCut + "." );
        this.highCut = highCut;
        this.useHighAsFraction = false;
    }

    /**
     * Set the high threshold for removal interpreted as a fraction of the rows.
     * @param highCut the threshold, inclusive
     */
    public void setHighCutAsFraction( double highCut ) {
        Assert.isTrue( Stats.isValidFraction( highCut ) );
        this.highCut = highCut;
        this.useHighAsFraction = true;
    }

    public void setLowCut( double lowCut ) {
        Assert.isTrue( lowCut >= method.minimumLowCut, "The low cut must be greater or equal to " + method.minimumLowCut + "." );
        this.lowCut = lowCut;
        this.useLowAsFraction = false;
    }

    /**
     * Set the low threshold for removal interpreted as a fraction of the rows.
     * @param lowCut     the threshold, inclusive
     */
    public void setLowCutAsFraction( double lowCut ) {
        Assert.isTrue( Stats.isValidFraction( lowCut ) );
        this.lowCut = lowCut;
        this.useLowAsFraction = true;
    }

    public void setTolerance( double tolerance ) {
        Assert.isTrue( tolerance >= 0, "Tolerance must be a positive number." );
        this.tolerance = tolerance;
    }

    @Override
    public String toString() {
        return String.format( "%s Method=%s Low=%f%s High=%f%s Tolerance=%f",
                "RowLevelFilter",
                method,
                useLowAsFraction ? 100 * lowCut : lowCut, useLowAsFraction ? "%" : "",
                useLowAsFraction ? 100 * highCut : highCut, useHighAsFraction ? "%" : "",
                tolerance );
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
            case CUSTOM_METHOD:
                if ( customMethod == null ) {
                    throw new IllegalStateException( "Custom values must be provided for the CUSTOM_VALUE method." );
                }
                Double val = customMethod.apply( designElement );
                if ( val == null ) {
                    throw new IllegalStateException( "No value was provided for " + designElement + "." );
                }
                criteria.add( val );
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
            case DISTINCT_VALUES:
                criteria.add( Stats.fractionDistinctValuesNonNA( rowAsList, this.tolerance ) );
                break;
            default:
                throw new UnsupportedOperationException( "Unknown method: " + method );
        }
    }

    private double getHighThreshold( DoubleArrayList sortedCriteria, int numValues ) {
        double realHighCut;
        if ( useHighAsFraction ) {
            if ( !Stats.isValidFraction( highCut ) ) {
                throw new IllegalStateException( "High level cut must be a fraction between 0 and 1" );
            }
            int thresholdIndex = ( int ) Math.floor( numValues * ( 1.0 - highCut ) ) - 1;
            if ( thresholdIndex == -1 ) {
                return Double.NEGATIVE_INFINITY;
            } else {
                realHighCut = sortedCriteria.get( thresholdIndex );
            }
        } else {
            realHighCut = highCut;
        }
        return realHighCut;
    }

    private double getLowThreshold( DoubleArrayList sortedCriteria, int numValues ) {
        double realLowCut;
        if ( useLowAsFraction ) {
            if ( !Stats.isValidFraction( lowCut ) ) {
                throw new IllegalStateException( "Low level cut must be a fraction between 0 and 1" );
            }
            int thresholdIndex = ( int ) Math.ceil( numValues * lowCut );
            if ( thresholdIndex == numValues ) {
                return Double.POSITIVE_INFINITY;
            }
            realLowCut = sortedCriteria.get( thresholdIndex );
        } else {
            realLowCut = lowCut;
        }
        return realLowCut;
    }

    public enum Method {
        CUSTOM_METHOD( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY ),
        MIN, MAX, MEDIAN, MEAN,
        RANGE( 0, Double.POSITIVE_INFINITY ),
        CV( 0, Double.POSITIVE_INFINITY ),
        VAR( 0, Double.POSITIVE_INFINITY ),
        DISTINCT_VALUES( 0, 1 );

        /**
         * Minimum value for the low cut.
         */
        private final double minimumLowCut;
        /**
         * Maximum value for the high cut.
         */
        private final double maximumHighCut;

        Method() {
            this( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY );
        }

        Method( double minimumLowCut, double maximumHighCut ) {
            this.minimumLowCut = minimumLowCut;
            this.maximumHighCut = maximumHighCut;
        }
    }
}