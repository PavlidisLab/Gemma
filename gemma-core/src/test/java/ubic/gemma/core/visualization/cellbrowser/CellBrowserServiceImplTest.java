package ubic.gemma.core.visualization.cellbrowser;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.junit.Assert.assertEquals;

@ContextConfiguration
public class CellBrowserServiceImplTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public static PropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.cellBrowser.baseUrl=http://localhost:8080/cellbrowser" );
        }

        @Bean
        public CellBrowserService cellBrowserService() {
            return new CellBrowserServiceImpl();
        }
    }

    @Autowired
    private CellBrowserService cellBrowserService;

    @Test
    public void testGetDatasetUrl() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE109281" );
        assertEquals( "http://localhost:8080/cellbrowser?ds=GSE109281", cellBrowserService.getBrowserUrl( ee ) );
        ee.setShortName( "GSE109281.1" );
        assertEquals( "http://localhost:8080/cellbrowser?ds=GSE109281_1", cellBrowserService.getBrowserUrl( ee ) );
    }
}