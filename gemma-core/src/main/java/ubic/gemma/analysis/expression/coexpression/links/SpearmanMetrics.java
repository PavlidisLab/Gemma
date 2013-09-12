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
package ubic.gemma.analysis.expression.coexpression.links;

import java.util.Collection;

import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix;
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
public class SpearmanMetrics extends MatrixRowPairPearsonAnalysis {

    double[][] rankTransformedData = null;

    /**
     * This overrides value from AbstractMatrixRowPairAnalysis. If fewer than this number values are available, the
     * correlation is rejected. This helps keep the correlation distribution reasonable.
     * <p>
     * FIXME we might want to set this even higher!
     */
    @SuppressWarnings("hiding")
    protected int minNumUsed = 8;

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
            results = new CompressedSparseDoubleMatrix<ExpressionDataMatrixRowElement, ExpressionDataMatrixRowElement>(
                    size, size );
        }
        keepers = new ObjectArrayList();
    }

    /**
     * Compute correlations.
     */
    @SuppressWarnings("null")
    @Override
    public void calculateMetrics() {

        if ( this.numMissing == 0 ) {
            this.calculateMetricsFast();
            return;
        }

        // int numused;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();

        if ( numcols < this.minNumUsed ) {
            throw new IllegalArgumentException(
                    "Sorry, Spearman correlations will not be computed unless there are at least " + this.minNumUsed
                            + " data points per vector, current data has only " + numcols + " columns." );
        }

        boolean doCalcs = this.needToCalculateMetrics();
        boolean[][] usedB = null;

        if ( doCalcs ) {
            // Temporarily copy the data in this matrix, for performance, and rank transform.
            usedB = new boolean[numrows][numcols];

            getRankTransformedData( usedB );
        }

        /* for each vector, compare it to all other vectors */

        ExpressionDataMatrixRowElement itemA = null;
        double[] vectorA = null;
        int skipped = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) { // first vector
            itemA = this.dataMatrix.getRowElement( i );

            if ( !this.hasGene( itemA ) ) {
                skipped++;
                continue;
            }

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
                    setCorrel( i, j, this.correlFast( vectorA, vectorB, i, j ), numcols );
                    continue;
                }

                spearman( vectorA, vectorB, usedB[i], usedB[j], i, j );

                ++numComputed;

            }

            ++numComputed;

            if ( ( i + 1 ) % 2000 == 0 ) {
                log.info( ( i + 1 ) + " rows done, " + numComputed + " correlations computed, last row was " + itemA
                        + " " + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }
        log.info( skipped + " rows skipped, due to no BLAT association" );
        finishMetrics();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.linkAnalysis.MatrixRowPairAnalysis#correctedPvalue(int, int, double, int)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.linkAnalysis.MatrixRowPairAnalysis#getMetricType()
     */
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
        m.setName( "Spearman's rank correlation" );
        m.setGeneralType( GeneralType.QUANTITATIVE );
        m.setRepresentation( PrimitiveType.DOUBLE );
        m.setScale( ScaleType.LINEAR );
        return m;
    }

    /**
     * Calculate mean and sumsqsqrt for each row -- using the ranks of course!
     */
    @Override
    protected void rowStatistics() {
        int numrows = rankTransformedData.length;
        this.rowMeans = new double[numrows];
        this.rowSumSquaresSqrt = new double[numrows];
        for ( int i = 0, numcols = rankTransformedData[0].length; i < numrows; i++ ) {
            double ax = 0.0;
            double sxx = 0.0;
            for ( int j = 0; j < numcols; j++ ) {
                ax += this.rankTransformedData[i][j];
            }
            rowMeans[i] = ( ax / numcols );

            for ( int j = 0; j < numcols; j++ ) {
                double xt = this.rankTransformedData[i][j] - rowMeans[i]; /* deviation from mean */
                sxx += xt * xt; /* sum of squared error */
            }
            rowSumSquaresSqrt[i] = Math.sqrt( sxx );
        }
    }

    /**
     * @param vectorA
     * @param vectorB
     * @param usedA
     * @param usedB
     * @param i
     * @param j
     * @return
     */
    protected double spearman( double[] vectorA, double[] vectorB, boolean[] usedA, boolean[] usedB, int i, int j ) {

        /* because we assume there might be ties, we compute the correlation of the ranks. */

        /*
         * Note that if there are missing values, the precomputed ranks will be wrong. Strictly the ranks need to be
         * -recomputed-.
         */

        // first count the number of mutually present values
        int numused = 0;
        for ( int k = 0; k < vectorA.length; k++ ) {
            if ( usedA[k] && usedB[k] ) {
                numused++;
            }
        }

        if ( numused < minNumUsed ) {
            setCorrel( i, j, Double.NaN, 0 );
            return Double.NaN;
        }

        double[] xjc;
        double[] yjc;

        if ( numused == vectorA.length ) {
            xjc = vectorA;
            yjc = vectorB;
        } else {
            xjc = new double[numused];
            yjc = new double[numused];
            int v = 0;
            for ( int k = 0; k < vectorA.length; k++ ) {
                if ( usedA[k] && usedB[k] ) {
                    xjc[v] = vectorA[k];
                    yjc[v] = vectorB[k];
                    v++;
                }
            }

            /*
             * Retransform
             */
            xjc = Rank.rankTransform( new DoubleArrayList( xjc ) ).elements();
            yjc = Rank.rankTransform( new DoubleArrayList( yjc ) ).elements();
        }

        double correl = 0.0;
        double sxy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sx = 0.0;
        double sy = 0.0;
        numused = 0;
        for ( int k = 0; k < xjc.length; k++ ) {
            double xj = xjc[k];
            double yj = yjc[k];
            sx += xj;
            sy += yj;
            sxy += xj * yj;
            sxx += xj * xj;
            syy += yj * yj;
            numused++;
        }

        double denom = correlationNorm( numused, sxx, sx, syy, sy );
        if ( denom <= 0.0 ) { // means variance is zero for one of the vectors.
            setCorrel( i, j, 0.0, numused );
            return 0.0;
        }

        correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );

        // small range deviations (roundoff) are okay but shouldn't be big ones!
        assert correl < 1.0001 && correl > -1.0001;

        // roundoff protection.
        if ( correl < -1.0 )
            correl = -1.0;
        else if ( correl > 1.0 ) correl = 1.0;

        setCorrel( i, j, correl, numused );

        return correl;
    }

    /**
     * If there are no missing values.
     */
    private void calculateMetricsFast() {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        boolean docalcs = this.needToCalculateMetrics();

        if ( docalcs ) {
            getRankTransformedData( null );
        }

        /*
         * For each vector, compare it to all other vectors, avoid repeating things; skip items that don't have genes
         * mapped to them.
         */
        ExpressionDataMatrixRowElement itemA = null;
        ExpressionDataMatrixRowElement itemB = null;
        double[] vectorA = null;
        int skipped = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) {
            itemA = this.dataMatrix.getRowElement( i );
            if ( !this.hasGene( itemA ) ) {
                skipped++;
                continue;
            }
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
                setCorrel( i, j, correlFast( vectorA, vectorB, i, j ), numcols );
                ++numComputed;
            }
            if ( ( i + 1 ) % 2000 == 0 ) {
                log.info( ( i + 1 ) + " rows done, " + numComputed + " correlations computed, last row was " + itemA
                        + " " + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }
        log.info( skipped + " rows skipped, due to no BLAT association" );
        finishMetrics();
    }

    /**
     * @param usedB will be filled in, if not null. This also precomputes the row statistics (row means and sumsq
     *        deviations)
     * @return rank-transformed data, breaking ties by averaging ranks, if necessary.
     */
    private void getRankTransformedData( boolean[][] usedB ) {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        rankTransformedData = new double[numrows][];

        for ( int i = 0; i < numrows; i++ ) {

            Double[] row = this.dataMatrix.getRow( i );

            // make a copy.
            double[] r = new double[row.length];
            for ( int m = 0, v = row.length; m < v; m++ ) {
                r[m] = row[m];
            }

            DoubleArrayList ranksIA = Rank.rankTransform( new DoubleArrayList( r ) );
            assert ranksIA != null;
            double ri[] = new double[ranksIA.size()];
            for ( int n = 0, w = ranksIA.size(); n < w; n++ ) {
                ri[n] = ranksIA.get( n );
            }

            rankTransformedData[i] = ri;

            if ( usedB != null ) {
                for ( int j = 0; j < numcols; j++ ) {
                    usedB[i][j] = used.get( i, j ); // this is only needed if we use it below, speeds things up
                    // slightly.
                }
            }
        }

        rowStatistics();
    }
}
