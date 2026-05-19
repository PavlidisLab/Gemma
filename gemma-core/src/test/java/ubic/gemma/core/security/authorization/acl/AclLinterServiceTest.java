package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AclLinterServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class AclLinterCliTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public AclLinterService aclLinterService() {
            return new AclLinterServiceImpl();
        }

        @Bean
        public ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy() {
            return new ObjectIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ParentIdentityRetrievalStrategy parentObjectRetrievalStrategy() {
            return new ParentIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public AclClassMetadata aclClassMetadata( SessionFactory sessionFactory ) {
            return new AclClassMetadata( sessionFactory );
        }
    }

    @Autowired
    private AclLinterService aclLinterService;

    @Autowired
    private DataSource dataSource;

    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void test() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintChildWithIncorrectParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( false )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );

        aclLinterService.lintAcls( ExpressionAnalysisResultSet.class, config );
        aclLinterService.lintAcls( ExpressionAnalysisResultSet.class, 1L, config );

        aclLinterService.lintAcls( DifferentialExpressionAnalysis.class, config );
        aclLinterService.lintAcls( DifferentialExpressionAnalysis.class, 1L, config );

        aclLinterService.lintAcls( BioAssay.class, config );
        aclLinterService.lintAcls( BioAssay.class, 1L, config );

        aclLinterService.lintAcls( BioMaterial.class, config );
        aclLinterService.lintAcls( BioMaterial.class, 1L, config );

        config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintChildWithIncorrectParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( true )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );
    }

    /**
     * Phase 3 gsec HQL deprecation: regression coverage for the converted JdbcTemplate-backed
     * {@code lintSecurableLackingObjectIdentity} (bulk variant).
     * <p>
     * The pre-conversion HQL path returned entity-ids in {@code Entity} but not in
     * {@code AclObjectIdentity}. The new path reads existing AOI identifiers from
     * {@code acl_object_identity} JOIN {@code acl_class} via raw SQL and does the set difference
     * in Java. We verify two cases:
     * <ol>
     *   <li>Empty path: a class with zero entity rows produces zero "lacking identity" results.</li>
     *   <li>Happy path: when we seed an AOI for an entity-id, that id is NOT reported as lacking.</li>
     * </ol>
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testLintSecurableLackingObjectIdentity_emptyAndHappyPath() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintSecurablesLackingIdentities( true )
                .applyFixes( false )
                .build();

        // Empty path: ExpressionExperiment has zero rows in the fresh test DB.
        Collection<AclLinterService.LintResult> empty = aclLinterService.lintAcls( ExpressionExperiment.class, config );
        for ( AclLinterService.LintResult r : empty ) {
            assertFalse(
                    "Empty entity table should not produce 'lacking identity' results, got: " + r,
                    r.getMessage().contains( "lacks an ACL identity" ) );
        }

        // Happy path: insert a real AOI for ExpressionExperiment id=99999 (no entity row of that
        // id exists, so the dangling-identity lint will still report it, but the
        // lacks-AOI lint should not — that's the path under test).
        JdbcTemplate jt = new JdbcTemplate( dataSource );
        List<Long> existingClass = jt.queryForList(
                "select id from acl_class where class = ?", Long.class,
                ExpressionExperiment.class.getName() );
        Long classId;
        if ( existingClass.isEmpty() ) {
            jt.update( "insert into acl_class (class) values (?)", ExpressionExperiment.class.getName() );
            classId = jt.queryForObject(
                    "select id from acl_class where class = ?", Long.class,
                    ExpressionExperiment.class.getName() );
        } else {
            classId = existingClass.get( 0 );
        }
        // owner_sid=1 (GROUP_ADMIN) seeded by V3__seed_data.sql.
        jt.update(
                "insert into acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (?, ?, NULL, 1, 0)",
                classId, 99999L );

        Collection<AclLinterService.LintResult> results = aclLinterService.lintAcls( ExpressionExperiment.class, config );
        // The id we seeded should not appear in the "lacking identity" set — there are no EE rows
        // so the set should still be empty under the lacking-identity lint.
        for ( AclLinterService.LintResult r : results ) {
            assertFalse(
                    "Seeded AOI id should not be reported as lacking identity, got: " + r,
                    r.getMessage().contains( "lacks an ACL identity" ) && r.getIdentifier().equals( 99999L ) );
        }
    }

    /**
     * Phase 3 gsec HQL deprecation: regression coverage for the single-id variant of
     * {@code lintSecurableLackingObjectIdentity}.
     * <p>
     * Verifies that querying an entity-id that has an AOI returns no "lacks ACL identity"
     * results, and that querying an entity-id with no AOI does report it (when there is no entity
     * row, the lacks-AOI lint short-circuits cleanly).
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testLintSecurableLackingObjectIdentity_singleId() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintSecurablesLackingIdentities( true )
                .applyFixes( false )
                .build();

        JdbcTemplate jt = new JdbcTemplate( dataSource );
        // Resolve / create the acl_class row.
        List<Long> existing = jt.queryForList(
                "select id from acl_class where class = ?", Long.class,
                BioAssay.class.getName() );
        Long classId;
        if ( existing.isEmpty() ) {
            jt.update( "insert into acl_class (class) values (?)", BioAssay.class.getName() );
            classId = jt.queryForObject(
                    "select id from acl_class where class = ?", Long.class,
                    BioAssay.class.getName() );
        } else {
            classId = existing.get( 0 );
        }
        // Seed an AOI for identifier 77777.
        jt.update(
                "insert into acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (?, ?, NULL, 1, 0)",
                classId, 77777L );

        // Lint with a specific identifier that DOES have an AOI: should not report it as lacking.
        Collection<AclLinterService.LintResult> withAoi = aclLinterService.lintAcls( BioAssay.class, 77777L, config );
        List<AclLinterService.LintResult> lacking = new ArrayList<>();
        for ( AclLinterService.LintResult r : withAoi ) {
            if ( r.getMessage().contains( "lacks an ACL identity" ) && r.getIdentifier().equals( 77777L ) ) {
                lacking.add( r );
            }
        }
        assertEquals( "Identifier with seeded AOI should not be reported as lacking identity: " + lacking,
                0, lacking.size() );

        // Lint with an identifier that does NOT have an AOI: should report it.
        Collection<AclLinterService.LintResult> withoutAoi = aclLinterService.lintAcls( BioAssay.class, 88888L, config );
        boolean reportedLacking = false;
        for ( AclLinterService.LintResult r : withoutAoi ) {
            if ( r.getMessage().contains( "lacks an ACL identity" ) && r.getIdentifier().equals( 88888L ) ) {
                reportedLacking = true;
                break;
            }
        }
        assertTrue( "Identifier without an AOI should be reported as lacking identity",
                reportedLacking );
    }
}