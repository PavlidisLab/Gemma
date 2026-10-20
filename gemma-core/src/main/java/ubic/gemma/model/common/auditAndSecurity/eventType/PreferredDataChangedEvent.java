package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Event emitted when the preferred data for an experiment is changed.
 *
 * @author poirigui
 * @see PreferredSingleCellDataChangedEvent
 * @see PreferredRawDataChangedEvent
 */
@Entity
public abstract class PreferredDataChangedEvent extends ExpressionExperimentAnalysisEvent {
}
