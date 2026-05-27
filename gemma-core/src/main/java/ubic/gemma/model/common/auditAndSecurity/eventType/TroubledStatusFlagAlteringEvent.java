package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import jakarta.persistence.Entity;

/**
 * Base class for events that alter the {@link CurationDetails#getTroubled()} flag.
 *
 * @author poirigui
 * @deprecated see {@link CurationDetailsEvent} &mdash; the trouble flag is being replaced by
 * open
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#QUALITY_REVIEW QUALITY_REVIEW}
 * tickets.
 */
@Deprecated
@Entity
public abstract class TroubledStatusFlagAlteringEvent extends CurationDetailsEvent {

}
