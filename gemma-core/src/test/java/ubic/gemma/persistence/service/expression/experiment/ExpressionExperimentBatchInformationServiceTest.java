package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationServiceImpl;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectType;

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

    @Autowired
    private SVDService svdService;

    @Before
    public void setUp() {
        when( expressionExperimentService.thawLiter( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
    }

    @After
    public void resetMocks() {
        reset( expressionExperimentService, svdService );
    }

    @Test
    public void test() {
        SVDValueObject svdResult = mock();
        when( svdResult.getDatePvals() ).thenReturn( Collections.singletonMap( 0, 0.0000001 ) );
        when( svdResult.getVariances() ).thenReturn( new double[] { 0.99 } );
        when( svdService.getSvdFactorAnalysis( 1L ) ).thenReturn( svdResult );
        ExperimentalFactor batchFactor = new ExperimentalFactor();
        batchFactor.setName( ExperimentalDesignUtils.BATCH_FACTOR_NAME );
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        batchFactor.setCategory( c );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.getExperimentalDesign().getExperimentalFactors().add( batchFactor );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertTrue( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).dataWasBatchCorrected() );
        BatchEffectDetails.BatchEffectStatistics stats = eeBatchService.getBatchEffectDetails( ee ).getBatchEffectStatistics();
        assertNotNull( stats );
        assertEquals( 1, stats.getComponent() );
        assertEquals( 0.0000001, stats.getPvalue(), 0 );
        assertEquals( 0.99, stats.getComponentVarianceProportion(), 0 );
    }

    @Test
    public void testGetBatchConfounds() {
        ExperimentalFactor batchFactor = new ExperimentalFactor();
        batchFactor.setName( ExperimentalDesignUtils.BATCH_FACTOR_NAME );
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        batchFactor.setCategory( c );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.getExperimentalDesign().getExperimentalFactors().add( batchFactor );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        assertTrue( eeBatchService.getSignificantBatchConfounds( ee ).isEmpty() );
        assertTrue( eeBatchService.getSignificantBatchConfoundsForSubsets( ee ).isEmpty() );
    }

    /**
     * Cover various edge cases of missing batch information.
     */
    @Test
    public void testMissingBatchInformation() {
        AuditEventType aet;
        AuditEvent ae;
        ExpressionExperiment ee;

        // no batch factor, no batch info attempt
        ee = new ExpressionExperiment();
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasUninformativeBatchInformation() );
        assertEquals( BatchEffectType.NO_BATCH_INFO, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        ee = new ExpressionExperiment();
        aet = new BatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertTrue( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertTrue( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertEquals( BatchEffectType.BATCH_EFFECT_UNDETERMINED_FAILURE, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        ee = new ExpressionExperiment();
        aet = new SingleBatchDeterminationEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertTrue( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertTrue( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertEquals( BatchEffectType.BATCH_EFFECT_UNDETERMINED_FAILURE, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        ee = new ExpressionExperiment();
        aet = new BatchInformationMissingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasProblematicBatchInformation() );
        assertEquals( BatchEffectType.NO_BATCH_INFO, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        // batch info missing (after 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b)
        ee = new ExpressionExperiment();
        aet = new BatchInformationMissingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Error while processing FASTQ headers for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576: No header file for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasProblematicBatchInformation() );
        assertEquals( BatchEffectType.NO_BATCH_INFO, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        // batch info failed (prior to 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b)
        ee = new ExpressionExperiment();
        aet = new FailedBatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Error while processing FASTQ headers for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576: No header file for ExpressionExperiment Id=35322 Name=Medial prefrontal cortex transcriptome of mice susceptible or resilient to chronic stress Short Name=GSE226576", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertFalse( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertFalse( eeBatchService.getBatchEffectDetails( ee ).hasProblematicBatchInformation() );
        assertEquals( BatchEffectType.NO_BATCH_INFO, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );

        // has batch information, but it's got some issues
        ee = new ExpressionExperiment();
        aet = new FailedBatchInformationFetchingEvent();
        ae = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "Invalid lane for sample GSM...", null, null, aet );
        when( auditEventService.getLastEvent( ee, BatchInformationEvent.class ) ).thenReturn( ae );
        assertTrue( eeBatchService.checkHasBatchInfo( ee ) );
        assertFalse( eeBatchService.checkHasUsableBatchInfo( ee ) );
        assertTrue( eeBatchService.getBatchEffectDetails( ee ).hasBatchInformation() );
        assertTrue( eeBatchService.getBatchEffectDetails( ee ).hasProblematicBatchInformation() );
        assertEquals( BatchEffectType.PROBLEMATIC_BATCH_INFO_FAILURE, getBatchEffectType( eeBatchService.getBatchEffectDetails( ee ) ) );
    }
}