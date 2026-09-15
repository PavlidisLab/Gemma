package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity

@DiscriminatorValue("SampleCorrelationAnalysisEvent")

public class SampleCorrelationAnalysisEvent extends ExpressionExperimentAnalysisEvent {

}
