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
package ubic.gemma.analysis.linkAnalysis;

import java.util.Collection;

import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author paul
 * @version $Id$
 */
public class SpearmanMetrics extends AbstractMatrixRowPairAnalysis {

    /**
     * @param
     */
    public SpearmanMetrics( ExpressionDataDoubleMatrix dataMatrix ) {
        this( dataMatrix.rows() );
        this.dataMatrix = dataMatrix;
        this.numMissing = this.fillUsed();
    }

    /**
     * @param dataMatrix DenseDoubleMatrix2DNamed
     * @param tmts Values of the correlation that are deemed too small to store in the matrix. Setting this as high as
     *        possible can greatly reduce memory requirements, but can slow things down.
     */
    public SpearmanMetrics( ExpressionDataDoubleMatrix dataMatrix, double tmts ) {
        this( dataMatrix );
        this.setStorageThresholdValue( tmts );
    }

    /**
     * @param size Dimensions of the required (square) matrix.
     */
    protected SpearmanMetrics( int size ) {
        if ( size > 0 ) {
            results = new CompressedSparseDoubleMatrix2DNamed<ExpressionDataMatrixRowElement, ExpressionDataMatrixRowElement>(
                    size, size );
        }
        keepers = new ObjectArrayList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.linkAnalysis.MatrixRowPairAnalysis#getMetricType()
     */
    public QuantitationType getMetricType() {
        QuantitationType m = QuantitationType.Factory.newInstance();
        m.setIsBackground( false );
        m.setIsBackgroundSubtracted( false );
        m.setIsNormalized( false );
        m.setIsPreferred( false );
        m.setIsRatio( false );
        m.setType( StandardQuantitationType.CORRELATION );
        m.setName( "Spearman's rank correlation" );
        m.setGeneralType( GeneralType.QUANTITATIVE );
        m.setRepresentation( PrimitiveType.DOUBLE );
        m.setScale( ScaleType.LINEAR );
        return m;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.linkAnalysis.MatrixRowPairAnalysis#correctedPvalue(int, int, double, int)
     */
    public double correctedPvalue( int i, int j, double correl, int numused ) {
        double p = CorrelationStats.spearmanPvalue( correl, numused );
        double k = 1, m = 1;
        Collection<Collection<Gene>> clusters = getGenesForRow( i );

        if ( clusters != null ) {
            for ( Collection<Gene> geneIdSet : clusters ) {
                /*
                 * Note we break on the first iteration because the number of probes per gene in the same cluster is
                 * constant.
                 */
                for ( Gene geneId : geneIdSet ) {
                    int tmpK = this.geneToProbeMap.get( geneId ).size() + 1;
                    if ( k < tmpK ) k = tmpK;
                    break;
                }
            }
        }

        clusters = getGenesForRow( j );
        if ( clusters != null ) {
            for ( Collection<Gene> geneIdSet : clusters ) {
                for ( Gene geneId : geneIdSet ) {
                    int tmpM = this.geneToProbeMap.get( geneId ).size() + 1;
                    if ( m < tmpM ) m = tmpM;
                    break;
                }
            }
        }

        return p * k * m;
    }

    /**
     * Main method.
     */
    public void calculateMetrics() {

        if ( this.numMissing == 0 ) {
            this.calculateMetricsFast();
            return;
        }

        // int numused;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();

        boolean doCalcs = this.needToCalculateMetrics();
        boolean[][] usedB = null;
        double[][] rankTransformedData = null;

        if ( doCalcs ) {
            // Temporarily copy the data in this matrix, for performance, and rank transform.
            usedB = new boolean[numrows][numcols];
            rankTransformedData = getRankTransformedData( usedB );
        }

        /* for each vector, compare it to all other vectors */

        // value to use if there are no missing values.
        double denom = ( Math.pow( numcols, 2 ) - 1 ) * numcols;

        ExpressionDataMatrixRowElement itemA = null;
        double[] vectorA = null;
        int count = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) { // first vector
            itemA = this.dataMatrix.getRowElement( i );

            if ( !this.hasGene( itemA ) ) continue;

            if ( doCalcs ) {
                vectorA = rankTransformedData[i];
            }

            boolean thisRowHasMissing = hasMissing[i];

            for ( int j = i + 1; j < numrows; j++ ) { // second vector
                ExpressionDataMatrixRowElement itemB = this.dataMatrix.getRowElement( j );
                if ( !this.hasGene( itemB ) ) continue;

                // second pass over matrix? Don't calculate it if we already have it. Just do the requisite checks.
                if ( !doCalcs || results.get( i, j ) != 0.0 ) {
                    keepCorrel( i, j, results.get( i, j ), numcols );
                    continue;
                }

                double[] vectorB = rankTransformedData[j];

                /* if there are no missing values, use the faster method of calculation */
                if ( !thisRowHasMissing && !hasMissing[j] ) {
                    setCorrel( i, j, this.correlFast( vectorA, vectorB, denom ), numcols );
                    continue;
                }

                /*
                 * Compute rho using method that allows for missing values.
                 */
                int numused = 0;
                double sse = 0.0;
                for ( int k = 0; k < vectorA.length; k++ ) {
                    if ( !usedB[i][k] || !usedB[j][k] ) { /* avoid double.isnan() calls */
                        continue;
                    }
                    sse += Math.pow( vectorA[k] - vectorB[k], 2 );
                    numused++;
                }

                if ( numused < minNumUsed ) {
                    setCorrel( i, j, Double.NaN, 0 );
                } else {
                    double rho = 1.0 - sse / ( Math.pow( numused, 3 ) - numused );
                    setCorrel( i, j, rho, numused );
                }
            }

            ++numComputed;

            if ( ++count % 2000 == 0 ) {
                log.info( count + " rows done, " + numComputed + " correlations computed, last row was " + itemA + " "
                        + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }
        finishMetrics();

    }

    /**
     * @param usedB will be filled in, if not null.
     * @return rank-transformed data.
     */
    private double[][] getRankTransformedData( boolean[][] usedB ) {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        double[][] rankTransformedData;
        rankTransformedData = new double[numrows][];
        for ( int i = 0; i < numrows; i++ ) { // first vector

            Double[] row = this.dataMatrix.getRow( i );
            double r[] = new double[row.length];
            for ( int m = 0, v = row.length; m < v; m++ ) {
                r[m] = row[m];
            }

            DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( r ) );
            double ri[] = new double[ranksIA.size()];
            for ( int n = 0, w = ranksIA.size(); n < w; n++ ) {
                ri[n] = ranksIA.get( n );
            }

            rankTransformedData[i] = ri;

            if ( usedB != null ) {
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    usedB[i][j] = used.get( i, j ); // this is only needed if we use it below, speeds things up
                    // slightly.
                }
            }
        }
        return rankTransformedData;
    }

    /**
     * Compute the rank correlation, when there are no missing values.
     * 
     * @param ival double[]
     * @param jval double[]
     * @param denom n(n^2-1), to avoid having to compute this every time.
     * @return double
     */
    protected double correlFast( double[] ival, double[] jval, double denom ) {
        double sse = 0.0;
        int n = ival.length;
        for ( int i = 0; i < n; i++ ) {
            sse += Math.pow( ival[i] - jval[i], 2 );
        }
        return 1.0 - 6.0 * sse / denom;
    }

    /**
     * If there are no missing values.
     */
    private void calculateMetricsFast() {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        boolean docalcs = this.needToCalculateMetrics();

        double[][] rankTransformedData = null;
        if ( docalcs ) {
            rankTransformedData = getRankTransformedData( null );
        }

        double denom = Math.pow( numcols, 3 ) - 1;

        /*
         * For each vector, compare it to all other vectors, avoid repeating things; skip items that don't have genes
         * mapped to them.
         */
        ExpressionDataMatrixRowElement itemA = null;
        ExpressionDataMatrixRowElement itemB = null;
        double[] vectorA = null;
        int count = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) {
            itemA = this.dataMatrix.getRowElement( i );
            if ( !this.hasGene( itemA ) ) continue;
            if ( docalcs ) {
                vectorA = rankTransformedData[i];
            }

            for ( int j = i + 1; j < numrows; j++ ) {
                itemB = this.dataMatrix.getRowElement( j );
                if ( !this.hasGene( itemB ) ) continue;
                if ( !docalcs || results.get( i, j ) != 0.0 ) { // second pass over matrix. Don't calculate it
                    // if we
                    // already have it. Just do the requisite checks.
                    keepCorrel( i, j, results.get( i, j ), numcols );
                    continue;
                }

                double[] vectorB = rankTransformedData[j];
                setCorrel( i, j, correlFast( vectorA, vectorB, denom ), numcols );
                ++numComputed;
            }
            if ( ++count % 2000 == 0 ) {
                log.info( count + " rows done, " + numComputed + " correlations computed, last row was " + itemA + " "
                        + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }

        finishMetrics();
    }

}
