package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.*;

@ContextConfiguration
public class FactorValueDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class FactorValueDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public FactorValueDao factorValueDao( SessionFactory sessionFactory ) {
            return new FactorValueDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private FactorValueDao factorValueDao;

    @Test
    public void testPersistStatement() {
        FactorValue fv = createFactorValue();
        Statement s1, s2;
        s1 = Statement.Factory.newInstance();
        s2 = Statement.Factory.newInstance();
        s1.setObject( s2 );
        fv.getCharacteristics().add( s1 );
        sessionFactory.getCurrentSession().persist( fv );
        assertNotNull( fv.getId() );
        assertNotNull( s1.getId() );
        assertNotNull( s2.getId() ); // persisted in cascade
        fv = reload( fv );
        assertEquals( 1, fv.getCharacteristics().size() );
        assertTrue( fv.getCharacteristics().contains( s1 ) );
    }

    @Test
    public void testDeleteStatement() {
        FactorValue fv = createFactorValue();
        Statement s1;
        Characteristic object;
        s1 = Statement.Factory.newInstance();
        object = Characteristic.Factory.newInstance();
        s1.setObject( object );
        fv.getCharacteristics().add( s1 );
        sessionFactory.getCurrentSession().persist( fv );
        assertNotNull( fv.getId() );
        assertNotNull( s1.getId() );
        assertNotNull( object.getId() ); // persisted in cascade

        // later on
        fv = reload( fv );
        s1 = ( Statement ) sessionFactory.getCurrentSession().get( Statement.class, s1.getId() );
        object = ( Characteristic ) sessionFactory.getCurrentSession().get( Characteristic.class, object.getId() );
        factorValueDao.removeCharacteristic( fv, s1 );
        // object is deleted in cascade
        assertFalse( sessionFactory.getCurrentSession().contains( object ) );

        fv = reload( fv );
        assertTrue( fv.getCharacteristics().isEmpty() );
    }

    @Test
    public void testDeleteStatementWhenObjectIsAlsoAStatementOfTheFactorValue() {
        FactorValue fv = createFactorValue();
        Statement s1, s2;
        s1 = Statement.Factory.newInstance();
        s2 = Statement.Factory.newInstance();
        s1.setObject( s2 );
        fv.getCharacteristics().add( s1 );
        fv.getCharacteristics().add( s2 );
        sessionFactory.getCurrentSession().persist( fv );
        assertNotNull( fv.getId() );
        assertNotNull( s1.getId() );
        assertNotNull( s2.getId() ); // persisted in cascade

        // later on
        fv = reload( fv );
        s1 = ( Statement ) sessionFactory.getCurrentSession().get( Statement.class, s1.getId() );
        factorValueDao.removeCharacteristic( fv, s1 );
        // s2 is retained
        assertTrue( fv.getCharacteristics().contains( s2 ) );

        fv = reload( fv );
        assertFalse( fv.getCharacteristics().contains( s1 ) );
        assertTrue( fv.getCharacteristics().contains( s2 ) );
    }

    private FactorValue createFactorValue() {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        sessionFactory.getCurrentSession().persist( ef );
        FactorValue fv = FactorValue.Factory.newInstance();
        fv.setExperimentalFactor( ef );
        sessionFactory.getCurrentSession().persist( fv );
        return fv;
    }

    private FactorValue reload( FactorValue fv ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        return ( FactorValue ) sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() );
    }
}