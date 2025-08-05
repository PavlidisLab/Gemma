package ubic.gemma.core.visualization.cellbrowser;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class CellBrowserServiceImplTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public static PropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.cellBrowser.baseUrl=http://localhost:8080/cellbrowser", "gemma.cellBrowser.dir=/tmp/cellBrowser" );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public CellBrowserService cellBrowserService() {
            return new CellBrowserServiceImpl();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
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