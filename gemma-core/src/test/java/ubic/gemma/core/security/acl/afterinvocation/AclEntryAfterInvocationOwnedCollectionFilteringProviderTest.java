/*
 * The gemma-core project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.acl.afterinvocation;

import ubic.gemma.core.security.acl.InMemoryAclService;
import ubic.gemma.core.security.model.Securable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast unit tests for {@link AclEntryAfterInvocationOwnedCollectionFilteringProvider}.
 * <p>
 * The "Owned" filter is for the "my data" UI surface: keep collection entries the current user
 * <em>owns</em> AND has the required permission on; drop entries owned by anyone else (even if
 * readable). Admins see everything. Both the owner-check and the permission-check have to pass.
 */
public class AclEntryAfterInvocationOwnedCollectionFilteringProviderTest {

    private static final String USER = "alice";
    private static final String OTHER = "bob";
    private static final String CONFIG_ATTR = "AFTER_ACL_FILTER_MY_DATA";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationOwnedCollectionFilteringProvider provider;
    private Collection<ConfigAttribute> config;

    @BeforeEach
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationOwnedCollectionFilteringProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Item.class );

        authenticateAs( USER );
        config = Collections.singletonList( new SecurityConfig( CONFIG_ATTR ) );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void keepsItemsOwnedByCurrentUser_dropsItemsOwnedByOthers() {
        // Both items are readable; only the one alice owns survives the filter.
        Item ownedByAlice = grantReadAs( USER, new Item( 1L ) );
        Item ownedByBob = grantReadAs( OTHER, new Item( 2L ) );
        // Make bob's item also readable to alice — owned-filter should still drop it.
        aclService.grantToPrincipal( ownedByBob, USER, BasePermission.READ );

        Object result = provider.decide( authentication(), null, config, listOf( ownedByAlice, ownedByBob ) );

        @SuppressWarnings("unchecked") Collection<Object> kept = ( Collection<Object> ) result; assertThat( kept ).containsExactlyElementsOf(
                Collections.singletonList( ownedByAlice ) );
    }

    @Test
    public void dropsItemsCurrentUserOwnsButLacksRequiredPermissionOn() {
        // alice owns the item (auth is already alice in setUp) but has no READ ACE — the
        // and-permission check fails, so the owned-filter drops it anyway.
        Item owned = new Item( 1L );
        aclService.ensureAcl( owned );

        Object result = provider.decide( authentication(), null, config, listOf( owned ) );

        assertThat( ( Collection<?> ) result ).isEmpty();
    }

    @Test
    public void adminUserKeepsEverythingTheyHavePermissionOn_evenIfNotTheOwner() {
        // admin authority bypasses the owner check (per ownedByCurrentUser short-circuit on admin).
        SecurityContextHolder.clearContext();
        TestingAuthenticationToken adminAuth = new TestingAuthenticationToken( "admin", "x", "GROUP_ADMIN" );
        adminAuth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( adminAuth );

        Item ownedByOther = grantReadAs( OTHER, new Item( 1L ) );
        // grant admin READ too
        aclService.grantToPrincipal( ownedByOther, "admin", BasePermission.READ );

        Object result = provider.decide( adminAuth, null, config, listOf( ownedByOther ) );

        @SuppressWarnings("unchecked") Collection<Object> kept = ( Collection<Object> ) result; assertThat( kept ).containsExactlyElementsOf(
                Collections.singletonList( ownedByOther ) );
    }

    @Test
    public void emptyCollection_passesThrough() {
        Object result = provider.decide( authentication(), null, config, new ArrayList<>() );
        assertThat( ( Collection<?> ) result ).isEmpty();
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        Item granted = grantReadAs( USER, new Item( 1L ) );
        List<Item> input = listOf( granted );
        Object result = provider.decide( authentication(), null, otherConfig, input );
        assertThat( result ).isSameAs( input );
    }

    // ---- helpers ---------------------------------------------------------------------------

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void authenticateAs( String principal ) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken( principal, "x" );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );
    }

    /**
     * Create an item, set the current authentication to {@code owner} so the in-memory ACL
     * service stamps {@code owner} as the AclImpl's owner sid, then restore the original auth.
     * Grants READ to {@code owner} so the filter's required-permission check also passes.
     */
    private Item grantReadAs( String owner, Item item ) {
        Authentication saved = authentication();
        authenticateAs( owner );
        try {
            aclService.grantToPrincipal( item, owner, BasePermission.READ );
        } finally {
            SecurityContextHolder.getContext().setAuthentication( saved );
        }
        return item;
    }

    @SafeVarargs
    private static <T> List<T> listOf( T... elements ) {
        return new ArrayList<>( Arrays.asList( elements ) );
    }

    public static class Item implements Securable {
        private final Long id;
        public Item( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }
}
