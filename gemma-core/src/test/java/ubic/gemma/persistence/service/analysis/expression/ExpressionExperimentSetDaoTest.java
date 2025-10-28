package ubic.gemma.persistence.service.analysis.expression;

import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration
public class ExpressionExperimentSetDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CC extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentSetDao expressionExperimentSetDao( SessionFactory sessionFactory, ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentSetDaoImpl( sessionFactory, expressionExperimentDao );
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    /**
     * This is an example of modeling a GEO super-series.
     */
    @Test
    public void testCreateSetWithAccession() {
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        sessionFactory.getCurrentSession().persist( geo );
        ExpressionExperimentSet expressionExperimentSet = new ExpressionExperimentSet();
        expressionExperimentSet.setAccession( DatabaseEntry.Factory.newInstance( "GSE000123", geo ) );
        expressionExperimentSet = expressionExperimentSetDao.create( expressionExperimentSet );
        assertNotNull( expressionExperimentSet.getId() );
        assertNotNull( expressionExperimentSet.getAccession() );
        assertNotNull( expressionExperimentSet.getAccession().getId() );
        assertThat( expressionExperimentSetDao.findByAccession( "GSE000123" ) )
                .contains( expressionExperimentSet );
        assertThat( expressionExperimentSetDao.findByAccession( "GSE000123", geo ) )
                .contains( expressionExperimentSet );
        sessionFactory.getCurrentSession().flush();

        // violates the unique key on the accession
        ExpressionExperimentSet set2 = new ExpressionExperimentSet();
        set2.setAccession( expressionExperimentSet.getAccession() );
        assertThatThrownBy( () -> expressionExperimentSetDao.create( set2 ) )
                .isInstanceOf( ConstraintViolationException.class );
        sessionFactory.getCurrentSession().clear();
    }

}