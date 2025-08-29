package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.NeedsAttentionEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Collections;

@Service
public class FactorValueNeedsAttentionServiceImpl implements FactorValueNeedsAttentionService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditEventService auditEventService;

    @Override
    @Transactional
    public void markAsNeedsAttention( FactorValue factorValue, String note ) {
        Assert.isTrue( !factorValue.getNeedsAttention(), "This FactorValue already needs attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( true );
        factorValueService.update( factorValue );
        if ( ee != null ) {
            auditTrailService.addUpdateEvent( ee, FactorValueNeedsAttentionEvent.class, String.format( "%s: %s", factorValue, note ) );
        }
    }

    @Override
    @Transactional
    public void clearNeedsAttentionFlag( FactorValue factorValue, String note ) {
        Assert.isTrue( factorValue.getNeedsAttention(), "This FactorValue does not need attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( false );
        factorValueService.update( factorValue );
        if ( ee != null ) {
            boolean needsAttention = ee.getCurationDetails().getNeedsAttention();
            AuditEvent ae = auditEventService.getLastEvent( ee, NeedsAttentionEvent.class,
                    Collections.singleton( FactorValueNeedsAttentionEvent.class ) );
            AuditEvent nae = auditEventService.getLastEvent( ee, DoesNotNeedAttentionEvent.class );
            // ensure that the last NeedsAttentionEvent hasn't been fixed already
            boolean hasNeedsAttentionEvent = ae != null && ( nae == null || ae.getDate().after( nae.getDate() ) );
            // check if all the FVs are OK
            boolean hasFactorValueThatNeedsAttention = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .flatMap( ef -> ef.getFactorValues().stream() )
                    .anyMatch( FactorValue::getNeedsAttention );
            if ( needsAttention && !hasNeedsAttentionEvent && !hasFactorValueThatNeedsAttention ) {
                auditTrailService.addUpdateEvent( ee, DoesNotNeedAttentionEvent.class, note );
            }
        }
    }
}
