package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity

@DiscriminatorValue("RawDataAddedEvent")

public class RawDataAddedEvent extends DataAddedEvent {
}
