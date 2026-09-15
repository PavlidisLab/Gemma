package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when a {@link CellTypeAssignmentEvent} is added.
 * @author poirigui
 */
@Entity
@DiscriminatorValue("CellTypeAssignmentAddedEvent")
public class CellTypeAssignmentAddedEvent extends CellTypeAssignmentEvent {

}
