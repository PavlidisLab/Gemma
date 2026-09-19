package ubic.gemma.apps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ProcessedDataComputeCLITest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ProcessedDataComputeCLITestContextConfiguration {

        @Bean
        @Scope("prototype")
        public ProcessedDataComputeCLI processedDataComputeCLI() {
            return new ProcessedDataComputeCLI();
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock();
        }

        @Bean
        public ProcessedExpressionDataVectorService processedExpressionDataVectorService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService eeService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
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

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private ProcessedDataComputeCLI cli;

    @Autowired
    private PreprocessorService preprocessorService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private EntityLocator entityLocator;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thaw( ee ) ).thenReturn( ee );
    }

    /**
     * {@code -mdate -30d}: a dataset whose processed data was computed 5 days ago is skipped.
     */
    @Test
    public void testLimitingDateSkipsADatasetProcessedSinceThatDate() {
        when( auditEventService.getEvents( ee ) ).thenReturn( Collections.singletonList( processedEventDaysAgo( 5 ) ) );
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-mdate", "-30d" )
                .fails();
        verifyNoInteractions( preprocessorService );
    }

    /**
     * {@code -mdate -30d}: a dataset whose processed data was last computed 60 days ago is processed.
     */
    @Test
    public void testLimitingDateProcessesADatasetLastProcessedBeforeThatDate() {
        when( auditEventService.getEvents( ee ) ).thenReturn( Collections.singletonList( processedEventDaysAgo( 60 ) ) );
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-mdate", "-30d" )
                .succeeds();
        verify( preprocessorService ).process( ee, false );
    }

    private AuditEvent processedEventDaysAgo( int days ) {
        return AuditEvent.Factory.newInstance( new Date( System.currentTimeMillis() - TimeUnit.DAYS.toMillis( days ) ),
                AuditAction.UPDATE, "Processed", null, null, new ProcessedVectorComputationEvent() );
    }
}
