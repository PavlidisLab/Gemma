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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import static org.assertj.core.api.Assertions.assertThat;

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
    // Alias-registered filterable properties (baselineGroup.characteristics.* under "bc",
    // analysis.subsetFactorValue.characteristics.* under "sfvc"). These reach the DAO as
    // Filter(objectAlias="bc", propertyName="value"), so the finder methods have to hand the
    // alias→prefix map to FilterJpaUtils; without it the path resolves as root.get("value")
    // and Hibernate throws PathElementException.
    // -----------------------------------------------------------------------

    /**
     * Persist an EE with a categorical factor whose baseline factor value carries a single
     * statement with the given subject, plus a result set pointing at that baseline group.
     */
    private ExpressionAnalysisResultSet createResultSetWithBaselineCharacteristic( String subject ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        sessionFactory.getCurrentSession().persist( ee );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setName( "factor for " + subject );
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ee.getExperimentalDesign() );
        FactorValue baseline = FactorValue.Factory.newInstance( ef,
                Statement.Factory.newInstance( "disease", null, subject, null ) );
        baseline.setIsBaseline( true );
        ef.getFactorValues().add( baseline );
        sessionFactory.getCurrentSession().persist( ef );
        sessionFactory.getCurrentSession().persist( baseline );

        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( ee );
        ExpressionAnalysisResultSet ears = new ExpressionAnalysisResultSet();
        ears.setAnalysis( dea );
        ears.setBaselineGroup( baseline );
        ears.getExperimentalFactors().add( ef );
        dea.getResultSets().add( ears );
        sessionFactory.getCurrentSession().persist( dea );
        sessionFactory.getCurrentSession().flush();
        return ears;
    }

    @Test
    public void testFindByFilterOnAliasedBaselineGroupCharacteristic() {
        ExpressionAnalysisResultSet sick = createResultSetWithBaselineCharacteristic( "sick" );
        createResultSetWithBaselineCharacteristic( "healthy" );

        Filters filters = Filters.by( expressionAnalysisResultSetDao
                .getFilter( "baselineGroup.characteristics.value", Filter.Operator.eq, "sick" ) );
        Slice<DifferentialExpressionAnalysisResultSetValueObject> slice = expressionAnalysisResultSetDao
                .findByBioAssaySetInAndDatabaseEntryInLimit( null, null, filters, 0, 10, null );
        assertThat( slice )
                .extracting( DifferentialExpressionAnalysisResultSetValueObject::getId )
                .containsExactly( sick.getId() );
        assertThat( slice.getTotalElements() ).isEqualTo( 1L );
    }

    @Test
    public void testFindBySortOnAliasedBaselineGroupCharacteristic() {
        ExpressionAnalysisResultSet sick = createResultSetWithBaselineCharacteristic( "sick" );
        ExpressionAnalysisResultSet healthy = createResultSetWithBaselineCharacteristic( "healthy" );

        Sort sort = expressionAnalysisResultSetDao
                .getSort( "baselineGroup.characteristics.value", Sort.Direction.ASC, Sort.NullMode.LAST );
        Slice<DifferentialExpressionAnalysisResultSetValueObject> slice = expressionAnalysisResultSetDao
                .findByBioAssaySetInAndDatabaseEntryInLimit( null, null, null, 0, 10, sort );
        // "healthy" sorts before "sick"
        assertThat( slice )
                .extracting( DifferentialExpressionAnalysisResultSetValueObject::getId )
                .containsExactly( healthy.getId(), sick.getId() );
    }

    @Test
    public void testFindByCursorWithFilterOnAliasedBaselineGroupCharacteristic() {
        ExpressionAnalysisResultSet sick = createResultSetWithBaselineCharacteristic( "sick" );
        createResultSetWithBaselineCharacteristic( "healthy" );

        Filters filters = Filters.by( expressionAnalysisResultSetDao
                .getFilter( "baselineGroup.characteristics.value", Filter.Operator.eq, "sick" ) );
        CursorPage<DifferentialExpressionAnalysisResultSetValueObject> page = expressionAnalysisResultSetDao
                .findByBioAssaySetInAndDatabaseEntryInByCursor( null, null, filters, null, 10 );
        assertThat( page )
                .extracting( DifferentialExpressionAnalysisResultSetValueObject::getId )
                .containsExactly( sick.getId() );
    }

    @Test
    public void testFindByFilterOnAliasedSubsetFactorValueCharacteristic() {
        createResultSetWithBaselineCharacteristic( "sick" );

        // no subset analyses exist here; what's being pinned is that the "sfvc" alias resolves to a
        // path instead of blowing up on the root entity
        Filters filters = Filters.by( expressionAnalysisResultSetDao
                .getFilter( "analysis.subsetFactorValue.characteristics.value", Filter.Operator.eq, "sick" ) );
        Slice<DifferentialExpressionAnalysisResultSetValueObject> slice = expressionAnalysisResultSetDao
                .findByBioAssaySetInAndDatabaseEntryInLimit( null, null, filters, 0, 10, null );
        assertThat( slice ).isEmpty();
    }

}
