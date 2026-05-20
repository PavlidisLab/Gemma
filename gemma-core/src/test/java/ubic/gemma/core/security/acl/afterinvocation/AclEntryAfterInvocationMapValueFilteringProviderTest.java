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
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fast unit tests for {@link AclEntryAfterInvocationMapValueFilteringProvider}: filters a
 * {@link Map}'s value set (values are Securable; keys are not). The provider mutates the
 * input map by removing entries whose values fail the ACL check.
 */
public class AclEntryAfterInvocationMapValueFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_MAP_VALUES_READ";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationMapValueFilteringProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @BeforeEach
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationMapValueFilteringProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Item.class );

        auth = new TestingAuthenticationToken( USER, "x" );
        auth.setAuthenticated( true );
        SecurityContextHolder.getContext().setAuthentication( auth );

        config = Collections.singletonList( new SecurityConfig( CONFIG_ATTR ) );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void filtersMapValues_keepingOnlyGrantedEntries() {
        Item granted = new Item( 1L );
        Item denied = new Item( 2L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );
        aclService.ensureAcl( denied );

        Map<String, Item> input = new LinkedHashMap<>();
        input.put( "key-a", granted );
        input.put( "key-b", denied );

        Object result = provider.decide( auth, null, config, input );

        assertThat( result ).isSameAs( input );
        assertThat( input ).containsOnlyKeys( "key-a" );
        assertThat( input.get( "key-a" ) ).isSameAs( granted );
    }

    @Test
    public void emptyMap_passesThroughEmpty() {
        Map<String, Item> input = new LinkedHashMap<>();
        Object result = provider.decide( auth, null, config, input );
        assertThat( result ).isSameAs( input );
        assertThat( input ).isEmpty();
    }

    @Test
    public void nonMapReturnObject_throwsAuthorizationServiceException() {
        assertThatThrownBy( () -> provider.decide( auth, null, config, Collections.singletonList( new Item( 1L ) ) ) )
                .isInstanceOf( AuthorizationServiceException.class );
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        Map<String, Item> input = new LinkedHashMap<>();
        input.put( "k", new Item( 1L ) );
        Object result = provider.decide( auth, null, otherConfig, input );
        assertThat( result ).isSameAs( input );
        assertThat( input ).hasSize( 1 );
    }

    public static class Item implements Securable {
        private final Long id;
        public Item( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }
}
