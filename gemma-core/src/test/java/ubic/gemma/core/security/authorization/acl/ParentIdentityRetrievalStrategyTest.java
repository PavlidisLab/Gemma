package ubic.gemma.core.security.authorization.acl;

import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 3 gsec HQL deprecation: regression coverage for {@link ParentIdentityRetrievalStrategyImpl}
 * after its HQL select against {@code AclObjectIdentity} was replaced with raw SQL against
 * {@code acl_object_identity JOIN acl_class}.
 * <p>
 * Two cases:
 * <ol>
 *   <li>Happy path: a seeded acl_object_identity row for ExpressionExperiment id=N is returned as
 *       an {@link AclObjectIdentity} carrying type + identifier + the row's PK id.</li>
 *   <li>Empty path: when no matching row exists, the strategy returns {@code null}.</li>
 * </ol>
 * The strategy resolves the parent identifier through {@link ExpressionExperimentService} for
 * non-{@code SecuredChild.getSecurityOwner()} domain objects; we mock the service to return a
 * known identifier so the test focuses on the ACL lookup itself.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ParentIdentityRetrievalStrategyTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ParentIdentityRetrievalStrategyTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy() {
            return new ParentIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }
    }

    @Autowired
    private ParentIdentityRetrievalStrategy strategy;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DataSource dataSource;

    /**
     * Seed an acl_class + acl_object_identity row for ExpressionExperiment id=4242, then drive
     * the strategy with a synthetic BioAssay whose owning EE id is 4242. The strategy should
     * resolve the parent ObjectIdentity to that AOI.
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testGetParentIdentity_returnsExistingAoi() {
        long eeId = 4242L;
        JdbcTemplate jt = new JdbcTemplate( dataSource );

        Long classId = ensureAclClass( jt, ExpressionExperiment.class.getName() );
        // owner_sid=1 (GROUP_ADMIN) seeded by V3__seed_data.sql.
        jt.update(
                "insert into acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (?, ?, NULL, 1, 0)",
                classId, eeId );

        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 1L );
        when( expressionExperimentService.findIdByBioAssay( ba, true ) ).thenReturn( eeId );

        ObjectIdentity oid = strategy.getParentIdentity( ba );
        assertNotNull( oid, "Strategy must return an ObjectIdentity for a seeded AOI" );
        assertEquals( ExpressionExperiment.class.getName(), oid.getType() );
        assertEquals( eeId, oid.getIdentifier() );
        AclObjectIdentity aoi = ( AclObjectIdentity ) oid;
        assertNotNull( aoi.getId(), "AclObjectIdentity.id (acl_object_identity PK) must be populated" );
    }

    /**
     * If no acl_object_identity row exists for the resolved parent type+identifier, the strategy
     * must return null (matching the prior HQL uniqueResult() behavior).
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testGetParentIdentity_returnsNullWhenNoAoi() {
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 2L );
        // No seeded AOI for id=999999; expressionExperimentService returns the EE id, but there
        // is no AOI row for it.
        when( expressionExperimentService.findIdByBioAssay( ba, true ) ).thenReturn( 999999L );

        ObjectIdentity oid = strategy.getParentIdentity( ba );
        assertNull( oid, "Strategy must return null when no acl_object_identity row matches" );
    }

    /**
     * If the upstream lookup returns a null identifier (e.g. orphan BioMaterial), the strategy
     * short-circuits to null without hitting the ACL tables.
     */
    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void testGetParentIdentity_returnsNullWhenParentIdentifierIsNull() {
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 3L );
        when( expressionExperimentService.findIdByBioAssay( ba, true ) ).thenReturn( null );

        ObjectIdentity oid = strategy.getParentIdentity( ba );
        assertNull( oid, "Strategy must return null when the resolved parent identifier is null" );
    }

    private static Long ensureAclClass( JdbcTemplate jt, String className ) {
        List<Long> existing = jt.queryForList(
                "select id from acl_class where class = ?", Long.class, className );
        if ( !existing.isEmpty() ) {
            return existing.get( 0 );
        }
        jt.update( "insert into acl_class (class) values (?)", className );
        return jt.queryForObject(
                "select id from acl_class where class = ?", Long.class, className );
    }
}
