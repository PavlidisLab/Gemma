package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static ubic.gemma.model.analysis.expression.diff.RandomDifferentialExpressionAnalysisUtils.randomAnalysis;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class DifferentialExpressionResultDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class DifferentialExpressionResultDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao( SessionFactory sessionFactory ) {
            return new DifferentialExpressionAnalysisDaoImpl( sessionFactory );
        }

        @Bean
        public DifferentialExpressionResultDao differentialExpressionResultDao( SessionFactory sessionFactory ) {
            return new DifferentialExpressionResultDaoImpl( sessionFactory, mock() );
        }
    }

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

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
    @WithMockUser
    public void testFindByGene() {
        Taxon taxon = new Taxon();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "ad", taxon );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        sessionFactory.getCurrentSession().persist( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().persist( ad );
        ExperimentalDesign ed = new ExperimentalDesign();
        ed.getExperimentalFactors().add( ExperimentalFactor.Factory.newInstance( ed, "age", FactorType.CONTINUOUS ) );
        sessionFactory.getCurrentSession().persist( ed );
        DifferentialExpressionAnalysis dea = randomAnalysis( ee, ed, ad );
        dea = differentialExpressionAnalysisDao.create( dea );
        Gene gene = new Gene();
        gene.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( gene );
        assertThat( gene.getId() ).isNotNull();
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();
        assertThat( dea.getResultSets() )
                .hasSize( 1 )
                .allSatisfy( rs -> {
                    assertThat( rs.getResults() )
                            .hasSize( 100 )
                            .satisfiesOnlyOnce( r -> {
                                assertThat( r.getProbe() ).isEqualTo( cs );
                            } );
                } );
        createProbeLink( gene, cs );
        sessionFactory.getCurrentSession().flush();
        assertThat( differentialExpressionResultDao.findByGene( gene, true ) )
                .hasSize( 1 )
                .containsKey( ee );
    }

    @Test
    @WithMockUser
    public void testFindByGeneAndThreshold() {
        Taxon taxon = new Taxon();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "ad", taxon );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        sessionFactory.getCurrentSession().persist( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().persist( ad );
        ExperimentalDesign ed = new ExperimentalDesign();
        ed.getExperimentalFactors().add( ExperimentalFactor.Factory.newInstance( ed, "age", FactorType.CONTINUOUS ) );
        sessionFactory.getCurrentSession().persist( ed );
        DifferentialExpressionAnalysis dea = randomAnalysis( ee, ed, ad );
        dea = differentialExpressionAnalysisDao.create( dea );
        Gene gene = new Gene();
        gene.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( gene );
        assertThat( gene.getId() ).isNotNull();
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();
        assertThat( dea.getResultSets() )
                .hasSize( 1 )
                .allSatisfy( rs -> {
                    assertThat( rs.getResults() )
                            .hasSize( 100 )
                            .satisfiesOnlyOnce( r -> {
                                assertThat( r.getProbe() ).isEqualTo( cs );
                            } );
                } );
        createProbeLink( gene, cs );
        assertThat( differentialExpressionResultDao.findByGene( gene, true, 1.0, 1000 ) )
                .hasSize( 1 )
                .containsKey( ee );
    }

    @Test
    public void testLoadContrastDetails() {
        Taxon taxon = new Taxon();
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        sessionFactory.getCurrentSession().persist( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().persist( ad );
        ExperimentalDesign ed = new ExperimentalDesign();
        ed.getExperimentalFactors().add( ExperimentalFactor.Factory.newInstance( ed, "age", FactorType.CONTINUOUS ) );
        sessionFactory.getCurrentSession().persist( ed );
        DifferentialExpressionAnalysis dea = randomAnalysis( ee, ed, ad );
        dea = differentialExpressionAnalysisDao.create( dea );
        DifferentialExpressionAnalysisResult dear = dea.getResultSets().iterator().next().getResults().iterator().next();
        assertThat( differentialExpressionResultDao.loadContrastDetailsForResults( Collections.singletonList( dear.getId() ) ) )
                .hasSize( 1 );
    }

    @Test
    public void testFindByGeneAndExperimentAnalyzed() {
        Gene gene = new Gene();
        sessionFactory.getCurrentSession().persist( gene );
        differentialExpressionResultDao.findByGeneAndExperimentAnalyzed( gene, true, Collections.singleton( 1L ), false );
    }

    @Test
    public void findByExperimentAnalyzed() {
        differentialExpressionResultDao.findByExperimentAnalyzed( Collections.singleton( 1L ), false, 0.0001, 100 );
    }


    @Test
    public void testFindByResultSets() {
        ExpressionAnalysisResultSet rs = new ExpressionAnalysisResultSet();
        sessionFactory.getCurrentSession().persist( rs );
        differentialExpressionResultDao.findByResultSet( rs, 0.0001, 100, 10 );
    }

    private void createProbeLink( Gene gene, CompositeSequence cs ) {
        // manually insert an entry in the GENE2CS table
        sessionFactory.getCurrentSession().createSQLQuery( "insert into GENE2CS (GENE, CS, AD) values (?, ?, ?)" )
                .setParameter( 0, gene.getId() )
                .setParameter( 1, cs.getId() )
                .setParameter( 2, cs.getArrayDesign().getId() )
                .executeUpdate();
    }
}