package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    public void testFindByGeneAndExperimentAnalyzedGroupingBySourceExperiment() {
        Gene gene = new Gene();
        sessionFactory.getCurrentSession().persist( gene );
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        CompositeSequence cs = new CompositeSequence();
        ad.getCompositeSequences().add( cs );
        cs.setArrayDesign( ad );
        sessionFactory.getCurrentSession().persist( ad );
        sessionFactory.getCurrentSession().createSQLQuery( "insert into GENE2CS (GENE, CS, AD) values (?, ?, ?)" )
                .setParameter( 0, gene.getId() )
                .setParameter( 1, cs.getId() )
                .setParameter( 2, ad.getId() )
                .executeUpdate();
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, true, null );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, false, null );
        assertThatThrownBy( () -> differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, false, new HashMap<>() ) )
                .isInstanceOf( IllegalArgumentException.class );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), false, true, null );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), false, false, null );
    }

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