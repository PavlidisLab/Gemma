package ubic.gemma.web.controller.expression.experiment;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.web.util.BaseWebTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unchecked")
@ContextConfiguration
public class ExpressionExperimentQCControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentQCControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public ExpressionExperimentQCController expressionExperimentQCController() {
            return new ExpressionExperimentQCController();
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public SVDService svdService() {
            return mock();
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock();
        }

        @Bean
        public OutlierDetectionService outlierDetectionService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ExpressionAnalysisResultSetService expressionAnalysisResultSetService() {
            return mock();
        }

        @Bean
        public CoexpressionAnalysisService coexpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ProcessedExpressionDataVectorService processedExpressionDataVectorService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @After
    public void resetMocks() {
        reset( expressionExperimentService, differentialExpressionAnalysisService, expressionAnalysisResultSetService, sampleCoexpressionAnalysisService );
    }

    @Test
    public void testVisualizeCorrelationMatrix() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( expressionExperimentService.loadOrFail( eq( 1L ), any( Function.class ) ) )
                .thenReturn( ee );
        when( expressionExperimentService.thawLiter( ee ) ).thenReturn( ee );
        DoubleMatrix<BioAssay, BioAssay> mat = mock();
        double[][] rawMat = { { 0, 1 }, { 1, 0 } };
        when( mat.getRowNames() ).thenReturn( Arrays.asList( BioAssay.Factory.newInstance( "a" ), BioAssay.Factory.newInstance( "b" ) ) );
        when( mat.getRawMatrix() ).thenReturn( rawMat );
        when( mat.rows() ).thenReturn( 2 );
        when( sampleCoexpressionAnalysisService.loadFullMatrix( ee ) ).thenReturn( mat );
        perform( get( "/expressionExperiment/visualizeCorrMat.html" ).param( "id", "1" )
                .param( "size", "10" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( MediaType.IMAGE_PNG ) );

        perform( get( "/expressionExperiment/visualizeCorrMat.html" ).param( "id", "1" )
                .param( "size", "10" )
                .param( "text", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( "text/tab-separated-values" ) );
    }


    @Test
    public void testVisualizePvalueDist() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        DifferentialExpressionAnalysis a = new DifferentialExpressionAnalysis();
        a.setId( 9182L );
        ExpressionAnalysisResultSet ears = new ExpressionAnalysisResultSet();
        ears.setId( 29182L );
        a.setExperimentAnalyzed( ee );
        ears.setAnalysis( a );
        when( expressionExperimentService.loadOrFail( eq( 1L ), any( Function.class ) ) )
                .thenReturn( ee );
        when( differentialExpressionAnalysisService.loadWithExperimentAnalyzed( 9182L ) )
                .thenReturn( a );
        when( expressionAnalysisResultSetService.loadWithAnalysis( 29182L ) )
                .thenReturn( ears );
        perform( get( "/expressionExperiment/visualizePvalueDist.html" )
                .param( "id", "1" )
                .param( "analysisId", "9182" )
                .param( "rsid", "29182" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( MediaType.IMAGE_PNG ) );
    }
}