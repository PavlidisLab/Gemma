package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nonnull;

/**
 * Interface for bulk expression data matrices that can be efficiently accessed as a primitive double matrix.
 *
 * @author poirigui
 */
public interface BulkExpressionDataPrimitiveDoubleMatrix extends BulkExpressionDataMatrix<Double>, ExpressionDataPrimitiveDoubleMatrix {

    @Nonnull
    @Override
    Double get( int row, int column );

    /**
     * Retrieve the value for a given design element and biomaterial without boxing.
     *
     * @return the value as a primitive double, or {@link Double#NaN} if not found
     */
    double getAsDouble( CompositeSequence designElement, BioMaterial bioMaterial );

    /**
     * Retrieve the value for a given design element and bioassay without boxing.
     *
     * @return the value as a primitive double, or {@link Double#NaN} if not found
     */
    double getAsDouble( CompositeSequence designElement, BioAssay bioAssay );

    /**
     * Retrieve the given column without boxing.
     *
     * @see #getColumn(BioAssay)
     */
    double[] getColumnAsDoubles( BioAssay bioAssay );

    /**
     * Obtain the raw matrix as a double array.
     *
     * @see #getMatrix()
     */
    double[][] getMatrixAsDoubles();
}
