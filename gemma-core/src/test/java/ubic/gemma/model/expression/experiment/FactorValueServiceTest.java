package ubic.gemma.model.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class FactorValueServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class FactorValueServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public FactorValueDao factorValueDao( SessionFactory sessionFactory ) {
            return new FactorValueDaoImpl( sessionFactory );
        }

        @Bean
        public StatementDao statementDao( SessionFactory sessionFactory ) {
            return new StatementDaoImpl( sessionFactory );
        }

        @Bean
        public FactorValueService factorValueService( FactorValueDao factorValueDao, StatementDao statementDao ) {
            return new FactorValueServiceImpl( mock(), mock(), factorValueDao, statementDao );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock();
        }
    }

    @Autowired
    private FactorValueService factorValueService;

    @Test
    public void testCreateStatement() {
        FactorValue fv = createFactorValue();
        Statement s1;
        s1 = Statement.Factory.newInstance();
        s1.setObject( "test" );
        fv.getCharacteristics().add( s1 );
        s1 = factorValueService.createStatement( fv, s1 );
        assertNotNull( s1.getId() );
        assertTrue( fv.getCharacteristics().contains( s1 ) );
    }

    @Test
    public void testCreateStatementWithDetachedFactorValue() {
        FactorValue fv = createFactorValue();
        sessionFactory.getCurrentSession().evict( fv );
        assertFalse( sessionFactory.getCurrentSession().contains( fv ) );
        Statement s1;
        s1 = Statement.Factory.newInstance();
        s1.setObject( "test" );
        fv.getCharacteristics().add( s1 );
        s1 = factorValueService.createStatement( fv, s1 );
        assertNotNull( s1.getId() );
        assertTrue( fv.getCharacteristics().contains( s1 ) );
        assertTrue( sessionFactory.getCurrentSession().contains( fv ) );
    }

    @Test
    public void testSaveStatement() {
        FactorValue fv = createFactorValue();
        Statement s1;
        s1 = Statement.Factory.newInstance();
        s1.setObject( "test" );
        fv.getCharacteristics().add( s1 );
        s1 = factorValueService.saveStatement( fv, s1 );
        assertNotNull( s1.getId() );
        s1 = factorValueService.saveStatement( fv, s1 );
        Long previousId = s1.getId();
        assertEquals( previousId, s1.getId() );
    }

    @Test
    public void testRemoveStatement() {
        FactorValue fv = createFactorValue();
        Statement s1;
        s1 = Statement.Factory.newInstance();
        s1.setObject( "test" );
        fv.getCharacteristics().add( s1 );
        sessionFactory.getCurrentSession().persist( fv );
        assertNotNull( fv.getId() );
        assertNotNull( s1.getId() );

        // later on
        fv = reload( fv );
        s1 = ( Statement ) sessionFactory.getCurrentSession().get( Statement.class, s1.getId() );
        factorValueService.removeStatement( fv, s1 );

        fv = reload( fv );
        assertTrue( fv.getCharacteristics().isEmpty() );
    }

    @Test
    public void testRemoveDetachedStatementFromDetachedFactorValue() {
        FactorValue fv = createFactorValue();
        Statement s1;
        s1 = Statement.Factory.newInstance();
        s1.setObject( "test" );
        sessionFactory.getCurrentSession().persist( s1 );
        fv.getCharacteristics().add( s1 );
        sessionFactory.getCurrentSession().persist( fv );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( fv );
        factorValueService.removeStatement( fv, s1 );
    }

    @Test
    public void testRemoveUnrelatedStatementRaisesAnException() {
        FactorValue fv = createFactorValue();
        Statement s = Statement.Factory.newInstance();
        sessionFactory.getCurrentSession().persist( s );
        assertThrows( IllegalArgumentException.class, () -> factorValueService.removeStatement( fv, s ) );
    }

    private FactorValue createFactorValue() {
        return createFactorValue( Collections.emptySet() );
    }

    private FactorValue createFactorValue( Set<Statement> statements ) {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        sessionFactory.getCurrentSession().persist( ef );
        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        fv.getCharacteristics().addAll( statements );
        sessionFactory.getCurrentSession().persist( fv );
        return fv;
    }

    private FactorValue reload( FactorValue fv ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        return ( FactorValue ) sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() );
    }
}
