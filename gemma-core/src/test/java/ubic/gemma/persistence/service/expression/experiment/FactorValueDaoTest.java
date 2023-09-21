package ubic.gemma.persistence.service.expression.experiment;

import org.assertj.core.api.Assertions;
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
        s1.setValue( "1" );
        s2 = Statement.Factory.newInstance();
        s2.setValue( "2" );
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
    public void testDeleteStatement() {
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
        factorValueDao.removeCharacteristic( fv, s1 );

        fv = reload( fv );
        assertTrue( fv.getCharacteristics().isEmpty() );
    }

    @Test
    public void testRemoveUnrelatedStatementRaisesAnException() {
        FactorValue fv = createFactorValue();
        Statement s = Statement.Factory.newInstance();
        sessionFactory.getCurrentSession().persist( s );
        assertThrows( IllegalArgumentException.class, () -> factorValueDao.removeCharacteristic( fv, s ) );
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

    @Test
    public void testCloneCharacteristics() {
        FactorValue fv = createFactorValue();
        Statement s1, s2;
        s1 = Statement.Factory.newInstance();
        s1.setValue( "1" );
        s2 = Statement.Factory.newInstance();
        s2.setValue( "2" );
        s1.setObject( "3" );
        s2.setSecondObject( "3" );
        fv.getCharacteristics().add( s1 );
        fv.getCharacteristics().add( s2 );
        sessionFactory.getCurrentSession().persist( fv );
        Set<Statement> clonedCharacteristics = factorValueDao.cloneCharacteristics( fv );
        for ( Statement s : clonedCharacteristics ) {
            assertNull( s.getId() );
        }
        FactorValue fvWithClonedCharacteristics = createFactorValue( clonedCharacteristics );
        assertEquals( 2, fvWithClonedCharacteristics.getCharacteristics().size() );
        Assertions.assertThat( fvWithClonedCharacteristics.getCharacteristics() )
                .hasSize( 2 )
                .satisfiesOnlyOnce( s -> {
                    assertEquals( "1", s.getValue() );
                    assertNotNull( s.getObject() );
                    assertEquals( "3", s.getObject() );
                } )
                .satisfiesOnlyOnce( s -> {
                    assertEquals( "2", s.getValue() );
                    assertNull( s.getObject() );
                    assertNotNull( s.getSecondObject() );
                    assertEquals( "3", s.getSecondObject() );
                } )
                .noneSatisfy( s -> assertEquals( "3", s.getValue() ) );
    }

    @Test
    public void testCloneNonPersistentStatement() {
        FactorValue fv = createFactorValue();
        fv.getCharacteristics().add( Statement.Factory.newInstance() );
        assertThrows( IllegalArgumentException.class, () -> factorValueDao.cloneCharacteristics( fv ) );
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