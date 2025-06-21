package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest;
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ExpressionExperimentManipulatingCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public TestSingleExperimentCli testSingleExperimentCli() {
            return new TestSingleExperimentCli();
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

    @Autowired
    private TestSingleExperimentCli testSingleExperimentCli;

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
                addExpressionDataFileOptions( options, "test data" );
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
}