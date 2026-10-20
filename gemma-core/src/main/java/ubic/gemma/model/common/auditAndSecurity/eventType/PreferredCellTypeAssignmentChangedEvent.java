package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when the preferred cell type assignment is changed.
 *
 * @author poirigui
 * @see CellTypeAssignment#isPreferred()
 */
@Entity
@DiscriminatorValue("PreferredCellTypeAssignmentChangedEvent")
public class PreferredCellTypeAssignmentChangedEvent extends CellTypeAssignmentEvent {
}
