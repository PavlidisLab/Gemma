package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.*;

@ContextConfiguration
public class DifferentialExpressionAnalysisDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class DifferentialExpressionAnalysisDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao( SessionFactory sessionFactory ) {
            return new DifferentialExpressionAnalysisDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Test
    public void testCreateAnalysisWithResultSetAndPvalueDistribution() {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        for ( int j = 0; j < 3; j++ ) {
            ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
            resultSet.setAnalysis( analysis );
            PvalueDistribution pvalueDist = new PvalueDistribution();
            pvalueDist.setNumBins( 2 );
            pvalueDist.setBinCounts( new byte[2] );
            for ( int i = 0; i < 100; i++ ) {
                DifferentialExpressionAnalysisResult der = new DifferentialExpressionAnalysisResult();
                CompositeSequence cs = new CompositeSequence();
                cs.setArrayDesign( ad );
                sessionFactory.getCurrentSession().persist( cs );
                der.setProbe( cs );
                der.setResultSet( resultSet );
                der.getContrasts().add( ContrastResult.Factory.newInstance() );
                der.getContrasts().add( ContrastResult.Factory.newInstance() );
                resultSet.getResults().add( der );
            }
            resultSet.setPvalueDistribution( pvalueDist );
            analysis.getResultSets().add( resultSet );
        }
        analysis = differentialExpressionAnalysisDao.create( analysis );
        sessionFactory.getCurrentSession().flush();
        assertNotNull( analysis.getId() );
        assertEquals( 3, analysis.getResultSets().size() );
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            assertNotNull( resultSet.getId() );
            assertNotNull( resultSet.getPvalueDistribution().getId() );
            assertEquals( 100, resultSet.getResults().size() );
            for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                assertNotNull( result.getId() );
                assertFalse( sessionFactory.getCurrentSession().contains( result ) );
                assertEquals( 2, result.getContrasts().size() );
                for ( ContrastResult contrast : result.getContrasts() ) {
                    assertNotNull( contrast.getId() );
                    assertFalse( sessionFactory.getCurrentSession().contains( contrast ) );
                }
            }
        }
    }
}