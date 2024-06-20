package ubic.gemma.persistence.service.expression.experiment;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationServiceImpl;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class ExpressionExperimentBatchInformationServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ExpressionExperimentBatchInformationServiceTestContextConfiguration {

        @Bean
        public ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService() {
            return new ExpressionExperimentBatchInformationServiceImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public SVDService svdService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }

    }

    @Autowired
    private ExpressionExperimentBatchInformationService eeBatchService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private AuditEventService auditEventService;

    @Before
    public void setUp() {
        when( expressionExperimentService.thawLiter( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    @Test
    public void testBatchInfo() {
        AuditEventType aet;
        AuditEvent ae;
        ExpressionExperiment ee;

        // no batch factor, no batch info attempt
        ee = new ExpressionExperiment();
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );

        ee = new ExpressionExperiment();
        aet = new BatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertTrue( eeBatchService.checkHasUsableBatchInfo( ee ) );

        ee = new ExpressionExperiment();
        aet = new SingleBatchDeterminationEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertTrue( eeBatchService.checkHasUsableBatchInfo( ee ) );

        ee = new ExpressionExperiment();
        aet = new BatchInformationMissingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );

        // batch info missing (after 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b)
        ee = new ExpressionExperiment();
        aet = new BatchInformationMissingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Error while processing FASTQ headers for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576: No header file for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );

        // batch info failed (prior to 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b)
        ee = new ExpressionExperiment();
        aet = new FailedBatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Error while processing FASTQ headers for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576: No header file for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );

        // has batch information, but it's got some issues
        ee = new ExpressionExperiment();
        aet = new FailedBatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Invalid lane for sample GSM...", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
    }
}