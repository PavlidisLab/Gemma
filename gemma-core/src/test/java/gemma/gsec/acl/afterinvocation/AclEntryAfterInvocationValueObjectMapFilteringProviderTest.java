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
package gemma.gsec.acl.afterinvocation;

import gemma.gsec.acl.InMemoryAclService;
import gemma.gsec.model.Securable;
import gemma.gsec.model.SecureValueObject;
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
 * Fast unit tests for {@link AclEntryAfterInvocationValueObjectMapFilteringProvider}: Map whose
 * KEYS are SecureValueObjects (values are arbitrary).
 */
public class AclEntryAfterInvocationValueObjectMapFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_VALUE_OBJECT_MAP_READ";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationValueObjectMapFilteringProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationValueObjectMapFilteringProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( ItemVO.class );

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
    public void grantedKeyVoPassesThrough_andSecurityStatusPopulated() {
        // See VOCollection test for why we don't include a denied entry here.
        ItemVO granted = new ItemVO( 1L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );

        Map<ItemVO, String> input = new LinkedHashMap<>();
        input.put( granted, "v1" );

        Object result = provider.decide( auth, null, config, input );

        assertThat( result ).isSameAs( input );
        assertThat( input ).containsOnlyKeys( granted );
        assertThat( granted.getUserOwned() ).isTrue();
    }

    @Test
    public void emptyMap_passesThroughEmpty() {
        Map<ItemVO, String> input = new LinkedHashMap<>();
        Object result = provider.decide( auth, null, config, input );
        assertThat( result ).isSameAs( input );
    }

    @Test
    public void nonMapReturnObject_throwsAuthorizationServiceException() {
        assertThatThrownBy( () -> provider.decide( auth, null, config, Collections.singletonList( new ItemVO( 1L ) ) ) )
                .isInstanceOf( AuthorizationServiceException.class );
    }

    public static class ItemVO implements SecureValueObject {
        private final Long id;
        private boolean isPublic, isShared, userOwned, userCanWrite;

        public ItemVO( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
        @Override public boolean getIsPublic() { return isPublic; }
        @Override public boolean getIsShared() { return isShared; }
        @Override public Class<? extends Securable> getSecurableClass() { return ItemVO.class; }
        @Override public boolean getUserCanWrite() { return userCanWrite; }
        @Override public boolean getUserOwned() { return userOwned; }
        @Override public void setIsPublic( boolean p ) { this.isPublic = p; }
        @Override public void setIsShared( boolean s ) { this.isShared = s; }
        @Override public void setUserCanWrite( boolean w ) { this.userCanWrite = w; }
        @Override public void setUserOwned( boolean o ) { this.userOwned = o; }

        // Use identityHashCode/equality so the map can keep dropped keys without confusion.
        @Override public int hashCode() { return System.identityHashCode( this ); }
        @Override public boolean equals( Object o ) { return o == this; }
    }
}
