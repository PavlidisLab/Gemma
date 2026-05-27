package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Indicates that we have updated an expression experiment's information from GEO, after it was already loaded in Gemma.
 */
@Entity
@DiscriminatorValue("ExpressionExperimentUpdateFromGEOEvent")
public class ExpressionExperimentUpdateFromGEOEvent extends AuditEventType {

}
