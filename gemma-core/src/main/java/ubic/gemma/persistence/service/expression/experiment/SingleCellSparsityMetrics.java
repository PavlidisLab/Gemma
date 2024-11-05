package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Collection;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.*;

public class SingleCellSparsityMetrics {

    /**
     * The minimum expression value to consider a gene as expressed.
     */
    private static final double minExpression = Double.MIN_VALUE;

    /**
     * Calculate the number of cells with at least one gene expressed.
     * @param ba             sample to restrict the calculation to
     * @param characteristic only cell with the given characteristic will be considered
     */
    public static int getNumberOfCells( Collection<SingleCellExpressionDataVector> vectors, @Nullable BioAssay ba, int sampleIndex, @Nullable Characteristic characteristic ) {
        boolean[] isExpressed = new boolean[vectors.iterator().next().getSingleCellDimension().getNumberOfCells()];
        for ( SingleCellExpressionDataVector vector : vectors ) {
            addExpressedCells( vector, ba, sampleIndex, characteristic, isExpressed );
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
    public static void addExpressedCells( SingleCellExpressionDataVector vector, @Nullable BioAssay ba, int sampleIndex, @Nullable Characteristic characteristic, boolean[] isExpressed ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
        for ( int i = start; i < end; i++ ) {
            int cellIndex = vector.getDataIndices()[i];
            if ( isExpressed[cellIndex] ) {
                continue;
            }
            if ( buf.get( i ) >= minExpression && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                isExpressed[cellIndex] = true;
            }
        }
    }

    /**
     * Calculate the number of genes expressed in at least one cell.
     */
    public static int getNumberOfDesignElements( Collection<SingleCellExpressionDataVector> vectors, @Nullable BioAssay ba, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            int start = getSampleStart( vector, sampleIndex, 0 );
            int end = getSampleEnd( vector, sampleIndex, start );
            DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
            for ( int i = start; i < end; i++ ) {
                int cellIndex = vector.getDataIndices()[i];
                if ( buf.get( i ) >= minExpression && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
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
    public static int getNumberOfCellsByDesignElements( Collection<SingleCellExpressionDataVector> vectors, @Nullable BioAssay ba, int sampleIndex, @Nullable Characteristic characteristic ) {
        int count = 0;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            int start = getSampleStart( vector, sampleIndex, 0 );
            int end = getSampleEnd( vector, sampleIndex, start );
            DoubleBuffer buf = ByteBuffer.wrap( vector.getData() ).asDoubleBuffer();
            for ( int i = start; i < end; i++ ) {
                int cellIndex = vector.getDataIndices()[i];
                if ( buf.get( i ) >= minExpression && ( characteristic == null || hasCharacteristic( vector, cellIndex, characteristic ) ) ) {
                    count++;
                }
            }
        }
        return count;
    }
}
