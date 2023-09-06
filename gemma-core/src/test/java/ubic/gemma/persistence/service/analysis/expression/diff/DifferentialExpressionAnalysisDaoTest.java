package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.assertNotNull;

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
        ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
        PvalueDistribution pvalueDist = new PvalueDistribution();
        pvalueDist.setNumBins( 2 );
        pvalueDist.setBinCounts( new byte[2] );
        resultSet.setPvalueDistribution( pvalueDist );
        analysis.getResultSets().add( resultSet );
        analysis = differentialExpressionAnalysisDao.create( analysis );
        assertNotNull( analysis.getId() );
        assertNotNull( resultSet.getId() );
        assertNotNull( pvalueDist.getId() );
    }
}