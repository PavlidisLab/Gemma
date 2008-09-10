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
package ubic.gemma.analysis.expression.coexpression.links;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.Matrix2D;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import cern.colt.bitvector.BitMatrix;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author paul
 * @version $Id$
 */
public abstract class AbstractMatrixRowPairAnalysis implements MatrixRowPairAnalysis {

    protected static final int NUM_BINS = 2048;
    protected static final int HALF_BIN = NUM_BINS / 2;
    protected static final Log log = LogFactory.getLog( MatrixRowPairPearsonAnalysis.class );
    protected CompressedSparseDoubleMatrix<ExpressionDataMatrixRowElement, ExpressionDataMatrixRowElement> results = null;
    protected int minSamplesToKeepCorrelation = 0;
    protected double storageThresholdValue;
    protected ObjectArrayList keepers = null;
    protected BitMatrix used = null;
    protected int numMissing;
    protected double globalTotal = 0.0; // used to store the running total of the matrix values.
    protected double globalMean = 0.0; // mean of the entire distribution.
    protected int numVals = 0; // number of values actually stored in the matrix
    protected ExpressionDataDoubleMatrix dataMatrix;

    protected boolean[] hasGenesCache;
    protected Map<ExpressionDataMatrixRowElement, DesignElement> rowMapCache = new HashMap<ExpressionDataMatrixRowElement, DesignElement>();
    protected int minNumUsed = 3;

    protected boolean[] hasMissing = null;
    protected double upperTailThreshold = 0.0;
    protected boolean useAbsoluteValue = false;
    protected double lowerTailThreshold = 0.0;
    protected double pValueThreshold = 0.0;
    protected boolean histogramIsFilled = false;
    protected Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = null;
    protected Map<Gene, Collection<CompositeSequence>> geneToProbeMap = null;

    protected int[] fastHistogram = new int[NUM_BINS];
    private int numUniqueGenes = 0;

    private boolean omitNegativeCorrelationLinks = false;

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
     * Identify the correlations that are above the set thresholds.
     * 
     * @return cern.colt.list.ObjectArrayList
     */
    public ObjectArrayList getKeepers() {
        return keepers;
    }

    /**
     * @return baseCode.dataStructure.NamedMatrix
     */
    public Matrix2D getMatrix() {
        return results;
    }

    public double getNumUniqueGenes() {
        return numUniqueGenes;
    }

    /**
     * @param rowEl
     * @return
     */
    public DesignElement getProbeForRow( ExpressionDataMatrixRowElement rowEl ) {
        return this.rowMapCache.get( rowEl );
    }

    public double getScoreInBin( int i ) {
        // bin 2048 = correlation of 1.0 2048/1024 - 1 = 1
        // bin 1024 = correlation of 0.0 1024/1024 - 1 = 0
        // bin 0 = correlation of -1.0 : 0/1024 - 1 = -1
        return ( ( double ) i / HALF_BIN ) - 1.0;
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
        for ( int i = 0, n = results.rows(); i < n; i++ ) {
            for ( int j = i + 1, m = results.columns(); j < m; j++ ) {
                double r;
                if ( results.get( i, j ) != 0 ) {
                    r = results.get( i, j );
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

    /**
     * Flag the correlation matrix as un-fillable. This means that when PearsonMatrix is called, only the histogram will
     * be filled in. Also trashes any values that might have been stored there.
     */
    public void nullMatrix() {
        results = null;
    }

    /**
     * @return The number of values stored in the correlation matrix.
     */
    public int numCached() {
        return results.cardinality();
    }

    /**
     * 
     */
    public void setDuplicateMap( Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap ) {
        this.probeToGeneMap = probeToGeneMap;
        init();
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
     * Note that you cannot set the value less than 3 (the default)
     */
    public void setMinNumpresent( int minSamplesToKeepCorrelation ) {
        if ( minSamplesToKeepCorrelation > 3 ) this.minNumUsed = minSamplesToKeepCorrelation;
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
     * Set the threshold, above which correlations are kept.
     * 
     * @param k double
     */
    public void setUpperTailThreshold( double k ) {
        upperTailThreshold = k;
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
     * @return java.lang.String
     */
    @Override
    public String toString() {
        return results.toString();
    }

    /**
     * Store information about whether data includes missing values.
     * 
     * @return int
     */
    protected int fillUsed() {

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
     *  
     */
    protected void finishMetrics() {
        this.histogramIsFilled = true;
        globalMean = globalTotal / numVals;
    }

    /**
     * @param j
     * @return
     */
    protected Collection<Collection<Gene>> getGenesForRow( int j ) {
        return this.probeToGeneMap.get( getProbeForRow( dataMatrix.getRowElement( j ) ) );
    }

    /**
     * Skip the probes without blat association
     */
    protected boolean hasGene( ExpressionDataMatrixRowElement rowEl ) {
        return hasGenesCache[rowEl.getIndex()];
    }

    /**
     * Initialize caches.
     */
    protected void init() {

        initGeneToProbeMap();

        List<ExpressionDataMatrixRowElement> rowElements = this.dataMatrix.getRowElements();
        hasGenesCache = new boolean[rowElements.size()];

        for ( ExpressionDataMatrixRowElement element : rowElements ) {

            DesignElement de = element.getDesignElement();
            rowMapCache.put( element, de );

            Collection geneIdSet = this.probeToGeneMap.get( de );
            Integer i = element.getIndex();
            hasGenesCache[i] = geneIdSet != null && geneIdSet.size() > 0;

        }
        assert rowMapCache.size() > 0;
        log.info( "Initialized caches for probe/gene information" );
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
    protected boolean keepCorrel( int i, int j, double correl, int numused ) {

        if ( keepers == null ) {
            return false;
        }

        if ( Double.isNaN( correl ) ) return false;

        if ( omitNegativeCorrelationLinks && correl < 0.0 ) {
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
     * Tests whether the correlations still need to be calculated for final retrieval, or if they can just be retrieved.
     * This looks at the current settings and decides whether the value would already have been cached.
     * 
     * @return boolean
     */
    protected boolean needToCalculateMetrics() {

        /* are we on the first pass, or haven't stored any values? */
        if ( !histogramIsFilled || results == null ) {
            return true;
        }

        if ( this.storageThresholdValue > 0.0 ) {

            if ( this.useAbsoluteValue ) {
                if ( upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    log.info( "Second pass, good news, all values are cached" );
                    return false;
                }
            } else {
                if ( Math.abs( lowerTailThreshold ) > storageThresholdValue
                        && upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    log.info( "Second pass, good news, all values are cached" );
                    return false;
                }
            }
        }
        log.info( "Second pass, have to recompute some values" );
        return true;
    }

    /**
     * Checks for valid values of correlation and encoding.
     * 
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     */
    protected void setCorrel( int i, int j, double correl, int numused ) {
        if ( Double.isNaN( correl ) ) return;
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

        if ( acorrel > storageThresholdValue && results != null ) {
            results.set( i, j, correl );
        }

        keepCorrel( i, j, correl, numused );

    }

    /**
     * Set an (absolute value) correlation, below which values are not maintained in the correlation matrix. They are
     * still kept in the histogram. (In some implementations this can greatly reduce the memory requirements for the
     * correlation matrix).
     * 
     * @param k double
     */
    protected void setStorageThresholdValue( double k ) {
        if ( k < 0.0 || k > 1.0 ) {
            throw new IllegalArgumentException( "Correlation must be given as between 0 and 1" );
        }
        storageThresholdValue = k;
    }

    /**
     * populate geneToProbeMap and gather stats.
     */
    private void initGeneToProbeMap() {
        int[] stats = new int[10];
        this.numUniqueGenes = 0;
        this.geneToProbeMap = new HashMap<Gene, Collection<CompositeSequence>>();
        for ( CompositeSequence cs : probeToGeneMap.keySet() ) {
            Collection<Collection<Gene>> genes = probeToGeneMap.get( cs );
            for ( Collection<Gene> cluster : genes ) {
                numUniqueGenes++;
                for ( Gene g : cluster ) {
                    if ( !geneToProbeMap.containsKey( g ) ) {
                        geneToProbeMap.put( g, new HashSet<CompositeSequence>() );
                    }
                    this.geneToProbeMap.get( g ).add( cs );
                }

                if ( cluster.size() >= stats.length ) {
                    stats[stats.length - 1]++;
                } else {
                    stats[cluster.size()]++;
                }
            }
        }
        log.info( "Mapping Stats: " + ArrayUtils.toString( stats ) );
    }

    /**
     * @param omitNegativeCorrelationLinks the omitNegativeCorrelationLinks to set
     */
    public void setOmitNegativeCorrelationLinks( boolean omitNegativeCorrelationLinks ) {
        this.omitNegativeCorrelationLinks = omitNegativeCorrelationLinks;
    }
}
