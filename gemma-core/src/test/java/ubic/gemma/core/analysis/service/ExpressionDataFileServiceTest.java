package ubic.gemma.core.analysis.service;

import org.apache.commons.io.file.PathUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.FileLockManagerImpl;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionDataFileServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import(BuildInfo.class)
    static class ExpressionDataFileServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            return new TestPropertyPlaceholderConfigurer(
                    "gemma.appdata.home=" + Files.createTempDirectory( "gemmaData" ),
                    "gemma.hosturl=https://gemma.msl.ubc.ca" );
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
        public ExpressionDataFileHelperService expressionDataFileHelperService() {
            return new ExpressionDataFileHelperService();
        }

        @Bean
        public FileLockManager fileLockManager() {
            return new FileLockManagerImpl();
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

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public ArrayDesignAnnotationService arrayDesignAnnotationService() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor expressionDataFileTaskExecutor() {
            return mock();
        }
    }

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Value("${gemma.appdata.home}")
    private Path appdataHome;

    @Test
    public void testDeleteAll() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( reportFile ).exists();

        expressionDataFileService.deleteAllFiles( ee );

        // make sure that metadata is not touched
        assertThat( reportFile ).exists();
    }

    @Test
    public void testGetMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( reportFile ).exists();

        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValueSatisfying( p -> {
                    assertThat( p )
                            .exists()
                            .isEqualTo( reportFile );
                } );

        // ensure that metadata of a split is stored in its original directory
        ee.setShortName( "test.1" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( reportFile );

        ee.setShortName( "test.1.2" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( appdataHome.resolve( "metadata/test.1/MultiQCReports/multiqc_report.html" ) );
    }

    @Test
    public void testCopyMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path tmpReportFile = Files.createTempFile( null, "multiqc_report.html" );
        expressionDataFileService.copyMetadataFile( ee, tmpReportFile, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( reportFile );
    }

    @Test
    public void testDeleteMetadata() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "test" );
        Path reportFile = appdataHome.resolve( "metadata/test/MultiQCReports/multiqc_report.html" );
        PathUtils.createParentDirectories( reportFile );
        PathUtils.touch( reportFile );
        assertThat( expressionDataFileService.deleteMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT ) )
                .isTrue();
        assertThat( expressionDataFileService.deleteMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT ) )
                .isFalse();
        assertThat( expressionDataFileService.getMetadataFile( ee, ExpressionExperimentMetaFileType.MUTLQC_REPORT, false )
                .map( LockedPath::closeAndGetPath ) )
                .hasValue( reportFile );
    }
}