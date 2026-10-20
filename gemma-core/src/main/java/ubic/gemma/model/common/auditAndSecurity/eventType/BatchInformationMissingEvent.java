package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Indicate that batch information has been looked for and was missing.
 * <p>
 * This does not indicate that the batch information is problematic unlike {@link FailedBatchInformationFetchingEvent}
 * @author poirigui
 */
@Entity
@DiscriminatorValue("BatchInformationMissingEvent")
public class BatchInformationMissingEvent extends BatchInformationEvent {

}
