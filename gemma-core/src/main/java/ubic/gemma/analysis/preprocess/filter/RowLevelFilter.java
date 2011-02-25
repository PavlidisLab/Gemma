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
package ubic.gemma.analysis.preprocess.filter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Stats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author pavlidis
 * @version $Id$
 */
public class RowLevelFilter implements Filter<ExpressionDataDoubleMatrix> {
    private static Log log = LogFactory.getLog( RowLevelFilter.class.getName() );
    private boolean removeAllNegative = false;
    private Method method = Method.MAX;
    protected double lowCut = -Double.MAX_VALUE;

    protected double highCut = Double.MAX_VALUE;

    protected boolean useLowAsFraction = false;

    protected boolean useHighAsFraction = false;
    private Map<CompositeSequence, Double> ranks = null;

    /**
     * @param ranks Map of rank values in range 0...1
     */
    public RowLevelFilter( Map<CompositeSequence, Double> ranks ) {
        this.ranks = ranks;
        this.setUseLowCutAsFraction( true );
        this.setUseHighCutAsFraction( true );
        this.setMethod( Method.RANK );
    }

    public RowLevelFilter() {
    }

    /**
     * @param data
     * @return
     */
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix data ) {

        if ( lowCut == -Double.MAX_VALUE && highCut == Double.MAX_VALUE ) {
            log.info( "No filtering requested" );
            return data;
        }

        int numRows = data.rows();
        DoubleArrayList criteria = new DoubleArrayList( new double[numRows] );

        int numAllNeg = computeCriteria( data, criteria );

        DoubleArrayList sortedCriteria = criteria.copy();
        sortedCriteria.sort();

        int consideredRows = numRows;
        int startIndex = 0;
        if ( removeAllNegative ) {
            consideredRows = numRows - numAllNeg;
            startIndex = numAllNeg;
        }

        double realHighCut = getHighThreshold( sortedCriteria, consideredRows );
        double realLowCut = getLowThreshold( numRows, sortedCriteria, consideredRows, startIndex );

        if ( Double.isNaN( realHighCut ) ) {
            throw new IllegalStateException( "High threshold cut is NaN" );
        }

        log.debug( "Low cut = " + realLowCut );
        log.debug( "High cut = " + realHighCut );

        if ( realHighCut <= realLowCut ) {
            throw new RuntimeException( "High cut " + realHighCut + " is lower or same as low cut " + realLowCut );
        }

        List<CompositeSequence> kept = new ArrayList<CompositeSequence>();

        for ( int i = 0; i < numRows; i++ ) {
            // greater than but not equal to realLowCut to account for case when realLowCut = 0 with many ties in
            // values,
            // zeros should always be removed
            if ( criteria.get( i ) > realLowCut && criteria.get( i ) <= realHighCut ) {
                kept.add( data.getDesignElementForRow( i ) );
            }
        }

        logInfo( numRows, kept );

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
     * @param highCut
     * @param isFraction
     */
    public void setHighCut( double highCut, boolean isFraction ) {
        setHighCut( highCut );
        setUseHighCutAsFraction( isFraction );
        useHighAsFraction = isFraction;
    }

    /**
     * Set the low threshold for removal.
     * 
     * @param lowCut the threshold
     */
    public void setLowCut( double lowCut ) {
        this.lowCut = lowCut;
    }

    /**
     * @param lowCut
     * @param isFraction
     */
    public void setLowCut( double lowCut, boolean isFraction ) {
        setLowCut( lowCut );
        setUseLowCutAsFraction( isFraction );
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
            log.info( "Rows with all negative values will be " + "removed PRIOR TO applying fraction-based criteria" );
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
        setUseHighCutAsFraction( setting );
        setUseLowCutAsFraction( setting );
    }

    /**
     * @param setting
     */
    public void setUseHighCutAsFraction( boolean setting ) {
        if ( setting == true && !Stats.isValidFraction( highCut ) ) {
            highCut = 0.0; // temporary, use sets this later, we hope.
        }
        useHighAsFraction = setting;
    }

    /**
     * @param setting
     */
    public void setUseLowCutAsFraction( boolean setting ) {
        if ( setting == true && !Stats.isValidFraction( lowCut ) ) {
            lowCut = 1.0; // temporary, use sets this later, we hope.
        }
        useLowAsFraction = setting;
    }

    /**
     * @param criteria
     * @param rowAsList
     * @param designElement
     * @param i
     */
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
                criteria.set( i, Descriptive.min( rowAsList ) );
                break;
            }
            case MAX: {
                criteria.set( i, Descriptive.max( rowAsList ) );
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
            default: {
                break;
            }
        }
    }

    /**
     * @param data
     * @param criteria
     * @return
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
            Double[] row = data.getRow( i );
            int numNeg = 0;
            /* stupid, copy into a DoubleArrayList so we can do stats */
            for ( int j = 0; j < numCols; j++ ) {
                double item = row[j].doubleValue();
                if ( Double.isNaN( item ) )
                    rowAsList.set( j, 0 );
                else
                    rowAsList.set( j, item );
                if ( item < 0.0 || Double.isNaN( item ) ) {
                    numNeg++;
                }
            }
            if ( numNeg == numCols ) {
                numAllNeg++;
            }

            addCriterion( criteria, rowAsList, data.getDesignElementForRow( i ), i );
        }
        return numAllNeg;
    }

    /**
     * @param sortedCriteria
     * @param consideredRows
     * @return
     */
    private double getHighThreshold( DoubleArrayList sortedCriteria, int consideredRows ) {
        double realHighCut;
        if ( useHighAsFraction ) {
            if ( !Stats.isValidFraction( highCut ) ) {
                throw new IllegalStateException( "High level cut must be a fraction between 0 and 1" );
            }
            int thresholdIndex = 0;
            thresholdIndex = ( int ) Math.ceil( consideredRows * ( 1.0 - highCut ) ) - 1;

            thresholdIndex = Math.max( 0, thresholdIndex );
            realHighCut = sortedCriteria.get( thresholdIndex );
        } else {
            realHighCut = highCut;
        }
        return realHighCut;
    }

    /**
     * @param numRows
     * @param sortedCriteria
     * @param consideredRows
     * @param startIndex
     * @return
     */
    private double getLowThreshold( int numRows, DoubleArrayList sortedCriteria, int consideredRows, int startIndex ) {
        double realLowCut;
        if ( useLowAsFraction ) {
            if ( !Stats.isValidFraction( lowCut ) ) {
                throw new IllegalStateException( "Low level cut must be a fraction between 0 and 1" );
            }

            int thresholdIndex = 0;
            thresholdIndex = startIndex + ( int ) Math.floor( consideredRows * lowCut );
            thresholdIndex = Math.min( numRows - 1, thresholdIndex );
            realLowCut = sortedCriteria.get( thresholdIndex );
        } else {
            realLowCut = lowCut;
        }
        return realLowCut;
    }

    /**
     * @param numRows
     * @param kept
     */
    private void logInfo( int numRows, List<CompositeSequence> kept ) {
        if ( kept.size() == 0 ) {
            log.warn( "All rows filtered out!" );
            return;
        }
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits( 2 );

        double fracFiltered = ( double ) ( numRows - kept.size() ) / numRows;

        log.info( "There are " + kept.size() + " rows left after " + this.method + " filtering. Filtered out "
                + ( numRows - kept.size() ) + " rows " + nf.format( 100 * fracFiltered ) + "%" );
    }

    public enum Method {
        RANK, MIN, MAX, MEDIAN, MEAN, RANGE, CV, VAR
    }
}