package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.experiment.*;

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
            return new FactorValueServiceImpl( factorValueDao, statementDao );
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
        // Pre-Phase-2 the DAO used Session#update(), which reattached the original detached instance,
        // so contains(fv) was true after createStatement. Hibernate 6's JPA-correct path is merge(),
        // which returns a new managed instance and leaves the input detached. The behavioural
        // promise of the operation is "the new Statement is persisted and linked to the FV in the
        // DB" — verify that via a fresh load instead of an in-memory identity check.
        FactorValue reloaded = sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() );
        assertNotNull( reloaded );
        Long newStatementId = s1.getId();
        assertTrue( reloaded.getCharacteristics().stream().anyMatch( c -> c.getId().equals( newStatementId ) ) );
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
        // The test's name promises BOTH the FV and the statement are detached. Pre-Phase-2 only fv
        // was evicted; the pre-existing Statement reference remained managed, but Hibernate 5's
        // Session#update() was lenient about that. Hibernate 6's merge() cascade creates a fresh
        // managed Statement#id during merge(fv), which then collides with the already-managed s1
        // reference at statementDao.remove() with "A different object with the same identifier".
        // Evict s1 too so the scenario matches the test name and the merge cascade has a clean
        // session to work with.
        sessionFactory.getCurrentSession().evict( fv );
        sessionFactory.getCurrentSession().evict( s1 );
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
