package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Event emitted when a cell-level characteristics is modified (either added or removed).
 * @author poirigui
 * @see CellLevelCharacteristicsAddedEvent
 * @see CellLevelCharacteristicsRemovedEvent
 */
@Entity
public abstract class CellLevelCharacteristicsEvent extends ExpressionExperimentAnalysisEvent {

}
