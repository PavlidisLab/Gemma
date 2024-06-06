package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.context.TestComponent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ContextConfiguration
public class FindObsoleteTermsCliTest extends AbstractJUnit4SpringContextTests {

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
        public ubic.basecode.ontology.providers.OntologyService ontology1() {
            return mock();
        }
    }

    @Autowired
    private FindObsoleteTermsCli findObsoleteTermsCli;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private ubic.basecode.ontology.providers.OntologyService ontology1;

    @Test
    public void test() {
        assertEquals( 0, findObsoleteTermsCli.executeCommand() );
        verify( ontology1 ).setSearchEnabled( false );
        verify( ontology1 ).setInferenceMode( ubic.basecode.ontology.providers.OntologyService.InferenceMode.NONE );
        verify( ontology1 ).initialize( true, false );
        verify( ontologyService ).findObsoleteTermUsage();
    }
}