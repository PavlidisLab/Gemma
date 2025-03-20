package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * Base class for events that alter the {@link CurationDetails#getTroubled()} flag.
 * @author poirigui
 */
public abstract class TroubledStatusFlagAlteringEvent extends CurationDetailsEvent {

}
