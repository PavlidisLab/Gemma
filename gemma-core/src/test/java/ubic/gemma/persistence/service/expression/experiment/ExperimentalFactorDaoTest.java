package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.persistence.util.TestComponent;

@ContextConfiguration
public class ExperimentalFactorDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExperimentalFactorServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExperimentalFactorDao experimentalFactorDao( SessionFactory sessionFactory ) {
            return new ExperimentalFactorDaoImpl( sessionFactory );
        }

    }

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void testDeleteExperimentalFactor() {
        ExperimentalFactor ef = experimentalFactorDao.create( createExperimentalFactor() );
        experimentalFactorDao.remove( ef );
    }

    private ExperimentalFactor createExperimentalFactor() {
        ExperimentalDesign ed = new ExperimentalDesign();
        sessionFactory.getCurrentSession().persist( ed );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setExperimentalDesign( ed );
        ef.setType( FactorType.CATEGORICAL );
        return ef;
    }
}