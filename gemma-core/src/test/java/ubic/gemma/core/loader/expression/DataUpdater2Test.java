package ubic.gemma.core.loader.expression;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawAndProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.TestComponent;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class DataUpdater2Test extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class DataUpdater2TestContextConfiguration {

        @Bean
        public DataUpdater dataUpdater() {
            return new DataUpdaterImpl();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock( BioAssayDimensionService.class );
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock( BioAssayService.class );
        }

        @Bean
        public ExpressionExperimentPlatformSwitchService experimentPlatformSwitchService() {
            return mock( ExpressionExperimentPlatformSwitchService.class );
        }

        @Bean
        public ExpressionExperimentService experimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public GeoService geoService() {
            return mock( GeoService.class );
        }

        @Bean
        public PrincipalComponentAnalysisService pcaService() {
            return mock( PrincipalComponentAnalysisService.class );
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock( PreprocessorService.class );
        }

        @Bean
        public QuantitationTypeService qtService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public RawExpressionDataVectorService rawExpressionDataVectorService() {
            return mock( RawExpressionDataVectorService.class );
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCorService() {
            return mock( SampleCoexpressionAnalysisService.class );
        }

        @Bean
        public VectorMergingService vectorMergingService() {
            return mock( VectorMergingService.class );
        }

        @Bean
        public RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService() {
            return mock( RawAndProcessedExpressionDataVectorService.class );
        }
    }

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private PreprocessorService preprocessorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    private ExpressionExperiment ee;
    private ArrayDesign ad;
    private DoubleMatrix<String, String> rpkmMatrix, countMatrix;

    @Before
    public void setUp() throws IOException {
        rpkmMatrix = new DoubleMatrixReader().read( getClass().getResourceAsStream( "/data/loader/expression/flatfileload/GSE19166_expression_count.test.txt" ) );
        countMatrix = new DoubleMatrixReader().read( getClass().getResourceAsStream( "/data/loader/expression/flatfileload/GSE19166_expression_RPKM.test.txt" ) );
        ee = new ExpressionExperiment();
        for ( String col : countMatrix.getColNames() ) {
            BioAssay ba = BioAssay.Factory.newInstance( col );
            BioMaterial bm = BioMaterial.Factory.newInstance( col );
            bm.setBioAssaysUsedIn( Collections.singleton( ba ) );
            ba.setSampleUsed( bm );
            ee.getBioAssays().add( ba );
        }
        ad = new ArrayDesign();
        ad.setId( 1L );
        for ( String row : countMatrix.getRowNames() ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( row, ad ) );
        }
    }

    @Test
    public void testSkipRecomputeLog2cpm() {
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );
        when( expressionExperimentService.addRawVectors( same( ee ), any() ) ).thenReturn( ee );
        when( expressionExperimentService.replaceRawVectors( same( ee ), any() ) ).thenReturn( ee );
        when( arrayDesignService.thaw( ad ) ).thenReturn( ad );
        when( expressionExperimentService.getArrayDesignsUsed( ee ) ).thenReturn( Collections.singleton( ad ) );
        when( bioAssayDimensionService.findOrCreate( any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        dataUpdater.addCountData( ee, ad, countMatrix, rpkmMatrix, 31, false, false, false );
        verify( preprocessorService ).process( ee );
        verify( expressionExperimentService, times( 2 ) ).addRawVectors( same( ee ), any() );
        dataUpdater.addCountData( ee, ad, countMatrix, rpkmMatrix, 30, false, false, true );
        verify( expressionExperimentService ).replaceRawVectors( same( ee ), any() );
        verifyNoMoreInteractions( preprocessorService );
    }
}