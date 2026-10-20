package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when a cell-level characteristics is removed.
 * @author poirigui
 */
@Entity
@DiscriminatorValue("CellLevelCharacteristicsRemovedEvent")
public class CellLevelCharacteristicsRemovedEvent extends CellLevelCharacteristicsEvent {

}
