package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;

/**
 * Event emitted when the preferred cell type assignment is changed.
 *
 * @author poirigui
 * @see CellTypeAssignment#isPreferred()
 */
public class PreferredCellTypeAssignmentChangedEvent extends CellTypeAssignmentEvent {
}
