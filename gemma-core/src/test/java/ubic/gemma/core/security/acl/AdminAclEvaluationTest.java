/*
 * The gemma-core project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.model.Securable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproducer for the long-standing Phase-3 ACL evaluation bug surfaced by
 * {@code UserGroupServiceTest#testUpdateUserGroup}: when the admin test user
 * (authority {@code GROUP_ADMIN}) calls a method protected by
 * {@code @PreAuthorize("hasPermission(#group, 'administration')")} on a
 * UserGroup whose ACL was created by {@link BaseAclAdvice#setupBaseAces} —
 * which inserts a {@code GrantedAuthoritySid("GROUP_ADMIN") ADMINISTRATION
 * granting=true} ACE — the evaluation returns {@code false} even though by
 * inspection it should return {@code true}.
 * <p>
 * The {@code hasAuthority('GROUP_ADMIN') or ...} disjunct in gsec
 * {@code UserService.removeUserFromGroup} is the workaround we want to drop;
 * this test is the gating fixture for that work.
 * <p>
 * Runs on the same in-memory ACL fixture as {@link AclEventListenerTest}
 * (no Hibernate, no JDBC, no MySQL), wired with the same Spring Security
 * collaborators production uses: {@link AclPermissionEvaluator}, gsec's
 * {@link AclSidRetrievalStrategyImpl} (role-hierarchy aware), and a real
 * {@link RoleHierarchyImpl} carrying Gemma's
 * {@code GROUP_ADMIN > GROUP_USER > IS_AUTHENTICATED_ANONYMOUSLY} hierarchy.
 */
public class AdminAclEvaluationTest {

    private InMemoryAclService aclService;
    private AclPermissionEvaluator evaluator;

    @BeforeEach
    public void setUp() {
        aclService = new InMemoryAclService();
        RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy(
                "GROUP_ADMIN > GROUP_USER\n" +
                        "GROUP_RUN_AS_ADMIN > GROUP_ADMIN\n" +
                        "GROUP_USER > IS_AUTHENTICATED_ANONYMOUSLY\n" +
                        "GROUP_RUN_AS_USER > GROUP_USER\n" +
                        "GROUP_ADMIN > GROUP_CURATOR\n" +
                        "GROUP_CURATOR > GROUP_USER\n" +
                        "GROUP_RUN_AS_CURATOR > GROUP_CURATOR\n" +
                        "GROUP_ADMIN > GROUP_AGENT\n" +
                        "GROUP_AGENT > IS_AUTHENTICATED_ANONYMOUSLY\n" +
                        "GROUP_RUN_AS_AGENT > GROUP_AGENT" );
        SidRetrievalStrategy sids = new AclSidRetrievalStrategyImpl( hierarchy );
        evaluator = new AclPermissionEvaluator( aclService );
        evaluator.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        evaluator.setSidRetrievalStrategy( sids );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sanity check: the role-hierarchy-aware sid retrieval really does emit a
     * {@code GrantedAuthoritySid("GROUP_ADMIN")} for an admin user, so the ACE
     * sid and the user sid should match.
     */
    @Test
    public void sidRetrievalForAdmin_includesGroupAdminAuthoritySid() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "administrator", "x", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );

        RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy( "GROUP_ADMIN > GROUP_USER" );
        SidRetrievalStrategy sidStrategy = new AclSidRetrievalStrategyImpl( hierarchy );
        List<Sid> sids = sidStrategy.getSids( auth );

        assertThat( sids ).anyMatch( s ->
                s instanceof GrantedAuthoritySid
                        && AuthorityConstants.ADMIN_GROUP_AUTHORITY
                        .equals( ( ( GrantedAuthoritySid ) s ).getGrantedAuthority() ) );
        assertThat( sids ).anyMatch( s ->
                s instanceof PrincipalSid
                        && "administrator".equals( ( ( PrincipalSid ) s ).getPrincipal() ) );
    }

    /**
     * The headline reproducer. ACL set up exactly as {@code BaseAclAdvice#setupBaseAces}
     * would (with the UserGroup-specific extra READ-by-own-group ACE), then ask
     * {@code AclPermissionEvaluator.hasPermission(admin, group, "administration")}.
     */
    @Test
    public void adminHasAdministrationOnUserGroupAcl() {
        FakeUserGroup group = new FakeUserGroup( 42L );
        ObjectIdentity oid = new ObjectIdentityImpl( group.getClass().getName(), group.getId() );

        // Mimic setupBaseAces + the UserGroup-specific extra READ ACE granted to the
        // group's own authority (see BaseAclAdvice line 340).
        MutableAcl acl = aclService.createAcl( oid );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ),
                true );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.AGENT_GROUP_AUTHORITY ) ),
                true );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( "GROUP_TESTING" ) ),
                true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken admin = new TestingAuthenticationToken(
                "administrator", "x", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        admin.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( admin );

        assertThat( evaluator.hasPermission( admin, group, "administration" ) )
                .as( "admin with GROUP_ADMIN authority should match the GROUP_ADMIN ADMINISTRATION ACE" )
                .isTrue();
    }

    /**
     * Same setup, but ask for WRITE. Spring's {@link BasePermission#WRITE} (mask=2) is NOT
     * the same as {@link BasePermission#ADMINISTRATION} (mask=16), so an
     * ADMINISTRATION-only ACE should NOT satisfy a WRITE permission check. This test exists
     * to pin down the contract — if the {@code hasPermission(group, 'write')} disjunct in
     * the SpEL is ever to do useful work for admin, it has to come from a different ACE.
     */
    @Test
    public void adminLacksWriteWhenOnlyAdministrationAceExists() {
        FakeUserGroup group = new FakeUserGroup( 42L );
        ObjectIdentity oid = new ObjectIdentityImpl( group.getClass().getName(), group.getId() );
        MutableAcl acl = aclService.createAcl( oid );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ),
                true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken admin = new TestingAuthenticationToken(
                "administrator", "x", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        admin.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( admin );

        // WRITE != ADMINISTRATION — this should legitimately be false.
        assertThat( evaluator.hasPermission( admin, group, "write" ) ).isFalse();
    }

    /**
     * A curator holds ADMINISTRATION through the ace {@code setupBaseAces} now grants
     * GROUP_CURATOR, which is what lets every ACL_SECURABLE_EDIT check pass for them.
     * <p>
     * ADMINISTRATION rather than WRITE is deliberate and this is where it shows: it is also what
     * {@code AclAuthorizationStrategyImpl} falls back to when the caller lacks the configured
     * authority, so the same ace covers editing a design AND changing a dataset's visibility. A
     * WRITE ace would satisfy the edit check and then fail on makePublic.
     */
    @Test
    public void curatorHasAdministrationOnAnObjectFromSetupBaseAces() {
        FakeUserGroup obj = new FakeUserGroup( 43L );
        MutableAcl acl = aclService.createAcl( new ObjectIdentityImpl( obj.getClass().getName(), obj.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ), true );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.CURATOR_GROUP_AUTHORITY ) ), true );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.AGENT_GROUP_AUTHORITY ) ), true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken curator = new TestingAuthenticationToken(
                "someCurator", "x", AuthorityConstants.CURATOR_GROUP_AUTHORITY );
        curator.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( curator );

        assertThat( evaluator.hasPermission( curator, obj, "administration" ) ).isTrue();

        // 🛑 NOT hasPermission(..., "read"). An ADMINISTRATION ace does not satisfy a mask check for
        // READ -- the same contract adminLacksWriteWhenOnlyAdministrationAceExists pins for WRITE.
        // Production reads go through the ACL_SECURABLE_READ *voter*, which is configured to accept
        // ADMINISTRATION *or* READ (applicationContext-gsec.xml), so the curator does read; it is the
        // SpEL hasPermission path that is mask-exact. Admins have always behaved identically here,
        // so this is a property of the ace, not something the curator grant changed.
        assertThat( evaluator.hasPermission( curator, obj, "read" ) )
                .as( "mask-exact SpEL check: ADMINISTRATION is not READ, for a curator as for an admin" )
                .isFalse();
    }

    /**
     * 🛑 The rule the whole widening rests on: an ADMINISTRATOR satisfies a GROUP_CURATOR ace
     * through the hierarchy. 61 routes moved from {@code hasAuthority('GROUP_ADMIN')} to
     * {@code hasAuthority('GROUP_CURATOR')}, and if this direction did not hold, every one of them
     * would have locked administrators out of work they could do the day before.
     */
    @Test
    public void anAdministratorSatisfiesACuratorAce() {
        FakeUserGroup obj = new FakeUserGroup( 44L );
        MutableAcl acl = aclService.createAcl( new ObjectIdentityImpl( obj.getClass().getName(), obj.getId() ) );
        // ONLY the curator ace -- no GROUP_ADMIN ace to fall back on.
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.CURATOR_GROUP_AUTHORITY ) ), true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken admin = new TestingAuthenticationToken(
                "administrator", "x", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        admin.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( admin );

        assertThat( evaluator.hasPermission( admin, obj, "administration" ) )
                .as( "GROUP_ADMIN > GROUP_CURATOR, so an admin presents the curator sid too" )
                .isTrue();
    }

    /**
     * 🛑 And it does NOT run the other way. A curator must never satisfy an admin-only ace, or the
     * user-account and server-operation routes that kept {@code hasAuthority('GROUP_ADMIN')} would
     * be open to them — which is the entire distinction between the two groups.
     */
    @Test
    public void aCuratorDoesNotSatisfyAnAdminOnlyAce() {
        FakeUserGroup obj = new FakeUserGroup( 45L );
        MutableAcl acl = aclService.createAcl( new ObjectIdentityImpl( obj.getClass().getName(), obj.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ), true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken curator = new TestingAuthenticationToken(
                "someCurator", "x", AuthorityConstants.CURATOR_GROUP_AUTHORITY );
        curator.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( curator );

        assertThat( evaluator.hasPermission( curator, obj, "administration" ) ).isFalse();
    }

    /** A plain user is unaffected by the curator ace -- the widening gave nothing to GROUP_USER. */
    @Test
    public void aPlainUserDoesNotSatisfyACuratorAce() {
        FakeUserGroup obj = new FakeUserGroup( 46L );
        MutableAcl acl = aclService.createAcl( new ObjectIdentityImpl( obj.getClass().getName(), obj.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.CURATOR_GROUP_AUTHORITY ) ), true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken user = new TestingAuthenticationToken(
                "someUser", "x", AuthorityConstants.USER_GROUP_AUTHORITY );
        user.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( user );

        assertThat( evaluator.hasPermission( user, obj, "administration" ) ).isFalse();
    }

    /**
     * 🛑 A curator has NO ace on a User or UserGroup, so they cannot reach
     * {@code BaseUserService.removeUserFromGroup} — whose gate is
     * {@code hasPermission(#group, 'write') or hasPermission(#group, 'administration')}.
     * <p>
     * Without the exclusion in {@code setupBaseAces}, a curator satisfies that check and can move
     * anyone between groups, themselves into Administrators included — which is precisely the
     * authority the group was defined not to have. This is not hypothetical: the first production
     * backfill granted over every acl_object_identity and 668 aces (661 users plus the groups) had
     * to be deleted back out an hour later.
     * <p>
     * The ace list here is what setupBaseAces writes for a user-group object: admin and agent, no
     * curator.
     */
    @Test
    public void aCuratorHasNoAuthorityOverAUserGroup() {
        FakeUserGroup group = new FakeUserGroup( 47L );
        MutableAcl acl = aclService.createAcl( new ObjectIdentityImpl( group.getClass().getName(), group.getId() ) );
        acl.insertAce( acl.getEntries().size(), BasePermission.ADMINISTRATION,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ), true );
        acl.insertAce( acl.getEntries().size(), BasePermission.READ,
                new GrantedAuthoritySid( new SimpleGrantedAuthority( AuthorityConstants.AGENT_GROUP_AUTHORITY ) ), true );
        aclService.updateAcl( acl );

        TestingAuthenticationToken curator = new TestingAuthenticationToken(
                "someCurator", "x", AuthorityConstants.CURATOR_GROUP_AUTHORITY );
        curator.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( curator );

        assertThat( evaluator.hasPermission( curator, group, "administration" ) )
                .as( "a curator must not be able to administer a user group" ).isFalse();
        assertThat( evaluator.hasPermission( curator, group, "write" ) )
                .as( "nor write it — removeUserFromGroup accepts either" ).isFalse();

        // Control: an administrator still can, so the exclusion took nothing from the group that
        // is supposed to manage users.
        TestingAuthenticationToken admin = new TestingAuthenticationToken(
                "administrator", "x", AuthorityConstants.ADMIN_GROUP_AUTHORITY );
        admin.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( admin );
        assertThat( evaluator.hasPermission( admin, group, "administration" ) ).isTrue();
    }

    /**
     * Minimal {@link Securable} stand-in — only needs a class name and id for the
     * {@link ObjectIdentityImpl} key.
     */
    public static class FakeUserGroup implements Securable {
        private static final long serialVersionUID = 1L;
        private final Long id;

        public FakeUserGroup( Long id ) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }
}
