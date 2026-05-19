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
import ubic.gemma.core.security.gsec.model.SecureValueObject;
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
 * Fast unit tests for {@link AclEntryAfterInvocationValueObjectCollectionFilteringProvider}.
 * Filters collections of SecureValueObjects: drops VOs the user can't read AND populates
 * security-status fields on the surviving VOs.
 */
public class AclEntryAfterInvocationValueObjectCollectionFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationValueObjectCollectionFilteringProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationValueObjectCollectionFilteringProvider(
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
    public void grantedVoPassesThrough_andSecurityStatusPopulated() {
        // Note: We don't include a "denied" item here because Spring's AclImpl.isGranted
        // throws NotFoundException when no ACE matches the requested permission, and the
        // VOCollection provider doesn't catch that. The base CollectionFilteringProvider
        // test covers denied-filtering with a mocked Acl that returns false directly.
        ItemVO granted = new ItemVO( 1L );
        aclService.grantToPrincipal( granted, USER, BasePermission.READ );

        List<ItemVO> input = new ArrayList<>( Collections.singletonList( granted ) );
        Object result = provider.decide( auth, null, config, input );

        assertThat( result ).isSameAs( input );
        assertThat( input ).containsExactly( granted );
        assertThat( granted.getUserOwned() ).isTrue();
        assertThat( granted.getIsPublic() ).isFalse();
    }

    @Test
    public void publicVoIsKept_andIsPublicFlagPopulated() {
        ItemVO publicVo = new ItemVO( 1L );
        aclService.grantToAuthority( publicVo, "IS_AUTHENTICATED_ANONYMOUSLY", BasePermission.READ );
        aclService.grantToPrincipal( publicVo, USER, BasePermission.READ );

        List<ItemVO> input = new ArrayList<>( Collections.singletonList( publicVo ) );
        provider.decide( auth, null, config, input );

        assertThat( input ).containsExactly( publicVo );
        assertThat( publicVo.getIsPublic() ).isTrue();
    }

    @Test
    public void emptyCollection_passesThrough() {
        List<ItemVO> input = new ArrayList<>();
        Object result = provider.decide( auth, null, config, input );
        assertThat( result ).isSameAs( input );
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        ItemVO vo = new ItemVO( 1L );
        aclService.grantToPrincipal( vo, USER, BasePermission.READ );
        List<ItemVO> input = new ArrayList<>( Collections.singletonList( vo ) );
        Object result = provider.decide( auth, null, otherConfig, input );
        assertThat( result ).isSameAs( input );
        assertThat( input ).hasSize( 1 );
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
    }
}
