package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Event emitted when the preferred set of single-cell vectors is changed.
 *
 * @author poirigui
 * @see QuantitationType#getIsSingleCellPreferred()
 */
@Entity
@DiscriminatorValue("PreferredSingleCellDataChangeEvent")
public class PreferredSingleCellDataChangedEvent extends PreferredDataChangedEvent {
}
