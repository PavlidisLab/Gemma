/*
 * The linkAnalysis project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression.links;

import cern.colt.list.ObjectArrayList;
import org.apache.commons.lang3.time.StopWatch;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix;
import ubic.basecode.math.Constants;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.*;

/**
 * A correlation analysis for a given data set, designed for selection of values based on criteria set by the user.
 * On the first pass over the data, a histogram is filled in to hold the distribution of the values found. You can set
 * criteria to have the correlations actually stored in a (sparse) matrix. This can take a lot of memory if you store
 * everything!
 * The correlation is only calculated if it isn't stored in the matrix, and values can be tested against a threshold.
 * This class is used in reality by one pass over the data to fill in the histogram. This is used to help select a
 * threshold. A second pass over the data is used to select correlations that meet the criteria.
 * Probes that do not map to genes are not used.
 *
 * @author Paul Pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class PearsonMetrics extends AbstractMatrixRowPairAnalysis {

    double[] rowMeans = null;
    double[] rowSumSquaresSqrt = null;

    public PearsonMetrics( ExpressionDataDoubleMatrix dataMatrix ) {
        this( dataMatrix.rows() );
        this.dataMatrix = dataMatrix;
        this.numMissing = this.fillUsed();
    }

    /**
     * @param dataMatrix DenseDoubleMatrix2DNamed
     * @param tmts       Values of the correlation that are deemed too small to store in the matrix. Setting this as high as
     *                   possible can greatly reduce memory requirements, but can slow things down.
     */
    public PearsonMetrics( ExpressionDataDoubleMatrix dataMatrix, double tmts ) {
        this( dataMatrix );
        this.setStorageThresholdValue( tmts );
    }

    PearsonMetrics() {
    }

    /**
     * @param size Dimensions of the required (square) matrix.
     */
    private PearsonMetrics( int size ) {
        if ( size > 0 ) {
            results = new CompressedSparseDoubleMatrix<>( size, size );
        }
        keepers = new ObjectArrayList();
    }

    /**
     * Calculate the linear correlation matrix of a matrix, allowing missing values. If there are no missing values,
     * this calls PearsonFast.
     */
    @Override
    public void calculateMetrics() {

        if ( this.numMissing == 0 ) {
            this.calculateMetricsFast();
            return;
        }

        int numused;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();

        if ( numcols < this.minNumUsed ) {
            throw new IllegalArgumentException(
                    "Sorry, correlations will not be computed unless there are at least " + this.minNumUsed
                            + " mutually present data points per vector pair, current data has only " + numcols
                            + " columns." );
        }

        boolean docalcs = this.needToCalculateMetrics();
        boolean[][] usedB = new boolean[][] {};
        double[][] data = new double[][] {};
        if ( docalcs ) {
            // Temporarily copy the data in this matrix, for performance.
            usedB = new boolean[numrows][numcols];
            data = new double[numrows][numcols];
            for ( int i = 0; i < numrows; i++ ) { // first vector
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    usedB[i][j] = used.get( i, j ); // this is only needed if we use it below, speeds things up
                    // slightly.
                    data[i][j] = this.dataMatrix.getAsDouble( i, j );
                }
            }

            this.rowStatistics();
        }

        /* for each vector, compare it to all other vectors */
        ExpressionDataMatrixRowElement itemA;
        StopWatch timer = new StopWatch();
        timer.start();
        double[] vectorA = new double[] {};
        double syy, sxy, sxx, sx, sy, xj, yj;
        int skipped = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) { // first vector
            itemA = this.dataMatrix.getRowElement( i );
            if ( !this.hasGene( itemA ) ) {
                skipped++;
                continue;
            }
            if ( docalcs ) {
                vectorA = data[i];
            }

            boolean thisRowHasMissing = hasMissing[i];

            for ( int j = i + 1; j < numrows; j++ ) { // second vector
                ExpressionDataMatrixRowElement itemB = this.dataMatrix.getRowElement( j );
                if ( !this.hasGene( itemB ) )
                    continue;

                // second pass over matrix? Don't calculate it if we already have it. Just do the requisite checks.
                if ( !docalcs || results.get( i, j ) != 0.0 ) {
                    this.keepCorrellation( i, j, results.get( i, j ), numcols );
                    continue;
                }

                double[] vectorB = data[j];

                /* if there are no missing values, use the faster method of calculation */
                if ( !thisRowHasMissing && !hasMissing[j] ) {
                    this.setCorrel( i, j, this.correlFast( vectorA, vectorB, i, j ), numcols );
                    continue;
                }

                /* do it the old fashioned way */
                numused = 0;
                sxy = 0.0;
                sxx = 0.0;
                syy = 0.0;
                sx = 0.0;
                sy = 0.0;
                for ( int k = 0; k < numcols; k++ ) {
                    xj = vectorA[k];
                    yj = vectorB[k];
                    if ( usedB[i][k] && usedB[j][k] ) { /* this is a bit faster than calling Double.isNan */
                        sx += xj;
                        sy += yj;
                        sxy += xj * yj;
                        sxx += xj * xj;
                        syy += yj * yj;
                        numused++;
                    }
                }

                // avoid -1 correlations or extremely noisy values (minNumUsed should be set high enough so that degrees
                // of freedom isn't too low.
                if ( numused < this.minNumUsed )
                    this.setCorrel( i, j, Double.NaN, 0 );
                else {
                    double denom = this.correlationNorm( numused, sxx, sx, syy, sy );
                    if ( denom <= 0.0 ) { // means variance is zero for one of the vectors.
                        this.setCorrel( i, j, 0.0, numused );
                    } else {
                        double correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );

                        this.setCorrel( i, j, correl, numused );
                    }
                }
                ++numComputed;

            }
            this.computeRow( timer, itemA, numComputed, i );
        }
        AbstractMatrixRowPairAnalysis.log.info( skipped + " rows skipped, where probe lacks a gene annotation" );
        this.finishMetrics();
    }

    @Override
    public QuantitationType getMetricType() {
        QuantitationType m = QuantitationType.Factory.newInstance();
        m.setIsBackground( false );
        m.setIsBackgroundSubtracted( false );
        m.setIsNormalized( false );
        m.setIsPreferred( false );
        m.setIsMaskedPreferred( false );
        m.setIsRatio( false );
        m.setType( StandardQuantitationType.CORRELATION );
        m.setName( "Pearson correlation" );
        m.setGeneralType( GeneralType.QUANTITATIVE );
        m.setRepresentation( PrimitiveType.DOUBLE );
        m.setScale( ScaleType.LINEAR );
        return m;

    }

    /**
     * Calculate mean and sumsqsqrt for each row
     */
    @Override
    protected void rowStatistics() {
        int numrows = dataMatrix.rows();
        this.rowMeans = new double[numrows];
        this.rowSumSquaresSqrt = new double[numrows];
        for ( int i = 0, numcols = dataMatrix.columns(); i < numrows; i++ ) {
            double ax = 0.0;
            double sxx = 0.0;
            for ( int j = 0; j < numcols; j++ ) {
                ax += this.dataMatrix.getAsDouble( i, j );
            }
            rowMeans[i] = ( ax / numcols );

            for ( int j = 0; j < numcols; j++ ) {
                double xt = this.dataMatrix.getAsDouble( i, j ) - rowMeans[i]; /* deviation from mean */
                sxx += xt * xt; /* sum of squared error */
            }
            rowSumSquaresSqrt[i] = Math.sqrt( sxx );
        }
    }

    @Override
    protected double correlFast( double[] ival, double[] jval, int i, int j ) {
        double ssi = rowSumSquaresSqrt[i];
        double ssj = rowSumSquaresSqrt[j];
        double mi = rowMeans[i];
        double mj = rowMeans[j];

        return this.correlFast( ival, jval, ssi, ssj, mi, mj );
    }

    double correlationNorm( int n, double sxx, double sx, double syy, double sy ) {
        return ( sxx - sx * sx / n ) * ( syy - sy * sy / n );
    }

    /**
     * Compute a correlation. For Spearman, the values entered must be the ranks.
     *
     * @param ssi root sum squared deviation
     * @param ssj root sum squared deviation
     * @param mi  row mean of the ranks
     * @param mj  row mean of the ranks
     * @return correlation, or NaN if the SSD values are too small, etc.
     */
    double correlFast( double[] ival, double[] jval, double ssi, double ssj, double mi, double mj ) {
        if ( ssi < Constants.SMALL || ssj < Constants.SMALL )
            return Double.NaN;
        double sxy = 0.0;
        for ( int k = 0, n = ival.length; k < n; k++ ) {

            sxy += ( ival[k] - mi ) * ( jval[k] - mj );
        }
        double c = sxy / ( ssi * ssj );

        // should never have roundoff errors this large.
        assert c > -1.0001 && c < 1.0001 : c;

        // roundoff guard
        if ( c < -1.0 ) {
            c = -1.0;
        } else if ( c > 1.0 ) {
            c = 1.0;
        }

        return c;
    }

    /**
     * Calculate a linear correlation matrix for a matrix. Use this if you know there are no missing values, or don't
     * care about NaNs. Rows that are not mapped to genes are skipped.
     */
    private void calculateMetricsFast() {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        boolean docalcs = this.needToCalculateMetrics();

        double[][] data = new double[][] {};
        if ( docalcs ) {
            this.rowStatistics();

            // Temporarily put the data in this matrix (performance)
            data = new double[numrows][numcols];
            for ( int i = 0; i < numrows; i++ ) { // first vector
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    data[i][j] = this.dataMatrix.getAsDouble( i, j );
                }
            }
        }

        /*
         * For each vector, compare it to all other vectors, avoid repeating things; skip items that don't have genes
         * mapped to them.
         */
        StopWatch timer = new StopWatch();
        timer.start();
        int skipped = 0;
        int numComputed = 0;
        skipped = this.computeMetrics( numrows, numcols, docalcs, timer, skipped, numComputed, data );
        AbstractMatrixRowPairAnalysis.log.info( skipped + " rows skipped, due to no gene association" );
        this.finishMetrics();
    }

}
