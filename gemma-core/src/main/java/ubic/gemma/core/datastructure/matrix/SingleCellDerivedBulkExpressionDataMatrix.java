package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;

/**
 * A {@link BulkExpressionDataMatrix} that was derived from a {@link SingleCellExpressionDataMatrix}.
 * <p>
 * It allows querying the number of cells behind individual entries of the matrix.
 * <p>
 * The methods of this interface return {@code null} if the number of cells is not populated.
 *
 * @author poirigui
 */
public interface SingleCellDerivedBulkExpressionDataMatrix<T> extends BulkExpressionDataMatrix<T> {

    @Nullable
    int[][] getNumberOfCells();

    @Nullable
    Integer getNumberOfCells( int row, int column );

    @Nullable
    int[] getNumberOfCellsForColumn( int row );

    @Nullable
    int[] getNumberOfCellsForColumn( BioMaterial bioMaterial );

    @Nullable
    int[] getNumberOfCellsForRow( int row );

    @Nullable
    int[] getNumberOfCellsForRow( CompositeSequence designElement );
}
