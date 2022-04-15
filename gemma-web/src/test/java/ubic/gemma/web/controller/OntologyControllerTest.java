package ubic.gemma.web.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class OntologyControllerTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    public static class OntologyControllerTestContextConfiguration {

        @Bean
        public OntologyController ontologyController() {
            return new OntologyController();
        }

        @Bean
        public GemmaOntologyService gemmaOntologyService() {
            return new GemmaOntologyService();
        }
    }

    @Autowired
    private OntologyController ontologyController;

    @Test
    public void testGetObo() {
        RedirectView rv = ontologyController.getObo();
        assertThat( rv.getUrl() ).isEqualTo( "https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL" );
    }
}