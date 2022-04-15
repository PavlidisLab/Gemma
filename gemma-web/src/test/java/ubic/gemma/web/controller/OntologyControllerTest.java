package ubic.gemma.web.controller;

import org.junit.Test;
import ubic.gemma.web.util.BaseSpringWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OntologyControllerTest extends BaseSpringWebTest {

    @Test
    public void testGetObo() throws Exception {
        mvc.perform( get( "/ont/TGEMO.OWL" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( "https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL" ) );
    }
}