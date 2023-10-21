package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Indicate that batch information has been looked for and was missing.
 * <p>
 * This does not indicate that the batch information is problematic unlike {@link FailedBatchInformationFetchingEvent}
 * @author poirigui
 */
public class BatchInformationMissingEvent extends BatchInformationEvent {
}
