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
package ubic.gemma.analysis.preprocess.batcheffects;

//import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.DesignMatrix;
import ubic.basecode.math.LeastSquaresFit;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;

/**
 * An implementation of the ComBat algorithm described by Johson et al ({@link http://jlab.byu.edu/ComBat/Download.html}
 * ), as described in:
 * <p>
 * Johnson, WE, Rabinovic, A, and Li, C (2007). Adjusting batch effects in microarray expression data using Empirical
 * Bayes methods. Biostatistics 8(1):118-127.
 */
public class ComBat<R, C> {

    private static final String BATCH_COLUMN_NAME = "batch";

    private static Log log = LogFactory.getLog( ComBat.class );

    private final ObjectMatrix<C, String, Object> sampleInfo;
    private final DoubleMatrix<R, C> data;
    private Algebra solver;

    private DoubleMatrix2D varpooled;

    private DoubleMatrix2D standMean;

    private LinkedHashMap<String, Collection<C>> batches;

    private HashMap<String, Map<C, Integer>> originalLocationsInMatrix;

    private int numSamples;

    private int numBatches;

    private boolean hasMissing = false;

    private int numProbes;

    /**
     * Prior distribution
     */
    private DoubleMatrix2D deltaHat;

    /**
     * The data matrix
     */
    private DoubleMatrix2D y;

    /**
     * The design matrix
     */
    private DoubleMatrix2D X;

    public ComBat( DoubleMatrix<R, C> data, ObjectMatrix<C, String, Object> sampleInfo ) {

        if ( data.columns() < 4 ) {
            throw new IllegalArgumentException( "Cannot run ComBat with fewer than 4 samples" );
        }

        this.data = data;
        this.sampleInfo = sampleInfo;
        solver = new Algebra();
        y = new DenseDoubleMatrix2D( data.asArray() );
        initPartA();
        // DesignMatrix dm = new DesignMatrix( sampleInfo );
        // this.X = dm.getMatrix();
        X = computeDesignMatrix();
    }

    public DoubleMatrix2D getDesignMatrix() {
        return this.X;
    }

    /**
     * ComBat parameterizes the model without an intercept, instead using all possible columns for batch (what the heck
     * do you call this parameterization?) This is important. Each batch has its own parameter.
     * 
     * @param sampleInfo
     * @return
     */
    private DoubleMatrix2D computeDesignMatrix() {
        DoubleMatrix2D design = null;
        /*
         * Find the batch
         */DesignMatrix d = null;
        int batchFactorColumnIndex = sampleInfo.getColIndexByName( BATCH_COLUMN_NAME );
        Object[] batchFactor = sampleInfo.getColumn( batchFactorColumnIndex );
        if ( batchFactor != null ) {
            List<String> batchFactorL = new ArrayList<String>();
            for ( Object s : batchFactor ) {
                batchFactorL.add( ( String ) s );
            }

            d = new DesignMatrix( batchFactor, 1, BATCH_COLUMN_NAME );

        }
        if ( d == null ) {
            throw new IllegalStateException( "No batch factor was found" );
        }
        ObjectMatrix<String, String, Object> sampleInfoWithoutBatchFactor = getSampleInfoWithoutBatchFactor( batchFactorColumnIndex );

        d.add( sampleInfoWithoutBatchFactor );
        design = d.getDoubleMatrix();
        return design;
    }

    /**
     * @param batchFactorColumnIndex
     * @return
     */
    private ObjectMatrix<String, String, Object> getSampleInfoWithoutBatchFactor( int batchFactorColumnIndex ) {
        ObjectMatrix<String, String, Object> sampleInfoWithoutBatchFactor = new ObjectMatrixImpl<String, String, Object>(
                sampleInfo.rows(), sampleInfo.columns() - 1 );

        int r = 0;
        for ( int i = 0; i < sampleInfo.rows(); i++ ) {
            int c = 0;
            for ( int j = 0; j < sampleInfo.columns(); j++ ) {
                if ( j == batchFactorColumnIndex ) continue;
                if ( i == 0 ) {
                    sampleInfoWithoutBatchFactor.addColumnName( sampleInfo.getColName( j ) );
                }
                sampleInfoWithoutBatchFactor.set( r, c++, sampleInfo.get( i, j ) );
            }
            r++;
        }
        return sampleInfoWithoutBatchFactor;
    }

    /**
     * @param data
     * @param sampleInfo
     * @return
     */
    public DoubleMatrix2D run() {

        StopWatch timer = new StopWatch();
        timer.start();

        DoubleMatrix2D sdata = standardize( y, X );

        checkForProblems( sdata );

        if ( timer.getTime() > 1000 ) {
            log.info( "Standardized" );
        }
        timer.reset();
        timer.start();

        DoubleMatrix2D gammaHat = gammaHat( sdata );

        deltaHat( sdata );
        // assertEquals( 1.618, deltaHat.get( 0, 0 ), 0.001 );

        // gamma.bar <- apply(gamma.hat, 1, mean)
        DoubleArrayList gammaBar = new DoubleArrayList();
        DoubleArrayList t2 = new DoubleArrayList();
        for ( int batchIndex = 0; batchIndex < gammaHat.rows(); batchIndex++ ) {
            double mean = DescriptiveWithMissing.mean( new DoubleArrayList( gammaHat.viewRow( batchIndex ).toArray() ) );
            gammaBar.add( mean );
            t2.add( DescriptiveWithMissing.sampleVariance( new DoubleArrayList( gammaHat.viewRow( batchIndex )
                    .toArray() ), mean ) );
        }

        // assertEquals( -0.092144, gammaBar.get( 0 ), 0.001 );
        // assertEquals( 0.2977, t2.get( 1 ), 0.001 );

        DoubleArrayList aPrior = aPrior( deltaHat );
        DoubleArrayList bPrior = bPrior( deltaHat );

        if ( timer.getTime() > 1000 ) {
            log.info( "Computed priors" );
        }

        // assertEquals( 17.4971, aPrior.get( 0 ), 0.0001 );
        // assertEquals( 4.514, bPrior.get( 1 ), 0.0001 );

        int batchIndex = 0;
        DoubleMatrix2D gammastar = new DenseDoubleMatrix2D( numBatches, numProbes );
        DoubleMatrix2D deltastar = new DenseDoubleMatrix2D( numBatches, numProbes );

        for ( String batchId : batches.keySet() ) {
            timer.reset();
            timer.start();
            DoubleMatrix2D batchData = this.getBatchData( sdata, batchId );

            DoubleMatrix1D[] batchResults = itSol( batchData, gammaHat.viewRow( batchIndex ), deltaHat
                    .viewRow( batchIndex ), gammaBar.get( batchIndex ), t2.get( batchIndex ), aPrior.get( batchIndex ),
                    bPrior.get( batchIndex ) );

            for ( int j = 0; j < batchResults[0].size(); j++ ) {
                gammastar.set( batchIndex, j, batchResults[0].get( j ) );
            }
            for ( int j = 0; j < batchResults[1].size(); j++ ) {
                deltastar.set( batchIndex, j, batchResults[1].get( j ) );
            }
            batchIndex++;
            if ( timer.getTime() > 1000 ) {
                log.info( "Corrected batch " + batchIndex );
            }
        }

        timer.reset();
        timer.start();

        DoubleMatrix2D adjustedData = rawAdjust( sdata, gammastar, deltastar );

        // assertEquals( -0.95099, adjustedData.get( 18, 0 ), 0.0001 );
        // assertEquals( -0.30273984, adjustedData.get( 14, 6 ), 0.0001 );
        // assertEquals( 0.2097977, adjustedData.get( 7, 3 ), 0.0001 );
        // log.info( adjustedData );
        DoubleMatrix2D result = restoreScale( adjustedData );
        if ( timer.getTime() > 1000 ) {
            log.info( "Done" );
        }
        return result;
    }

    /**
     * Check sdata for problems. If the design is not of full rank, we get NaN in standardized data.
     */
    private void checkForProblems( DoubleMatrix2D sdata ) {
        int numMissing = 0;
        int total = 0;
        for ( int i = 0; i < sdata.rows(); i++ ) {
            DoubleMatrix1D row = sdata.viewRow( i );
            for ( int j = 0; j < sdata.columns(); j++ ) {
                if ( Double.isNaN( row.getQuick( j ) ) ) {
                    numMissing++;
                }
                total++;
            }
        }

        if ( total == numMissing ) {
            throw new IllegalStateException( "Could not complete batch correction due, model must not be of full rank." );
        }
    }

    /**
     * @param d
     * @return
     */
    private DoubleArrayList aPrior( DoubleMatrix2D d ) {
        DoubleArrayList result = new DoubleArrayList();
        for ( int i = 0; i < d.rows(); i++ ) {
            DoubleArrayList dd = new DoubleArrayList( d.viewRow( i ).toArray() );
            double mean = DescriptiveWithMissing.mean( dd );
            double var = DescriptiveWithMissing.sampleVariance( dd, mean );

            result.add( ( 2.0 * var + Math.pow( mean, 2 ) ) / var );
        }
        return result;
    }

    /**
     * @param d
     * @return
     */
    private DoubleArrayList bPrior( DoubleMatrix2D d ) {
        DoubleArrayList result = new DoubleArrayList();
        for ( int i = 0; i < d.rows(); i++ ) {
            DoubleArrayList dd = new DoubleArrayList( d.viewRow( i ).toArray() );
            double mean = DescriptiveWithMissing.mean( dd );

            double var = DescriptiveWithMissing.sampleVariance( dd, mean );
            result.add( ( mean * var + Math.pow( mean, 3 ) ) / var );
        }
        return result;
    }

    /**
     * @param X
     * @param sdata
     * @return
     */
    private DoubleMatrix2D gammaHat( DoubleMatrix2D sdata ) {

        DoubleMatrix2D Xb = X.viewPart( 0, 0, X.rows(), numBatches );
        DoubleMatrix2D gammaHat = new LeastSquaresFit( Xb, sdata ).getCoefficients();

        // assertEquals( -0.6671, gammaHat.get( 0, 0 ), 0.0001 );
        return gammaHat;
    }

    /**
     * @param sdata data to be sliced
     * @param batchId which batch
     * @param testMatrix needed to figure out which rows we need -- assumes sdata is in same order.
     * @param batches batch information
     */
    private DoubleMatrix2D getBatchData( DoubleMatrix2D sdata, String batchId ) {
        Collection<C> sampleNames = batches.get( batchId );

        DoubleMatrix2D result = new DenseDoubleMatrix2D( sdata.rows(), sampleNames.size() );

        int i = 0;
        for ( C sname : sampleNames ) {
            DoubleMatrix1D colInBatch = sdata.viewColumn( data.getColIndexByName( sname ) );
            for ( int k = 0; k < colInBatch.size(); k++ ) {
                result.set( k, i, colInBatch.get( k ) );
            }
            i++;
        }
        // log.info( result );
        return result;
    }

    /**
     * @param X
     * @param batchId
     * @param testMatrix
     * @param batches
     * @return
     */
    private DoubleMatrix2D getBatchDesign( String batchId ) {
        Collection<C> sampleNames = batches.get( batchId );

        DoubleMatrix2D result = new DenseDoubleMatrix2D( sampleNames.size(), batches.keySet().size() );

        for ( int j = 0; j < batches.keySet().size(); j++ ) {
            int i = 0;

            for ( C sname : sampleNames ) {
                DoubleMatrix1D rowInBatch = X.viewRow( data.getColIndexByName( sname ) );
                result.set( i, j, rowInBatch.get( j ) );
                i++;
            }
        }
        // log.info( result );
        return result;
    }

    /**
     * 
     */
    private void initPartA() {
        numSamples = sampleInfo.rows();

        for ( int i = 0; i < data.rows(); i++ ) {
            for ( int j = 0; j < data.columns(); j++ ) {
                if ( data.isMissing( i, j ) ) {
                    this.hasMissing = true;
                    break;
                }
            }
        }

        int batchColumnIndex = sampleInfo.getColIndexByName( BATCH_COLUMN_NAME );
        batches = new LinkedHashMap<String, Collection<C>>();
        originalLocationsInMatrix = new HashMap<String, Map<C, Integer>>();
        for ( int i = 0; i < numSamples; i++ ) {
            C sampleName = sampleInfo.getRowName( i );
            String batchId = ( String ) sampleInfo.get( i, batchColumnIndex );
            if ( !batches.containsKey( batchId ) ) {
                batches.put( batchId, new ArrayList<C>() );
                originalLocationsInMatrix.put( batchId, new LinkedHashMap<C, Integer>() );
            }
            batches.get( batchId ).add( sampleName );

            originalLocationsInMatrix.get( batchId ).put( sampleName, i );

        }

        /*
         * Make sure all batches have at least 2 samples, or else this won't work.
         */
        for ( String batchId : batches.keySet() ) {
            if ( batches.get( batchId ).size() < 2 ) {
                throw new IllegalArgumentException(
                        "Batch correction not possible with less than 2 samples in any batch. Consider combining batches." );
            }
        }

        numBatches = batches.keySet().size();
        numProbes = y.rows();
    }

    /**
     * @param matrix
     * @param gHat
     * @param dHat
     * @param gbar
     * @param t2
     * @param a
     * @param b
     * @return
     */
    private DoubleMatrix1D[] itSol( DoubleMatrix2D matrix, DoubleMatrix1D gHat, DoubleMatrix1D dHat, double gbar,
            double t2, double a, double b ) {

        DoubleMatrix1D n = rowNonMissingCounts( matrix );
        DoubleMatrix1D gold = gHat;
        DoubleMatrix1D dold = dHat;
        double conv = 0.0001;
        double change = 1.0;
        int count = 0;

        int MAXITERS = 200;

        while ( change > conv ) {
            DoubleMatrix1D gnew = postMean( gHat, gbar, n, dold, t2 );
            DoubleMatrix1D sum2 = stepSum( matrix, gnew );
            DoubleMatrix1D dnew = postVar( sum2, n, a, b );

            DoubleMatrix1D gnewtmp = gnew.copy().assign( gold, Functions.minus ).assign( Functions.abs ).assign( gold,
                    Functions.div );

            DoubleMatrix1D dnewtmp = dnew.copy().assign( dold, Functions.minus ).assign( Functions.abs ).assign( dold,
                    Functions.div );
            double gnewmax = 0.0;
            double dnewmax = 0.0;
            if ( hasMissing ) {
                gnewmax = DescriptiveWithMissing.max( new DoubleArrayList( gnewtmp.toArray() ) );
                dnewmax = DescriptiveWithMissing.max( new DoubleArrayList( dnewtmp.toArray() ) );
            } else {
                gnewmax = gnewtmp.aggregate( Functions.max, Functions.identity );
                dnewmax = dnewtmp.aggregate( Functions.max, Functions.identity );
            }

            change = Math.max( gnewmax, dnewmax );
            // System.err.println( count + " " + gnewmax + " " + dnewmax + " " + change );

            gold = gnew;
            dold = dnew;
            count++;

            if ( count > MAXITERS ) {
                /*
                 * For certain data sets, we just flail around; for example if there are only two samples. We really
                 * shouldn't end up running so this is a bailout for exceptional circumstances.
                 */
                throw new IllegalStateException( "Failed to converge" );
            }
        }

        return new DoubleMatrix1D[] { gold, dold };
    }

    /**
     * The method used by ComBat is veerrryyy slow, and I don't know how many cases it will prove to be necessary.
     * Probably it can be sped up computing corrections for each batch in parallel, and maybe Java is just plain faster.
     * 
     * @param matrix
     * @param gHat
     * @param dHat
     * @return
     */
    @SuppressWarnings("unused")
    private DoubleMatrix1D[] nonParametricFit( DoubleMatrix2D matrix, DoubleMatrix1D gHat, DoubleMatrix1D dHat ) {
        throw new UnsupportedOperationException( "This is too slow to want to implement yet." );
    }

    /**
     * @param ghat
     * @param gbar
     * @param n
     * @param dstar
     * @param t2
     * @return
     */
    private DoubleMatrix1D postMean( DoubleMatrix1D ghat, double gbar, DoubleMatrix1D n, DoubleMatrix1D dstar, double t2 ) {
        DoubleMatrix1D result = new DenseDoubleMatrix1D( ghat.size() );
        for ( int i = 0; i < ghat.size(); i++ ) {
            result.set( i, ( t2 * n.get( i ) * ghat.get( i ) + dstar.get( i ) * gbar )
                    / ( t2 * n.get( i ) + dstar.get( i ) ) );
        }
        return result;
    }

    /**
     * @param sum2
     * @param n
     * @param a
     * @param b
     * @return
     */
    private DoubleMatrix1D postVar( DoubleMatrix1D sum2, DoubleMatrix1D n, double a, double b ) {
        DoubleMatrix1D result = new DenseDoubleMatrix1D( sum2.size() );
        for ( int i = 0; i < sum2.size(); i++ ) {
            result.set( i, ( 0.5 * sum2.get( i ) + b ) / ( n.get( i ) / 2.0 + a - 1.0 ) );
        }
        return result;
    }

    private DoubleMatrix2D rawAdjust( DoubleMatrix2D sdata, DoubleMatrix2D gammastar, DoubleMatrix2D deltastar ) {
        int batchIndex;
        int batchNum = 0;

        DoubleMatrix2D adjustedData = new DenseDoubleMatrix2D( sdata.rows(), sdata.columns() );

        for ( String batchId : batches.keySet() ) {
            DoubleMatrix2D batchData = this.getBatchData( sdata, batchId );

            DoubleMatrix2D Xbb = this.getBatchDesign( batchId );

            DoubleMatrix2D adjustedBatch = batchData.copy().assign( solver.transpose( solver.mult( Xbb, gammastar ) ),
                    Functions.minus );

            DoubleMatrix1D deltaStarRow = deltastar.viewRow( batchNum );
            deltaStarRow.assign( Functions.sqrt );

            DoubleMatrix1D ones = new DenseDoubleMatrix1D( batchData.columns() );
            ones.assign( 1.0 );
            DoubleMatrix2D divisor = solver.multOuter( deltaStarRow, ones, null );

            adjustedBatch.assign( divisor, Functions.div );

            /*
             * Now we have to put the data back in the right order -- the batches are all together.
             */

            Map<C, Integer> locations = originalLocationsInMatrix.get( batchId );
            for ( batchIndex = 0; batchIndex < adjustedBatch.rows(); batchIndex++ ) {
                int j = 0;
                for ( Integer index : locations.values() ) {
                    adjustedData.set( batchIndex, index, adjustedBatch.get( batchIndex, j ) );
                    j++;
                }
            }

            batchNum++;
        }
        return adjustedData;
    }

    private DoubleMatrix2D restoreScale( DoubleMatrix2D adjustedData ) {
        DoubleMatrix2D ones = new DenseDoubleMatrix2D( 1, numSamples );
        ones.assign( 1.0 );
        DoubleMatrix2D adj = solver.mult( varpooled.copy().assign( Functions.sqrt ), ones );
        DoubleMatrix2D varRestore = adjustedData.assign( adj, Functions.mult );
        // log.info( varRestore );
        DoubleMatrix2D finalResult = varRestore.assign( standMean, Functions.plus );
        return finalResult;
    }

    /**
     * @param matrix
     * @return
     */
    private DoubleMatrix1D rowNonMissingCounts( DoubleMatrix2D matrix ) {
        DoubleMatrix1D result = new DenseDoubleMatrix1D( matrix.rows() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            result.set( i, DescriptiveWithMissing.sizeWithoutMissingValues( new DoubleArrayList( matrix.viewRow( i )
                    .toArray() ) ) );
        }
        return result;
    }

    /**
     * @param matrix
     * @param gnew
     * @return
     */
    private DoubleMatrix1D stepSum( DoubleMatrix2D matrix, DoubleMatrix1D gnew ) {

        Algebra s = new Algebra();

        DoubleMatrix2D g = new DenseDoubleMatrix2D( 1, gnew.size() );
        for ( int i = 0; i < gnew.size(); i++ ) {
            g.set( 0, i, gnew.get( i ) );
        }

        DoubleMatrix2D a = new DenseDoubleMatrix2D( 1, matrix.columns() );
        a.assign( 1.0 );

        /*
         * subtract column gnew from each column of data; square; then sum over each row.
         */

        DoubleMatrix2D deltas = matrix.copy().assign( ( s.mult( s.transpose( g ), a ) ), Functions.minus ).assign(
                Functions.square );
        DoubleMatrix1D sumsq = new DenseDoubleMatrix1D( deltas.rows() );
        sumsq.assign( 0.0 );

        for ( int i = 0; i < deltas.rows(); i++ ) {
            sumsq.set( i, DescriptiveWithMissing.sum( new DoubleArrayList( deltas.viewRow( i ).toArray() ) ) );
        }
        return sumsq;
    }

    /**
     * @param sdata
     */
    protected void deltaHat( DoubleMatrix2D sdata ) {
        int batchIndex;
        deltaHat = new DenseDoubleMatrix2D( numBatches, numProbes );
        batchIndex = 0;
        for ( String batchId : batches.keySet() ) {
            DoubleMatrix2D batchData = getBatchData( sdata, batchId );
            for ( int j = 0; j < batchData.rows(); j++ ) {
                DoubleArrayList row = new DoubleArrayList( batchData.viewRow( j ).toArray() );
                double variance = DescriptiveWithMissing.sampleVariance( row, DescriptiveWithMissing.mean( row ) );
                deltaHat.set( batchIndex, j, variance );
            }
            batchIndex++;
        }
    }

    /**
     * Special standardization: partial regression of covariates
     * 
     * @param b
     * @param A
     * @return
     */
    protected DoubleMatrix2D standardize( DoubleMatrix2D b, DoubleMatrix2D A ) {

        DoubleMatrix2D beta = new LeastSquaresFit( A, b ).getCoefficients();

        // assertEquals( 3.7805, beta.get( 0, 0 ), 0.001 );
        // assertEquals( 0.0541, beta.get( 2, 18 ), 0.001 );

        int batchIndex = 0;
        DoubleMatrix2D bba = new DenseDoubleMatrix2D( 1, numBatches );
        for ( String batchId : batches.keySet() ) {
            bba.set( 0, batchIndex++, ( double ) batches.get( batchId ).size() / numSamples );
        }

        /*
         * Weight the non-batch coefficients by the batch sizes.
         */
        DoubleMatrix2D grandMeanM = solver.mult( bba, beta.viewPart( 0, 0, numBatches, beta.columns() ) );

        // assertEquals( 5.8134, grandMeanM.get( 0, 1 ), 0.001 );

        if ( hasMissing ) {
            varpooled = y.copy().assign( solver.transpose( solver.mult( X, beta ) ), Functions.minus );
            DoubleMatrix2D var = new DenseDoubleMatrix2D( varpooled.rows(), 1 );
            for ( int i = 0; i < varpooled.rows(); i++ ) {
                DoubleMatrix1D row = varpooled.viewRow( i );
                double m = DescriptiveWithMissing.mean( new DoubleArrayList( row.toArray() ) );
                double v = DescriptiveWithMissing.sampleVariance( new DoubleArrayList( row.toArray() ), m );
                var.set( i, 0, v );
            }
            varpooled = var;
        } else {
            varpooled = y.copy().assign( solver.transpose( solver.mult( X, beta ) ), Functions.minus ).assign(
                    Functions.pow( 2 ) );
            DoubleMatrix2D scale = new DenseDoubleMatrix2D( numSamples, 1 );
            scale.assign( 1.0 / numSamples );
            varpooled = solver.mult( varpooled, scale );
        }

        DoubleMatrix2D size = new DenseDoubleMatrix2D( numSamples, 1 );
        size.assign( 1.0 );

        /*
         * The coefficients repeated for each sample.
         */
        standMean = solver.mult( solver.transpose( grandMeanM ), solver.transpose( size ) );

        /*
         * Erase the batch factors from a copy of the design matrix
         */
        DoubleMatrix2D tmpX = X.copy();
        for ( batchIndex = 0; batchIndex < numBatches; batchIndex++ ) {
            for ( int j = 0; j < X.rows(); j++ ) {
                tmpX.set( j, batchIndex, 0.0 );
            }
        }

        /*
         * row means, adjusted "per group", and ignoring batch effects.
         */
        standMean = standMean.assign( solver.transpose( solver.mult( tmpX, beta ) ), Functions.plus );

        DoubleMatrix2D varsq = solver.mult( varpooled.copy().assign( Functions.sqrt ), solver.transpose( size ) );

        /*
         * Subtract the mean and divide by the standard deviations.
         */
        DoubleMatrix2D meansubtracted = y.copy().assign( standMean, Functions.minus );
        DoubleMatrix2D sdata = meansubtracted.assign( varsq, Functions.div );
        return sdata;
    }

}
