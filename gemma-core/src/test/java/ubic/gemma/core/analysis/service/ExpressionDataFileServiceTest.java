package ubic.gemma.core.analysis.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.TestComponent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionDataFileServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ExpressionDataFileServiceTestContextConfiguration {

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
        public CoexpressionService gene2geneCoexpressionService() {
            return mock( CoexpressionService.class );
        }

        @Bean
        public RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService() {
            return mock( RawAndProcessedExpressionDataVectorService.class );
        }
    }

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    private Object prevGemmaDataDir;
    private Path tmpDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory( "gemmaData" );
        prevGemmaDataDir = Settings.getProperty( "gemma.data.dir" );
        Settings.setProperty( "gemma.appdata.home", tmpDir.toAbsolutePath().toString() );
    }

    @After
    public void tearDown() throws IOException {
        PathUtils.deleteDirectory( tmpDir );
        Settings.setProperty( "gemma.appdata.home", prevGemmaDataDir );
    }

    @Test
    public void testGetMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        File reportFile = tmpDir.resolve( "metadata/test/MultiQCReports/multiqc_report.html" ).toFile();
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
                .isEqualTo( tmpDir.resolve( "metadata/test.1/MultiQCReports/multiqc_report.html" ).toFile() );
    }
}