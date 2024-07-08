package ubic.gemma.web.controller.common;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseWebTest;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class HomePageControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class HomePageControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public HomePageController homePageController() {
            return new HomePageController();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }
    }

    @Test
    public void testHomePage() throws Exception {
        perform( get( "/home.html" ) )
                .andExpect( view().name( "home" ) )
                .andExpect( model().attributeExists( "googleData" ) )
                .andExpect( model().attributeExists( "googleLabels" ) );
    }

    @Test
    public void testRedirectionToHomePage() throws Exception {
        perform( get( "/" ) )
                .andExpect( redirectedUrl( "/home.html" ) );
    }

    @Test
    public void testRedirectionToHomePageWithQueryParams() throws Exception {
        perform( get( "/?a=b" ) )
                .andExpect( redirectedUrl( "/home.html?a=b" ) );
    }
}