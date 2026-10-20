package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.MappedSuperclass;
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
 * <p>
 * The {@code vector} association + the {@code numberOfCells} payload live on the concrete subclasses because the
 * {@code vector} association target type is subclass-specific (raw vs processed) and shared-PK mapping via
 * {@link jakarta.persistence.MapsId} must be declared on the owning entity.
 *
 * @author poirigui
 */
@MappedSuperclass
abstract class BulkExpressionDataVectorNumberOfCells extends AbstractIdentifiable {
}
