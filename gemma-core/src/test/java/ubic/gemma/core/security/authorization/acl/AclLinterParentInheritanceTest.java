package ubic.gemma.core.security.authorization.acl;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.acl.GemmaAclConfiguration;
import ubic.gemma.core.security.acl.ObjectIdentityRetrievalStrategyImpl;
import ubic.gemma.core.security.acl.domain.AclDao;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Coverage for the {@code entries_inheriting} half of
 * {@code AclLinterServiceImpl.setParentAcl}.
 * <p>
 * This test wires the ACL stack the way production does — {@link org.springframework.security.acls.jdbc.JdbcMutableAclService}
 * over the canonical four tables — instead of the Hibernate {@code AclServiceImpl} /
 * {@code AclDaoImpl} pair that {@link BaseDatabaseTest5} defaults to. That is not a preference:
 * {@code AclObjectIdentity} is {@code @Entity} and {@code @Immutable}, so on the Hibernate path
 * Hibernate discards the dirty state and NO ACL update reaches the database at all. A test on
 * that path can neither confirm nor refute anything about what {@code setParentAcl} writes — it
 * would pass whatever the linter did. Overriding the bean is what makes the assertions mean
 * something.
 *
 * @see AclLinterServiceTest for the rest of the linter's coverage, which does not depend on an
 * ACL write landing.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AclLinterParentInheritanceTest extends BaseDatabaseTest5 {

    private static final String EE_CLASS = ExpressionExperiment.class.getName();
    private static final String ED_CLASS = ExperimentalDesign.class.getName();

    @Configuration
    @TestComponent
    static class AclLinterParentInheritanceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        /**
         * Replaces the base class's Hibernate-backed ACL service with the JDBC one production
         * runs. {@code aclDao} is ignored rather than removed: the base configuration still
         * defines it, and the override has to keep the inherited signature.
         */
        @Override
        @Bean
        public AclService aclService( AclDao aclDao ) {
            AclAuthorizationStrategy authorizationStrategy = new AclAuthorizationStrategyImpl(
                    new GrantedAuthority[] { new SimpleGrantedAuthority( "GROUP_ADMIN" ),
                            new SimpleGrantedAuthority( "GROUP_ADMIN" ),
                            new SimpleGrantedAuthority( "GROUP_ADMIN" ) } );
            PermissionGrantingStrategy grantingStrategy =
                    new DefaultPermissionGrantingStrategy( new ConsoleAuditLogger() );
            AclCache cache = new SpringCacheBasedAclCache(
                    new ConcurrentMapCache( "aclJdbc" ), grantingStrategy, authorizationStrategy );
            LookupStrategy lookupStrategy = new BasicLookupStrategy(
                    dataSource(), cache, authorizationStrategy, grantingStrategy );
            return new GemmaAclConfiguration().aclService( dataSource(), lookupStrategy, cache );
        }

        @Bean
        public AclLinterService aclLinterService() {
            return new AclLinterServiceImpl();
        }

        @Bean
        public ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy() {
            return new ObjectIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy() {
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

        @Bean
        public ubic.gemma.core.security.acl.BaseAclAdvice aclAdvice( AclService aclService,
                SessionFactory sessionFactory,
                ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy ) {
            return new AclAdvice( aclService, sessionFactory, objectIdentityRetrievalStrategy );
        }
    }

    @Autowired
    private AclLinterService aclLinterService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DataSource dataSource;

    /**
     * A child whose identity has {@code entries_inheriting} off gains nothing from a parent link —
     * Spring only walks to the parent when the flag is on — so the linter has to set both. The
     * flag is off on exactly the rows this repair targets: {@code BaseAclAdvice} sets it from
     * {@code inheritFromParent}, false when no parent was discoverable at insert time, which is
     * the same branch that leaves the child holding its own ACEs. On production that shape is
     * 631,709 BioAssay and 631,709 BioMaterial identities, each with the GROUP_ADMIN +
     * GROUP_AGENT pair and no parent.
     */
    @Test
    @WithMockUser(username = "administrator", authorities = { "GROUP_ADMIN" })
    public void testLinkingAParentTurnsOnEntriesInheriting() {
        JdbcTemplate jt = new JdbcTemplate( dataSource );

        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        sessionFactory.getCurrentSession().persist( ed );
        sessionFactory.getCurrentSession().flush();

        long eeAoiId = seedIdentity( jt, EE_CLASS, ee.getId(), null, true );
        // GROUP_ADMIN administration + GROUP_AGENT read on the parent, so it is a plausible one
        // to inherit from.
        jt.update( "insert into acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) values (?, 0, 1, 16, 1, 0, 0)", eeAoiId );
        jt.update( "insert into acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) values (?, 1, 3, 1, 1, 0, 0)", eeAoiId );

        // The child: no parent, inheriting OFF, holding the same two ACEs of its own.
        long edAoiId = seedIdentity( jt, ED_CLASS, ed.getId(), null, false );
        jt.update( "insert into acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) values (?, 0, 1, 16, 1, 0, 0)", edAoiId );
        jt.update( "insert into acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) values (?, 1, 3, 1, 1, 0, 0)", edAoiId );

        when( expressionExperimentService.findIdByDesign( any() ) ).thenReturn( ee.getId() );

        AclLinterConfig config = AclLinterConfig.builder()
                .lintChildWithoutParent( true )
                .applyFixes( true )
                .build();
        aclLinterService.lintAcls( ExperimentalDesign.class, ed.getId(), config );

        Long parent = jt.queryForObject(
                "select parent_object from acl_object_identity where id = ?", Long.class, edAoiId );
        assertEquals( eeAoiId, parent, "The linter did not write the parent link." );
        Integer inheriting = jt.queryForObject(
                "select entries_inheriting from acl_object_identity where id = ?", Integer.class, edAoiId );
        assertEquals( 1, inheriting,
                "The parent link was written with entries_inheriting off, so Spring never walks to it and the repair grants nothing." );
    }

    private long seedIdentity( JdbcTemplate jt, String className, Long identifier,
            @org.springframework.lang.Nullable Long parentAoiId, boolean entriesInheriting ) {
        Long classId = jt.query( "select id from acl_class where class = ?",
                rs -> rs.next() ? rs.getLong( 1 ) : null, className );
        if ( classId == null ) {
            jt.update( "insert into acl_class (class) values (?)", className );
            classId = jt.queryForObject( "select id from acl_class where class = ?", Long.class, className );
        }
        // owner_sid=1 (GROUP_ADMIN), seeded by V3__seed_data.sql.
        jt.update( "insert into acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) values (?, ?, ?, 1, ?)",
                classId, identifier, parentAoiId, entriesInheriting ? 1 : 0 );
        return jt.queryForObject(
                "select id from acl_object_identity where object_id_class = ? and object_id_identity = ?",
                Long.class, classId, identifier );
    }
}
