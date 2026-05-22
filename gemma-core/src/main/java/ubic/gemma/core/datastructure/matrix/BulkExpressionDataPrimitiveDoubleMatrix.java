package ubic.gemma.core.datastructure.matrix;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Interface for bulk expression data matrices that can be efficiently accessed as a primitive double matrix.
 * @author poirigui
 */
public interface BulkExpressionDataPrimitiveDoubleMatrix extends BulkExpressionDataMatrix<Double>, ExpressionDataPrimitiveDoubleMatrix {

    /**
     * Retrieve the given column without boxing, or {@code null} if the assay is not part of this matrix.
     * @see #getColumn(BioAssay)
     */
    @Nullable
    double[] getColumnAsDoubles( BioAssay bioAssay );

    /**
     * Obtain the raw matrix as a double array.
     * @see #getRawMatrix()
     */
    double[][] getRawMatrixAsDoubles();
}
