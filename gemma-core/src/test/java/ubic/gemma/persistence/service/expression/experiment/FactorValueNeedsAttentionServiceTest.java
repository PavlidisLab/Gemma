package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.NeedsAttentionEvent;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class FactorValueNeedsAttentionServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class FactorValueNeedsAttentionServiceTestContextConfiguration {

        @Bean
        public FactorValueNeedsAttentionService factorValueNeedsAttentionService() {
            return new FactorValueNeedsAttentionServiceImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }
    }

    @Autowired
    private FactorValueNeedsAttentionService factorValueNeedsAttentionService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @After
    public void tearDown() {
        reset( expressionExperimentService, auditTrailService, auditEventService );
    }

    @Test
    public void testClearNeedsAttentionFlag() {
        FactorValue fv = createFactorValue();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );
        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "test" );
        ee.getCurationDetails().setNeedsAttention( true );
        assertTrue( fv.getNeedsAttention() );
        verify( auditTrailService ).addUpdateEvent( ee, FactorValueNeedsAttentionEvent.class, "FactorValue [Needs Attention]: test" );
        when( auditEventService.getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) ) )
                .thenReturn( null );
        when( auditEventService.getLastEvent( ee, DoesNotNeedAttentionEvent.class ) )
                .thenReturn( null );
        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "foo" );
        verify( auditEventService ).getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) );
        verify( auditEventService ).getLastEvent( ee, DoesNotNeedAttentionEvent.class );
        verify( auditTrailService ).addUpdateEvent( ee, DoesNotNeedAttentionEvent.class, "foo" );
    }

    @Test
    public void testClearNeedsAttentionFlagThatAlreadyNeedsAttention() {
        FactorValue fv = createFactorValue();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );
        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "test" );
        ee.getCurationDetails().setNeedsAttention( true );
        assertTrue( fv.getNeedsAttention() );
        verify( auditTrailService ).addUpdateEvent( ee, FactorValueNeedsAttentionEvent.class, "FactorValue [Needs Attention]: test" );
        when( auditEventService.getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) ) )
                .thenReturn( new AuditEvent() );
        when( auditEventService.getLastEvent( ee, DoesNotNeedAttentionEvent.class ) )
                .thenReturn( null );
        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "foo" );
        verify( auditEventService ).getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) );
        verify( auditEventService ).getLastEvent( ee, DoesNotNeedAttentionEvent.class );
        verifyNoMoreInteractions( auditEventService );
    }

    @Test
    public void testClearNeedsAttentionFlagWhenANeedsAttentionEventWasResolved() {
        FactorValue fv = createFactorValue();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( fv.getExperimentalFactor().getExperimentalDesign() );
        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        factorValueNeedsAttentionService.markAsNeedsAttention( fv, "test" );
        ee.getCurationDetails().setNeedsAttention( true );
        assertTrue( fv.getNeedsAttention() );
        verify( auditTrailService ).addUpdateEvent( ee, FactorValueNeedsAttentionEvent.class, "FactorValue [Needs Attention]: test" );
        when( auditEventService.getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) ) )
                .thenReturn( AuditEvent.Factory.newInstance( new Date( 1000 ), null, null, null, null, new NeedsAttentionEvent() ) );
        when( auditEventService.getLastEvent( ee, DoesNotNeedAttentionEvent.class ) )
                .thenReturn( AuditEvent.Factory.newInstance( new Date( 2000 ), null, null, null, null, new NeedsAttentionEvent() ) );
        factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, "foo" );
        verify( auditEventService ).getLastEvent( ee, NeedsAttentionEvent.class, Collections.singleton( FactorValueNeedsAttentionEvent.class ) );
        verify( auditEventService ).getLastEvent( ee, DoesNotNeedAttentionEvent.class );
        verify( auditTrailService ).addUpdateEvent( ee, DoesNotNeedAttentionEvent.class, "foo" );
    }

    private FactorValue createFactorValue() {
        return createFactorValue( Collections.emptySet() );
    }

    private FactorValue createFactorValue( Set<Statement> statements ) {
        ExperimentalDesign ed = new ExperimentalDesign();
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.getCharacteristics().addAll( statements );
        return fv;
    }
}