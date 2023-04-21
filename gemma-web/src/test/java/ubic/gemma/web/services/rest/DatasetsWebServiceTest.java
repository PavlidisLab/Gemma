package ubic.gemma.web.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.web.services.rest.util.BaseJerseyTest;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DatasetsWebServiceTest extends BaseJerseyTest {

    @Configuration
    @TestComponent
    static class DatasetsWebServiceTestContextConfiguration {

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock( ExpressionDataFileService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock( BioAssayService.class );
        }

        @Bean
        public ProcessedExpressionDataVectorService processedExpressionDataVectorService() {
            return mock( ProcessedExpressionDataVectorService.class );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public SVDService svdService() {
            return mock( SVDService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public OutlierDetectionService outlierDetectionService() {
            return mock( OutlierDetectionService.class );
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public ObjectMapper objectMapper() {
            return Json.mapper();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    private ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ee = ExpressionExperiment.Factory.newInstance();
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        reset( expressionExperimentService, quantitationTypeService );
    }

    @Test
    public void testGetDatasetQuantitationTypes() {
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getQuantitationTypeValueObjects( ee ) ).thenReturn( Collections.emptyList() );
        assertThat( target( "/datasets/1/quantitationTypes" ).request().get() ).hasFieldOrPropertyWithValue( "status", 200 );
        verify( expressionExperimentService ).load( 1L );
        verify( expressionExperimentService ).getQuantitationTypeValueObjects( ee );
    }

    @Test
    public void testGetDatasetProcessedExpression() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( expressionExperimentService.getMaskedPreferredQuantitationType( ee ) )
                .thenReturn( qt );
        assertThat( target( "/datasets/1/data/processed" ).request().get() ).hasFieldOrPropertyWithValue( "status", 200 );
        verify( expressionExperimentService ).getMaskedPreferredQuantitationType( ee );
    }

    @Test
    public void testGetDatasetProcessedExpressionByQuantitationType() throws IOException {
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( expressionExperimentService.getMaskedPreferredQuantitationType( ee ) ).thenReturn( qt );
        assertThat( target( "/datasets/1/data/processed" )
                .queryParam( "quantitationType", "12" ).request().get() )
                .hasFieldOrPropertyWithValue( "status", 200 );
        verify( expressionDataFileService ).writeProcessedExpressionData( eq( ee ), eq( qt ), any() );
    }

    @Test
    public void testGetDatasetRawExpression() throws IOException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( expressionExperimentService.getPreferredQuantitationType( ee ) )
                .thenReturn( qt );
        assertThat( target( "/datasets/1/data/raw" ).request().get() ).hasFieldOrPropertyWithValue( "status", 200 );
        verify( expressionExperimentService ).getPreferredQuantitationType( ee );
        verifyNoInteractions( quantitationTypeService );
        verify( expressionDataFileService ).writeRawExpressionData( eq( ee ), eq( qt ), any() );
    }

    @Test
    public void testGetDatasetRawExpressionByQuantitationType() throws IOException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( quantitationTypeService.findByIdAndDataVectorType( ee, 12L, RawExpressionDataVector.class ) ).thenReturn( qt );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).findByIdAndDataVectorType( ee, 12L, RawExpressionDataVector.class );
        verify( expressionDataFileService ).writeRawExpressionData( eq( ee ), eq( qt ), any() );
        assertThat( res.getStatus() ).isEqualTo( 200 );
    }
}