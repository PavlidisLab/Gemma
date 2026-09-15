package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Fired when a curator renames an ExpressionExperiment's {@code shortName}.
 * Carries the rename context in the audit event's note (typically "old -> new").
 */
@Entity
@DiscriminatorValue("DatasetShortNameChangedEvent")
public class DatasetShortNameChangedEvent extends ExpressionExperimentAnalysisEvent {

}
