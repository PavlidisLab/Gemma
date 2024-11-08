package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Collection;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.*;

public class SingleCellSparsityMetrics {

    /**
     * Calculate the number of cells with at least one gene expressed.
     * @param characteristic only cell with the given characteristic will be considered
     */
    public static int getNumberOfCells( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
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
    public static void addExpressedCells( SingleCellExpressionDataVector vector, int sampleIndex, @Nullable Characteristic characteristic, boolean[] isExpressed ) {
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
    public static int getNumberOfDesignElements( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            int start = getSampleStart( vector, sampleIndex, 0 );
            int end = getSampleEnd( vector, sampleIndex, start );
            DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
            for ( int i = start; i < end; i++ ) {
                int cellIndex = vector.getDataIndices()[i];
                if ( isExpressed( buf.get( i ), vector.getQuantitationType().getScale() ) && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Calculate the number of expressed cell by gene pairs.
     */
    public static int getNumberOfCellsByDesignElements( Collection<SingleCellExpressionDataVector> vectors, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            int start = getSampleStart( vector, sampleIndex, 0 );
            int end = getSampleEnd( vector, sampleIndex, start );
            DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
            for ( int i = start; i < end; i++ ) {
                int cellIndex = vector.getDataIndices()[i];
                if ( isExpressed( buf.get( i ), vector.getQuantitationType().getScale() ) && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Check if a value is expressed on a given scale.
     */
    private static boolean isExpressed( double value, ScaleType scaleType ) {
        switch ( scaleType ) {
            case LINEAR:
            case COUNT:
            case LOG1P:
                // in the case of log1p, 0 is mapped to 0
            case PERCENT:
            case PERCENT1:
                return value > 0;
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return value > Double.NEGATIVE_INFINITY;
            default:
                throw new IllegalArgumentException( "Cannot determine if data on scale " + scaleType + " is expressed." );
        }
    }
}
