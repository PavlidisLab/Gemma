package ubic.gemma.model.expression.bioAssayData;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Utilities for working with {@link SingleCellExpressionDataVector}.
 * @author poirigui
 */
public class SingleCellExpressionDataVectorUtils {

    /**
     * @see #getSampleStart(SingleCellExpressionDataVector, int, int)
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, BioAssay sample ) {
        return getSampleStart( vector, sample, 0 );
    }

    /**
     * @see #getSampleStart(SingleCellExpressionDataVector, int, int)
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, BioAssay sample, int after ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( sample + " is not found in the single-cell dimension." );
        }
        return getSampleStart( vector, sampleIndex, after );
    }

    /**
     * Get the index of the first cell for a given sample.
     * @param vector      vector
     * @param sampleIndex index of the sample in the single-cell dimension, see {@link SingleCellDimension#getBioAssays()}
     * @param after       starting position for the search
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, int sampleIndex, int after ) {
        int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
        // check where the next sample begins, only search past this sample starting point
        int start = Arrays.binarySearch( vector.getDataIndices(), after, vector.getDataIndices().length, sampleOffset );
        if ( start < 0 ) {
            start = -start - 1;
        }
        return start;
    }

    public static int getSampleEnd( SingleCellExpressionDataVector vector, BioAssay sample ) {
        return getSampleEnd( vector, sample, 0 );
    }

    public static int getSampleEnd( SingleCellExpressionDataVector vector, BioAssay sample, int after ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( sample + " is not found in the single-cell dimension." );
        }
        return getSampleEnd( vector, sampleIndex, after );
    }

    /**
     * Get the *exclusive* index of the last cell for a given sample.
     * @param vector      vector
     * @param sampleIndex index of the sample in the single-cell dimension, see {@link SingleCellDimension#getBioAssays()}
     * @param after       starting position for the search, if you previously called {@link #getSampleStart(SingleCellExpressionDataVector, int, int)},
     *                    you should use this value here to save some time
     */
    public static int getSampleEnd( SingleCellExpressionDataVector vector, int sampleIndex, int after ) {
        return getSampleEnd( vector.getSingleCellDimension(), vector.getDataIndices(), sampleIndex, after );
    }

    /**
     * Only exposed for internal use, prefer {@link #getSampleEnd(SingleCellExpressionDataVector, int, int)}.
     */
    public static int getSampleEnd( SingleCellDimension dimension, int[] dataIndices, int sampleIndex, int after ) {
        int sampleOffset = dimension.getBioAssaysOffset()[sampleIndex];
        int numberOfCells = dimension.getNumberOfCellsBySample( sampleIndex );
        int nextSampleOffset = sampleOffset + numberOfCells;
        // check where the next sample begins, only search past this sample starting point
        int end = Arrays.binarySearch( dataIndices, after, dataIndices.length, nextSampleOffset );
        if ( end < 0 ) {
            end = -end - 1;
        }
        return end;
    }

    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( sample + " is not found in the single-cell dimension." );
        }
        return getSampleDataAsDoubles( vector, sampleIndex );
    }

    /**
     * Obtain the data of a sample.
     */
    public static float[] getSampleDataAsFloats( SingleCellExpressionDataVector vector, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        return Arrays.copyOfRange( vector.getDataAsFloats(), start, end );
    }

    /**
     * Obtain the data of a sample.
     */
    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        return Arrays.copyOfRange( vector.getDataAsDoubles(), start, end );
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        return Arrays.copyOfRange( vector.getDataAsInts(), start, end );
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        return Arrays.copyOfRange( vector.getDataAsLongs(), start, end );
    }

    /**
     * Obtain the data of a sample.
     */
    public static float[] getSampleDataAsFloats( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        float[] data = vector.getDataAsFloats();
        FloatArrayList arr = new FloatArrayList();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[i] == row ) {
                arr.add( data[i] );
            }
        }
        return arr.elements();
    }

    /**
     * Obtain the data of a sample.
     */
    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        double[] data = vector.getDataAsDoubles();
        DoubleArrayList arr = new DoubleArrayList();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[i] == row ) {
                arr.add( data[i] );
            }
        }
        return arr.elements();
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int[] data = vector.getDataAsInts();
        IntArrayList arr = new IntArrayList();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[i] == row ) {
                arr.add( data[i] );
            }
        }
        return arr.elements();
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        long[] data = vector.getDataAsLongs();
        LongArrayList arr = new LongArrayList();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[i] == row ) {
                arr.add( data[i] );
            }
        }
        return arr.elements();
    }

    public static Consumer<SingleCellExpressionDataVector> createStreamMonitor( String logCategory, long numVecs ) {
        Log log = LogFactory.getLog( logCategory );
        return new Consumer<SingleCellExpressionDataVector>() {
            final StopWatch timer = StopWatch.createStarted();
            final AtomicInteger i = new AtomicInteger();

            @Override
            public void accept( SingleCellExpressionDataVector x ) {
                int done = i.incrementAndGet();
                if ( done % 10 == 0 ) {
                    log.info( String.format( "Processed %d/%d vectors (%f.2 vectors/sec)", done, numVecs, 1000.0 * done / timer.getTime() ) );
                }
            }
        };
    }
}
