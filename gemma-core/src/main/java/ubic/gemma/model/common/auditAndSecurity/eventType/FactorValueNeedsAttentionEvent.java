package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Indicates that a factor value needs attention.
 * @author poirigui
 */
@Entity
@DiscriminatorValue("FactorValueNeedsAttentionEvent")
public class FactorValueNeedsAttentionEvent extends NeedsAttentionEvent {

}
