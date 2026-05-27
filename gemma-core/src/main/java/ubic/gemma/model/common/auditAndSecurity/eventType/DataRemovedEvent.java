package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Indicates that a data type (for a specific QuantitationType, possibly new) was removed.
 * <p>
 * Prefer one of its more specific subclasses if possible, this will eventually become an abstract class.
 *
 * @author poirigui
 * @see RawDataRemovedEvent
 * @see SingleCellDataRemovedEvent
 */
@Entity
@DiscriminatorValue("DataRemovedEvent")
public class DataRemovedEvent extends ExpressionExperimentAnalysisEvent {
}
