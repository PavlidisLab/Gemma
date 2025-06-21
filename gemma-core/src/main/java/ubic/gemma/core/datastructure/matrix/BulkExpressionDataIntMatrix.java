package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.List;

/**
 * A bulk expression data matrix that can be efficiently accessed as a primitive int matrix.
 * @author poirigui
 */
public class BulkExpressionDataIntMatrix extends AbstractBulkExpressionDataMatrix<Integer> implements BulkExpressionDataPrimitiveIntMatrix {

    private final int[][] matrix;

    public BulkExpressionDataIntMatrix( List<BulkExpressionDataVector> vectors ) {
        super( vectors );
        this.matrix = new int[vectors.size()][];
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            this.matrix[i] = vector.getDataAsInts();
        }
    }

    @Override
    public boolean hasMissingValues() {
        return false;
    }

    @Override
    public Integer[][] getRawMatrix() {
        Integer[][] result = new Integer[matrix.length][];
        for ( int i = 0; i < matrix.length; i++ ) {
            result[i] = ArrayUtils.toObject( matrix[i] );
        }
        return result;
    }

    @Override
    public int[][] getRawMatrixAsInts() {
        return matrix;
    }

    @Override
    public Integer[] getColumn( int column ) {
        return ArrayUtils.toObject( getColumnAsInts( column ) );
    }

    @Override
    public int[] getColumnAsInts( int column ) {
        int[] col = new int[rows()];
        for ( int i = 0; i < rows(); i++ ) {
            col[i] = matrix[i][column];
        }
        return col;
    }

    @Override
    public int[] getColumnAsInts( BioAssay bioAssay ) {
        int column = getColumnIndex( bioAssay );
        if ( column == -1 ) {
            return null;
        }
        return getColumnAsInts( column );
    }

    @Override
    public Integer[] getRow( int index ) {
        return ArrayUtils.toObject( getRowAsInts( index ) );
    }

    @Override
    public int[] getRowAsInts( int index ) {
        return matrix[index];
    }

    @Override
    public int[] getRowAsInts( CompositeSequence designElement ) {
        int index = getRowIndex( designElement );
        if ( index == -1 ) {
            return null;
        }
        return matrix[index];
    }

    @Override
    public Integer get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public int getAsInt( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    protected String format( int i, int j ) {
        return format( matrix[i][j] );
    }
}
