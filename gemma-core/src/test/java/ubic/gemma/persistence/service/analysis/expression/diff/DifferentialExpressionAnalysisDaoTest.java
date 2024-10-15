package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        DifferentialExpressionAnalysis analysis = createAnalysis( 3, 100, 2 );
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
        analysis = reload( analysis );
        assertNotNull( analysis.getId() );
        assertEquals( 3, analysis.getResultSets().size() );
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            assertNotNull( resultSet.getId() );
            assertNotNull( resultSet.getPvalueDistribution().getId() );
            assertEquals( 100, resultSet.getResults().size() );
            for ( DifferentialExpressionAnalysisResult result : resultSet.getResults() ) {
                assertNotNull( result.getId() );
                assertTrue( sessionFactory.getCurrentSession().contains( result ) );
                assertEquals( 2, result.getContrasts().size() );
                for ( ContrastResult contrast : result.getContrasts() ) {
                    assertNotNull( contrast.getId() );
                    assertTrue( sessionFactory.getCurrentSession().contains( contrast ) );
                }
            }
        }
        differentialExpressionAnalysisDao.remove( analysis );
        analysis = reload( analysis );
        assertNull( analysis );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidAnalysis() {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        List<CompositeSequence> probes = createPlatform( 100 );
        for ( int j = 0; j < 3; j++ ) {
            ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
            resultSet.setAnalysis( analysis );
            PvalueDistribution pvalueDist = new PvalueDistribution();
            pvalueDist.setNumBins( 2 );
            pvalueDist.setBinCounts( new double[2] );
            for ( int i = 0; i < 100; i++ ) {
                DifferentialExpressionAnalysisResult der = new DifferentialExpressionAnalysisResult();
                der.setProbe( probes.get( i ) );
                der.setResultSet( resultSet );
                // this is invalid because FVs are not shared
                der.getContrasts().add( ContrastResult.Factory.newInstance( null, null, null, null, new FactorValue(), new FactorValue() ) );
                der.getContrasts().add( ContrastResult.Factory.newInstance( null, null, null, null, new FactorValue(), new FactorValue() ) );
                resultSet.getResults().add( der );
            }
            resultSet.setPvalueDistribution( pvalueDist );
            analysis.getResultSets().add( resultSet );
        }
        //noinspection ResultOfMethodCallIgnored
        differentialExpressionAnalysisDao.create( analysis );
    }

    @Test
    public void testCreateAnalysisWithoutResults() {
        DifferentialExpressionAnalysis analysis = createAnalysis( 3, 0, 2 );
        assertNotNull( analysis.getId() );
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            assertEquals( 0, rs.getResults().size() );
        }
    }

    @Test
    public void testCreateAnalysisWithoutContrasts() {
        DifferentialExpressionAnalysis analysis = createAnalysis( 2, 100, 0 );
        assertNotNull( analysis.getId() );
        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            assertEquals( 100, rs.getResults().size() );
            for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                assertEquals( 0, result.getContrasts().size() );
            }
        }
    }

    @Test
    public void testFindByFactor() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setId( 1L );
        differentialExpressionAnalysisDao.findByFactor( ef );
    }

    @Test
    public void testFindByFactors() {
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setId( 1L );
        differentialExpressionAnalysisDao.findByFactors( Collections.singleton( ef ) );
    }

    private DifferentialExpressionAnalysis createAnalysis( int numResultSets, int numResults, int numContrasts ) {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        List<CompositeSequence> probes = createPlatform( numResults );
        for ( int j = 0; j < numResultSets; j++ ) {
            ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
            resultSet.setAnalysis( analysis );
            PvalueDistribution pvalueDist = new PvalueDistribution();
            pvalueDist.setNumBins( 2 );
            pvalueDist.setBinCounts( new double[2] );
            for ( int i = 0; i < numResults; i++ ) {
                DifferentialExpressionAnalysisResult der = new DifferentialExpressionAnalysisResult();
                der.setProbe( probes.get( i ) );
                der.setResultSet( resultSet );
                for ( int k = 0; k < numContrasts; k++ ) {
                    der.getContrasts().add( ContrastResult.Factory.newInstance() );
                }
                resultSet.getResults().add( der );
            }
            resultSet.setPvalueDistribution( pvalueDist );
            analysis.getResultSets().add( resultSet );
        }
        return differentialExpressionAnalysisDao.create( analysis );
    }

    private List<CompositeSequence> createPlatform( int numProbes ) {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        List<CompositeSequence> probes = new ArrayList<>( numProbes );
        for ( int i = 0; i < numProbes; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
            probes.add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        return probes;
    }

    private DifferentialExpressionAnalysis reload( DifferentialExpressionAnalysis analysis ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        return differentialExpressionAnalysisDao.load( analysis.getId() );
    }
}