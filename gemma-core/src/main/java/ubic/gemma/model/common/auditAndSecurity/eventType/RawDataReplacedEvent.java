package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity

@DiscriminatorValue("RawDataReplacedEvent")

public class RawDataReplacedEvent extends DataReplacedEvent {
}
