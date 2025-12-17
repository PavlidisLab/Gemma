package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Interface for bulk expression data matrices that can be efficiently accessed as a primitive double matrix.
 * @author poirigui
 */
public interface BulkExpressionDataPrimitiveDoubleMatrix extends BulkExpressionDataMatrix<Double>, ExpressionDataPrimitiveDoubleMatrix {

    /**
     * Retrieve the given column without boxing.
     * @see #getColumn(BioAssay)
     */
    double[] getColumnAsDoubles( BioAssay bioAssay );

    /**
     * Obtain the raw matrix as a double array.
     * @see #getRawMatrix()
     */
    double[][] getRawMatrixAsDoubles();
}
