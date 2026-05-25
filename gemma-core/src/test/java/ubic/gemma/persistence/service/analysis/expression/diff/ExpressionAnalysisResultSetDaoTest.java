package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.core.context.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExpressionAnalysisResultSetDaoTest extends BaseDatabaseTest5 {

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

    // -----------------------------------------------------------------------
    // binPvalues — closes the native-SQL coverage gap from
    // NATIVE_SQL_COVERAGE_AUDIT_2026_05_25.md Tier 1.
    // -----------------------------------------------------------------------

    @Test
    public void testBinPvaluesRejectsNullResultSetId() {
        assertThatThrownBy( () -> expressionAnalysisResultSetDao.binPvalues( null, "raw", 10 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testBinPvaluesRejectsZeroBins() {
        assertThatThrownBy( () -> expressionAnalysisResultSetDao.binPvalues( 1L, "raw", 0 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testBinPvaluesRejectsUnknownColumn() {
        assertThatThrownBy( () -> expressionAnalysisResultSetDao.binPvalues( 1L, "bogus", 10 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "raw" )
                .hasMessageContaining( "corrected" );
    }

    @Test
    public void testBinPvaluesOnEmptyResultSetReturnsAllZeros() {
        ExpressionExperiment sourceEE = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( sourceEE );
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( sourceEE );
        ExpressionAnalysisResultSet ears = new ExpressionAnalysisResultSet();
        dea.getResultSets().add( ears );
        ears.setAnalysis( dea );
        sessionFactory.getCurrentSession().persist( dea );
        sessionFactory.getCurrentSession().flush();
        // Empty result set: every bin should be zero, regardless of column choice.
        long[] rawBins = expressionAnalysisResultSetDao.binPvalues( ears.getId(), "raw", 10 );
        assertThat( rawBins ).hasSize( 10 );
        for ( long n : rawBins ) {
            assertThat( n ).isZero();
        }
        long[] correctedBins = expressionAnalysisResultSetDao.binPvalues( ears.getId(), "corrected", 20 );
        assertThat( correctedBins ).hasSize( 20 );
        for ( long n : correctedBins ) {
            assertThat( n ).isZero();
        }
    }
}