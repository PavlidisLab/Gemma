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
package gemma.gsec.acl;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.model.Securable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy(
                "GROUP_ADMIN > GROUP_USER\n" +
                        "GROUP_RUN_AS_ADMIN > GROUP_ADMIN\n" +
                        "GROUP_USER > IS_AUTHENTICATED_ANONYMOUSLY\n" +
                        "GROUP_RUN_AS_USER > GROUP_USER\n" +
                        "GROUP_ADMIN > GROUP_AGENT\n" +
                        "GROUP_AGENT > IS_AUTHENTICATED_ANONYMOUSLY\n" +
                        "GROUP_RUN_AS_AGENT > GROUP_AGENT" );
        SidRetrievalStrategy sids = new AclSidRetrievalStrategyImpl( hierarchy );
        evaluator = new AclPermissionEvaluator( aclService );
        evaluator.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        evaluator.setSidRetrievalStrategy( sids );
    }

    @After
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
