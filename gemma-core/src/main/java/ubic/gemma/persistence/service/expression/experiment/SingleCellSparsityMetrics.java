package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Collection;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.*;

/**
 * Compute sparsity metrics for single cell data.
 * @author poirigui
 */
public class SingleCellSparsityMetrics {

    /**
     * Default threshold to use when determining if a value is expressed.
     */
    public static final double DEFAULT_THRESHOLD = 0.0;

    private final double
            threshold,
            thresholdCount,
            thresholdLn,
            thresholdLog2,
            thresholdLog1p,
            thresholdLog10;

    public SingleCellSparsityMetrics() {
        this( DEFAULT_THRESHOLD );
    }

    /**
     *
     * @param threshold exclusive value to use to assess if data on a {@link ScaleType#LINEAR} scale is expressed. The
     *                  threshold is transformed depending on the scale type of the vectors being tested:
     *                  <ul>
     *                  <li>{@link ScaleType#COUNT} uses {@link Math#rint(double)}</li>
     *                  <li>{@link ScaleType#LOG1P} uses {@link Math#log1p(double)}</li>
     *                  <li>{@link ScaleType#LOG2} uses {@link Math#log(double)} / {@code Math.log(2)}</li>
     *                  <li>{@link ScaleType#LN} uses {@link Math#log(double)}</li>
     *                  <li>{@link ScaleType#LOG10} uses {@link Math#log10(double)}</li>
     *                  </ul>
     *                  Note that for {@link ScaleType#PERCENT}, {@link ScaleType#PERCENT1} and {@link ScaleType#LOGBASEUNKNOWN},
     *                  only zero is supported. If a scale type is not supported, an exception will be raised. You can
     *                  use {@link #isSupported(SingleCellExpressionDataVector)} to safely perform the check.
     */
    public SingleCellSparsityMetrics( double threshold ) {
        Assert.isTrue( threshold >= 0.0, "The threshold must be zero or greated." );
        this.threshold = threshold;
        this.thresholdCount = Math.rint( threshold );
        this.thresholdLog1p = Math.log1p( threshold );
        this.thresholdLog2 = Math.log( threshold ) / Math.log( 2 );
        this.thresholdLn = Math.log( threshold );
        this.thresholdLog10 = Math.log10( threshold );
    }

    /**
     * Check if sparsity metrics can be computed for a given collection of vectors.
     */
    public boolean isSupported( SingleCellExpressionDataVector vector ) {
        try {
            //noinspection ResultOfMethodCallIgnored
            isExpressed( threshold + 0.1, vector.getQuantitationType().getScale() );
            return true;
        } catch ( IllegalArgumentException e ) {
            return false;
        }
    }

    /**
     * Calculate the number of cells with at least one gene expressed.
     * @param characteristic only cell with the given characteristic will be considered
     */
    public int getNumberOfCells( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
        boolean[] isExpressed = new boolean[vectors.iterator().next().getSingleCellDimension().getNumberOfCells()];
        for ( SingleCellExpressionDataVector vector : vectors ) {
            addExpressedCells( vector, sampleIndex, characteristic, isExpressed );
        }
        int count = 0;
        for ( boolean b : isExpressed ) {
            if ( b ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Populate a boolean vector that indicates if a cell has at least one expressed gene.
     */
    public void addExpressedCells( SingleCellExpressionDataVector vector, int sampleIndex, @Nullable Characteristic characteristic, boolean[] isExpressed ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
        for ( int i = start; i < end; i++ ) {
            int cellIndex = vector.getDataIndices()[i];
            if ( isExpressed[cellIndex] ) {
                continue;
            }
            if ( isExpressed( buf.get( i ), vector.getQuantitationType().getScale() ) && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                isExpressed[cellIndex] = true;
            }
        }
    }

    /**
     * Calculate the number of genes expressed in at least one cell.
     */
    public int getNumberOfDesignElements( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            count += getNumberOfDesignElements( vector, sampleIndex, characteristic );
        }
        return count;
    }

    public int getNumberOfDesignElements( SingleCellExpressionDataVector vector, int sampleIndex, @Nullable Characteristic characteristic ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
        for ( int i = start; i < end; i++ ) {
            int cellIndex = vector.getDataIndices()[i];
            if ( isExpressed( buf.get( i ), vector.getQuantitationType().getScale() ) && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Calculate the number of expressed cell by gene pairs.
     */
    public int getNumberOfCellsByDesignElements( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            count += getNumberOfCellsByDesignElements( vector, sampleIndex, characteristic );
        }
        return count;
    }

    public int getNumberOfCellsByDesignElements( SingleCellExpressionDataVector vector, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
        for ( int i = start; i < end; i++ ) {
            int cellIndex = vector.getDataIndices()[i];
            if ( isExpressed( buf.get( i ), vector.getQuantitationType().getScale() ) && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if a value is expressed on a given scale.
     */
    private boolean isExpressed( double value, ScaleType scaleType ) {
        switch ( scaleType ) {
            case LINEAR:
                return value > threshold;
            case COUNT:
                return value > thresholdCount;
            case PERCENT:
            case PERCENT1:
                if ( threshold == 0 ) {
                    return value > 0;
                }
            case LOG1P:
                return value > thresholdLog1p;
            case LOG2:
                return value > thresholdLog2;
            case LN:
                return value > thresholdLn;
            case LOG10:
                return value > thresholdLog10;
            case LOGBASEUNKNOWN:
                if ( threshold == 0.0 ) {
                    return value > Double.NEGATIVE_INFINITY;
                }
            default:
                throw new IllegalArgumentException( "Cannot determine if data on scale " + scaleType + " is expressed." );
        }
    }
}
