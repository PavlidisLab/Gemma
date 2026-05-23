package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Fired when a curator renames an ExpressionExperiment's {@code shortName}.
 * Carries the rename context in the audit event's note (typically "old -> new").
 */
public class DatasetShortNameChangedEvent extends ExpressionExperimentAnalysisEvent {

}
