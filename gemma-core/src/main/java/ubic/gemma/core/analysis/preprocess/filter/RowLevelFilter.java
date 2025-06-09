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
class RowLevelFilter implements Filter<ExpressionDataDoubleMatrix> {

    private static final Log log = LogFactory.getLog( RowLevelFilter.class.getName() );

    private boolean removeAllNegative = false;
    private Method method = Method.MAX;
    private double lowCut = -Double.MAX_VALUE;
    private double highCut = Double.MAX_VALUE;
    private boolean useLowAsFraction = false;
    private boolean useHighAsFraction = false;
    @Nullable
    private Map<CompositeSequence, Double> ranks = null;
    private double tolerance = Constants.SMALL;

    public RowLevelFilter() {
    }

    /**
     * @param ranks Map of rank values in range 0...1
     */
    public RowLevelFilter( Map<CompositeSequence, Double> ranks ) {
        this.ranks = ranks;
        this.useLowAsFraction = true;
        this.useHighAsFraction = true;
        this.method = Method.RANK;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix data ) {

        if ( lowCut == -Double.MAX_VALUE && highCut == Double.MAX_VALUE ) {
            RowLevelFilter.log.info( "No filtering requested" );
            return data;
        }

        int numRows = data.rows();
        DoubleArrayList criteria = new DoubleArrayList( new double[numRows] );

        int numAllNeg = this.computeCriteria( data, criteria );

        DoubleArrayList sortedCriteria = criteria.copy();
        sortedCriteria.sort();

        int consideredRows = numRows;
        int startIndex = 0;
        if ( removeAllNegative ) {
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

        this.logInfo( numRows, kept );

        return new ExpressionDataDoubleMatrix( data, kept );
    }

    /**
     * Set the high threshold for removal. If not set, no filtering will occur.
     *
     * @param h the threshold
     */
    public void setHighCut( double h ) {
        highCut = h;
    }

    /**
     * Set the low threshold for removal.
     *
     * @param lowCut the threshold
     */
    public void setLowCut( double lowCut ) {
        this.lowCut = lowCut;
    }

    public void setLowCut( double lowCut, boolean isFraction ) {
        this.setLowCut( lowCut );
        this.setUseLowCutAsFraction( isFraction );
        useLowAsFraction = isFraction;
    }

    /**
     * Choose the method that will be used for filtering. Default is 'MAX'. Those rows with the lowest values are
     * removed during 'low' filtering.
     *
     * @param method one of the filtering method constants.
     */
    public void setMethod( Method method ) {
        this.method = method;
    }

    /**
     * Set the filter to remove all rows that have only negative values. This is applied BEFORE applying fraction-based
     * criteria. In other words, if you request filtering 0.5 of the values, and 0.5 have all negative values, you will
     * get 0.25 of the data back. Default = false.
     *
     * @param t boolean
     */
    public void setRemoveAllNegative( boolean t ) {
        if ( t ) {
            RowLevelFilter.log.info( "Rows with all negative values will be "
                    + "removed PRIOR TO applying fraction-based criteria" );
        }
        removeAllNegative = t;
    }

    /**
     * Set the filter to interpret the low and high cuts as fractions; that is, if true, lowcut 0.1 means remove 0.1 of
     * the rows with the lowest values. Otherwise the cuts are interpeted as actual values. Default = false.
     *
     * @param setting boolean
     */
    public void setUseAsFraction( boolean setting ) {
        this.setUseHighCutAsFraction( setting );
        this.setUseLowCutAsFraction( setting );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setUseHighCutAsFraction( boolean setting ) {
        if ( setting && !Stats.isValidFraction( highCut ) ) {
            highCut = 0.0; // temporary, user sets this later, we hope.
        }
        useHighAsFraction = setting;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setUseLowCutAsFraction( boolean setting ) {
        if ( setting && !Stats.isValidFraction( lowCut ) ) {
            lowCut = 1.0; // temporary, use sets this later, we hope.
        }
        useLowAsFraction = setting;
    }

    /**
     * Set the value considered to be an insignificant difference between two numbers. Default is Constants.SMALL. Used
     * by DISTINCTVALUE filter.
     * <p>
     * Changed to ignore NAs in distinct value counting mode. All the other methods already did that.
     *
     * @param tolerance tolerance
     */
    public void setTolerance( Double tolerance ) {
        this.tolerance = Math.abs( tolerance );
    }

    private void addCriterion( DoubleArrayList criteria, DoubleArrayList rowAsList, CompositeSequence designElement,
            int i ) {
        switch ( method ) {
            case RANK: {
                assert ranks != null;
                if ( ranks.containsKey( designElement ) ) {
                    criteria.set( i, ranks.get( designElement ) );
                } else {
                    throw new IllegalStateException( "No rank was provided for " + designElement
                            + "; only ProcessedDataVectors have ranks, are you sure you are using the right data?" );
                }
                break;
            }
            case MIN: {
                criteria.set( i, DescriptiveWithMissing.min( rowAsList ) );
                break;
            }
            case MAX: {
                criteria.set( i, DescriptiveWithMissing.max( rowAsList ) );
                break;
            }
            case MEAN: {
                criteria.set( i, DescriptiveWithMissing.mean( rowAsList ) );
                break;
            }
            case MEDIAN: {
                criteria.set( i, DescriptiveWithMissing.median( rowAsList ) );
                break;
            }
            case RANGE: {
                criteria.set( i, Stats.range( rowAsList ) );
                break;
            }
            case CV: {
                criteria.set( i, Stats.cv( rowAsList ) );
                break;
            }
            case VAR: {
                criteria.set( i, DescriptiveWithMissing.variance( rowAsList ) );
                break;
            }
            case DISTINCTVALUES: {
                criteria.set( i,
                        Stats.fractionDistinctValuesNonNA( rowAsList, this.tolerance ) );
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public String toString() {
        return String.format( "%s Method=%s Low=%f%s High=%f%s Tolerance=%f%s",
                "RowLevelFilter",
                method,
                useLowAsFraction ? 100 * lowCut : lowCut, useLowAsFraction ? "%" : "",
                useLowAsFraction ? 100 * highCut : highCut, useHighAsFraction ? "%" : "",
                tolerance,
                removeAllNegative ? " [Drop Negatives]" : "" );
    }

    /**
     *
     * @param data to be inspected
     * @param criteria will contain the computed criteria on return
     * @return as a side-product, the number of rows having all negative values
     */
    private int computeCriteria( ExpressionDataDoubleMatrix data, DoubleArrayList criteria ) {
        int numRows = data.rows();
        int numCols = data.columns();

        /*
         * compute criteria.
         */
        DoubleArrayList rowAsList = new DoubleArrayList( new double[numCols] );
        int numAllNeg = 0;
        for ( int i = 0; i < numRows; i++ ) {
            double[] row = data.getRowAsDoubles( i );
            int numNeg = 0;
            /* stupid, copy into a DoubleArrayList so we can do stats */
            for ( int j = 0; j < numCols; j++ ) {
                double item = row[j];
                if ( Double.isNaN( item ) )
                    rowAsList.set( j, Double.NaN ); // previously: we set to zero! Just leave it and use "stats with missing" classes.
                else
                    rowAsList.set( j, item );

                if ( item < 0.0 || Double.isNaN( item ) ) {
                    numNeg++;
                }
            }
            if ( numNeg == numCols ) {
                numAllNeg++;
            }

//            String elementName = data.getRowNames().get( i ).getName();
//            if ( elementName.equals( "102636238" ) ) {
//                log.info( "foo" );
//            }

            this.addCriterion( criteria, rowAsList, data.getDesignElementForRow( i ), i );
        }
        return numAllNeg;
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

    private void logInfo( int numRows, List<CompositeSequence> kept ) {
        if ( kept.isEmpty() ) {
            RowLevelFilter.log.warn( "All rows filtered out!" );
            return;
        }

        if ( RowLevelFilter.log.isDebugEnabled() ) {
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits( 2 );

            double fracFiltered = ( double ) ( numRows - kept.size() ) / numRows;

            RowLevelFilter.log
                    .debug( "There are " + kept.size() + " rows left after " + this.method + " filtering. Filtered out "
                            + ( numRows - kept.size() ) + " rows " + nf.format( 100 * fracFiltered ) + "%" );
        }
    }

    public enum Method {
        RANK, MIN, MAX, MEDIAN, MEAN, RANGE, CV, VAR, DISTINCTVALUES
    }
}