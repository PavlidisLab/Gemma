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
package ubic.gemma.core.analysis.expression.coexpression.links;

import cern.colt.bitvector.BitMatrix;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.Link;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.Matrix2D;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.util.*;

/**
 * @author paul
 */
public abstract class AbstractMatrixRowPairAnalysis implements MatrixRowPairAnalysis {
    /**
     * Absolute lower limit to minNumUsed. (This used to be 3, and then 5). It doesn't make much sense to set this
     * higher than ExpressionExperimentFilter.MIN_NUMBER_OF_SAMPLES_PRESENT
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final int HARD_LIMIT_MIN_NUM_USED = 8;
    static final Log log = LogFactory.getLog( PearsonMetrics.class );
    private static final int HALF_BIN = MatrixRowPairAnalysis.NUM_BINS / 2;
    private final int[] fastHistogram = new int[MatrixRowPairAnalysis.NUM_BINS];
    private final Map<ExpressionDataMatrixRowElement, CompositeSequence> rowMapCache = new HashMap<>();
    ExpressionDataDoubleMatrix dataMatrix;
    CompressedSparseDoubleMatrix<ExpressionDataMatrixRowElement, ExpressionDataMatrixRowElement> results = null;
    BitMatrix used = null;
    Map<Gene, Collection<CompositeSequence>> geneToProbeMap = null;
    boolean[] hasMissing = null;
    ObjectArrayList keepers = null;
    /**
     * If fewer than this number values are available, the correlation is rejected. This helps keep the correlation
     * distribution reasonable. This is primarily relevant when there are missing values in the data, but to be
     * consistent we check it for other cases as well.
     */
    int minNumUsed = AbstractMatrixRowPairAnalysis.HARD_LIMIT_MIN_NUM_USED;
    // store total number of missing values.
    int numMissing;
    private double globalMean = 0.0; // mean of the entire distribution.
    private double globalTotal = 0.0; // used to store the running total of the matrix values.
    private boolean[] hasGenesCache;
    private boolean histogramIsFilled = false;
    private double lowerTailThreshold = 0.0;
    private int numVals = 0; // number of values actually stored in the matrix
    private Map<CompositeSequence, Set<Gene>> probeToGeneMap = null;
    private double pValueThreshold = 0.0;
    private double storageThresholdValue;
    private double upperTailThreshold = 0.0;
    private boolean useAbsoluteValue = false;
    private boolean usePvalueThreshold = true;
    private long crossHybridizationRejections = 0;
    private int numUniqueGenes = 0;
    private boolean omitNegativeCorrelationLinks = false;

    /**
     * Read back the histogram as a DoubleArrayList of counts.
     *
     * @return cern.colt.list.DoubleArrayList
     */
    @Override
    public DoubleArrayList getHistogramArrayList() {
        DoubleArrayList r = new DoubleArrayList( fastHistogram.length );
        for ( int aFastHistogram : fastHistogram ) {
            r.add( aFastHistogram );
        }
        return r;
    }

    /**
     * Identify the correlations that are above the set thresholds.
     *
     * @return cern.colt.list.ObjectArrayList
     */
    @Override
    public ObjectArrayList getKeepers() {
        return keepers;
    }

    @Override
    public CompositeSequence getProbeForRow( ExpressionDataMatrixRowElement rowEl ) {
        return this.rowMapCache.get( rowEl );
    }

    @Override
    public double getScoreInBin( int i ) {
        // bin 2048 = correlation of 1.0 2048/1024 - 1 = 1
        // bin 1024 = correlation of 0.0 1024/1024 - 1 = 0
        // bin 0 = correlation of -1.0 : 0/1024 - 1 = -1
        return ( ( double ) i / AbstractMatrixRowPairAnalysis.HALF_BIN ) - 1.0;
    }

    /**
     * @return The number of values stored in the correlation matrix.
     */
    @Override
    public int numCached() {
        return results.cardinality();
    }

    @Override
    public void setDuplicateMap( Map<CompositeSequence, Set<Gene>> probeToGeneMap ) {
        this.probeToGeneMap = probeToGeneMap;
        this.init();
    }

    /**
     * Set the threshold, below which correlations are kept (e.g., negative values)
     *
     * @param k double
     */
    @Override
    public void setLowerTailThreshold( double k ) {
        lowerTailThreshold = k;
    }

    /**
     * Set the number of mutually present values in a pairwise comparison that must be attained before the correlation
     * is stored. Note that you cannot set the value less than HARD_LIMIT_MIN_NUM_USED.
     */
    @Override
    public void setMinNumpresent( int minSamplesToKeepCorrelation ) {
        if ( minSamplesToKeepCorrelation > AbstractMatrixRowPairAnalysis.HARD_LIMIT_MIN_NUM_USED )
            this.minNumUsed = minSamplesToKeepCorrelation;
    }

    /**
     * @param omitNegativeCorrelationLinks the omitNegativeCorrelationLinks to set
     */
    @Override
    public void setOmitNegativeCorrelationLinks( boolean omitNegativeCorrelationLinks ) {
        this.omitNegativeCorrelationLinks = omitNegativeCorrelationLinks;
    }

    /**
     * @param k double
     */
    @Override
    public void setPValueThreshold( double k ) {
        if ( k < 0.0 || k > 1.0 ) {
            throw new IllegalArgumentException(
                    "P value threshold must be greater or equal to zero than 0.0 and less than or equal to 1.0" );
        }
        this.pValueThreshold = k;
    }

    /**
     * Set the threshold, above which correlations are kept.
     *
     * @param k double
     */
    @Override
    public void setUpperTailThreshold( double k ) {
        upperTailThreshold = k;
    }

    /**
     * If set to true, then the absolute value of the correlation is used for histograms and choosing correlations to
     * keep. The correlation matrix, if actually used to store all the values, maintains the actual number.
     *
     * @param k boolean
     */
    @Override
    public void setUseAbsoluteValue( boolean k ) {
        useAbsoluteValue = k;
    }

    /**
     * @return baseCode.dataStructure.NamedMatrix
     */
    @SuppressWarnings("unused") // Possible external use
    public Matrix2D<ExpressionDataMatrixRowElement, ExpressionDataMatrixRowElement, Double> getMatrix() {
        return results;
    }

    /**
     * @return the usePvalueThreshold
     */
    @SuppressWarnings("unused") // Possible external use
    public boolean isUsePvalueThreshold() {
        return usePvalueThreshold;
    }

    /**
     * @param usePvalueThreshold the usePvalueThreshold to set
     */
    @Override
    public void setUsePvalueThreshold( boolean usePvalueThreshold ) {
        this.usePvalueThreshold = usePvalueThreshold;
    }

    @Override
    public long getCrossHybridizationRejections() {
        return crossHybridizationRejections;
    }

    @Override
    public double getNumUniqueGenes() {
        return numUniqueGenes;
    }

    @Override
    public int size() {
        return this.dataMatrix.rows();
    }

    /**
     * @return double
     */
    @SuppressWarnings("unused") // Possible external use
    public double kurtosis() {
        if ( !histogramIsFilled ) {
            throw new IllegalStateException( "Don't call kurtosis when histogram isn't filled!" );
        }
        double sumsquare = 0.0;
        for ( int i = 0, n = results.rows(); i < n; i++ ) {
            for ( int j = i + 1, m = results.columns(); j < m; j++ ) {
                double r;
                if ( results.get( i, j ) != 0 ) {
                    r = results.get( i, j );
                } else {
                    r = 0;
                    // todo calculate the value
                }
                double deviation = r - globalMean;
                sumsquare += deviation * deviation;
            }
        }
        return sumsquare * numVals * ( numVals + 1.0 ) / ( numVals - 1.0 )
                - 3 * sumsquare * sumsquare / ( numVals - 2 ) * ( numVals - 3 );
    }

    /**
     * Flag the correlation matrix as un-fillable. This means that when PearsonMatrix is called, only the histogram will
     * be filled in. Also trashes any values that might have been stored there.
     */
    @SuppressWarnings("unused") // Possible external use
    public void nullMatrix() {
        results = null;
    }

    /**
     * @return java.lang.String
     */
    @Override
    public String toString() {
        return results.toString();
    }

    @SuppressWarnings("unused") // Possible external use
    protected abstract void rowStatistics();

    /**
     * Get correlation pvalue corrected for multiple testing of the genes by different probes.
     * Current implementation: We conservatively penalize the p-values for each additional test the gene received. For
     * example, if correlation is between two genes that are each assayed twice on the platform, the pvalue is penalized
     * by a factor of 4.0. If either probe assays more than one gene, we penalize according to the gene which is tested
     * the most times on the platform.
     *
     * @param i       int
     * @param j       int
     * @param correl  double
     * @param numused int
     * @return double (can be greater than 1.0, we don't care)
     */
    double correctedPvalue( int i, int j, double correl, int numused ) {

        // raw value.
        double p = CorrelationStats.pvalue( correl, numused );

        return p * this.numberOfTestsForGeneAtRow( i ) * this.numberOfTestsForGeneAtRow( j );
    }

    /**
     * Store information about whether data includes missing values.
     *
     * @return int
     */
    int fillUsed() {

        int missingCount = 0;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();
        hasMissing = new boolean[numrows];

        if ( used == null ) {
            used = new BitMatrix( numrows, numcols );
        }

        for ( int i = 0; i < numrows; i++ ) {
            int rowmissing = 0;
            for ( int j = 0; j < numcols; j++ ) {
                if ( Double.isNaN( this.dataMatrix.getAsDouble( i, j ) ) ) {
                    missingCount++;
                    rowmissing++;
                    used.put( i, j, false );
                } else {
                    used.put( i, j, true );
                }
            }
            hasMissing[i] = ( rowmissing > 0 );
        }

        if ( missingCount == 0 ) {
            AbstractMatrixRowPairAnalysis.log.info( "No missing values" );
        } else {
            AbstractMatrixRowPairAnalysis.log.info( missingCount + " missing values" );
        }
        return missingCount;
    }

    void finishMetrics() {
        this.histogramIsFilled = true;
        globalMean = globalTotal / numVals;
    }

    Set<Gene> getGenesForRow( int j ) {
        return this.probeToGeneMap.get( this.getProbeForRow( dataMatrix.getRowElement( j ) ) );
    }

    /**
     * Skip the probes without blat association
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    // Better semantics
    boolean hasGene( ExpressionDataMatrixRowElement rowEl ) {
        return hasGenesCache[rowEl.getIndex()];
    }

    /**
     * Decide whether to keep the correlation. The correlation must be greater or equal to the set thresholds.
     */
    void keepCorrellation( int i, int j, double correl, int numused ) {

        if ( keepers == null ) {
            return;
        }

        if ( Double.isNaN( correl ) )
            return;

        if ( omitNegativeCorrelationLinks && correl < 0.0 ) {
            return;
        }

        double acorrel = Math.abs( correl );

        double c;
        if ( useAbsoluteValue ) {
            c = acorrel;
        } else {
            c = correl;
        }

        if ( upperTailThreshold != 0.0 && c >= upperTailThreshold && ( !this.usePvalueThreshold
                || this.correctedPvalue( i, j, correl, numused ) <= this.pValueThreshold ) ) {

            keepers.add( new Link( i, j, correl ) );
        } else if ( !useAbsoluteValue && lowerTailThreshold != 0.0 && c <= lowerTailThreshold && (
                !this.usePvalueThreshold || this.correctedPvalue( i, j, correl, numused ) <= this.pValueThreshold ) ) {
            keepers.add( new Link( i, j, correl ) );
        }

    }

    /**
     * Tests whether the correlations still need to be calculated for final retrieval, or if they can just be retrieved.
     * This looks at the current settings and decides whether the value would already have been cached.
     *
     * @return boolean
     */
    boolean needToCalculateMetrics() {

        /* are we on the first pass, or haven't stored any values? */
        if ( !histogramIsFilled || results == null ) {
            return true;
        }

        if ( this.storageThresholdValue > 0.0 ) {

            if ( this.useAbsoluteValue ) {
                if ( upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    AbstractMatrixRowPairAnalysis.log.info( "Second pass, good news, all values are cached" );
                    return false;
                }
            } else {
                if ( Math.abs( lowerTailThreshold ) > storageThresholdValue
                        && upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    AbstractMatrixRowPairAnalysis.log.info( "Second pass, good news, all values are cached" );
                    return false;
                }
            }
        }
        AbstractMatrixRowPairAnalysis.log.info( "Second pass, have to recompute some values" );
        return true;
    }

    void computeRow( StopWatch timer, ExpressionDataMatrixRowElement itemA, int numComputed, int i ) {
        if ( ( i + 1 ) % 2000 == 0 ) {
            double t = timer.getTime() / 1000.0;
            AbstractMatrixRowPairAnalysis.log
                    .info( ( i + 1 ) + " rows done, " + numComputed + " correlations computed, last row was " + itemA
                            + " " + ( keepers.size() > 0 ? keepers.size() + " scores retained" : "" ) + String
                            .format( ", time elapsed since last check: %.2f", t ) + "s" );
            timer.reset();
            timer.start();
        }
    }

    int computeMetrics( int numrows, int numcols, boolean docalcs, StopWatch timer, int skipped, int numComputed,
            double[][] data ) {
        ExpressionDataMatrixRowElement itemA;
        double[] vectorA = null;
        for ( int i = 0; i < numrows; i++ ) {
            itemA = this.dataMatrix.getRowElement( i );
            if ( !this.hasGene( itemA ) ) {
                skipped++;
                continue;
            }
            if ( docalcs ) {
                vectorA = data[i];
            }

            numComputed = this.getNumComputed( numrows, numcols, docalcs, data, timer, itemA, vectorA, numComputed, i );
        }
        return skipped;
    }

    abstract double correlFast( double[] ival, double[] jval, int i, int j );

    /**
     * Checks for valid values of correlation and encoding.
     */
    void setCorrel( int i, int j, double correl, int numused ) {

        if ( this.crossHybridizes( i, j ) ) {
            crossHybridizationRejections++;
            return;
        }

        if ( Double.isNaN( correl ) )
            return;

        if ( correl < -1.00001 || correl > 1.00001 ) {
            throw new IllegalArgumentException( "Correlation out of valid range: " + correl );
        }

        if ( correl < -1.0 ) {
            correl = -1.0;
        } else if ( correl > 1.0 ) {
            correl = 1.0;
        }

        double acorrel = Math.abs( correl );

        // it is possible, due to roundoff, to overflow the bins.
        int lastBinIndex = fastHistogram.length - 1;
        if ( !histogramIsFilled ) {

            if ( useAbsoluteValue ) {
                int bin = Math
                        .min( ( int ) ( ( 1.0 + acorrel ) * AbstractMatrixRowPairAnalysis.HALF_BIN ), lastBinIndex );
                fastHistogram[bin]++;
                globalTotal += acorrel;
                // histogram.fill( acorrel ); // this is suprisingly slow due to zillions of calls to Math.floor.
            } else {
                globalTotal += correl;
                int bin = Math
                        .min( ( int ) ( ( 1.0 + correl ) * AbstractMatrixRowPairAnalysis.HALF_BIN ), lastBinIndex );
                fastHistogram[bin]++;
                // histogram.fill( correl );
            }
            numVals++;
        }

        if ( acorrel > storageThresholdValue && results != null ) {
            results.set( i, j, correl );
        }

        this.keepCorrellation( i, j, correl, numused );

    }

    /**
     * Set an (absolute value) correlation, below which values are not maintained in the correlation matrix. They are
     * still kept in the histogram. (In some implementations this can greatly reduce the memory requirements for the
     * correlation matrix).
     */
    void setStorageThresholdValue( double k ) {
        if ( k < 0.0 || k > 1.0 ) {
            throw new IllegalArgumentException( "Correlation must be given as between 0 and 1" );
        }
        storageThresholdValue = k;
    }

    private int getNumComputed( int numrows, int numcols, boolean docalcs, double[][] data, StopWatch timer,
            ExpressionDataMatrixRowElement itemA, double[] vectorA, int numComputed, int i ) {
        ExpressionDataMatrixRowElement itemB;
        for ( int j = i + 1; j < numrows; j++ ) {
            itemB = this.dataMatrix.getRowElement( j );
            if ( !this.hasGene( itemB ) )
                continue;
            if ( !docalcs || results.get( i, j ) != 0.0 ) { // second pass over matrix. Don't calculate it
                // if we
                // already have it. Just do the requisite checks.
                this.keepCorrellation( i, j, results.get( i, j ), numcols );
                continue;
            }

            double[] vectorB = data[j];
            this.setCorrel( i, j, this.correlFast( vectorA, vectorB, i, j ), numcols );
            ++numComputed;
        }
        this.computeRow( timer, itemA, numComputed, i );
        return numComputed;
    }

    /**
     * Initialize caches.
     */
    private void init() {

        this.initGeneToProbeMap();

        List<ExpressionDataMatrixRowElement> rowElements = this.dataMatrix.getRowElements();
        hasGenesCache = new boolean[rowElements.size()];

        for ( ExpressionDataMatrixRowElement element : rowElements ) {

            CompositeSequence de = element.getDesignElement();
            rowMapCache.put( element, de );

            Set<Gene> geneIdSet = this.probeToGeneMap.get( de );
            int i = element.getIndex();
            hasGenesCache[i] = geneIdSet != null && geneIdSet.size() > 0;

        }
        assert rowMapCache.size() > 0;
        AbstractMatrixRowPairAnalysis.log.info( "Initialized caches for probe/gene information" );
    }

    /**
     * Determine if the probes at this location in the matrix assay any of the same gene(s). This has nothing to do with
     * whether the probes are specific, though non-specific probes (which hit more than one gene) are more likely to be
     * affected by this.
     *
     * @return true if the probes hit the same gene; false otherwise. If the probes hit more than one gene, and any of
     * the genes are in common, the result is 'true'.
     */
    private boolean crossHybridizes( int i, int j ) {
        if ( this.dataMatrix == null )
            return false; // can happen in tests.
        ExpressionDataMatrixRowElement itemA = this.dataMatrix.getRowElement( i );
        ExpressionDataMatrixRowElement itemB = this.dataMatrix.getRowElement( j );

        Collection<Gene> genesA = this.probeToGeneMap.get( itemA.getDesignElement() );
        Collection<Gene> genesB = this.probeToGeneMap.get( itemB.getDesignElement() );

        return CollectionUtils.containsAny( genesA, genesB );
    }

    /**
     * Convert the probeToGeneMap to a geneToProbeMap and gather stats.
     */
    private void initGeneToProbeMap() {

        this.numUniqueGenes = 0;
        this.geneToProbeMap = new HashMap<>();
        for ( CompositeSequence cs : probeToGeneMap.keySet() ) {

            Set<Gene> genes = probeToGeneMap.get( cs );

            /*
             * Genes will be empty if the probe does not map to any genes.
             */
            if ( genes == null )
                continue; // defensive.
            for ( Gene g : genes ) {
                numUniqueGenes++;

                if ( !geneToProbeMap.containsKey( g ) ) {
                    geneToProbeMap.put( g, new HashSet<CompositeSequence>() );
                }
                this.geneToProbeMap.get( g ).add( cs );

            }
        }

        if ( numUniqueGenes == 0 ) {
            AbstractMatrixRowPairAnalysis.log
                    .warn( "There are no genes for this data set, " + this.probeToGeneMap.size() + " probes." );
        }

        int max = 0;
        for ( Gene g : geneToProbeMap.keySet() ) {
            int c = geneToProbeMap.get( g ).size();
            if ( c > max ) {
                max = c;
            }
        }
        int[] stats = new int[max]; // how many probes have N genes that they hit.
        for ( Gene g : geneToProbeMap.keySet() ) {
            int c = geneToProbeMap.get( g ).size();
            assert c > 0;
            stats[c - 1]++;
        }

        AbstractMatrixRowPairAnalysis.log
                .info( "Mapping Stats: " + numUniqueGenes + " unique genes; gene representation summary: " + ArrayUtils
                        .toString( stats ) );
    }

    private double numberOfTestsForGeneAtRow( int index ) {
        double testCount = 0;
        Set<Gene> clusters = this.getGenesForRow( index );
        for ( Gene geneId : clusters ) {
            // how many probes assay that gene
            int c = this.geneToProbeMap.get( geneId ).size();
            if ( c > testCount )
                testCount = c;
        }
        return testCount;
    }
}
