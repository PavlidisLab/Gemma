package ubic.gemma.persistence.service.association.coexpression;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class CoexpressionDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CoexpressionDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CoexpressionDao coexpressionDao() {
            return new CoexpressionDaoImpl();
        }

        @Bean
        public CoexpressionCache gene2GeneCoexpressionCache() {
            return mock();
        }

        @Bean
        public GeneTestedInCache geneTestedInCache() {
            return mock();
        }
    }

    @Autowired
    private CoexpressionDao coexpressionDao;

    @Test
    public void testHasLinks() {
        Taxon taxon = new Taxon();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        assertFalse( coexpressionDao.hasLinks( taxon, ee ) );
    }
}