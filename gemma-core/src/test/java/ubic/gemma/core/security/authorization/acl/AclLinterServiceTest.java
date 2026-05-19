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
import java.util.Collection;
import java.util.List;

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
     * {@code lintAclObjectIdentityLackingSecurable} (dangling-AOI variant).
     * <p>
     * The pre-conversion HQL path selected gsec-mapped {@code AclObjectIdentity} entities whose
     * {@code identifier} was absent from the corresponding entity table. The new path reads
     * existing AOI identifiers for the class via raw SQL against {@code acl_object_identity JOIN
     * acl_class}, fetches entity ids via Hibernate, and computes the dangling-set in Java.
     * <p>
     * Seeded scenario: insert an AOI for {@code ExpressionExperiment} id=98765 with no
     * corresponding entity row in {@code INVESTIGATION}. The linter should flag that id as
     * dangling. A subsequent run with the same identifier should still flag it (idempotent in
     * read-only mode).
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testLintAclObjectIdentityLackingSecurable_reportsDangling() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .applyFixes( false )
                .build();

        JdbcTemplate jt = new JdbcTemplate( dataSource );
        List<Long> existing = jt.queryForList(
                "select id from acl_class where class = ?", Long.class,
                ExpressionExperiment.class.getName() );
        Long classId;
        if ( existing.isEmpty() ) {
            jt.update( "insert into acl_class (class) values (?)", ExpressionExperiment.class.getName() );
            classId = jt.queryForObject(
                    "select id from acl_class where class = ?", Long.class,
                    ExpressionExperiment.class.getName() );
        } else {
            classId = existing.get( 0 );
        }
        // owner_sid=1 (GROUP_ADMIN) seeded by V3__seed_data.sql. No INVESTIGATION row exists
        // for id=98765 in the fresh test DB, so this AOI is dangling.
        long danglingId = 98765L;
        jt.update(
                "insert into acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (?, ?, NULL, 1, 0)",
                classId, danglingId );

        Collection<AclLinterService.LintResult> results = aclLinterService.lintAcls( ExpressionExperiment.class, config );
        boolean reported = false;
        for ( AclLinterService.LintResult r : results ) {
            if ( Long.valueOf( danglingId ).equals( r.getIdentifier() )
                    && r.getMessage().contains( "no corresponding entity" ) ) {
                reported = true;
                break;
            }
        }
        assertTrue( "Dangling AOI id " + danglingId + " should be reported by the linter", reported );
    }

    /**
     * Phase 3 gsec HQL deprecation: empty path for {@code lintAclObjectIdentityLackingSecurable}.
     * <p>
     * With no acl_object_identity rows seeded for {@code BioAssay}, the dangling-AOI lint must
     * report nothing for that class.
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testLintAclObjectIdentityLackingSecurable_emptyPath() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .applyFixes( false )
                .build();
        Collection<AclLinterService.LintResult> results = aclLinterService.lintAcls( BioAssay.class, config );
        for ( AclLinterService.LintResult r : results ) {
            assertFalse(
                    "Empty AOI table should not produce dangling results for BioAssay, got: " + r,
                    r.getMessage().contains( "no corresponding entity" ) );
        }
    }
}