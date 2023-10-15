package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.TestComponent;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@Deprecated
@ContextConfiguration
public class FactorValueMigratorCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class FactorValueMigratorCLITestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public FactorValueMigratorCLI factorValueMigratorCLI() {
            return new FactorValueMigratorCLI();
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }
    }

    @Autowired
    private FactorValueMigratorCLI cli;

    @Autowired
    private FactorValueService factorValueService;

    @Test
    public void testMigrateFactorValues() throws IOException {
        FactorValue fv = new FactorValue();
        Characteristic c1 = new Characteristic();
        c1.setId( 1L );
        c1.setCategory( "test" );
        c1.setValue( "foo" );
        Characteristic c2 = new Characteristic();
        c2.setId( 3L );
        c2.setCategory( "test2" );
        c2.setValue( "bar" );
        fv.getOldStyleCharacteristics().add( c1 );
        fv.getOldStyleCharacteristics().add( c2 );
        when( factorValueService.loadWithOldStyleCharacteristics( 1L ) ).thenReturn( fv );
        when( factorValueService.saveStatement( any(), any() ) ).thenAnswer( a -> a.getArgument( 1, Statement.class ) );
        assertEquals( AbstractCLI.SUCCESS, cli.executeCommand( new String[] { "-migrationFile", new ClassPathResource( "ubic/gemma/core/apps/factor-value-migration.tsv" ).getFile().getAbsolutePath() } ) );
        verify( factorValueService ).loadWithOldStyleCharacteristics( 1L );
    }
}