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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.expression.diff.DiffExAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.preprocess.svd.SVDServiceHelper;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.list.DoubleArrayList;

/**
 * Methods to (attempt to) detect outliers in data sets.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class OutlierDetectionServiceImpl implements OutlierDetectionService {

    private static final double DEFAULT_FRACTION = 0.90;

    private static final int DEFAULT_QUANTILE = 15;

    private static Log log = LogFactory.getLog( OutlierDetectionServiceImpl.class );

    // Optional: the maximum fraction of samples that can be outliers
    @SuppressWarnings("unused")
    private static final double MAX_FRACTION_OUTLIERS = 0.3;

    @Autowired
    private ExpressionExperimentService eeService;

    // // For test purposes. Allows the printing of matrices based on residuals.
    // private boolean printMatrices = false;

    // For working with filtered data
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private DiffExAnalyzer lma;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private SVDServiceHelper svdService;

    private OutlierDetectionTestDetails testDetails;

    private boolean testMode = false;

    /*
     * Calculate index (rank) of desired quantile using R's method #8
     */
    public double findDesiredQuantileIndex( int numCors, int quantileThreshold ) {

        double index = 0.0;
        double n = numCors;
        double fraction = ( quantileThreshold / 100.0 );

        if ( fraction < ( 2.0 / 3.0 ) / ( n + ( 1.0 / 3.0 ) ) ) {
            index = 1;
        } else if ( fraction >= ( ( n - ( 1.0 / 3.0 ) ) / ( n + ( 1.0 / 3.0 ) ) ) ) {
            index = n;
        } else {
            index = ( ( ( n + ( 1.0 / 3.0 ) ) * fraction ) + ( 1.0 / 3.0 ) );
        }

        return index;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.OutlierDetectionService#identifyOutliers(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee ) {
        return this.identifyOutliers( ee, false, DEFAULT_QUANTILE, DEFAULT_FRACTION );
    }

    /* Runs in testmode; returns OutlierDetectionTestDetails */
    @Override
    public OutlierDetectionTestDetails identifyOutliers( ExpressionExperiment ee, boolean useRegression,
            boolean findByMedian ) {

        testMode = true;
        testDetails = new OutlierDetectionTestDetails( ee.getShortName() );

        Collection<OutlierDetails> outliers;

        if ( findByMedian ) {
            outliers = identifyOutliersByMedianCorrelation( ee, useRegression );
        } else {
            outliers = this.identifyOutliers( ee, useRegression, DEFAULT_QUANTILE, DEFAULT_FRACTION );
        }

        testDetails.setOutliers( outliers );
        testDetails.setNumOutliers( outliers.size() );

        return testDetails;
    }

    @Override
    public Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee, boolean useRegression,
            int quantileThreshold, double fractionThreshold ) {

        DoubleMatrix<BioAssay, BioAssay> cormat = getCorrelationMatrix( ee, useRegression );

        if ( cormat == null || cormat.rows() == 0 ) {
            log.warn( "Correlation matrix is empty, cannot check for outliers" );
            return new HashSet<>();
        }

        return identifyOutliers( ee, cormat, quantileThreshold, fractionThreshold );
    }

    /**
     * @param ee
     * @param cormat
     * @return
     */
    @Override
    public Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee, DoubleMatrix<BioAssay, BioAssay> cormat ) {
        return identifyOutliers( ee, cormat, DEFAULT_QUANTILE, DEFAULT_FRACTION );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.OutlierDetectionService#identifyOutliers(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.basecode.dataStructure.matrix.DoubleMatrix, int, double)
     */
    @Override
    public Collection<OutlierDetails> identifyOutliers( ExpressionExperiment ee,
            DoubleMatrix<BioAssay, BioAssay> cormat, int quantileThreshold, double fractionThreshold ) {

        /*
         * Raymond's algorithm: "A sample which has a correlation of less than a threshold with more than 80% of the
         * other samples. The threshold is just the first quartile of sample correlations."
         */

        /*
         * First pass: Determine the threshold
         */
        DoubleArrayList cors = getCorrelationList( cormat );

        if ( cors.isEmpty() ) {
            log.warn( "No correlations" );
            return new HashSet<>();
        }

        /*
         * TODO sanity checks to make sure correlations aren't all the same, etc.
         */

        double valueAtDesiredQuantile = findValueAtDesiredQuantile( cors, quantileThreshold );

        if ( valueAtDesiredQuantile == Double.MIN_VALUE ) {
            throw new IllegalStateException( "Could not determine desired quantile" );
        }

        log.info( "Threshold correlation is " + String.format( "%.2f", valueAtDesiredQuantile ) );

        // second pass; for each sample, how many correlations does it have which are below the selected quantile.
        Collection<OutlierDetails> outliers = new HashSet<OutlierDetails>();
        for ( int i = 0; i < cormat.rows(); i++ ) {
            BioAssay ba = cormat.getRowName( i );
            int countBelow = 0;
            for ( int j = 0; j < cormat.rows(); j++ ) {
                double cor = cormat.get( i, j );
                if ( cor < valueAtDesiredQuantile ) {
                    countBelow++;
                }
            }

            // if it has more than the threshold fraction of low correlations, we flag it.
            if ( countBelow > fractionThreshold * ( cormat.columns() - 1 ) ) {
                OutlierDetails outlier = new OutlierDetails( ba, countBelow / ( double ) ( cormat.columns() - 1 ),
                        valueAtDesiredQuantile );
                outliers.add( outlier );

            }
        }

        // Jenni's code: make note of correlation threshold etc.
        if ( testMode ) {
            testDetails.setLastThreshold( valueAtDesiredQuantile );
            testDetails.setNumOutliersByBasicAlgorithm( outliers.size() );
        }

        log.info( "Found " + outliers.size() + " outlier(s) in " + ee );

        /*
         * TODO additional checks
         */

        return outliers;

    }

    /*
     * Runs in testmode; returns OutlierDetectionTestDetails
     */
    @Override
    public OutlierDetectionTestDetails identifyOutliersByCombinedMethod( ExpressionExperiment ee ) {

        testMode = true;
        testDetails = new OutlierDetectionTestDetails( ee.getShortName() );

        Collection<OutlierDetails> outliers = new HashSet<OutlierDetails>();

        // Always use regression when calculating the correlation matrix:
        DoubleMatrix<BioAssay, BioAssay> cormat = getCorrelationMatrix( ee, true );

        outliers.addAll( this.identifyOutliers( ee, cormat, DEFAULT_QUANTILE, DEFAULT_FRACTION ) );
        outliers.addAll( this.identifyOutliersByMedianCorrelation( ee, cormat ) );

        testDetails.setOutliers( outliers );
        testDetails.setNumOutliers( outliers.size() );

        log.info( "Total number of outliers: " + testDetails.getNumOutliers() );

        return testDetails;
    }

    @Override
    public Collection<OutlierDetails> identifyOutliersByMedianCorrelation( ExpressionExperiment ee,
            boolean useRegression ) {

        DoubleMatrix<BioAssay, BioAssay> cormat = getCorrelationMatrix( ee, useRegression );

        if ( cormat == null || cormat.rows() == 0 ) {
            log.warn( "Correlation matrix is empty, cannot check for outliers" );
            return new HashSet<>();
        }

        return identifyOutliersByMedianCorrelation( ee, cormat );
    }

    /**
     * Jenni's (almost) fool proof method for finding quantiles using R's method #8
     */
    private double findValueAtDesiredQuantile( DoubleArrayList cors, int quantileThreshold ) {

        double lowerQuantileValue = Double.MIN_VALUE;
        double upperQuantileValue = Double.MIN_VALUE;

        double desiredQuantileIndex = findDesiredQuantileIndex( cors.size(), quantileThreshold );

        double[] sortedCors = new double[cors.size()];

        /*
         * Get all sample correlations
         */
        for ( int i = 0; i < cors.size(); i++ ) {
            sortedCors[i] = cors.get( i );
        }

        Arrays.sort( sortedCors );

        // Get the correlations from the sorted array. Use -1 b/c rank indices start at 1 but array entries start at 0
        lowerQuantileValue = sortedCors[( int ) ( Math.floor( desiredQuantileIndex ) - 1 )];
        upperQuantileValue = sortedCors[( int ) Math.floor( desiredQuantileIndex )];

        return ( lowerQuantileValue + ( ( desiredQuantileIndex - Math.floor( desiredQuantileIndex ) ) * ( upperQuantileValue - lowerQuantileValue ) ) );
    }

    private DoubleArrayList getCorrelationList( DoubleMatrix<BioAssay, BioAssay> cormat ) {
        assert cormat.rows() == cormat.columns();
        DoubleArrayList cors = new DoubleArrayList();
        for ( int i = 0; i < cormat.rows(); i++ ) {
            for ( int j = i + 1; j < cormat.rows(); j++ ) {
                double d = cormat.get( i, j );
                cors.add( d );
            }
        }
        return cors;
    }

    private DoubleMatrix<BioAssay, BioAssay> getCorrelationMatrix( ExpressionExperiment ee, boolean useRegression ) {

        /*
         * Get the experimental design
         */
        ee = eeService.thawLite( ee );

        /*
         * Get the data matrix
         */
        Collection<ProcessedExpressionDataVector> vectos = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );

        if ( vectos.isEmpty() ) {
            log.warn( "Experiment has no processed data vectors" );
            return null;
        }

        /*
         * Work with filtered data
         */
        FilterConfig fconfig = new FilterConfig();
        fconfig.setIgnoreMinimumRowsThreshold( true );
        fconfig.setIgnoreMinimumSampleThreshold( true );
        ExpressionDataDoubleMatrix mat = expressionDataMatrixService.getFilteredMatrix( ee, fconfig, vectos );

        /* For test purposes: make note of the number of experimental factors */
        if ( testMode ) {
            testDetails.setNumExpFactors( ee.getExperimentalDesign().getExperimentalFactors().size() );
        }

        /*
         * Optional: Regress out any 'major' factors; work with residuals only.
         */
        if ( useRegression && !ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {

            double importanceThreshold = 0.01;
            Set<ExperimentalFactor> importantFactors = svdService.getImportantFactors( ee, ee.getExperimentalDesign()
                    .getExperimentalFactors(), importanceThreshold );
            /* Remove 'batch' from important factors */
            ExperimentalFactor batch = null;
            for ( ExperimentalFactor factor : importantFactors ) {
                if ( factor.getName().toLowerCase().equals( "batch" ) ) batch = factor;
            }
            if ( batch != null ) {
                importantFactors.remove( batch );
                log.info( "Removed 'batch' from the list of significant factors." );
            }
            if ( !importantFactors.isEmpty() ) {
                /* If in test mode, make note of significant experimental factors */
                if ( testMode ) {
                    testDetails.setNumSigFactors( importantFactors.size() );
                    testDetails.setSignificantFactors( importantFactors );
                }
                log.info( "Regressing out covariates" );
                DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
                config.setFactorsToInclude( importantFactors );
                mat = lma.regressionResiduals( mat, config, true );
            }
        }
        // // For testing purposes:
        // if ( useRegression && printMatrices ) {
        // printResidualMatrix( mat );
        // }

        /*
         * Determine the correlation of samples.
         */
        DoubleMatrix<BioAssay, BioAssay> cormat = SampleCoexpressionMatrixServiceImpl.getMatrix( mat );

        // Remove any existing outliers from cormat:
        int col = 0;
        int numRemoved = 0;
        while ( col < cormat.columns() ) {
            if ( cormat.getColName( col ).getIsOutlier() ) {
                log.info( "Removing existing outlier " + cormat.getColName( col ) + " from " + ee.getShortName() );
                List<BioAssay> colNames = getRemainingColumns( cormat, cormat.getColName( col ) );
                cormat = cormat.subsetRows( colNames );
                cormat = cormat.subsetColumns( colNames );
                numRemoved++;
            } else
                col++; // increment only if sample is not an outlier so as not to skip columns
        }

        if ( testMode ) {
            testDetails.setNumRemoved( numRemoved );
        }

        // // For testing purposes:
        // if ( useRegression && printMatrices ) {
        // printCorrelationMatrix( cormat );
        // }

        return cormat;

    }

    private List<BioAssay> getRemainingColumns( DoubleMatrix<BioAssay, BioAssay> cormat, BioAssay outlier ) {
        List<BioAssay> bas = new ArrayList<BioAssay>();
        for ( int i = 0; i < cormat.columns(); i++ ) {
            if ( cormat.getColName( i ) != outlier ) bas.add( cormat.getColName( i ) );
        }
        return bas;
    }

    /*** Identify outliers by sorting by median, then looking for non-overlap of first quartile-second quartile range ***/
    private Collection<OutlierDetails> identifyOutliersByMedianCorrelation( ExpressionExperiment ee,
            DoubleMatrix<BioAssay, BioAssay> cormat ) {

        List<OutlierDetails> allSamples = new ArrayList<OutlierDetails>();
        OutlierDetails sample;

        /* Find the 1st, 2nd, and 3rd quartiles of each sample */
        for ( int i = 0; i < cormat.rows(); i++ ) {
            DoubleArrayList cors = new DoubleArrayList();
            sample = new OutlierDetails( cormat.getRowName( i ) );
            for ( int j = 0; j < cormat.columns(); j++ ) {
                if ( j != i ) { // get all sample correlations except correlation with self
                    double d = cormat.get( i, j );
                    cors.add( d );
                }
            }
            assert ( cors.size() == cormat.rows() - 1 );

            sample.setFirstQuartile( findValueAtDesiredQuantile( cors, 25 ) );
            sample.setMedianCorrelation( findValueAtDesiredQuantile( cors, 50 ) );
            sample.setThirdQuartile( findValueAtDesiredQuantile( cors, 75 ) );

            if ( sample.getFirstQuartile() == Double.MIN_VALUE || sample.getMedianCorrelation() == Double.MIN_VALUE
                    || sample.getThirdQuartile() == Double.MIN_VALUE ) {
                throw new IllegalStateException( "Could not determine one or more quartiles for a sample; " );
            }

            allSamples.add( sample );
        }

        /* Sort all samples by median correlation */
        Collections.sort( allSamples, OutlierDetails.MedianComparator );

        int numOutliers = 0;

        /* Check for overlap of first quartile and median of consecutive samples */
        for ( int k = 0; k < allSamples.size() - 1; k++ ) {
            // if ( allSamples.get( k ).getMedianCorrelation() < allSamples.get( k + 1 ).getFirstQuartile() ) {
            if ( allSamples.get( k ).getThirdQuartile() < allSamples.get( k + 1 ).getFirstQuartile() ) {
                numOutliers = k + 1;
            }
        }

        /* TO DO: Add sanity checks here ... */
        // if ( numOutliers >= allSamples.size() * MAX_FRACTION_OUTLIERS )
        // numOutliers = 0;

        List<OutlierDetails> outliers = new ArrayList<OutlierDetails>();

        for ( int m = 0; m < numOutliers; m++ ) {
            outliers.add( allSamples.get( m ) );
        }

        /*
         * Check that all outliers are legitimate (controls for situations where sorting by median does not give 'true'
         * order)
         */
        if ( numOutliers > 0 ) {
            log.info( "Removing false positives; number of outliers before test: " + numOutliers );
            outliers = removeFalsePositives( allSamples, outliers, numOutliers );

            numOutliers = outliers.size();
            log.info( "Number of outliers after removing false positives: " + numOutliers );
        }

        if ( testMode ) {
            testDetails.setNumOutliers( numOutliers );
            testDetails.setNumOutliersByMedian( numOutliers );
        }

        log.info( "Found " + numOutliers + " outlier(s) in " + ee );

        return outliers;

    }

    private List<OutlierDetails> removeFalsePositives( List<OutlierDetails> outliers, double threshold ) {

        log.info( "outliers.size() = " + outliers.size() + "; threshold = " + threshold );

        for ( int i = 0; i < outliers.size(); i++ ) {
            // if ( outliers.get( i ).getMedianCorrelation() >= threshold ) {
            if ( outliers.get( i ).getThirdQuartile() >= threshold ) {
                if ( outliers.get( i ).getFirstQuartile() < threshold ) {
                    threshold = outliers.get( i ).getFirstQuartile();
                }
                outliers.remove( i );
                outliers = removeFalsePositives( outliers, threshold );
            }
        }
        return outliers;
    }

    /* 
     *  
     */
    private List<OutlierDetails> removeFalsePositives( List<OutlierDetails> allSamples, List<OutlierDetails> outliers,
            int numOutliers ) {

        List<OutlierDetails> inliers = new ArrayList<OutlierDetails>();

        for ( int j = numOutliers; j < allSamples.size(); j++ ) {
            inliers.add( allSamples.get( j ) );
        }

        Collections.sort( inliers, OutlierDetails.FirstQuartileComparator );

        double threshold = inliers.get( 0 ).getFirstQuartile();

        return removeFalsePositives( outliers, threshold );

    }

}