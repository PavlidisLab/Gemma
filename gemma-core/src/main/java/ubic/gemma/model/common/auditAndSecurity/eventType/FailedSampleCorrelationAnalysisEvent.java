package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity

@DiscriminatorValue("FailedSampleCorrelationAnalysisEvent")

public class FailedSampleCorrelationAnalysisEvent extends NeedsAttentionEvent {

}
