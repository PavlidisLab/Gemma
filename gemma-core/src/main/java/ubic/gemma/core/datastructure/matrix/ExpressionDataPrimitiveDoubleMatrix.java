package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;

/**
 * Interface for matrices that can provide unboxed doubles.
 * <p>
 * Use the methods in this interface to avoid the overhead of boxing and unboxing {@link Double}.
 *
 * @author poirigui
 */
public interface ExpressionDataPrimitiveDoubleMatrix extends ExpressionDataMatrix<Double> {

    /**
     * Retrieve the value at the given row and column without boxing.
     *
     * @see #get(int, int)
     */
    double getAsDouble( int row, int column );

    /**
     * Retrieve a row without boxing.
     *
     * @see #getRow(int)
     */
    double[] getRowAsDoubles( int index );

    /**
     * Retrieve the row for the given design element without boxing.
     *
     * @see #getRow(CompositeSequence)
     */
    @Nullable
    double[] getRowAsDoubles( CompositeSequence designElement );

    /**
     * Retrieve the given column without boxing.
     *
     * @see #getColumn(int)
     */
    double[] getColumnAsDoubles( int column );
}
