package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;

public interface ExpressionDataPrimitiveIntMatrix extends ExpressionDataMatrix<Integer> {

    /**
     * Retrieve the value at the given row and column without boxing.
     *
     * @see #get(int, int)
     */
    int getAsInt( int row, int column );

    /**
     * Retrieve a row without boxing.
     *
     * @see #getRow(int)
     */
    int[] getRowAsInts( int index );

    /**
     * Retrieve the row for the given design element without boxing.
     *
     * @see #getRow(CompositeSequence)
     */
    @Nullable
    int[] getRowAsInts( CompositeSequence designElement );

    /**
     * Retrieve the given column without boxing.
     *
     * @see #getColumn(int)
     */
    int[] getColumnAsInts( int column );
}
