package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * Base class for events altering {@link CurationDetails#getNeedsAttention()}.
 */
public abstract class NeedsAttentionAlteringEvent extends CurationDetailsEvent {
}
