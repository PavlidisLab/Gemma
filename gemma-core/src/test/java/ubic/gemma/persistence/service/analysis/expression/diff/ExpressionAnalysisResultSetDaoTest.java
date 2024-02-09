package ubic.gemma.persistence.service.analysis.expression.diff;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ExpressionAnalysisResultSetDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExpressionAnalysisResultSetDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao( SessionFactory sessionFactory ) {
            return new ExpressionAnalysisResultSetDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    @Autowired
    private AclService aclService;

    /**
     * This test covers the application of ACLs on the source experiment when a subset analysis is retrieved.
     */
    @Test
    @WithMockUser
    public void testLoadAnalysisOnSubset() {
        ExpressionExperiment sourceEE = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( sourceEE );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( sourceEE );
        sessionFactory.getCurrentSession().persist( subset );
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( subset );
        ExpressionAnalysisResultSet ears = new ExpressionAnalysisResultSet();
        dea.getResultSets().add( ears );
        ears.setAnalysis( dea );
        sessionFactory.getCurrentSession().persist( dea );
        aclService.createAcl( new AclObjectIdentity( sourceEE ) );
        assertThat( expressionAnalysisResultSetDao.load( null, null ) )
                .contains( ears );
    }
}