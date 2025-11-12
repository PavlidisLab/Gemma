package ubic.gemma.web.controller.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.web.service.ExpressionExperimentControllerHelperService;
import ubic.gemma.web.service.ExpressionExperimentEditControllerHelperService;
import ubic.gemma.web.util.BaseWebTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class ExpressionExperimentEditControllerTest extends BaseWebTest {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Configuration
    @TestComponent
    static class ExpressionExperimentFormControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public ExpressionExperimentEditController expressionExperimentFormController() {
            return new ExpressionExperimentEditController();
        }

        @Bean
        public ExpressionExperimentEditControllerHelperService expressionExperimentEditControllerHelperService() {
            return new ExpressionExperimentEditControllerHelperService();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock();
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock();
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public Persister persisterHelper() {
            return mock();
        }

        @Bean
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    private ExpressionExperiment ee;
    private QuantitationType qt;

    @Before
    public void setUp() {
        qt = new QuantitationType();
        qt.setId( 1L );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Set<QuantitationType> qts = new HashSet<>();
        qts.add( qt );
        ee = new ExpressionExperiment();
        ee.getQuantitationTypes().addAll( qts );
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 1L ), any(), any() ) ).thenReturn( ee );
        when( expressionExperimentService.getQuantitationTypes( ee ) ).thenReturn( qts );
        when( expressionExperimentService.getQuantitationTypesByVectorType( ee ) ).thenReturn( Collections.singletonMap( RawExpressionDataVector.class, qts ) );
        when( expressionExperimentService.thaw( ee ) ).thenReturn( ee );
    }

    @After
    public void tearDown() {
        reset( expressionExperimentService, singleCellExpressionExperimentService, preprocessorService );
    }

    @Test
    public void test() throws Exception {
        perform( get( "/expressionExperiment/editExpressionExperiment.html?id=1" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( model().attributeExists( "expressionExperiment", "keywords",
                        "standardQuantitationTypes", "scaleTypes", "generalQuantitationTypes" ) );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", "log2cpm" )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( model().attributeExists( "expressionExperiment", "keywords",
                        "standardQuantitationTypes", "scaleTypes", "generalQuantitationTypes" ) );

        assertThat( qt.getName() ).isEqualTo( "log2cpm" );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verifyNoInteractions( preprocessorService );
    }

    @Test
    public void testSignificantChangeOnPreferredQt() throws Exception {
        qt.setIsPreferred( true );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", "true" ) // false -> true
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) )
                .param( "quantitationTypes[0].isPreferred", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( request().sessionAttribute( "messages", Collections.singletonList( "Preferred raw quantitation type has been significantly changed, reprocessing will be performed." ) ) );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verify( preprocessorService ).process( ee );
    }

    @Test
    public void testSwitchNonPreferredVectorToPreferred() throws Exception {
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) )
                .param( "quantitationTypes[0].isPreferred", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( request().sessionAttribute( "messages", Collections.singletonList( "Preferred raw quantitation type has been significantly changed, reprocessing will be performed." ) ) );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verify( preprocessorService ).process( ee );
    }

    @Test
    public void testSwitchPreferredVectorToNonPreferred() throws Exception {
        qt.setIsPreferred( true );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) )
                .param( "quantitationTypes[0].isPreferred", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( request().sessionAttribute( "messages", Collections.singletonList( "There is no preferred quantitation type, however existing processed data will be kept." ) ) );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verifyNoInteractions( preprocessorService );
    }

    @Test
    public void testFixDenormalizedQt() throws Exception {
        ee.getQuantitationTypes().remove( qt );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) );

        assertThat( ee.getQuantitationTypes() ).contains( qt );

        verify( expressionExperimentService ).update( ee );
        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
    }

    @Test
    public void testUpdatePreferredSingleCellVectors() throws Exception {
        when( expressionExperimentService.getQuantitationTypesByVectorType( ee ) ).thenReturn( Collections.singletonMap( SingleCellExpressionDataVector.class, Collections.singleton( qt ) ) );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) )
                .param( "quantitationTypes[0].isSingleCellPreferred", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( request().sessionAttribute( "messages", Collections.singletonList( "Preferred single-cell quantitation type has been significantly changed, single-cell sparsity metrics will be recomputed." ) ) );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verify( singleCellExpressionExperimentService ).updateSparsityMetrics( ee );
    }

    @Test
    public void testUpdatePreferredSingleCellVectorsToNonPreferred() throws Exception {
        qt.setIsSingleCellPreferred( true );
        when( expressionExperimentService.getQuantitationTypesByVectorType( ee ) ).thenReturn( Collections.singletonMap( SingleCellExpressionDataVector.class, Collections.singleton( qt ) ) );
        perform( post( "/expressionExperiment/editExpressionExperiment.html?id=1" )
                .param( "quantitationTypes[0].id", String.valueOf( qt.getId() ) )
                .param( "quantitationTypes[0].name", String.valueOf( qt.getName() ) )
                .param( "quantitationTypes[0].generalType", qt.getGeneralType().name() )
                .param( "quantitationTypes[0].type", qt.getType().name() )
                .param( "quantitationTypes[0].scale", qt.getScale().name() )
                .param( "quantitationTypes[0].representation", qt.getRepresentation().name() )
                .param( "quantitationTypes[0].isBackground", String.valueOf( qt.getIsBackground() ) )
                .param( "quantitationTypes[0].isBackgroundSubtracted", String.valueOf( qt.getIsBackgroundSubtracted() ) )
                .param( "quantitationTypes[0].isNormalized", String.valueOf( qt.getIsNormalized() ) )
                .param( "quantitationTypes[0].isBatchCorrected", String.valueOf( qt.getIsBatchCorrected() ) )
                .param( "quantitationTypes[0].isRatio", String.valueOf( qt.getIsRatio() ) )
                .param( "quantitationTypes[0].isRecomputedFromRawData", String.valueOf( qt.getIsRecomputedFromRawData() ) )
                .param( "quantitationTypes[0].isSingleCellPreferred", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) )
                .andExpect( request().sessionAttribute( "messages", Collections.singletonList( "There is no preferred single-cell quantitation type, single-cell sparsity metrics will be cleared." ) ) );

        verify( expressionExperimentService ).updateQuantitationType( ee, qt );
        verify( singleCellExpressionExperimentService ).updateSparsityMetrics( ee );
    }
}