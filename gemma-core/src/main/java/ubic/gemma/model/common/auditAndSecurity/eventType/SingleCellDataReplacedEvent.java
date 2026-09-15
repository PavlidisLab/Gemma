package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * @author poirigui
 */
@Entity
@DiscriminatorValue("SingleCellDataReplacedEvent")
public class SingleCellDataReplacedEvent extends DataReplacedEvent {
}
