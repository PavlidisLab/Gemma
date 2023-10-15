package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.TestComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Deprecated
@ContextConfiguration
public class FactorValueMigratorServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class FVMSTCC {

        @Bean
        public FactorValueMigratorService factorValueMigratorService() {
            return new FactorValueMigratorServiceImpl();
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock();
        }
    }

    @Autowired
    private FactorValueMigratorService factorValueMigratorService;

    @Autowired
    private FactorValueService factorValueService;

    @Test
    public void testMigrationThatReusesExistingStatement() {
        FactorValueMigratorService.Migration migration = FactorValueMigratorService.Migration.builder()
                .factorValueId( 1L )
                .build();
        when( factorValueService.load( 1L ) );
        factorValueMigratorService.performMigration( migration, true );
        verify( factorValueService ).load( 1L );
        verify( factorValueService ).saveStatement( any(), any() );
    }
}