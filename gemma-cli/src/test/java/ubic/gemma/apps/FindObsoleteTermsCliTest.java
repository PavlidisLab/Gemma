package ubic.gemma.apps;

import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.TestCLIContext;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FindObsoleteTermsCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class FindObsoleteTermsCliTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "load.ontologies=false" );
        }

        @Bean
        public FindObsoleteTermsCli findObsoleteTermsCli() {
            return new FindObsoleteTermsCli();
        }

        @Bean
        public OntologyService ontologyService() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor ontologyTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }

        @Bean
        public ubic.gemma.core.ontology.basecode.providers.OntologyService ontology1() {
            return mock();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private FindObsoleteTermsCli findObsoleteTermsCli;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private ubic.gemma.core.ontology.basecode.providers.OntologyService ontology1;

    @Test
    @WithMockUser
    public void test() throws TimeoutException {
        TestCLIContext cliContext = new TestCLIContext( null, new String[] {} );
        findObsoleteTermsCli.executeCommand( cliContext );
        assertEquals( 0, cliContext.getExitStatus() );
        verify( ontology1 ).setSearchEnabled( false );
        verify( ontology1 ).setInferenceMode( ubic.gemma.core.ontology.basecode.providers.OntologyService.InferenceMode.NONE );
        verify( ontology1 ).initialize( true, false );
        verify( ontologyService ).findObsoleteTermUsage( 4, TimeUnit.HOURS );
    }
}