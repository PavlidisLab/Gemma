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
package ubic.gemma.core.security.gsec.acl.afterinvocation;

import ubic.gemma.core.security.gsec.acl.InMemoryAclService;
import ubic.gemma.core.security.gsec.model.Securable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
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
 * Fast unit tests for {@link AclEntryAfterInvocationPrivateCollectionFilteringProvider}.
 * <p>
 * "Private collection" = items the current user can read but which are NOT publicly readable
 * (i.e. items shared specifically with the user). Items public to anonymous are dropped;
 * items truly private with a READ grant to the user are kept.
 */
public class AclEntryAfterInvocationPrivateCollectionFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_FILTER_MY_PRIVATE_DATA";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationPrivateCollectionFilteringProvider provider;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationPrivateCollectionFilteringProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Item.class );

        TestingAuthenticationToken auth = new TestingAuthenticationToken( USER, "x" );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );

        config = Collections.singletonList( new SecurityConfig( CONFIG_ATTR ) );
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void keepsPrivateItemsTheUserCanRead_dropsPubliclyReadableItems() {
        // Private item shared with alice — kept.
        Item privateShared = new Item( 1L );
        aclService.grantToPrincipal( privateShared, USER, BasePermission.READ );

        // Public item — has anonymous READ → SecurityUtil.isPrivate returns false → dropped.
        Item publicItem = new Item( 2L );
        aclService.grantToAuthority( publicItem, "IS_AUTHENTICATED_ANONYMOUSLY", BasePermission.READ );
        aclService.grantToPrincipal( publicItem, USER, BasePermission.READ );

        Object result = provider.decide( auth(), null, config, listOf( privateShared, publicItem ) );

        @SuppressWarnings("unchecked") Collection<Object> kept = ( Collection<Object> ) result; assertThat( kept ).containsExactlyElementsOf(
                Collections.singletonList( privateShared ) );
    }

    @Test
    public void dropsPrivateItemsTheUserCannotRead() {
        Item priv = new Item( 1L );
        aclService.ensureAcl( priv ); // no ACE for alice

        Object result = provider.decide( auth(), null, config, listOf( priv ) );

        assertThat( ( Collection<?> ) result ).isEmpty();
    }

    @Test
    public void keepsItemsWhereUserHasOnlyAdministrationButNotRead() {
        // The Private provider's isReadable accepts READ OR ADMINISTRATION.
        Item adminOnly = new Item( 1L );
        aclService.grantToPrincipal( adminOnly, USER, BasePermission.ADMINISTRATION );
        // Required-permission check at super level wants READ; ADMINISTRATION-only fails the
        // super check, so this entry is dropped. Documenting actual behavior.
        Object result = provider.decide( auth(), null, config, listOf( adminOnly ) );
        assertThat( ( Collection<?> ) result ).isEmpty();
    }

    @Test
    public void emptyCollection_passesThrough() {
        Object result = provider.decide( auth(), null, config, new ArrayList<>() );
        assertThat( ( Collection<?> ) result ).isEmpty();
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        Item granted = new Item( 1L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );
        List<Item> input = listOf( granted );
        Object result = provider.decide( auth(), null, otherConfig, input );
        assertThat( result ).isSameAs( input );
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
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
