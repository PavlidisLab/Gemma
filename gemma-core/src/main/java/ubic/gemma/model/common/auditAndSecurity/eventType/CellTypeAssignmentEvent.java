package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Event emitted when a cell type assignment is modified (either added or removed).
 * @author poirigui
 * @see CellTypeAssignmentAddedEvent
 * @see CellTypeAssignmentRemovedEvent
 */
@Entity
public abstract class CellTypeAssignmentEvent extends ExpressionExperimentAnalysisEvent {

}
