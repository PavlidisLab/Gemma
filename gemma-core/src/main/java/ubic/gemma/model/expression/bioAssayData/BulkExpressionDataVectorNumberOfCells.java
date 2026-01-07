package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractIdentifiable;

/**
 * Container for the number of cells backing each value of a {@link BulkExpressionDataVector}.
 * <p>
 * This was initially intended to be a simple {@code int[]} array within the {@link BulkExpressionDataVector} model, but
 * modifying that table has become very difficult due to its size. Moreover, it would imply that most vectors would have
 * an unused column.
 * <p>
 * This is intentionally made package-private, you should only interact with {@link BulkExpressionDataVector#getNumberOfCells()}
 * and {@link BulkExpressionDataVector#setNumberOfCells(int[])}.
 *
 * @author poirigui
 */
@Getter
@Setter
abstract class BulkExpressionDataVectorNumberOfCells extends AbstractIdentifiable {

    private BulkExpressionDataVector vector;

    private int[] numberOfCells;
}
