package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.util.Collections;

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
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, null, null, null, 1.0, false, true );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, null, null, null, 1.0, false, true );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), false, null, null, null, 1.0, false, true );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), false, null, null, null, 1.0, false, true );
        assertThatThrownBy( () -> differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, null, null, null, 1.2, false, true ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, Collections.singleton( 1L ), true, null, null, null, -1, false, true ) )
                .isInstanceOf( IllegalArgumentException.class );
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