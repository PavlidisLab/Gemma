package ubic.gemma.core.ontology;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class FactorValueOntologyServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class FVOSTCC {

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public FactorValueOntologyService factorValueOntologyService( FactorValueService fvs, EntityUrlBuilder entityUrlBuilder ) {
            return new FactorValueOntologyServiceImpl( fvs, entityUrlBuilder );
        }
    }

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    @Autowired
    private FactorValueService factorValueService;

    @Test
    public void testWriteToRdf() {
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( new ExperimentalFactor() );
        fv.setId( 1L );
        Statement s = new Statement();
        s.setSubject( "foo" );
        fv.getCharacteristics().add( s );
        when( factorValueService.load( Collections.singleton( 1L ) ) ).thenReturn( Collections.singleton( fv ) );
        StringWriter writer = new StringWriter();
        factorValueOntologyService.writeToRdf( Collections.singleton( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ), writer );
        verify( factorValueService ).load( Collections.singleton( 1L ) );
        verify( factorValueService ).getExperimentalFactorCategoriesIgnoreAcls( Collections.singleton( fv ) );
        assertThat( writer.toString() )
                .contains( "http://gemma.msl.ubc.ca/ont/TGFVO/1" )
                .contains( "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" );
    }
}