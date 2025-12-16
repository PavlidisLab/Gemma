package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;

/**
 * Interface for bulk expression data matrices that can be efficiently accessed as a primitive int matrix.
 *
 * @author poirigui
 */
public interface BulkExpressionDataPrimitiveIntMatrix extends BulkExpressionDataMatrix<Integer>, ExpressionDataPrimitiveIntMatrix {

    /**
     * Retrieve the given column without boxing.
     *
     * @see #getColumn(int)
     */
    @Nullable
    int[] getColumnAsInts( BioAssay bioAssay );

    /**
     * Obtain the raw matrix as a int array.
     *
     * @see #getMatrix()
     */
    int[][] getMatrixAsInts();
}
