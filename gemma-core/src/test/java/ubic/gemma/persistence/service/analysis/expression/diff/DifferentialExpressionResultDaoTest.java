package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.core.context.TestComponent;

import java.util.Collections;

import static org.mockito.Mockito.mock;

@ContextConfiguration
public class DifferentialExpressionResultDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class DifferentialExpressionResultDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public DifferentialExpressionResultDao differentialExpressionResultDao( SessionFactory sessionFactory ) {
            return new DifferentialExpressionResultDaoImpl( sessionFactory, mock() );
        }
    }

    @Autowired
    private DifferentialExpressionResultDao differentialExpressionResultDao;

    @Test
    public void testFindByGene() {
        Gene gene = new Gene();
        sessionFactory.getCurrentSession().persist( gene );
        differentialExpressionResultDao.findByGene( gene );
    }

    @Test
    public void testFindByGeneAndExperimentAnalyzed() {
        Gene gene = new Gene();
        sessionFactory.getCurrentSession().persist( gene );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ) );
    }

    @Test
    public void findByExperimentAnalyzed() {
        differentialExpressionResultDao.findByExperimentAnalyzed( Collections.singleton( 1L ), 0.0001, 100 );
    }


    @Test
    public void testFindInResultSets() {
        ExpressionAnalysisResultSet rs = new ExpressionAnalysisResultSet();
        sessionFactory.getCurrentSession().persist( rs );
        differentialExpressionResultDao.findInResultSet( rs, 0.0001, 100, 10 );
    }
}