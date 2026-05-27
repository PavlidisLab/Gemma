package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Special case of {@link DataAddedEvent} for single-cell data.
 *
 * @author poirigui
 */
@Entity
@DiscriminatorValue("SingleCellDataAddedEvent")
public class SingleCellDataAddedEvent extends DataAddedEvent {
}
