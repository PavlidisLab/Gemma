package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event triggered when the release details of a {@link ubic.gemma.model.common.description.Versioned} entity are
 * updated.
 */
@Entity
@DiscriminatorValue("ReleaseDetailsUpdateEvent")
public class ReleaseDetailsUpdateEvent extends VersionedEvent {
}
