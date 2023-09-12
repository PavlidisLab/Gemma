package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ContextConfiguration
public class FindObsoleteTermsCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class FindObsoleteTermsCliTestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            Properties props = new Properties();
            props.setProperty( "load.ontologies", "false" );
            configurer.setProperties( props );
            return configurer;
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
        assertEquals( AbstractCLI.SUCCESS, findObsoleteTermsCli.executeCommand( new String[] {} ) );
        verify( ontology1 ).setSearchEnabled( false );
        verify( ontology1 ).setInferenceMode( ubic.basecode.ontology.providers.OntologyService.InferenceMode.NONE );
        verify( ontology1 ).initialize( true, false );
        verify( ontologyService ).findObsoleteTermUsage();
    }
}