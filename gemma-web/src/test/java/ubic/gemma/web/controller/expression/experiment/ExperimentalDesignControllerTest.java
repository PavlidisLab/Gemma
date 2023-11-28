package ubic.gemma.web.controller.expression.experiment;

import gemma.gsec.SecurityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.expression.experiment.FactorValueDeletion;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.BaseWebTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ExperimentalDesignControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class ExperimentalDesignControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public ExperimentalDesignController experimentalDesignController() {
            return new ExperimentalDesignController();
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock();
        }

        @Bean
        public ExperimentalDesignImporter experimentalDesignImporter() {
            return mock();
        }

        @Bean
        public ExperimentalDesignService experimentalDesignService() {
            return mock();
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentReportService experimentReportService() {
            return mock();
        }

        @Bean
        public FactorValueDeletion factorValueDeletion() {
            return mock();
        }

        @Bean
        public SecurityService securityService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExperimentalDesignController experimentalDesignController;

    @Autowired
    private FactorValueService factorValueService;

    /* fixtures */
    private FactorValue fv;

    @Before
    public void setUp() {
        ExpressionExperiment ee = new ExpressionExperiment();
        fv = new FactorValue();
        fv.setId( 1L );
        when( expressionExperimentService.findByFactorValue( fv ) ).thenReturn( ee );
        when( factorValueService.load( fv.getId() ) ).thenReturn( fv );
    }

    @After
    public void tearDown() {
        reset( factorValueService, expressionExperimentService );
    }

    @Test
    public void testCreateFactorValueCharacteristic() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        CharacteristicValueObject cvo = new CharacteristicValueObject();
        cvo.setCategory( "test" );
        cvo.setValue( "test2" );
        experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo );
        verify( factorValueService ).load( 1L );
        Statement stmt = new Statement();
        stmt.setCategory( "test" );
        stmt.setSubject( "test2" );
        verify( factorValueService ).load( 1L );
        verify( factorValueService ).createStatement( fv, stmt );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testCreateFactorValueWithBlankCategory() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        CharacteristicValueObject cvo = new CharacteristicValueObject();
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The category cannot be blank" );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testCreateFactorValueWithBlankValue() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        CharacteristicValueObject cvo = new CharacteristicValueObject();
        cvo.setCategory( "test" );
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The value cannot be blank" );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testMarkAsNeedsAttention() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        when( factorValueService.loadOrFail( eq( 1L ), any(), any() ) ).thenReturn( fv );

        experimentalDesignController.markFactorValuesAsNeedsAttention( new Long[] { fv.getId() }, "foo" );
        verify( factorValueService ).loadOrFail( eq( 1L ), any(), any() );
        verify( factorValueService ).markAsNeedsAttention( fv, "foo" );
    }

    @Test
    public void testMarkAsNeedsAttentionWhenFactorValueIsAlreadyMarked() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        fv.setNeedsAttention( true );
        when( factorValueService.loadOrFail( eq( 1L ), any(), any() ) ).thenReturn( fv );
        assertThatThrownBy( () -> {
            experimentalDesignController.markFactorValuesAsNeedsAttention( new Long[] { fv.getId() }, "" );
        } ).isInstanceOf( IllegalArgumentException.class );
        verify( factorValueService ).loadOrFail( eq( 1L ), any(), any() );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testClearNeedsAttention() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        fv.setNeedsAttention( true );
        when( factorValueService.loadOrFail( eq( 1L ), any(), any() ) ).thenReturn( fv );
        experimentalDesignController.clearFactorValuesNeedsAttention( new Long[] { fv.getId() }, "" );
        verify( factorValueService ).loadOrFail( eq( 1L ), any(), any() );
        verify( factorValueService ).clearNeedsAttentionFlag( fv, "" );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testNeedsAttentionIsResetWhenFVIsSaved() {
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        fv.setExperimentalFactor( new ExperimentalFactor() );
        fv.setNeedsAttention( true );
        FactorValueValueObject fvvo = new FactorValueValueObject();
        fvvo.setId( fv.getId() );
        when( factorValueService.loadOrFail( eq( 1L ), any(), any() ) ).thenReturn( fv );
        experimentalDesignController.updateFactorValueCharacteristics( new FactorValueValueObject[] { new FactorValueValueObject( fv ) } );
        verify( factorValueService ).loadOrFail( eq( 1L ), any(), any() );
        verify( factorValueService ).saveStatement( eq( fv ), any() );
        verify( factorValueService ).clearNeedsAttentionFlag( fv, "The dataset does not need attention and all of its factor values were fixed." );
        verifyNoMoreInteractions( factorValueService );
    }
}
