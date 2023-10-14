package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Set;

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
        s1.setSubject( "1" );
        s2 = Statement.Factory.newInstance();
        s2.setSubject( "2" );
        fv.getCharacteristics().add( s1 );
        fv.getCharacteristics().add( s2 );
        sessionFactory.getCurrentSession().persist( fv );
        assertNotNull( fv.getId() );
        assertNotNull( s1.getId() );
        assertNotNull( s2.getId() ); // persisted in cascade
        fv = reload( fv );
        assertEquals( 2, fv.getCharacteristics().size() );
        assertTrue( fv.getCharacteristics().contains( s1 ) );
    }

    @Test
    public void testLoadFactorValueWithRegularCharacteristic() {
        FactorValue fv = createFactorValue();
        Statement s1, s2;
        s1 = Statement.Factory.newInstance();
        s1.setSubject( "1" );
        s2 = Statement.Factory.newInstance();
        s2.setSubject( "2" );
        fv.getCharacteristics().add( s1 );
        fv.getCharacteristics().add( s2 );
        sessionFactory.getCurrentSession().persist( fv );
        // create a regular characteristic
        FactorValue finalFv = fv;
        sessionFactory.getCurrentSession().doWork( work -> {
            PreparedStatement stmt = work.prepareStatement( "insert into CHARACTERISTIC (CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, FACTOR_VALUE_FK) values ('foo', null, 'foo', null, ?)" );
            stmt.setLong( 1, finalFv.getId() );
            assertEquals( 1, stmt.executeUpdate() );
        } );
        fv = reload( fv );
        fv.getCharacteristics().forEach( System.out::println );
        assertEquals( 2, fv.getCharacteristics().size() );
        assertEquals( 1, fv.getOldStyleCharacteristics().size() );
    }

    @Test
    public void testRemove() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        FactorValue fv = createFactorValue();
        ExperimentalFactor ef = fv.getExperimentalFactor();
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        bm.getFactorValues().add( fv );
        sessionFactory.getCurrentSession().persist( bm );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ef = ( ExperimentalFactor ) sessionFactory.getCurrentSession().get( ExperimentalFactor.class, ef.getId() );
        fv = ( FactorValue ) sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() );
        bm = ( BioMaterial ) sessionFactory.getCurrentSession().get( BioMaterial.class, bm.getId() );
        assertTrue( ef.getFactorValues().contains( fv ) );
        assertTrue( bm.getFactorValues().contains( fv ) );
        factorValueDao.remove( fv );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ef = ( ExperimentalFactor ) sessionFactory.getCurrentSession().get( ExperimentalFactor.class, ef.getId() );
        fv = ( FactorValue ) sessionFactory.getCurrentSession().get( FactorValue.class, fv.getId() );
        bm = ( BioMaterial ) sessionFactory.getCurrentSession().get( BioMaterial.class, bm.getId() );
        assertFalse( ef.getFactorValues().contains( fv ) );
        assertFalse( bm.getFactorValues().contains( fv ) );
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