package ubic.gemma.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.web.util.BaseSpringWebTest;
import ubic.gemma.web.util.EntityNotFoundException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OntologyControllerTest extends BaseSpringWebTest {

    @Value("${url.gemmaOntology}")
    private String gemmaOntologyUrl;

    @Autowired
    private OntologyService gemmaOntology;

    @Before
    public void setUp() throws InterruptedException {
        OntologyUtils.ensureInitialized( gemmaOntology );
    }

    @Test
    public void testGetObo() throws Exception {
        mvc.perform( get( "/ont/TGEMO.OWL" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( gemmaOntologyUrl ) );
    }

    @Test
    public void testGetTerm() throws Exception {
        mvc.perform( get( "/ont/TGEMO_00001" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( gemmaOntologyUrl + "#http://gemma.msl.ubc.ca/ont/TGEMO_00001" ) );
    }

    @Test
    public void testGetMissingTerm() throws Exception {
        mvc.perform( get( "/ont/TGEMO_02312312" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "exception", instanceOf( EntityNotFoundException.class ) ) );
    }
}