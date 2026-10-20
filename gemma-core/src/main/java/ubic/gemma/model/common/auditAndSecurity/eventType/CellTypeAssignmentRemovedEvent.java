package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when a {@link CellTypeAssignmentEvent} is removed.
 * @author poirigui
 */
@Entity
@DiscriminatorValue("CellTypeAssignmentRemovedEvent")
public class CellTypeAssignmentRemovedEvent extends CellTypeAssignmentEvent {

}
