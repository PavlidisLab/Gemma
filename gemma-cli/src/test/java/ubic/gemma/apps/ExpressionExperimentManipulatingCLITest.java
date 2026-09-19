package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExpressionExperimentManipulatingCLITest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public TestSingleExperimentCli testSingleExperimentCli() {
            return new TestSingleExperimentCli();
        }

        @Bean
        public TestMultiExperimentCli testMultiExperimentCli() {
            return new TestMultiExperimentCli();
        }

        @Bean
        public TestMultiExperimentCliWithForce testMultiExperimentCliWithForce() {
            return new TestMultiExperimentCliWithForce();
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

    static class TestSingleExperimentCli extends ExpressionExperimentManipulatingCLI {

        public TestSingleExperimentCli() {
            setSingleExperimentMode();
        }

        @Override
        protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
            getCliContext().getOutputStream().print( expressionExperiment );
        }
    }

    static class TestMultiExperimentCli extends ExpressionExperimentManipulatingCLI {

        @Override
        protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
            addSuccessObject( expressionExperiment, "Processed." );
        }
    }

    static class TestMultiExperimentCliWithForce extends TestMultiExperimentCli {

        @Override
        protected void buildExperimentOptions( Options options ) {
            addForceOption( options );
        }
    }

    @Autowired
    private TestSingleExperimentCli testSingleExperimentCli;

    @Autowired
    private TestMultiExperimentCli testMultiExperimentCli;

    @Autowired
    private TestMultiExperimentCliWithForce testMultiExperimentCliWithForce;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private EntityLocator entityLocator;

    @Test
    @WithMockUser
    public void testSingleExperimentMode() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( entityLocator.locateExpressionExperiment( eq( "test" ), anyBoolean() ) ).thenReturn( ee );
        assertThat( testSingleExperimentCli )
                .withArguments( "-e", "test" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .isEqualTo( ee.toString() );
        // -all does not work in single experiment mode
        assertThat( testSingleExperimentCli )
                .withArguments( "-all" )
                .fails()
                .standardError()
                .asString( StandardCharsets.UTF_8 )
                .startsWith( "Unrecognized option: -all" );
    }

    @Test
    public void testGetExperimentDataFileResult() {
        ExpressionExperimentManipulatingCLI cli = new ExpressionExperimentManipulatingCLI() {
            @Override
            protected void buildExperimentOptions( Options options ) {
                addDataFileOptions( options, "test data", true );
            }
        };
        assertThat( cli )
                .withArguments( "--help" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "-standardLocation,--standard-location" )
                .contains( "-stdout,--stdout" )
                .contains( "-o,--output-file" )
                .contains( "-d,--output-dir" );
        assertThat( cli ).withArguments( "--" );
    }

    /**
     * A troubled dataset dropped from a multi-dataset run is listed in the batch summary. This CLI has no
     * {@code -force} option, so the summary must not tell the user to pass it.
     */
    @Test
    @WithMockUser
    public void testATroubledDatasetDroppedFromABatchIsReportedAsAWarning() {
        mockTwoDatasetsWithTheSecondTroubled();
        assertThat( testMultiExperimentCli )
                .withArguments( "-e", "GSE1,GSE2", "--batch-format", "TSV" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "GSE1\tSUCCESS\tProcessed." )
                .contains( "GSE2\tWARNING\tSkipped because it is troubled." )
                .doesNotContain( "-force" );
    }

    @Test
    @WithMockUser
    public void testATroubledDatasetDroppedFromABatchPointsAtForceWhenTheCliAcceptsIt() {
        mockTwoDatasetsWithTheSecondTroubled();
        assertThat( testMultiExperimentCliWithForce )
                .withArguments( "-e", "GSE1,GSE2", "--batch-format", "TSV" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "GSE2\tWARNING\tSkipped because it is troubled; use -force to include it." );
    }

    private void mockTwoDatasetsWithTheSecondTroubled() {
        ExpressionExperiment ee1 = new ExpressionExperiment();
        ee1.setId( 1L );
        ee1.setShortName( "GSE1" );
        ExpressionExperiment ee2 = new ExpressionExperiment();
        ee2.setId( 2L );
        ee2.setShortName( "GSE2" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee1 );
        when( entityLocator.locateExpressionExperiment( eq( "GSE2" ), anyBoolean() ) ).thenReturn( ee2 );
        when( eeService.loadTroubledIds() ).thenReturn( Collections.singletonList( 2L ) );
    }
}
