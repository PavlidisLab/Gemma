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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.NamedMatrix;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import cern.colt.bitvector.BitMatrix;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * A correlation analysis for a given data set, designed for selection of values based on critera set by the user.
 * <p>
 * On the first pass over the data, a histogram is filled in to hold the distribution of the values found. You can set
 * criteria to have the correlations actually stored in a (sparse) matrix. This can take a lot of memory if you store
 * everything!
 * <p>
 * The correlation is only calculated if it isn't stored in the matrix, and values can be tested against a threshold.
 * <p>
 * This class is used in reality by one pass over the data to fill in the histogram. This is used to help select a
 * threshold. A second pass over the data is used to select correlations that meet the criteria.
 * <p>
 * 
 * @author Paul Pavlidis
 * @version $Id$
 */
public class MatrixRowPairPearsonAnalysis implements MatrixRowPairAnalysis {

    private static final int NUM_BINS = 2048;
    private static final int HALF_BIN = NUM_BINS / 2;
    private double storageThresholdValue = 0.5;
    private CompressedSparseDoubleMatrix2DNamed C = null;
    private double[] rowMeans = null;
    private double[] rowSumSquaresSqrt = null;
    private boolean[] hasMissing = null;
    private ObjectArrayList keepers = null;
    private double upperTailThreshold = 0.0;
    private boolean useAbsoluteValue = false;
    private double lowerTailThreshold = 0.0;
    private double pValueThreshold = 0.0;
    private boolean histogramIsFilled = false;
    private Map<CompositeSequence, Collection<Gene>> probeToGeneMap = null;
    private Map<Gene, Collection<CompositeSequence>> geneToProbeMap = null;
    private BitMatrix used = null;
    private int numMissing;
    private double globalTotal = 0.0; // used to store the running total of the matrix values.
    private double globalMean = 0.0; // mean of the entire distribution.
    private int numVals = 0; // number of values actually stored in the matrix
    protected static final Log log = LogFactory.getLog( MatrixRowPairPearsonAnalysis.class );
    private ExpressionDataDoubleMatrix dataMatrix;
    private boolean[] hasGenesCache;
    private Map<ExpressionDataMatrixRowElement, DesignElement> rowMapCache = new HashMap<ExpressionDataMatrixRowElement, DesignElement>();

    private int[] fastHistogram = new int[NUM_BINS];

    /**
     *
     */
    private MatrixRowPairPearsonAnalysis() {
        this( 0 );
    }

    /**
     * @param size Dimensions of the required (square) matrix.
     */
    private MatrixRowPairPearsonAnalysis( int size ) {
        if ( size > 0 ) {
            C = DoubleMatrix2DNamedFactory.compressedsparse( size, size );
        }
        keepers = new ObjectArrayList();
    }

    /**
     * @param
     */
    public MatrixRowPairPearsonAnalysis( ExpressionDataDoubleMatrix dataMatrix ) {
        this( dataMatrix.rows() );
        this.dataMatrix = dataMatrix;
        this.numMissing = this.fillUsed();
    }

    /**
     * Initialize caches.
     */
    private void init() {
        List<ExpressionDataMatrixRowElement> rowElements = this.dataMatrix.getRowElements();
        hasGenesCache = new boolean[rowElements.size()];

        for ( ExpressionDataMatrixRowElement element : rowElements ) {

            DesignElement de = element.getDesignElement();
            rowMapCache.put( element, de );

            Collection<Gene> geneIdSet = this.probeToGeneMap.get( de );
            Integer i = element.getIndex();
            hasGenesCache[i] = geneIdSet != null && geneIdSet.size() > 0;

        }
        assert rowMapCache.size() > 0;
        log.info( "Initialized caches for probe/gene information" );
    }

    /**
     * @param dataMatrix DenseDoubleMatrix2DNamed
     * @param tmts Values of the correlation that are deemed too small to store in the matrix. Setting this as high as
     *        possible can greatly reduce memory requirements, but can slow things down.
     */
    public MatrixRowPairPearsonAnalysis( ExpressionDataDoubleMatrix dataMatrix, double tmts ) {
        this( dataMatrix );
        this.setStorageThresholdValue( tmts );
    }

    /**
     * Identify the correlations that are above the set thresholds.
     * 
     * @return cern.colt.list.ObjectArrayList
     */
    public ObjectArrayList getKeepers() {
        return keepers;
    }

    /**
     * @return The number of values stored in the correlation matrix.
     */
    public int numCached() {
        return C.cardinality();
    }

    /**
     * Set the threshold, above which correlations are kept.
     * 
     * @param k double
     */
    public void setUpperTailThreshold( double k ) {
        upperTailThreshold = k;
    }

    /**
     * Set the threshold, below which correlations are kept (e.g., negative values)
     * 
     * @param k double
     */
    public void setLowerTailThreshold( double k ) {
        lowerTailThreshold = k;
    }

    /**
     * If set to true, then the absolute value of the correlation is used for histograms and choosing correlations to
     * keep. The correlation matrix, if actually used to store all the values, maintains the actual number.
     * 
     * @param k boolean
     */
    public void setUseAbsoluteValue( boolean k ) {
        useAbsoluteValue = k;
    }

    /**
     * @param k double
     */
    public void setPValueThreshold( double k ) {
        if ( k < 0.0 || k > 1.0 ) {
            throw new IllegalArgumentException(
                    "P value threshold must be greater or equal to zero than 0.0 and less than or equal to 1.0" );
        }
        this.pValueThreshold = k;
    }

    /**
     * 
     */
    public void setDuplicateMap( Map<CompositeSequence, Collection<Gene>> probeToGeneMap,
            Map<Gene, Collection<CompositeSequence>> geneToProbeMap ) {
        this.geneToProbeMap = geneToProbeMap;
        this.probeToGeneMap = probeToGeneMap;
        init();
    }

    /**
     * Set an (absolute value) correlation, below which values are not maintained in the correlation matrix. They are
     * still kept in the histogram. (In some implementations this can greatly reduce the memory requirements for the
     * correlation matrix).
     * 
     * @param k double
     */
    private void setStorageThresholdValue( double k ) {
        if ( k < 0.0 || k > 1.0 ) {
            throw new IllegalArgumentException( "Correlation must be given as between 0 and 1" );
        }
        storageThresholdValue = k;
    }

    /**
     * Read back the histogram as a DoubleArrayList of counts.
     * 
     * @return cern.colt.list.DoubleArrayList
     * @todo - put this somewhere more generically useful!
     */
    public DoubleArrayList getHistogramArrayList() {
        DoubleArrayList r = new DoubleArrayList( fastHistogram.length );
        for ( int i = 0; i < fastHistogram.length; i++ ) {
            r.add( fastHistogram[i] );
        }
        return r;
    }

    /**
     * @return baseCode.dataStructure.NamedMatrix
     */
    public NamedMatrix getMatrix() {
        return C;
    }

    /**
     * Flag the correlation matrix as un-fillable. This means that when PearsonMatrix is called, only the histogram will
     * be filled in. Also trashes any values that might have been stored there.
     */
    public void nullMatrix() {
        C = null;
    }

    /**
     * @param ival double[]
     * @param jval double[]
     * @param i int
     * @param j int
     * @return double
     */
    private double correlFast( double[] ival, double[] jval, int i, int j ) {
        double sxy = 0.0;
        for ( int k = 0, n = ival.length; k < n; k++ ) {
            sxy += ( ival[k] - rowMeans[i] ) * ( jval[k] - rowMeans[j] );
        }
        return sxy / ( rowSumSquaresSqrt[i] * rowSumSquaresSqrt[j] );
    }

    /**
     * Skip the probes without blat association
     */
    private boolean hasGene( ExpressionDataMatrixRowElement rowEl ) {
        return hasGenesCache[rowEl.getIndex()];
    }

    /**
     * @param rowEl
     * @return
     */
    public DesignElement getProbeForRow( ExpressionDataMatrixRowElement rowEl ) {
        return this.rowMapCache.get( rowEl );
    }

    /**
     * Calculate a linear correlation matrix for a matrix. Use this if you know there are no missing values, or don't
     * care about NaNs.
     * 
     * @param duplicates The map containing information about what items are the 'same' as other items; such are
     *        skipped.
     */
    private void calculateMetricsFast() {
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        boolean docalcs = this.needToCalculateMetrics();

        double[][] data = null;
        if ( docalcs ) {
            rowStatistics();

            // Temporarily put the data in this matrix (performance)
            data = new double[numrows][numcols];
            for ( int i = 0; i < numrows; i++ ) { // first vector
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    data[i][j] = this.dataMatrix.get( i, j );
                }
            }
        }

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
                vectorA = data[i];
            }

            for ( int j = i + 1; j < numrows; j++ ) {
                itemB = this.dataMatrix.getRowElement( j );
                if ( !this.hasGene( itemB ) ) continue;
                if ( !docalcs || C.getQuick( i, j ) != 0.0 ) { // second pass over matrix. Don't calculate it if we
                    // already have it. Just do the requisite checks.
                    keepCorrel( i, j, C.getQuick( i, j ), numcols );
                    continue;
                }

                setCorrel( i, j, correlFast( vectorA, data[j], i, j ), numcols );
                ++numComputed;
            }
            if ( ++count % 2000 == 0 ) {
                log.info( count + " rows done, " + numComputed + " correlations computed, last row was " + itemA + " "
                        + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }

        finishMetrics();

    }

    /**
     * Calculate the linear correlation matrix of a matrix, allowing missing values. If there are no missing values,
     * this calls PearsonFast.
     */
    public void calculateMetrics() {

        if ( this.numMissing == 0 ) {
            calculateMetricsFast();
            return;
        }

        int numused;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();

        boolean docalcs = this.needToCalculateMetrics();
        boolean[][] usedB = null;
        double[][] data = null;
        if ( docalcs ) {
            // Temporarily copy the data in this matrix, for performance.
            usedB = new boolean[numrows][numcols];
            data = new double[numrows][numcols];
            for ( int i = 0; i < numrows; i++ ) { // first vector
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    usedB[i][j] = used.get( i, j ); // this is only needed if we use it below, speeds things up
                    // slightly.
                    data[i][j] = this.dataMatrix.get( i, j );
                }
            }
        }

        /* for each vector, compare it to all other vectors */
        ExpressionDataMatrixRowElement itemA = null;
        double[] vectorA = null;
        double syy, sxy, sxx, sx, sy, xj, yj;
        int count = 0;
        int numComputed = 0;
        for ( int i = 0; i < numrows; i++ ) { // first vector
            itemA = this.dataMatrix.getRowElement( i );
            if ( !this.hasGene( itemA ) ) continue;
            if ( docalcs ) {
                rowStatistics();
                vectorA = data[i];
            }

            boolean thisRowHasMissing = hasMissing[i];

            for ( int j = i + 1; j < numrows; j++ ) { // second vector
                ExpressionDataMatrixRowElement itemB = this.dataMatrix.getRowElement( j );
                if ( !this.hasGene( itemB ) ) continue;

                // second pass over matrix? Don't calculate it if we already have it. Just do the requisite checks.
                if ( !docalcs || C.getQuick( i, j ) != 0.0 ) {
                    keepCorrel( i, j, C.getQuick( i, j ), numcols );
                    continue;
                }

                double[] vectorB = data[j];

                /* if there are no missing values, use the faster method of calculation */
                if ( !thisRowHasMissing && !hasMissing[j] ) {
                    setCorrel( i, j, correlFast( vectorA, vectorB, i, j ), numcols );
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
                    if ( usedB[i][k] && usedB[j][k] ) { /* this is a bit faster */
                        sx += xj;
                        sy += yj;
                        sxy += xj * yj;
                        sxx += xj * xj;
                        syy += yj * yj;
                        numused++;
                    }
                }

                double denom = correlationNorm( numused, sxx, sx, syy, sy );
                if ( denom <= 0.0 ) { // means variance is zero for one of the vectors.
                    setCorrel( i, j, 0.0, numused );
                } else {
                    double correl = ( sxy - sx * sy / numused ) / Math.sqrt( denom );
                    setCorrel( i, j, correl, numused );
                }
                ++numComputed;

            }
            if ( ++count % 2000 == 0 ) {
                log.info( count + " rows done, " + numComputed + " correlations computed, last row was " + itemA + " "
                        + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) );
            }
        }
        finishMetrics();
    }

    /**
     * @return double
     * @param n int
     * @param sxx double
     * @param sx double
     * @param syy double
     * @param sy double
     */
    private double correlationNorm( int n, double sxx, double sx, double syy, double sy ) {
        return ( sxx - sx * sx / n ) * ( syy - sy * sy / n );
    }

    /**
     * Store information about whether data includes missing values.
     * 
     * @return int
     */
    private int fillUsed() {

        int numMissing = 0;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        hasMissing = new boolean[numrows];

        if ( used == null ) {
            used = new BitMatrix( numrows, numcols );
        }

        for ( int i = 0; i < numrows; i++ ) {
            int rowmissing = 0;
            for ( int j = 0; j < numcols; j++ ) {
                if ( Double.isNaN( this.dataMatrix.get( i, j ) ) ) {
                    numMissing++;
                    rowmissing++;
                    used.put( i, j, false );
                } else {
                    used.put( i, j, true );
                }
            }
            hasMissing[i] = ( rowmissing > 0 );
        }

        if ( numMissing == 0 ) {
            log.info( "No missing values" );
        } else {
            log.info( numMissing + " missing values" );
        }
        return numMissing;
    }

    /**
     * Get pvalue corrected for multiple testing of the genes. We conservatively penalize the pvalues for each
     * additional test the gene received. For example, if correlation is between two probes that each assay two genes,
     * the pvalue is penalized by a factor of 4.0.
     * 
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     * @return double
     */
    private double correctedPvalue( int i, int j, double correl, int numused ) {

        double p = CorrelationStats.pvalue( correl, numused );

        double k = 1, m = 1;
        Collection<Gene> geneIdSet = getGenesForRow( i );
        if ( geneIdSet != null ) {
            for ( Gene geneId : geneIdSet ) {
                int tmpK = this.geneToProbeMap.get( geneId ).size() + 1;
                if ( k < tmpK ) k = tmpK;
            }
        }

        geneIdSet = getGenesForRow( j );
        if ( geneIdSet != null ) {
            for ( Gene geneId : geneIdSet ) {
                int tmpM = this.geneToProbeMap.get( geneId ).size() + 1;
                if ( m < tmpM ) m = tmpM;
            }
        }

        return p * k * m;
    }

    /**
     * @param j
     * @return
     */
    private Collection<Gene> getGenesForRow( int j ) {
        return this.probeToGeneMap.get( getProbeForRow( dataMatrix.getRowElement( j ) ) );
    }

    /**
     * Decide whether to keep the correlation.
     * 
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     * @return boolean
     */
    private boolean keepCorrel( int i, int j, double correl, int numused ) {

        if ( keepers == null ) {
            return false;
        }

        double acorrel = Math.abs( correl );

        if ( acorrel < storageThresholdValue ) {
            // return false;
        }

        double c;
        if ( useAbsoluteValue ) {
            c = acorrel;
        } else {
            c = correl;
        }

        if ( upperTailThreshold != 0.0 && c > upperTailThreshold
                && correctedPvalue( i, j, correl, numused ) < this.pValueThreshold ) {

            keepers.add( new Link( i, j, correl ) );
            return true;
        }

        else if ( !useAbsoluteValue && lowerTailThreshold != 0.0 && c < lowerTailThreshold
                && correctedPvalue( i, j, correl, numused ) < this.pValueThreshold ) {
            keepers.add( new Link( i, j, correl ) );
            return true;
        }

        return false;
    }

    /**
     * Checks for valid values of correlation and encoding.
     * 
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     */
    private void setCorrel( int i, int j, double correl, int numused ) {
        double acorrel = Math.abs( correl );

        // it is possible, due to roundoff, to overflow the bins.
        int lastBinIndex = fastHistogram.length - 1;

        if ( !histogramIsFilled ) {
            if ( useAbsoluteValue ) {
                int bin = Math.min( ( int ) ( ( 1.0 + acorrel ) * HALF_BIN ), lastBinIndex );
                fastHistogram[bin]++;
                globalTotal += acorrel;
                // histogram.fill( acorrel ); // this is suprisingly slow due to zillions of calls to Math.floor.
            } else {
                globalTotal += correl;
                int bin = Math.min( ( int ) ( ( 1.0 + correl ) * HALF_BIN ), lastBinIndex );
                fastHistogram[bin]++;
                // histogram.fill( correl );
            }
            numVals++;
        }

        if ( acorrel > storageThresholdValue && C != null ) {
            C.setQuick( i, j, correl );
        }

        keepCorrel( i, j, correl, numused );

    }

    /**
     * @return java.lang.String
     */
    @Override
    public String toString() {
        return C.toString();
    }

    /**
     * Tests whether the correlations still need to be calculated for final retrieval, or if they can just be retrieved.
     * This looks at the current settings and decides whether the value would already have been cached.
     * 
     * @return boolean
     */
    private boolean needToCalculateMetrics() {

        /* are we on the first pass, or haven't stored any values? */
        if ( !histogramIsFilled || C == null ) {
            return true;
        }

        if ( this.storageThresholdValue > 0.0 ) {

            if ( this.useAbsoluteValue ) {
                if ( upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    log.info( "Second pass, have to recompute some values" );
                    return false;
                }
            } else {
                if ( Math.abs( lowerTailThreshold ) > storageThresholdValue
                        && upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    log.info( "Second pass, have to recompute some values" );
                    return false;
                }
            }
        }
        log.info( "Second pass, good news, all values are cached" );
        return true;
    }

    /**
     * Calculate mean and sumsqsqrt for each row
     */
    private void rowStatistics() {
        ;
        int numrows = dataMatrix.rows();
        this.rowMeans = new double[numrows];
        this.rowSumSquaresSqrt = new double[numrows];
        for ( int i = 0, numcols = dataMatrix.columns(); i < numrows; i++ ) {
            double ax = 0.0;
            double sxx = 0.0;
            for ( int j = 0; j < numcols; j++ ) {
                ax += this.dataMatrix.get( i, j );
            }
            rowMeans[i] = ( ax / numcols );

            for ( int j = 0; j < numcols; j++ ) {
                double xt = this.dataMatrix.get( i, j ) - rowMeans[i]; /* deviation from mean */
                sxx += xt * xt; /* sum of squared error */
            }
            rowSumSquaresSqrt[i] = Math.sqrt( sxx );
        }
    }

    /**
     * @param duplicates Map
     */
    private void finishMetrics() {
        if ( !this.histogramIsFilled && this.probeToGeneMap != null && this.geneToProbeMap != null ) {
            // log.info( "Skipped " + duplicateSkip + " pairs of duplicates" );
        }
        this.histogramIsFilled = true;
        globalMean = globalTotal / numVals;
    }

    /**
     * @return double
     */
    public double kurtosis() {
        if ( !histogramIsFilled ) {
            throw new IllegalStateException( "Don't call kurtosis when histogram isn't filled!" );
        }

        double sumfour = 0.0;
        double sumsquare = 0.0;
        for ( int i = 0, n = C.rows(); i < n; i++ ) {
            for ( int j = i + 1, m = C.columns(); j < m; j++ ) {
                double r;
                if ( C.getQuick( i, j ) != 0 ) {
                    r = C.getQuick( i, j );
                } else {
                    r = 0;
                    /** @todo calculate the value */
                }
                double deviation = r - globalMean;
                sumfour += Math.pow( deviation, 4.0 );
                sumsquare += deviation * deviation;
            }
        }
        return sumsquare * numVals * ( numVals + 1.0 ) / ( numVals - 1.0 ) - 3 * sumsquare * sumsquare / ( numVals - 2 )
                * ( numVals - 3 );
    }

    public double getScoreInBin( int i ) {
        // bin 2048 = correlation of 1.0 2048/1024 - 1 = 1
        // bin 1024 = correlation of 0.0 1024/1024 - 1 = 0
        // bin 0 = correlation of -1.0 : 0/1024 - 1 = -1
        return ( ( double ) i / HALF_BIN ) - 1.0;
    }

    public QuantitationType getMetricType() {
        QuantitationType m = QuantitationType.Factory.newInstance();
        m.setIsBackground( false );
        m.setIsBackgroundSubtracted( false );
        m.setIsNormalized( false );
        m.setIsPreferred( false );
        m.setIsRatio( false );
        m.setType( StandardQuantitationType.CORRELATION );
        m.setName( "Pearson correlation" );
        m.setGeneralType( GeneralType.QUANTITATIVE );
        m.setRepresentation( PrimitiveType.DOUBLE );
        m.setScale( ScaleType.LINEAR );
        return m;
    }

}
