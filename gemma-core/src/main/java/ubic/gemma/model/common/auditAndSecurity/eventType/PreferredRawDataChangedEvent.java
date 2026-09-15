package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when the preferred raw data is changed.
 *
 * @see QuantitationType#getIsPreferred()
 */
@Entity
@DiscriminatorValue("PreferredRawDataChangeEvent")
public class PreferredRawDataChangedEvent extends PreferredDataChangedEvent {
}
