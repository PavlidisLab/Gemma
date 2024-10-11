package ubic.gemma.core.analysis.service;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionDataFileServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ExpressionDataFileServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            return new TestPropertyPlaceholderConfigurer( "gemma.appdata.home=" + Files.createTempDirectory( "gemmaData" ) );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return new ExpressionDataFileServiceImpl();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public ExpressionDataMatrixService expressionDataMatrixService() {
            return mock( ExpressionDataMatrixService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService() {
            return mock();
        }

        @Bean
        public CoexpressionService gene2geneCoexpressionService() {
            return mock( CoexpressionService.class );
        }

        @Bean
        public RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService() {
            return mock( RawAndProcessedExpressionDataVectorService.class );
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Value("${gemma.appdata.home}")
    private Path appdataHome;

    @Test
    public void testGetMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        File reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" ).toFile();
        FileUtils.forceMkdirParent( reportFile );
        FileUtils.touch( reportFile );
        assertThat( reportFile ).exists();

        File f = expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT );
        assertThat( f )
                .exists()
                .isEqualTo( reportFile );

        ee.setShortName( "test.1" );
        f = expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT );
        assertThat( f )
                .isEqualTo( reportFile );

        ee.setShortName( "test.1.2" );
        f = expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT );
        assertThat( f )
                .isEqualTo( appdataHome.resolve( "metadata/test.1/MultiQCReports/multiqc_report.html" ).toFile() );
    }
}