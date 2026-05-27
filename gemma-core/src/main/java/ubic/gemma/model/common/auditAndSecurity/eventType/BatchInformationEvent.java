package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Abstract class for events related to batch information.
 * <p>
 * These events indicate whether batch information is available, missing or problematic.
 * @author poirigui
 */
@Entity
public abstract class BatchInformationEvent extends ExpressionExperimentAnalysisEvent {

}
