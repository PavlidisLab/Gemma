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
package ubic.gemma.core.analysis.preprocess;

import cern.colt.list.DoubleArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Methods to (attempt to) detect outliers in data sets.
 *
 * @author paul
 */
@Service
public class OutlierDetectionServiceImpl implements OutlierDetectionService {
    private static final Log log = LogFactory.getLog( OutlierDetectionServiceImpl.class );

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Nullable
    @Override
    public Collection<OutlierDetails> getOutlierDetails( ExpressionExperiment ee ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = sampleCoexpressionAnalysisService.loadBestMatrix( ee );
        if ( cormat == null || cormat.rows() == 0 ) {
            OutlierDetectionServiceImpl.log.warn( "Correlation matrix is empty, cannot check for outliers" );
            return new HashSet<>();
        }
        return this.identifyOutliersByMedianCorrelation( cormat );
    }

    @Override
    public Collection<OutlierDetails> identifyOutliersByMedianCorrelation( ExpressionExperiment ee ) {
        DoubleMatrix<BioAssay, BioAssay> cormat = sampleCoexpressionAnalysisService.loadBestMatrix( ee );
        if ( cormat == null ) {
            cormat = sampleCoexpressionAnalysisService.computeIfNecessary( ee );
        }
        if ( cormat.rows() == 0 ) {
            OutlierDetectionServiceImpl.log.warn( "Correlation matrix is empty, cannot check for outliers" );
            return new HashSet<>();
        }
        return this.identifyOutliersByMedianCorrelation( cormat );
    }

    @Override
    public Collection<OutlierDetails> identifyOutliersByMedianCorrelation( DoubleMatrix<BioAssay, BioAssay> cormat ) {

        List<OutlierDetails> allSamples = new ArrayList<>();
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

            sample.setFirstQuartile( this.findValueAtDesiredQuantile( cors, 25 ) );
            sample.setMedianCorrelation( this.findValueAtDesiredQuantile( cors, 50 ) );
            sample.setThirdQuartile( this.findValueAtDesiredQuantile( cors, 75 ) );

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

        List<OutlierDetails> outliers = new ArrayList<>();

        for ( int m = 0; m < numOutliers; m++ ) {
            outliers.add( allSamples.get( m ) );
        }

        /*
         * Check that all outliers are legitimate (controls for situations where sorting by median does not give 'true'
         * order)
         */
        if ( numOutliers > 0 ) {
            OutlierDetectionServiceImpl.log
                    .debug( "Removing false positives; number of outliers before test: " + numOutliers );
            outliers = this.removeFalsePositives( allSamples, outliers, numOutliers );

            numOutliers = outliers.size();
            OutlierDetectionServiceImpl.log.debug( "Number of outliers after removing false positives: " + numOutliers );
            OutlierDetectionServiceImpl.log.info( "Found " + numOutliers + " outlier(s)" );
        }

        return outliers;

    }

    /**
     * Calculate index (rank) of desired quantile using R's method #8
     *
     * @param numCors           n
     * @param quantileThreshold quantile threshold
     * @return index (rank) of desired quantile using R's method #8
     */
    private double findDesiredQuantileIndex( int numCors, int quantileThreshold ) {
        double index;
        double fraction = ( quantileThreshold / 100.0 );

        if ( fraction < ( 2.0 / 3.0 ) / ( numCors + ( 1.0 / 3.0 ) ) ) {
            index = 1;
        } else if ( fraction >= ( ( numCors - ( 1.0 / 3.0 ) ) / ( numCors + ( 1.0 / 3.0 ) ) ) ) {
            index = numCors;
        } else {
            index = ( ( ( numCors + ( 1.0 / 3.0 ) ) * fraction ) + ( 1.0 / 3.0 ) );
        }
        return index;
    }

    /**
     * Jenni's (almost) fool proof method for finding quantiles using R's method #8
     */
    private double findValueAtDesiredQuantile( DoubleArrayList cors, int quantileThreshold ) {

        double lowerQuantileValue;
        double upperQuantileValue;

        double desiredQuantileIndex = this.findDesiredQuantileIndex( cors.size(), quantileThreshold );

        double[] sortedCors = new double[cors.size()];

        /*
         * Get all sample correlations
         */
        for ( int i = 0; i < cors.size(); i++ ) {
            sortedCors[i] = cors.get( i );
        }

        Arrays.sort( sortedCors );

        // Get the correlations from the sorted array. Use -1 b/c rank indices start at 1 but array entries start at 0
        int up = ( int ) ( Math.floor( desiredQuantileIndex ) );
        lowerQuantileValue = sortedCors[up - 1];
        upperQuantileValue = sortedCors[up < sortedCors.length ? up : up - 1];

        return ( lowerQuantileValue + ( ( desiredQuantileIndex - Math.floor( desiredQuantileIndex ) ) * (
                upperQuantileValue - lowerQuantileValue ) ) );
    }

    private List<OutlierDetails> removeFalsePositives( List<OutlierDetails> outliers, double threshold ) {

        OutlierDetectionServiceImpl.log.debug( String.format( "outliers = %d; threshold %.2f", outliers.size(), threshold ) );

        for ( int i = 0; i < outliers.size(); i++ ) {
            if ( outliers.get( i ).getThirdQuartile() >= threshold ) {
                if ( outliers.get( i ).getFirstQuartile() < threshold ) {
                    threshold = outliers.get( i ).getFirstQuartile();
                }
                outliers.remove( i );
                outliers = this.removeFalsePositives( outliers, threshold );
            }
        }
        return outliers;
    }

    private List<OutlierDetails> removeFalsePositives( List<OutlierDetails> allSamples, List<OutlierDetails> outliers,
            int numOutliers ) {

        List<OutlierDetails> inliers = new ArrayList<>();

        for ( int j = numOutliers; j < allSamples.size(); j++ ) {
            inliers.add( allSamples.get( j ) );
        }

        Collections.sort( inliers, OutlierDetails.FirstQuartileComparator );

        double threshold = inliers.get( 0 ).getFirstQuartile();

        return this.removeFalsePositives( outliers, threshold );

    }

}