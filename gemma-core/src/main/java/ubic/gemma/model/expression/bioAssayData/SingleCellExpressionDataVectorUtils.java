package ubic.gemma.model.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static ubic.gemma.core.util.NetUtils.bytePerSecondToDisplaySize;

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
        int numberOfCellIds = dimension.getNumberOfCellIdsBySample( sampleIndex );
        int nextSampleOffset = sampleOffset + numberOfCellIds;
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
        return getSampleDataAsFloats( vector, vector.getDataAsFloatBuffer(), sampleIndex );
    }

    public static float[] getSampleDataAsFloats( SingleCellExpressionDataVector vector, FloatBuffer buffer, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        float[] dst = new float[end - start];
        buffer.position( start );
        buffer.limit( end );
        buffer.get( dst );
        return dst;
    }

    /**
     * Obtain the data of a sample.
     */
    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, int sampleIndex ) {
        return getSampleDataAsDoubles( vector, vector.getDataAsDoubleBuffer(), sampleIndex );
    }

    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, DoubleBuffer buffer, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        double[] dst = new double[end - start];
        buffer.position( start );
        buffer.limit( end );
        buffer.get( dst );
        return dst;
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, int sampleIndex ) {
        return getSampleDataAsInts( vector, vector.getDataAsIntBuffer(), sampleIndex );
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, IntBuffer buffer, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int[] dst = new int[end - start];
        buffer.position( start );
        buffer.limit( end );
        buffer.get( dst );
        return dst;
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, int sampleIndex ) {
        return getSampleDataAsLongs( vector, vector.getDataAsLongBuffer(), sampleIndex );
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, LongBuffer buffer, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        long[] dst = new long[end - start];
        buffer.position( start );
        buffer.limit( end );
        buffer.get( dst );
        return dst;
    }

    public static float[] getSampleDataAsFloats( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return getSampleDataAsFloats( vector, vector.getDataAsFloatBuffer(), sampleIndex, cellLevelCharacteristics, row );
    }

    /**
     * Obtain the data of a sample.
     */
    public static float[] getSampleDataAsFloats( SingleCellExpressionDataVector vector, FloatBuffer data, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int[] dataIndices = vector.getDataIndices();
        int nnz = 0;
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                nnz++;
            }
        }
        int k = 0;
        float[] arr = new float[nnz];
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                arr[k++] = data.get( i );
            }
        }
        return arr;
    }

    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return getSampleDataAsDoubles( vector, vector.getDataAsDoubleBuffer(), sampleIndex, cellLevelCharacteristics, row );
    }

    /**
     * Obtain the data of a sample.
     */
    public static double[] getSampleDataAsDoubles( SingleCellExpressionDataVector vector, DoubleBuffer data, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int nnz = 0;
        int[] dataIndices = vector.getDataIndices();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                nnz++;
            }
        }
        int k = 0;
        double[] arr = new double[nnz];
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                arr[k++] = data.get( i );
            }
        }
        return arr;
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return getSampleDataAsInts( vector, vector.getDataAsIntBuffer(), sampleIndex, cellLevelCharacteristics, row );
    }

    public static int[] getSampleDataAsInts( SingleCellExpressionDataVector vector, IntBuffer data, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int nnz = 0;
        int[] dataIndices = vector.getDataIndices();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                nnz++;
            }
        }
        int k = 0;
        int[] arr = new int[nnz];
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                arr[k++] = data.get( i );
            }
        }
        return arr;
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return getSampleDataAsLongs( vector, vector.getDataAsLongBuffer(), sampleIndex, cellLevelCharacteristics, row );
    }

    public static long[] getSampleDataAsLongs( SingleCellExpressionDataVector vector, LongBuffer data, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int nnz = 0;
        int[] dataIndices = vector.getDataIndices();
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                nnz++;
            }
        }
        int k = 0;
        long[] arr = new long[nnz];
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[dataIndices[i]] == row ) {
                arr[k++] = data.get( i );
            }
        }
        return arr;
    }

    /**
     * Create a consumer for a {@link java.util.stream.Stream} that will report progress of retrieving single-cell
     * vectors.
     * <p>
     * The logging is similar to that of {@link ubic.gemma.core.util.ProgressReporter}.
     */
    public static Consumer<SingleCellExpressionDataVector> createStreamMonitor( ExpressionExperiment ee, QuantitationType qt, String logCategory, int reportFrequency, long numVecs ) {
        String what = "single-cell vectors of " + qt.getName() + " in " + ee.getShortName();
        if ( reportFrequency <= 0 ) {
            return x -> { /* no-op */ };
        }
        Log log = LogFactory.getLog( logCategory );
        return new Consumer<SingleCellExpressionDataVector>() {
            final StopWatch timer = StopWatch.createStarted();
            final AtomicInteger i = new AtomicInteger();
            final AtomicLong bytesRetrieved = new AtomicLong();

            @Override
            public void accept( SingleCellExpressionDataVector x ) {
                int done = i.incrementAndGet();
                long br = bytesRetrieved.addAndGet( estimateSizeInBytes( x ) );
                if ( done % reportFrequency == 0 ) {
                    log.info( String.format( "Retrieving %s [%d/%s] @ %f.2 vectors/sec and @ ~%s",
                            what,
                            done, numVecs >= 0 ? numVecs : "?",
                            1000.0 * done / timer.getTime(),
                            bytePerSecondToDisplaySize( 1000.0 * br / timer.getTime() ) ) );
                }
            }
        };
    }

    private static long estimateSizeInBytes( SingleCellExpressionDataVector x ) {
        long s = 0;
        if ( x.getDataIndices() != null ) {
            s += 8L * x.getDataIndices().length;
        }
        if ( x.getData() != null ) {
            s += x.getData().length;
        }
        return s;
    }
}
