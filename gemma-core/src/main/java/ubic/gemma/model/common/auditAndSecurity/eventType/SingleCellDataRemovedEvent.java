package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity

@DiscriminatorValue("SingleCellDataRemovedEvent")

public class SingleCellDataRemovedEvent extends DataRemovedEvent {
}
