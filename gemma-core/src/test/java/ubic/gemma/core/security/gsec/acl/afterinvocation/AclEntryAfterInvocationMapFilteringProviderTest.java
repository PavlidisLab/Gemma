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
 * Fast unit tests for {@link AclEntryAfterInvocationMapFilteringProvider}, which filters a
 * {@link Map}'s key set in place (keys are Securable; values may or may not be Securable —
 * value security isn't checked by this provider, only key security).
 * <p>
 * The provider mutates the input map's key set via {@code map.keySet()} → super.decide on the
 * keySet view, which removes denied entries directly from the map.
 */
public class AclEntryAfterInvocationMapFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_MAP_READ";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationMapFilteringProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationMapFilteringProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
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
    public void filtersMapKeys_keepingOnlyGrantedSecurables() {
        Item granted = new Item( 1L );
        Item denied = new Item( 2L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );
        aclService.ensureAcl( denied );

        Map<Item, String> input = new LinkedHashMap<>();
        input.put( granted, "value-1" );
        input.put( denied, "value-2" );

        Object result = provider.decide( auth, null, config, input );

        assertThat( result ).isSameAs( input );
        // The provider mutates the input map's keySet to drop denied keys.
        assertThat( input ).containsOnlyKeys( granted );
    }

    @Test
    public void emptyMap_passesThroughEmpty() {
        Map<Item, String> input = new LinkedHashMap<>();
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
        Map<Item, String> input = new LinkedHashMap<>();
        input.put( new Item( 1L ), "v" );
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
