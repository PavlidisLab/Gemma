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
package ubic.gemma.core.security.acl.afterinvocation;

import ubic.gemma.core.security.acl.InMemoryAclService;
import ubic.gemma.core.security.model.Securable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fast unit tests for {@link AclEntryAfterInvocationStreamFilteringProvider}, demonstrating the
 * {@link InMemoryAclService} fixture in an after-invocation-provider context.
 * <p>
 * Each test runs in under 10 ms — no Spring context, no database. Same fixture pattern as
 * {@link ubic.gemma.core.security.acl.AclEventListenerTest}; the {@code grantToPrincipal} /
 * {@code grantToAuthority} / {@code ensureAcl} helpers make per-test ACL setup terse.
 * <p>
 * Template for the other 9 untested after-invocation providers (Map, MapValue, Owned,
 * Private, ValueObject, ValueObjectMap, ValueObjectMapValue, ValueObjectCollection,
 * ByAssociation, ByAssociationCollection): same shape, swap the {@code decide} input/output
 * type (stream vs collection vs map) and the provider class under construction.
 */
public class AclEntryAfterInvocationStreamFilteringProviderTest {

    private static final String CONFIG_ATTR = "AFTER_ACL_STREAM_READ";
    private static final String USER = "alice";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationStreamFilteringProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        List<Permission> requiredPermissions = Collections.singletonList( BasePermission.READ );
        provider = new AclEntryAfterInvocationStreamFilteringProvider( aclService, requiredPermissions );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Item.class );

        auth = new TestingAuthenticationToken( USER, "x" );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );

        config = Collections.singletonList( new SecurityConfig( CONFIG_ATTR ) );
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void grantedItems_passThroughAndDeniedItemsAreFiltered() {
        Item granted = new Item( 1L );
        Item denied = new Item( 2L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );
        aclService.ensureAcl( denied ); // ACL exists, but no READ grant for USER

        Object result = provider.decide( auth, null, config, Stream.of( granted, denied ) );

        assertThat( result ).isInstanceOf( Stream.class );
        @SuppressWarnings("unchecked") List<Object> kept = ( ( Stream<Object> ) result ).collect( Collectors.toList() );
        assertThat( kept ).containsExactlyElementsOf( Collections.singletonList( granted ) );
    }

    @Test
    public void itemsWithoutAclAreFiltered() {
        Item known = new Item( 1L );
        Item unknown = new Item( 99L ); // never registered in ACL service
        aclService.grantToPrincipal( known, USER, BasePermission.READ );

        Object result = provider.decide( auth, null, config, Stream.of( known, unknown ) );
        @SuppressWarnings("unchecked") List<Object> kept = ( ( Stream<Object> ) result ).collect( Collectors.toList() );
        assertThat( kept ).containsExactlyElementsOf( Collections.singletonList( known ) );
    }

    @Test
    public void itemsOfNonDomainClassAreFiltered() {
        Item allowed = new Item( 1L );
        aclService.grantToPrincipal( allowed, USER, BasePermission.READ );
        // The non-Item is a String — does not implement the configured process-domain-object-class.

        Object result = provider.decide( auth, null, config, Stream.of( allowed, "not an Item" ) );
        @SuppressWarnings("unchecked") List<Object> kept = ( ( Stream<Object> ) result ).collect( Collectors.toList() );
        assertThat( kept ).containsExactlyElementsOf( Collections.singletonList( allowed ) );
    }

    @Test
    public void emptyStream_passesThroughEmpty() {
        Object result = provider.decide( auth, null, config, Stream.empty() );
        assertThat( ( ( Stream<?> ) result ).collect( Collectors.toList() ) ).isEmpty();
    }

    @Test
    public void nonStreamReturnObject_throwsAuthorizationServiceException() {
        Item granted = new Item( 1L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );

        assertThatThrownBy( () -> provider.decide( auth, null, config, Arrays.asList( granted ) ) )
                .isInstanceOf( AuthorizationServiceException.class );
    }

    @Test
    public void unsupportedConfigAttribute_passesObjectThrough() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "SOME_OTHER_ATTR" ) );
        Item granted = new Item( 1L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );

        Stream<Item> input = Stream.of( granted );
        Object result = provider.decide( auth, null, otherConfig, input );
        // Provider should return the input untouched when no config attribute matches.
        assertThat( result ).isSameAs( input );
    }

    @Test
    public void authorityGrantsAreHonored_inAdditionToPrincipalGrants() {
        // The provider's SidRetrievalStrategy expands Authentication into PrincipalSid +
        // GrantedAuthoritySids. A grant to one of the user's authorities should let the item
        // through even if there's no direct principal grant.
        Authentication withAuthority = new TestingAuthenticationToken( USER, "x", "ROLE_VIEWER" );
        withAuthority.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( withAuthority );

        Item item = new Item( 1L );
        aclService.grantToAuthority( item, "ROLE_VIEWER", BasePermission.READ );

        Object result = provider.decide( withAuthority, null, config, Stream.of( item ) );
        @SuppressWarnings("unchecked") List<Object> kept = ( ( Stream<Object> ) result ).collect( Collectors.toList() );
        assertThat( kept ).containsExactlyElementsOf( Collections.singletonList( item ) );
    }

    // Minimal test entity. Public + Securable so ObjectIdentityRetrievalStrategyImpl can
    // reflectively reach getId(), and so AclService can build an ObjectIdentity for it.
    public static class Item implements Securable {
        private final Long id;
        Item( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }
}
