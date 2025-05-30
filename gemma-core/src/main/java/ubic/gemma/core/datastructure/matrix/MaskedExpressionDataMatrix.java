package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.core.datastructure.MatrixMask;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper for an {@link ExpressionDataMatrix} that applies a mask to the data, allowing selective access to the
 * underlying matrix.
 *
 * @param <T> the type of data in the matrix
 * @see MatrixMask
 */
public class MaskedExpressionDataMatrix<T> extends AbstractExpressionDataMatrix<T> implements ExpressionDataMatrix<T> {

    /**
     * Mask individual elements of the given matrix.
     * @see MatrixMask#maskElements(int, int, boolean[][])
     * @param maskedValue value to use for masked elements, usually {@code null}
     */
    public static <T> MaskedExpressionDataMatrix<T> maskElements( ExpressionDataMatrix<T> matrix, boolean[][] mask, @Nullable T maskedValue ) {
        return new MaskedExpressionDataMatrix<>( matrix, MatrixMask.maskElements( matrix.rows(), matrix.columns(), mask ), maskedValue );
    }

    /**
     * Mask individual elements of the given matrix at the specified coordinates.
     * <p>
     * This uses a sparse representation internally, which is more efficient for large matrices with few masked
     * elements.
     * @see MatrixMask#maskElements(int, int, int[], int[])
     */
    public static <T> MaskedExpressionDataMatrix<T> maskElements( ExpressionDataMatrix<T> matrix, int[] i, int[] j, @Nullable T maskedValue ) {
        return new MaskedExpressionDataMatrix<>( matrix, MatrixMask.maskElements( matrix.rows(), matrix.columns(), i, j ), maskedValue );
    }

    /**
     * Mask whole rows of the given matrix.
     * @see MatrixMask#maskRows(int, int, boolean[])
     */
    public static <T> MaskedExpressionDataMatrix<T> maskRows( ExpressionDataMatrix<T> matrix, boolean[] rowMask, @Nullable T maskedValue ) {
        return new MaskedExpressionDataMatrix<>( matrix, MatrixMask.maskRows( matrix.rows(), matrix.columns(), rowMask ), maskedValue );
    }

    /**
     * Mask whole columns of the given matrix.
     * @see MatrixMask#maskColumns(int, int, boolean[])
     */
    public static <T> MaskedExpressionDataMatrix<T> maskColumns( ExpressionDataMatrix<T> matrix, boolean[] columnMask, @Nullable T maskedValue ) {
        return new MaskedExpressionDataMatrix<>( matrix, MatrixMask.maskColumns( matrix.rows(), matrix.columns(), columnMask ), maskedValue );
    }

    private final ExpressionDataMatrix<T> matrix;
    private final MatrixMask mask;
    @Nullable
    private final T maskedValue;

    private MaskedExpressionDataMatrix( ExpressionDataMatrix<T> matrix, MatrixMask mask, @Nullable T maskedValue ) {
        this.matrix = matrix;
        this.mask = mask;
        this.maskedValue = maskedValue;
    }

    @Nullable
    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return matrix.getExpressionExperiment();
    }

    @Override
    public QuantitationType getQuantitationType() {
        return matrix.getQuantitationType();
    }

    @Override
    public List<CompositeSequence> getDesignElements() {
        return matrix.getDesignElements();
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        return matrix.getDesignElementForRow( index );
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public T[] getColumn( int column ) {
        // this is always a copy, so it's safe to modify
        T[] col = matrix.getColumn( column );
        if ( mask.isColumnMasked( column ) ) {
            Arrays.fill( col, maskedValue );
            return col;
        }
        if ( mask.hasRowMask() || mask.hasElementMask() ) {
            for ( int i = 0; i < col.length; i++ ) {
                if ( mask.isMasked( i, column ) ) {
                    col[i] = maskedValue;
                }
            }
        }
        return col;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public T[] getRow( int index ) {
        // this is always a copy, so it's safe to modify
        T[] row = matrix.getRow( index );
        if ( mask.isRowMasked( index ) ) {
            Arrays.fill( row, maskedValue );
            return row;
        }
        if ( mask.hasColumnMask() || mask.hasElementMask() ) {
            for ( int j = 0; j < row.length; j++ ) {
                if ( mask.isMasked( index, j ) ) {
                    row[j] = maskedValue;
                }
            }
        }
        return row;
    }

    @Nullable
    @Override
    public T[] getRow( CompositeSequence designElement ) {
        int index = getRowIndex( designElement );
        if ( index == -1 ) {
            return null;
        }
        return getRow( index );
    }

    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        return matrix.getRowIndex( designElement );
    }

    @Nullable
    @Override
    public int[] getRowIndices( CompositeSequence designElement ) {
        return matrix.getRowIndices( designElement );
    }

    @Override
    @Deprecated
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        return matrix.getRowElements();
    }

    @Override
    @Deprecated
    public ExpressionDataMatrixRowElement getRowElement( int row ) {
        return matrix.getRowElement( row );
    }

    @Override
    public T get( int row, int column ) {
        if ( mask.isMasked( row, column ) ) {
            return maskedValue;
        }
        return matrix.get( row, column );
    }

    public MaskedExpressionDataMatrix<T> inverted() {
        return new MaskedExpressionDataMatrix<>( matrix, mask.inverted(), maskedValue );
    }

    @Override
    protected String getRowLabel( int i ) {
        if ( matrix instanceof AbstractExpressionDataMatrix ) {
            return ( ( AbstractExpressionDataMatrix<T> ) matrix ).getRowLabel( i );
        }
        return String.valueOf( i );
    }

    @Override
    protected String getColumnLabel( int j ) {
        if ( matrix instanceof AbstractExpressionDataMatrix ) {
            return ( ( AbstractExpressionDataMatrix<T> ) matrix ).getColumnLabel( j );
        }
        return String.valueOf( j );
    }

    @Override
    protected String format( int row, int column ) {
        if ( mask.isMasked( row, column ) ) {
            return "masked";
        }
        if ( matrix instanceof AbstractExpressionDataMatrix ) {
            return ( ( AbstractExpressionDataMatrix<T> ) matrix ).format( row, column );
        }
        return String.valueOf( matrix.get( row, column ) );
    }
}
