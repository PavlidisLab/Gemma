package ubic.gemma.apps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.FactorValueNeedsAttentionService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Deprecated
@ContextConfiguration
public class FactorValueMigratorServiceTest extends BaseCliTest {

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

        @Bean
        public FactorValueNeedsAttentionService factorValueNeedsAttentionService() {
            return mock();
        }

        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return mock();
        }
    }

    @Autowired
    private FactorValueMigratorService factorValueMigratorService;

    @Autowired
    private FactorValueService factorValueService;

    @Before
    public void setUp() {
        when( factorValueService.saveStatementIgnoreAcl( any(), any() ) ).thenAnswer( a -> a.getArgument( 1 ) );
    }

    @After
    public void tearDown() {
        reset( factorValueService );
    }

    @Test
    public void testMigrationThatReusesExistingStatement() {
        FactorValue fv = new FactorValue();
        Statement stmt = new Statement();
        stmt.setCategory( "genotype" );
        stmt.setSubject( "VPLL1" );
        fv.getCharacteristics().add( stmt );
        FactorValueMigratorService.Migration migration = FactorValueMigratorService.Migration.builder()
                .factorValueId( 1L )
                .category( "genotype" )
                .subject( "VPLL1" )
                .build();
        when( factorValueService.loadWithOldStyleCharacteristics( 1L, false ) ).thenReturn( fv );
        factorValueMigratorService.performMigration( migration, false );
        verify( factorValueService ).loadWithOldStyleCharacteristics( 1L, false );
        verify( factorValueService ).saveStatementIgnoreAcl( same( fv ), same( stmt ) );
    }

    @Test
    public void testMigrationThatReuseObject() {
        FactorValue fv = new FactorValue();
        Characteristic c = new Characteristic();
        c.setId( 1L );
        c.setValue( "bob" );
        fv.getOldStyleCharacteristics().add( c );
        FactorValueMigratorService.Migration migration = FactorValueMigratorService.Migration.builder()
                .factorValueId( 1L )
                .category( "genotype" )
                .subject( "VPLL1" )
                .predicate( "has" )
                .oldStyleCharacteristicIdUsedAsObject( 1L )
                .secondPredicate( "also has" )
                .oldStyleCharacteristicIdUsedAsSecondObject( 1L )
                .build();
        Statement stmt = new Statement();
        stmt.setCategory( "genotype" );
        stmt.setSubject( "VPLL1" );
        stmt.setPredicate( "has" );
        stmt.setObject( "bob" );
        stmt.setSecondPredicate( "also has" );
        stmt.setSecondObject( "bob" );
        when( factorValueService.loadWithOldStyleCharacteristics( 1L, false ) ).thenReturn( fv );
        factorValueMigratorService.performMigration( migration, false );
        verify( factorValueService ).loadWithOldStyleCharacteristics( 1L, false );
        verify( factorValueService ).saveStatementIgnoreAcl( same( fv ), eq( stmt ) );
    }

    @Test
    public void testMigrationWithFreeTextSubject() {
        FactorValue fv = new FactorValue();
        Characteristic c = new Characteristic();
        c.setId( 2L );
        c.setCategory( "bar" );
        c.setCategoryUri( "http://bar" );
        c.setValueUri( "http://foo/" );
        fv.getOldStyleCharacteristics().add( c );
        FactorValueMigratorService.Migration migration = FactorValueMigratorService.Migration.builder()
                .factorValueId( 1L )
                .oldStyleCharacteristicIdUsedAsSubject( 2L )
                .subject( "foo" )
                .subjectUri( null )
                .build();
        when( factorValueService.loadWithOldStyleCharacteristics( 1L, false ) ).thenReturn( fv );
        FactorValueMigratorService.MigrationResult result = factorValueMigratorService.performMigration( migration, false );
        assertThat( result.getStatement() )
                .hasFieldOrPropertyWithValue( "category", "bar" )
                .hasFieldOrPropertyWithValue( "categoryUri", "http://bar" )
                .hasFieldOrPropertyWithValue( "subject", "foo" )
                .hasFieldOrPropertyWithValue( "subjectUri", null );
        verify( factorValueService ).saveStatementIgnoreAcl( same( fv ), any() );
    }
}