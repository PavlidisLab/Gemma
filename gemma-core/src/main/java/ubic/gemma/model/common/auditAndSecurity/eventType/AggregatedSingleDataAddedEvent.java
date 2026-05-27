package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Special case of {@link DataAddedEvent} for aggregated single-cell data.
 */
@Entity
@DiscriminatorValue("AggregatedSingleDataAddedEvent")
public class AggregatedSingleDataAddedEvent extends DataAddedEvent {
}
