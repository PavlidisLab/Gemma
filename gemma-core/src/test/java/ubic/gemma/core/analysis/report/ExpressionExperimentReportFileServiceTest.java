package ubic.gemma.core.analysis.report;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.VelocityConfig;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class ExpressionExperimentReportFileServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import(VelocityConfig.class)
    static class CC {

        @Bean
        public static PropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.appdata.home=/tmp" );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public ExpressionExperimentReportFileService expressionExperimentReportFileService() {
            return new ExpressionExperimentReportFileService();
        }

        @Bean
        public ExpressionExperimentReportService expressionExperimentReportService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentReportFileService expressionExperimentReportFileService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Test
    public void testGenerateDataProcessingReport() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "foo" );
        ee.setName( "Foo" );
        ExpressionExperimentDataProcessingReport report = ExpressionExperimentDataProcessingReport.builder()
                .geneMapping( new ExpressionExperimentDataProcessingReport.Details( "This", new Date() ) )
                .build();
        when( expressionExperimentReportService.generateDataProcessingReport( ee ) )
                .thenReturn( report );
        StringWriter sw = new StringWriter();
        expressionExperimentReportFileService.writeDataProcessingReport( ee, sw );
        assertThat( sw.toString() )
                .contains( "# Data processing report for foo: Foo" )
                .contains( "## Gene mapping details" );
    }
}