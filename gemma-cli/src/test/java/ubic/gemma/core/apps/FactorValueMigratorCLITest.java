package ubic.gemma.core.apps;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.TestComponent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@Deprecated
@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class FactorValueMigratorCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class FactorValueMigratorCLITestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public FactorValueMigratorCLI factorValueMigratorCLI() {
            return new FactorValueMigratorCLI();
        }

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
    private FactorValueMigratorCLI cli;

    @Autowired
    private FactorValueService factorValueService;

    private final FactorValue[] fvs = {
            createFactorValue( 1L, 1L, 2L, 3L ),
            createFactorValue( 2L ),
            createFactorValue( 3L, 4L ),
            createFactorValue( 4L, 5L, 6L ),
            createFactorValue( 5L, 7L, 8L, 9L ),
            createFactorValue( 6L, 10L, 11L ),
            createFactorValue( 7L, 12L, 13L ),
            createFactorValue( 8L, 14L, 15L )
    };

    @Before
    public void setUp() {
        when( factorValueService.loadWithOldStyleCharacteristics( any(), anyBoolean() ) )
                .thenAnswer( a -> fvs[a.getArgument( 0, Long.class ).intValue() - 1] );
        when( factorValueService.countAll() ).thenReturn( ( long ) fvs.length );
        AtomicLong id = new AtomicLong( 0L );
        when( factorValueService.saveStatement( any(), any() ) ).thenAnswer( a -> {
            Statement s = a.getArgument( 1, Statement.class );
            if ( s.getId() == null ) {
                s.setId( id.incrementAndGet() );
            }
            return s;
        } );
    }

    @Test
    @WithMockUser
    public void testMigrateFactorValues() throws IOException {
        assertEquals( AbstractCLI.SUCCESS, cli.executeCommand(
                "-migrationFile", new ClassPathResource( "ubic/gemma/core/apps/factor-value-migration.tsv" ).getFile().getAbsolutePath(),
                "-batchFormat", "suppress" ) );
        verify( factorValueService ).load( 1L );
    }

    private FactorValue createFactorValue( Long id, Long... osIds ) {
        FactorValue fv = new FactorValue();
        fv.setId( id );
        for ( Long osId : osIds ) {
            fv.getOldStyleCharacteristics().add( createCharacteristic( osId ) );
        }
        return fv;
    }

    private Characteristic createCharacteristic( Long id ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( RandomStringUtils.randomAlphanumeric( 10 ) );
        c.setValue( RandomStringUtils.randomAlphanumeric( 10 ) );
        c.setId( id );
        return c;
    }
}