package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

/**
 * In a single-cell expression data matrix, each column represents a cell.
 * <p>
 * Note that the matrix implementation does not deal with all the complication that you may encounter in
 * {@link BulkExpressionDataMatrix}. For example, a single-cell data matrix may only hold vectors from one given
 * quantitation type and single cell dimension at a time.
 *
 * @author poirigui
 */
public interface SingleCellExpressionDataMatrix<T> extends ExpressionDataMatrix<T> {

    /**
     * Return the quantitation type for this matrix.
     */
    QuantitationType getQuantitationType();

    /**
     * Return the single-cell dimension for this matrix.
     */
    SingleCellDimension getSingleCellDimension();

    /**
     * {@inheritDoc}
     * <p>
     * <b>Important note:</b> Retrieving a column is a {@code O(n log m)} operation where {@code n} is the number of
     * vectors and {@code m} is the number of cells. Always favour row-oriented operations when possible.
     */
    @Override
    T[] getColumn( int column );
}
