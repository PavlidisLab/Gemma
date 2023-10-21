package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testFactorValueStatementsNotListedInCharacteristics() {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        sessionFactory.getCurrentSession().persist( ef );
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        Characteristic c1 = new Characteristic();
        c1.setValue( "test" );
        Characteristic c2 = new Characteristic();
        c1.setValue( "test2" );
        fv.getCharacteristics().add( c1 );
        fv.getCharacteristics().add( c2 );
        fv = factorValueDao.create( fv );
        assertThat( fv.getId() ).isNotNull();
        assertThat( c1.getId() ).isNotNull();
        assertThat( c2.getId() ).isNotNull();
        // make c2 a statement
        sessionFactory.getCurrentSession()
                .createSQLQuery( "update CHARACTERISTIC set class = 'Statement' where ID = :id" )
                .setParameter( "id", c2.getId() )
                .executeUpdate();
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( fv );
        // reload
        fv = factorValueDao.load( fv.getId() );
        assertThat( fv ).isNotNull();
        assertThat( fv.getCharacteristics() )
                .contains( c1 )
                .doesNotContain( c2 );
        factorValueDao.remove( fv );
    }
}