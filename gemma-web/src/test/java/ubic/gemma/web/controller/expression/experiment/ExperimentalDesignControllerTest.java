package ubic.gemma.web.controller.expression.experiment;

import gemma.gsec.SecurityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.web.controller.util.EntityDelegator;
import ubic.gemma.web.util.BaseWebTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ExperimentalDesignControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class ExperimentalDesignControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.download.path=/tmp" );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

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
        fv.setExperimentalFactor( new ExperimentalFactor() );
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
    public void testUpdateFactorValueCharacteristic() {
        Statement s1 = new Statement();
        s1.setCategory( "bar" );
        s1.setSubject( "foo" );
        Statement s2 = new Statement();
        s2.setCategory( "bar" );
        s2.setSubject( "bar" );
        when( factorValueService.loadOrFail( eq( 1L ), any( Function.class ) ) ).thenReturn( fv );
        experimentalDesignController.updateFactorValueCharacteristics( new FactorValueValueObject[] {
                new FactorValueValueObject( fv, s1 ),
                new FactorValueValueObject( fv, s2 ) } );
        verify( factorValueService, times( 1 ) ).loadOrFail( eq( 1L ), any( Function.class ) );
        verify( factorValueService ).saveStatement( fv, s1 );
        verify( factorValueService ).saveStatement( fv, s2 );
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
        FactorValueValueObject fvvo = new FactorValueValueObject( fv );
        fvvo.setCategory( "test" );
        fvvo.setValue( "test" );
        when( factorValueService.loadOrFail( eq( 1L ), any( Function.class ) ) ).thenReturn( fv );
        experimentalDesignController.updateFactorValueCharacteristics( new FactorValueValueObject[] { fvvo } );
        verify( factorValueService ).loadOrFail( eq( 1L ), any( Function.class ) );
        verify( factorValueService ).saveStatement( eq( fv ), any() );
        verify( factorValueService ).clearNeedsAttentionFlag( fv, "The dataset does not need attention and all of its factor values were fixed." );
        verifyNoMoreInteractions( factorValueService );
    }
}
