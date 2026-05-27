package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;

/**
 * Base class for events relating to a {@link ubic.gemma.model.common.description.Versioned} entity.
 * @author poirigui
 */
@Entity
public abstract class VersionedEvent extends AuditEventType {

}
