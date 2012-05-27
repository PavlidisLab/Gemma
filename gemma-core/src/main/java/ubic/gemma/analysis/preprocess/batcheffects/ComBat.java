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

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.MatrixUtil;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.DesignMatrix;
import ubic.basecode.math.LeastSquaresFit;
import ubic.basecode.math.distribution.Histogram;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import cern.jet.random.Gamma;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;

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

    private final ObjectMatrix<C, String, ?> sampleInfo;
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
    private DoubleMatrix2D deltaHat = null;

    /**
     * The data matrix
     */
    private DoubleMatrix2D y = null;

    /**
     * The design matrix
     */
    private DoubleMatrix2D X = null;

    private DoubleArrayList aPrior = null;

    private DoubleArrayList bPrior = null;

    private DoubleMatrix2D gammaHat = null;

    private DoubleArrayList t2 = null;

    private DoubleArrayList gammaBar = null;

    public ComBat( DoubleMatrix<R, C> data, ObjectMatrix<C, String, ?> sampleInfo ) {

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
     * Make diagnostic plots.
     * <p>
     * TODO: save somewhere more reasonable than in the tmp directory.
     * <p>
     * FIXME: As in the original ComBat, this only graphs the first batch's statistics. In principle we can (and perhaps
     * should) examine these plots for all the batches.
     * 
     * @param filePrefix
     */
    public void plot( String filePrefix ) {

        if ( this.gammaHat == null ) throw new IllegalArgumentException( "You must call 'run' first" );

        /*
         * View the distribution of gammaHat, which we assume will have a normal distribution
         */
        DoubleMatrix1D ghr = gammaHat.viewRow( 0 );
        int NUM_HIST_BINS = 100;
        Histogram gammaHatHist = new Histogram( "GammaHat", NUM_HIST_BINS, ghr );
        XYSeries ghplot = gammaHatHist.plot();

        Normal rn = new Normal( this.gammaBar.get( 0 ), Math.sqrt( this.t2.get( 0 ) ), new MersenneTwister() );

        Histogram ghtheoryT = new Histogram( "Gamma", NUM_HIST_BINS, gammaHatHist.min(), gammaHatHist.max() );
        for ( int i = 0; i < 10000; i++ ) {
            double n = rn.nextDouble();
            ghtheoryT.fill( n );
        }
        XYSeries ghtheory = ghtheoryT.plot();

        OutputStream os = null;

        try {
            File tmpfile = File.createTempFile( filePrefix + ".gammahat.histogram.", ".png" );
            log.info( tmpfile );
            os = new FileOutputStream( tmpfile );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        writePlot( os, ghplot, ghtheory );

        /*
         * View the distribution of deltaHat, which we assume has an inverse gamma distribution
         */
        DoubleMatrix1D dhr = deltaHat.viewRow( 0 );
        Histogram deltaHatHist = new Histogram( "DeltaHat", NUM_HIST_BINS, dhr );
        XYSeries dhplot = deltaHatHist.plot();
        Gamma g = new Gamma( aPrior.get( 0 ), bPrior.get( 0 ), new MersenneTwister() );

        Histogram deltaHatT = new Histogram( "Delta", NUM_HIST_BINS, deltaHatHist.min(), deltaHatHist.max() );

        for ( int i = 0; i < 10000; i++ ) {
            double invg = 1.0 / g.nextDouble();
            deltaHatT.fill( invg );
        }
        XYSeries dhtheory = deltaHatT.plot();

        os = null;

        try {
            File tmpfile = File.createTempFile( filePrefix + ".deltahat.histogram.", ".png" );
            log.info( tmpfile );
            os = new FileOutputStream( tmpfile );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        writePlot( os, dhplot, dhtheory );
    }

    /**
     * @param os
     * @param empirical
     * @param theory
     */
    private void writePlot( OutputStream os, XYSeries empirical, XYSeries theory ) {
        // ChartFactory.setChartTheme( StandardChartTheme.createLegacyTheme() );
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.addSeries( empirical );
        xySeriesCollection.addSeries( theory );
        JFreeChart chart = ChartFactory.createXYLineChart( "", "Magnitude", "Density", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.getXYPlot().setRangeGridlinesVisible( false );
        chart.getXYPlot().setDomainGridlinesVisible( false );
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        renderer.setBasePaint( Color.white );

        try {
            int size = 500;
            ChartUtilities.writeChartAsPNG( os, chart, 500, size );
            os.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @return data corrected using parametric prior estimator
     * @throws ComBatException
     */
    public DoubleMatrix2D run() throws ComBatException {
        return this.run( true );
    }

    /**
     * @param parametric if false, use the non-parametric (slower) method for estimating the priors.
     * @return corrected data
     * @throws ComBatException
     */
    public DoubleMatrix2D run( boolean parametric ) throws ComBatException {

        StopWatch timer = new StopWatch();
        timer.start();

        final DoubleMatrix2D sdata = standardize( y, X );

        checkForProblems( sdata );

        if ( timer.getTime() > 1000 ) {
            log.info( "Standardized" );
        }
        timer.reset();
        timer.start();

        gammaHat( sdata );

        deltaHat( sdata );
        // assertEquals( 1.618, deltaHat.get( 0, 0 ), 0.001 );

        // gamma.bar <- apply(gamma.hat, 1, mean)
        gammaBar = new DoubleArrayList();
        t2 = new DoubleArrayList();
        for ( int batchIndex = 0; batchIndex < gammaHat.rows(); batchIndex++ ) {
            double mean = DescriptiveWithMissing.mean( new DoubleArrayList( gammaHat.viewRow( batchIndex ).toArray() ) );
            gammaBar.add( mean );
            t2.add( DescriptiveWithMissing.sampleVariance( new DoubleArrayList( gammaHat.viewRow( batchIndex )
                    .toArray() ), mean ) );
        }

        // assertEquals( -0.092144, gammaBar.get( 0 ), 0.001 );
        // assertEquals( 0.2977, t2.get( 1 ), 0.001 );

        aPrior = aPrior( deltaHat );
        bPrior = bPrior( deltaHat );

        if ( timer.getTime() > 1000 ) {
            log.info( "Computed priors" );
        }

        // assertEquals( 17.4971, aPrior.get( 0 ), 0.0001 );
        // assertEquals( 4.514, bPrior.get( 1 ), 0.0001 );

        DoubleMatrix2D gammastar = new DenseDoubleMatrix2D( numBatches, numProbes );
        DoubleMatrix2D deltastar = new DenseDoubleMatrix2D( numBatches, numProbes );

        if ( !parametric ) {
            runNonParametric( sdata, gammastar, deltastar );
        } else {
            runParametric( sdata, gammastar, deltastar );
        }

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
     * @param sdata
     * @param gammastar
     * @param deltastar
     */
    private void runParametric( final DoubleMatrix2D sdata, DoubleMatrix2D gammastar, DoubleMatrix2D deltastar ) {
        int batchIndex = 0;
        for ( String batchId : batches.keySet() ) {

            DoubleMatrix2D batchData = this.getBatchData( sdata, batchId );

            DoubleMatrix1D[] batchResults;

            batchResults = itSol( batchData, gammaHat.viewRow( batchIndex ), deltaHat.viewRow( batchIndex ),
                    gammaBar.get( batchIndex ), t2.get( batchIndex ), aPrior.get( batchIndex ), bPrior.get( batchIndex ) );

            for ( int j = 0; j < batchResults[0].size(); j++ ) {
                gammastar.set( batchIndex, j, batchResults[0].get( j ) );
            }
            for ( int j = 0; j < batchResults[1].size(); j++ ) {
                deltastar.set( batchIndex, j, batchResults[1].get( j ) );
            }
            batchIndex++;
        }
    }

    /**
     * Multithreaded
     * 
     * @param sdata
     * @param gammastar
     * @param deltastar
     */
    private void runNonParametric( final DoubleMatrix2D sdata, DoubleMatrix2D gammastar, DoubleMatrix2D deltastar ) {
        final ConcurrentHashMap<String, DoubleMatrix1D[]> results = new ConcurrentHashMap<String, DoubleMatrix1D[]>();
        int numThreads = Math.min( batches.size(), Runtime.getRuntime().availableProcessors() );

        log.info( "Runing nonparametric estimation on " + numThreads + " threads" );

        Future<?>[] futures = new Future[numThreads];
        ExecutorService service = Executors.newCachedThreadPool();

        /*
         * Divvy up batches over threads.
         */

        int batchesPerThread = batches.size() / numThreads;

        final String[] batchIds = batches.keySet().toArray( new String[] {} );

        for ( int i = 0; i < numThreads; i++ ) {

            final int firstBatch = i * batchesPerThread;
            final int lastBatch = i == ( numThreads - 1 ) ? batches.size() : firstBatch + batchesPerThread;

            futures[i] = service.submit( new Runnable() {
                @Override
                public void run() {
                    for ( int k = firstBatch; k < lastBatch; k++ ) {
                        String batchId = batchIds[k];
                        DoubleMatrix2D batchData = getBatchData( sdata, batchId );
                        DoubleMatrix1D[] batchResults = nonParametricFit( batchData, gammaHat.viewRow( k ),
                                deltaHat.viewRow( k ) );
                        results.put( batchId, batchResults );
                    }
                }
            } );
        }

        service.shutdown();

        boolean allDone = false;
        do {
            for ( Future<?> f : futures ) {
                allDone = true;
                if ( !f.isDone() && !f.isCancelled() ) {
                    allDone = false;
                    break;
                }
            }
        } while ( !allDone );

        for ( int i = 0; i < batchIds.length; i++ ) {
            String batchId = batchIds[i];
            DoubleMatrix1D[] batchResults = results.get( batchId );
            for ( int j = 0; j < batchResults[0].size(); j++ ) {
                gammastar.set( i, j, batchResults[0].get( j ) );
            }
            for ( int j = 0; j < batchResults[1].size(); j++ ) {
                deltastar.set( i, j, batchResults[1].get( j ) );
            }
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
     * Check sdata for problems. If the design is not of full rank, we get NaN in standardized data.
     * 
     * @param sdata
     * @throws ComBatException
     */
    private void checkForProblems( DoubleMatrix2D sdata ) throws ComBatException {
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
            /*
             * Alternative that can help in some cases: back out and drop factors. There are definitely strategies for
             * doing this (drop factors that have no major PC loadings, for example), but it might be bad to do this
             * "automagically".
             */
            throw new ComBatException( "Could not complete batch correction: model must not be of full rank." );
        }
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
     * @param sdata
     * @return
     */
    private void gammaHat( DoubleMatrix2D sdata ) {
        DoubleMatrix2D Xb = X.viewPart( 0, 0, X.rows(), numBatches );
        gammaHat = new LeastSquaresFit( Xb, sdata ).getCoefficients();
    }

    /**
     * @param sdata data to be sliced
     * @param batchId which batch
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
     * @param batchId
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
     * 
     */
    private void initPartA() {
        numSamples = sampleInfo.rows();

        /*
         * TODO: remove rows that have too many missing values, or do that earlier.
         */

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
     * @param t2b
     * @param a
     * @param b
     * @return
     * @thrwos ComBatException if convergence fails.
     */
    private DoubleMatrix1D[] itSol( DoubleMatrix2D matrix, DoubleMatrix1D gHat, DoubleMatrix1D dHat, double gbar,
            double t2b, double a, double b ) throws ComBatException {

        DoubleMatrix1D n = rowNonMissingCounts( matrix );
        DoubleMatrix1D gold = gHat;
        DoubleMatrix1D dold = dHat;
        final double conv = 0.0001;
        double change = 1.0;
        int count = 0;

        int MAXITERS = 500;

        while ( change > conv ) {
            DoubleMatrix1D gnew = postMean( gHat, gbar, n, dold, t2b );
            DoubleMatrix1D sum2 = stepSum( matrix, gnew );
            DoubleMatrix1D dnew = postVar( sum2, n, a, b );

            DoubleMatrix1D gnewtmp = gnew.copy().assign( gold, Functions.minus ).assign( Functions.abs )
                    .assign( gold, Functions.div );

            DoubleMatrix1D dnewtmp = dnew.copy().assign( dold, Functions.minus ).assign( Functions.abs )
                    .assign( dold, Functions.div );
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

            gold = gnew;
            dold = dnew;

            if ( count++ > MAXITERS ) {
                /*
                 * For certain data sets, we just flail around; for example if there are only two samples. This is a
                 * bailout for exceptional circumstances.
                 */
                throw new ComBatException( "Failed to converge within " + MAXITERS + " iterations, last delta was "
                        + String.format( "%.2g", change ) );
            }
        }

        return new DoubleMatrix1D[] { gold, dold };
    }

    /**
     * Estimation of parameters for one batch.
     * 
     * @param matrix
     * @param gHat
     * @param dHat
     * @return
     */
    private DoubleMatrix1D[] nonParametricFit( DoubleMatrix2D matrix, DoubleMatrix1D gHat, DoubleMatrix1D dHat ) {
        DoubleMatrix1D gstar = new DenseDoubleMatrix1D( matrix.rows() );
        DoubleMatrix1D dstar = new DenseDoubleMatrix1D( matrix.rows() );
        double twopi = 2.0 * Math.PI;

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * Vectorized schmectorized. In R you end up looping over the data many times. It's slow here too... but not too
         * horrible. 1000 rows of a 10k probe data set with 10 samples takes about 7.5 seconds on my laptop -- but this
         * has to be done for each batch. It's O( M*N^2 )
         */
        int c = 1;
        for ( int i = 0; i < matrix.rows(); i++ ) {

            double[] x = MatrixUtil.removeMissing( matrix.viewRow( i ) ).toArray();
            int n = x.length;
            double no2 = n / 2.0;

            double sumLH = 0.0;
            double sumgLH = 0.0;
            double sumdLH = 0.0;
            for ( int j = 0; j < matrix.rows(); j++ ) {

                if ( j == i ) continue;
                double g = gHat.getQuick( j );
                double d = dHat.getQuick( j );

                // compute the sum of squares of the difference between gHat[j] and the current data row.
                // this is slower, though it's the "colt api" way.
                // double sum2 = x.copy().assign( Functions.minus( g ) ).aggregate( Functions.plus, Functions.square );

                double sum2 = 0.0;
                for ( int k = 0; k < n; k++ ) {
                    sum2 += Math.pow( x[k] - g, 2 );
                }

                double LH = ( 1.0 / Math.pow( twopi * d, no2 ) ) * Math.exp( -sum2 / ( 2 * d ) );

                if ( Double.isNaN( LH ) ) continue;

                double gLH = g * LH;
                double dLH = d * LH;

                sumLH += LH;
                sumgLH += gLH;
                sumdLH += dLH;
            }

            gstar.set( i, sumgLH / sumLH );
            dstar.set( i, sumdLH / sumLH );

            if ( c++ % 1000 == 0 ) {
                log.info( i + String.format( " rows done, %.1fs elapsed", timer.getTime() / 1000.00 ) );
            }
        }

        return new DoubleMatrix1D[] { gstar, dstar };
    }

    /**
     * @param ghat
     * @param gbar
     * @param n
     * @param dstar
     * @param t2b
     * @return
     */
    private DoubleMatrix1D postMean( DoubleMatrix1D ghat, double gbar, DoubleMatrix1D n, DoubleMatrix1D dstar,
            double t2b ) {
        DoubleMatrix1D result = new DenseDoubleMatrix1D( ghat.size() );
        for ( int i = 0; i < ghat.size(); i++ ) {
            result.set( i,
                    ( t2b * n.get( i ) * ghat.get( i ) + dstar.get( i ) * gbar ) / ( t2b * n.get( i ) + dstar.get( i ) ) );
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

    /**
     * @param sdata
     * @param gammastar
     * @param deltastar
     * @return
     */
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

    /**
     * @param adjustedData
     * @return
     */
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

        DoubleMatrix2D deltas = matrix.copy().assign( ( s.mult( s.transpose( g ), a ) ), Functions.minus )
                .assign( Functions.square );
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
            varpooled = y.copy().assign( solver.transpose( solver.mult( X, beta ) ), Functions.minus )
                    .assign( Functions.pow( 2 ) );
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
