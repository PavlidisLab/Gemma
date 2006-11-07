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

import hep.aida.IHistogram1D;
import hep.aida.ref.Histogram1D;

import java.util.Map;
import java.util.Set;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.dataStructure.Link;
import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.NamedMatrix;
import ubic.basecode.math.CorrelationStats;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
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
 * @todo This should not be in baseCode
 */
public class MatrixRowPairPearsonAnalysis implements MatrixRowPairAnalysis {

    private double storageThresholdValue = 0.5;
    private IHistogram1D histogram = null;
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
    private DoubleMatrixNamed dataMatrix = null;
    private GeneAnnotations duplicateMap = null;
    private Map probeToGeneMap = null;
    private Map geneToProbeMap = null;
    private DesignElementDataVectorService deService = null;
    private GeneService geneService = null;
    private BitMatrix used = null;
    private int numMissing;
    private double globalTotal = 0.0; // used to store the running total of the matrix values.
    private double globalMean = 0.0; // mean of the entire distribution.
    private int numVals = 0; // number of values actually stored in the matrix

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
        histogram = new Histogram1D( "Correlation histogram", 2000, -1.0, 1.0 );
        keepers = new ObjectArrayList( 10000 );
    }

    /**
     * @param dataMatrix DenseDoubleMatrix2DNamed
     */
    public MatrixRowPairPearsonAnalysis( DoubleMatrixNamed dataMatrix ) {
        this( dataMatrix.rows() );
        this.dataMatrix = dataMatrix;
        this.numMissing = this.fillUsed();
    }

    /**
     * @param dataMatrix DenseDoubleMatrix2DNamed
     * @param tmts Values of the correlation that are deemed too small to store in the matrix. Setting this as high as
     *        possible can greatly reduce memory requirements, but can slow things down.
     */
    public MatrixRowPairPearsonAnalysis( DoubleMatrixNamed dataMatrix, double tmts ) {
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
     * @param k GroupMap
     */
    public void setDuplicateMap( Map m1, Map m2 ) {
        this.geneToProbeMap = m1;
        this.probeToGeneMap = m2;
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
     * @return hep.aida.IHistogram1D
     */
    public IHistogram1D getHistogram() {
        return histogram;
    }

    /**
     * Read back the histogram as a DoubleArrayList of counts. Stupid that the IHistogram1D interface doesn't define
     * this...
     * 
     * @return cern.colt.list.DoubleArrayList
     * @todo - put this somewhere more generically useful!
     */
    public DoubleArrayList getHistogramArrayList() {
        DoubleArrayList r = new DoubleArrayList( histogram.xAxis().bins() );
        for ( int i = 0; i < histogram.xAxis().bins(); i++ ) {
            r.add( histogram.binHeight( i ) );
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
     * check if probeA and probeB are mapped to the same gene
     * 
     * @param probeA String
     * @param probeB String
     * @param geneData GeneAnnotations
     * @return
     */
    private boolean checkAssociation( Object probeA, Object probeB ) {
        if ( this.probeToGeneMap == null || this.geneToProbeMap == null ) return false;
        Gene gene = ( Gene ) this.probeToGeneMap.get( probeA );
        // Map geneToProbeMap = geneData.getProbeToGeneMap();
        // return ((Set)geneToProbeMap.get(geneId)).contains(probeB);
        if ( gene != null ) return ( ( Set ) geneToProbeMap.get( gene ) ).contains( probeB );
        return false;
    }

    /**
     * Check probe duplication in Gene Annotations
     * 
     * @param String probId
     * @param GeneAnnotations geneData
     */
    private boolean checkDuplication(  Object probe ) {
        if ( this.probeToGeneMap == null || this.geneToProbeMap == null ) return false;
        Gene gene = ( Gene ) this.probeToGeneMap.get( probe );
        ;

        if ( gene != null ) return ( ( Set ) geneToProbeMap.get( gene ) ).size() > 1;

        return false;
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

        /* for each vector, compare it to all other vectors, avoid repeating things */
        Object itemA = null;
        double[] ival = null;
        boolean AhasDuplicates = false;
        int duplicateSkip = 0;
        for ( int i = 0; i < numrows; i++ ) {

            if ( docalcs ) {
                if ( this.probeToGeneMap != null && this.geneToProbeMap != null ) {
                    itemA = this.dataMatrix.getRowName( i );
                    // AhasDuplicates = ( ( Set ) probeToGeneMap.get( itemA ) ).size() > 0;
                    AhasDuplicates = checkDuplication( itemA );
                }
                ival = data[i];
            }

            for ( int j = i + 1; j < numrows; j++ ) {
                if ( !docalcs || C.getQuick( i, j ) != 0.0 ) { // second pass over matrix. Don't calculate it if we
                    // already have it. Just do the requisite checks.
                    keepCorrel( i, j, C.getQuick( i, j ), numcols );
                    continue;
                }
                if ( AhasDuplicates && this.checkAssociation( itemA, this.dataMatrix.getRowName( j ) ) ) {
                    duplicateSkip++;
                    continue;
                }
                setCorrel( i, j, correlFast( ival, data[j], i, j ), numcols );
            }
            if ( i > 0 && i % 1000 == 0 ) {
                System.err.print( "." ); // progress.
            }
        }
        System.err.println( "" );

        finishMetrics( duplicateSkip );

    }

    /**
     * Calculate the linear correlation matrix of a matrix, allowing missing values. Slower. If there are no missing
     * values, this calls PearsonFast.
     * 
     * @param duplicates Defines values that should not be compared to each other.
     */
    public void calculateMetrics() {

        int numused;
        int numrows = this.dataMatrix.rows();
        int numcols = this.dataMatrix.columns();

        boolean docalcs = this.needToCalculateMetrics();

        if ( this.numMissing == 0 ) {
            calculateMetricsFast();
            return;
        }

        boolean[][] usedB = null;
        double[][] data = null;
        if ( docalcs ) {
            // Temporarily copy the data in this matrix, for performance.
            usedB = new boolean[numrows][numcols];
            data = new double[numrows][numcols];
            for ( int i = 0; i < numrows; i++ ) { // first vector
                double[] ival = this.dataMatrix.getRow( i );
                for ( int j = 0; j < numcols; j++ ) { // second vector
                    usedB[i][j] = used.get( i, j ); // this is only needed if we use it below, speeds things up
                    // slightly.
                    data[i][j] = this.dataMatrix.get( i, j );
                }
            }
        }

        /* for each vector, compare it to all other vectors */
        Object itemA = null;
        boolean AhasDuplicates = false;
        int duplicateSkip = 0;
        double[] ival = null;
        double syy, sxy, sxx, sx, sy, xj, yj;
        for ( int i = 0; i < numrows; i++ ) { // first vector

            if ( docalcs ) {
                rowStatistics();
                if ( this.geneToProbeMap != null && this.probeToGeneMap != null ) {
                    itemA = this.dataMatrix.getRowName( i );
                    AhasDuplicates = this.checkDuplication( itemA );
                }
                ival = data[i];
            }

            boolean thisRowHasMissing = hasMissing[i];

            for ( int j = i + 1; j < numrows; j++ ) { // second vector

                // second pass over matrix? Don't calculate it if we already have it. Just do the requisite checks.
                if ( !docalcs || C.getQuick( i, j ) != 0.0 ) {
                    keepCorrel( i, j, C.getQuick( i, j ), numcols );
                    continue;
                }

                /* skip duplicates */
                if ( AhasDuplicates && this.checkAssociation( itemA, this.dataMatrix.getRowName( j ) ) ) {
                    duplicateSkip++;
                    continue;
                }

                double[] jval = data[j];

                /* if there are no missing values, use the faster method of calculation */
                if ( !thisRowHasMissing && !hasMissing[j] ) {
                    setCorrel( i, j, correlFast( ival, jval, i, j ), numcols );
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
                    xj = ival[k];
                    yj = jval[k];
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

            }
            if ( i > 0 && i % 1000 == 0 ) {
                System.err.print( "." );
            }
        }
        System.err.println( "" );
        finishMetrics( duplicateSkip );
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
            System.err.println( "No missing values" );
        } else {
            System.err.println( numMissing + " missing values" );
        }
        return numMissing;
    }

    /**
     * @param i int
     * @param j int
     * @param correl double
     * @param numused int
     * @return double
     */
    private double correctedPvalue( int i, int j, double correl, int numused ) {

        double p = CorrelationStats.pvalue( correl, numused );

        // correct it for duplicates.
        if ( this.geneToProbeMap != null && this.probeToGeneMap != null ) {
            // Map geneToProbeMap = geneData.getProbeToGeneMap();
            // return ((Set)geneToProbeMap.get(geneId)).contains(probeB);
            double k = 1, m = 1;
            // String geneId = duplicateMap.getProbeGeneName(dataMatrix.getRowName(i));
            Gene gene = ( Gene ) this.probeToGeneMap.get( dataMatrix.getRowName( i ) );

            if ( gene != null )
            // k = ( double ) duplicateMap.numProbesForGene( geneId ) + 1;
                k = ( ( Set ) this.geneToProbeMap.get( gene ) ).size() + 1;

            // geneId = duplicateMap.getProbeGeneName(dataMatrix.getRowName(j));
            gene = ( Gene ) this.probeToGeneMap.get( dataMatrix.getRowName( j ) );
            if ( gene != null )
            // m = ( double ) duplicateMap.numProbesForGene( geneId ) + 1;
                m = ( ( Set ) this.geneToProbeMap.get( gene ) ).size() + 1;
            p = p * k * m;
        }

        return p;
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

        // System.err.println(i + "\t" + j + "\t" + correl);
        if ( !histogramIsFilled ) {
            if ( useAbsoluteValue ) {
                globalTotal += acorrel;
                histogram.fill( acorrel );
            } else {
                globalTotal += correl;
                histogram.fill( correl );
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
                    return false;
                }
            } else {
                if ( Math.abs( lowerTailThreshold ) > storageThresholdValue
                        && upperTailThreshold > storageThresholdValue ) { // then we would have stored it already.
                    return false;
                }
            }
        }
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
     * @param duplicateSkip int
     * @param duplicates Map
     */
    private void finishMetrics( int duplicateSkip ) {
        if ( !this.histogramIsFilled && this.probeToGeneMap != null && this.geneToProbeMap != null ) {
            System.err.println( "Skipped " + duplicateSkip + " pairs of duplicates" );
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

    // public static void main(String[] args) {
    // DoubleMatrixReader m = new DoubleMatrixReader();
    // DenseDoubleMatrix2DNamed mm;
    // String filename = args[0];
    // if (args.length > 0) {
    // try {
    // mm = (DenseDoubleMatrix2DNamed) m.read(filename);
    // } catch (java.io.IOException e) {
    // System.err.println("Cannot to open file " + filename);
    // return;
    // }
    // MatrixRowPairPearsonAnalysis k = new MatrixRowPairPearsonAnalysis(mm);
    // k.calculateMetrics();
    // // HistogramWriter h = new HistogramWriter();
    // // h.write(k.getHistogram(), System.err);
    // } else {
    // System.err.println(
    // "Please specify data filename by passing it as program arguments");
    // }
    // }
}
