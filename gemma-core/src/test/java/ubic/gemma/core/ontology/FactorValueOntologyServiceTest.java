package ubic.gemma.core.ontology;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.core.context.TestComponent;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class FactorValueOntologyServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class FVOSTCC {

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }

        @Bean
        public FactorValueOntologyService factorValueOntologyService( FactorValueService fvs ) {
            return new FactorValueOntologyServiceImpl( fvs );
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
        when( factorValueService.loadWithExperimentalFactor( 1L ) ).thenReturn( fv );
        StringWriter writer = new StringWriter();
        factorValueOntologyService.writeToRdf( "http://gemma.msl.ubc.ca/ont/TGFVO/1", writer );
        verify( factorValueService ).loadWithExperimentalFactor( 1L );
        assertThat( writer.toString() )
                .contains( "http://gemma.msl.ubc.ca/ont/TGFVO/1" )
                .contains( "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" );
    }
}