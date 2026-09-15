package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Emitted when a collection of {@link ExpressionExperimentSubSet} for holding aggregated single-cell data is created.
 * @author poirigui
 */
@Entity
@DiscriminatorValue("SingleCellSubSetsCreatedEvent")
public class SingleCellSubSetsCreatedEvent extends ExpressionExperimentAnalysisEvent {

}
