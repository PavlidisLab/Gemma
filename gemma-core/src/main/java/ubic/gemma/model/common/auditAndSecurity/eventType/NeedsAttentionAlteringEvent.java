package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import jakarta.persistence.Entity;

/**
 * Base class for events altering {@link CurationDetails#getNeedsAttention()}.
 *
 * @deprecated see {@link CurationDetailsEvent} &mdash; the needs-attention flag is being replaced
 * by open
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#GENERIC GENERIC} /
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#BATCH_INFO_NEEDED BATCH_INFO_NEEDED}
 * tickets.
 */
@Deprecated
@Entity
public abstract class NeedsAttentionAlteringEvent extends CurationDetailsEvent {

}
