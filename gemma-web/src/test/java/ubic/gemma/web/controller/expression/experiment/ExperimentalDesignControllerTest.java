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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.expression.experiment.StatementValueObject;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ExperimentalDesignControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class ExperimentalDesignControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public ExperimentalDesignController experimentalDesignController() {
            return new ExperimentalDesignControllerImpl();
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
        StatementValueObject cvo = new StatementValueObject();
        cvo.setCategory( "test" );
        cvo.setSubject( "test2" );
        cvo.setPredicate( "has" );
        cvo.setObject( "test3" );
        experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo );
        verify( factorValueService ).load( 1L );
        verify( factorValueService ).update( fv );
        verifyNoMoreInteractions( factorValueService );
        assertThat( fv.getCharacteristics() ).hasSize( 1 )
                .first()
                .satisfies( stmt -> {
                    assertThat( stmt.getCategory() ).isEqualTo( "test" );
                    assertThat( stmt.getSubject() ).isEqualTo( "test2" );
                    assertThat( stmt.getObject() ).isEqualTo( "test3" );
                    assertThat( stmt.getSecondObject() ).isNull();
                } );
    }

    @Test
    public void testCreateFactorValueWithBlankCategory() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        StatementValueObject cvo = new StatementValueObject();
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The category cannot be blank" );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testCreateFactorValueWithBlankValue() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        StatementValueObject cvo = new StatementValueObject();
        cvo.setCategory( "test" );
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The value cannot be blank" );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testCreateFactorValueWithAlreadyExistingStatement() {
        Statement s = new Statement();
        s.setCategory( "test" );
        s.setSubject( "test" );
        fv.getCharacteristics().add( s );
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        StatementValueObject cvo = new StatementValueObject();
        cvo.setCategory( "test" );
        cvo.setSubject( "test" );
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The factor value already has this characteristic." );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }

    @Test
    public void testCreateFactorValueWithMissingObject() {
        EntityDelegator<FactorValue> fvDelegate = new EntityDelegator<>( fv );
        StatementValueObject cvo = new StatementValueObject();
        cvo.setCategory( "test" );
        cvo.setSubject( "test" );
        cvo.setPredicate( "test" );
        assertThatThrownBy( () -> experimentalDesignController.createFactorValueCharacteristic( fvDelegate, cvo ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "The predicate and object must be either both present or absent." );
        verify( factorValueService ).load( 1L );
        verifyNoMoreInteractions( factorValueService );
    }
}
