package ubic.gemma.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
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
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.FactorValueNeedsAttentionService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@Deprecated
@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class FactorValueMigratorCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class FactorValueMigratorCLITestContextConfiguration {

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

        @Bean
        public FactorValueNeedsAttentionService factorValueNeedsAttentionService() {
            return mock();
        }

        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return mock();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
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

    private Characteristic getObjectById( long fvId, long id ) {
        return fvs[( int ) ( fvId - 1 )].getOldStyleCharacteristics().stream()
                .filter( c -> c.getId().equals( id ) )
                .findAny()
                .orElse( null );
    }

    private String getCategory( long fvId, long id ) {
        return getObjectById( fvId, id ).getCategory();
    }

    private String getObject( long fvId, long id ) {
        return getObjectById( fvId, id ).getValue();
    }

    @Before
    public void setUp() {
        when( factorValueService.loadWithOldStyleCharacteristics( any(), anyBoolean() ) )
                .thenAnswer( a -> fvs[a.getArgument( 0, Long.class ).intValue() - 1] );
        when( factorValueService.countAll() ).thenReturn( ( long ) fvs.length );
        AtomicLong id = new AtomicLong( 0L );
        when( factorValueService.saveStatementIgnoreAcl( any(), any() ) ).thenAnswer( a -> {
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
        assertThat( cli )
                .withArguments(
                        "-migrationFile", new ClassPathResource( "ubic/gemma/apps/factor-value-migration.tsv" ).getFile().getAbsolutePath(),
                        "-batchFormat", "suppress" )
                .succeeds();
        verify( factorValueService, times( 8 ) ).loadWithOldStyleCharacteristics( any(), eq( false ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 1L, 1L ), "Pax6", "has_modifier", getObject( 1L, 2L ), "has_modifier", getObject( 1L, 3L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( "Gene", "Pax6" ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 3L, 4L ), getObject( 3L, 4L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 4L, 5L ), getObject( 4L, 5L ), "has_modifier", getObject( 4L, 6L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 5L, 7L ), getObject( 5L, 7L ), "has_modifier", getObject( 5L, 8L ), "has_modifier", getObject( 5L, 9L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 6L, 10L ), getObject( 6L, 10L ), "has_dose", "5mg", "has_modifier", getObject( 6L, 11L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( "genotype", "Pax7", "has_modifier", getObject( 7L, 12L ), "has_modifier", getObject( 7L, 13L ) ) ) );
        verify( factorValueService ).saveStatementIgnoreAcl( any(), eq( createStatement( getCategory( 8L, 14L ), getObject( 8L, 14L ), "has_modifier", getObject( 8L, 15L ), "has_modifier", getObject( 8L, 15L ) ) ) );
        verifyNoMoreInteractions( factorValueService );
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

    private Statement createStatement( String category, String value ) {
        return createStatement( category, value, null, null, null, null );
    }

    private Statement createStatement( String category, String value, String predicate, String object ) {
        return createStatement( category, value, predicate, object, null, null );
    }

    private Statement createStatement( String category, String value, String predicate, String object, String secondPredicate, String secondObject ) {
        Statement s = new Statement();
        s.setCategory( category );
        s.setSubject( value );
        s.setPredicate( predicate );
        s.setObject( object );
        s.setSecondPredicate( secondPredicate );
        s.setSecondObject( secondObject );
        return s;
    }
}