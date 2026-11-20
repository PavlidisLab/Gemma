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
import ubic.gemma.cli.audit.CliExpressionExperimentAuditService;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.UnreadableSampleCoexpressionMatrixException;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * {@code corrMat} without {@code -force} computes a matrix only when the experiment has none it can use. A stored
 * matrix that cannot be decoded is a corrupt row: it is recomputed, replacing it, rather than failing the run.
 */
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExpressionDataCorrMatCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class ExpressionDataCorrMatCliTestContextConfiguration {

        @Bean
        @Scope("prototype")
        public ExpressionDataCorrMatCli expressionDataCorrMatCli() {
            return new ExpressionDataCorrMatCli();
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock();
        }

        @Bean
        public CliExpressionExperimentAuditService cliExpressionExperimentAuditService() {
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
    private ExpressionDataCorrMatCli cli;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    private CliExpressionExperimentAuditService cliExpressionExperimentAuditService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private EntityLocator entityLocator;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLiter( ee ) ).thenReturn( ee );
    }

    @Test
    public void testAnUnreadableStoredMatrixIsRecomputed() throws Exception {
        when( sampleCoexpressionAnalysisService.retrieveExisting( ee ) ).thenThrow(
                new UnreadableSampleCoexpressionMatrixException( "The stored sample correlation matrix for "
                        + "BioAssayDimension Id=1 cannot be read: 100 bytes fit neither format for 3 bioassays.",
                        new IllegalArgumentException() ) );
        assertThat( cli )
                .withArguments( "-e", "GSE1" )
                .succeeds();
        verify( cliExpressionExperimentAuditService ).computeSampleCorrelation( ee );
    }

    @Test
    public void testAReadableStoredMatrixIsKept() throws Exception {
        when( sampleCoexpressionAnalysisService.retrieveExisting( ee ) ).thenReturn( new DenseDoubleMatrix<>( 1, 1 ) );
        assertThat( cli )
                .withArguments( "-e", "GSE1" )
                .succeeds();
        verify( cliExpressionExperimentAuditService, never() ).computeSampleCorrelation( any() );
    }

    @Test
    public void testAMissingMatrixIsComputed() throws Exception {
        when( sampleCoexpressionAnalysisService.retrieveExisting( ee ) ).thenReturn( null );
        assertThat( cli )
                .withArguments( "-e", "GSE1" )
                .succeeds();
        verify( cliExpressionExperimentAuditService ).computeSampleCorrelation( ee );
    }

    @Test
    public void testForceRecomputesWithoutReadingTheStoredMatrix() throws Exception {
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-force" )
                .succeeds();
        verify( cliExpressionExperimentAuditService ).computeSampleCorrelation( ee );
        verify( sampleCoexpressionAnalysisService, never() ).retrieveExisting( any() );
    }
}
