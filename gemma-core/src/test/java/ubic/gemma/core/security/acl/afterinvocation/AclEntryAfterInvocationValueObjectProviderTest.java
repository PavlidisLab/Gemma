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
import ubic.gemma.core.security.model.SecureValueObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fast unit tests for {@link AclEntryAfterInvocationValueObjectProvider}: single value-object
 * security check that returns null (instead of throwing) when access is denied AND populates
 * security-status fields on the VO when granted.
 */
public class AclEntryAfterInvocationValueObjectProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_VALUE_OBJECT_READ";

    private InMemoryAclService aclService;
    private AclEntryAfterInvocationValueObjectProvider provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @BeforeEach
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new AclEntryAfterInvocationValueObjectProvider(
                aclService, Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( ItemVO.class );

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
    public void grantedVo_returnsTheVo_andPopulatesSecurityStatus() {
        ItemVO vo = new ItemVO( 1L );
        aclService.grantToPrincipal( vo, USER, BasePermission.READ );

        Object result = provider.decide( auth, null, config, vo );

        assertThat( result ).isSameAs( vo );
        // Populated as a side effect since the user is logged in.
        assertThat( vo.getIsPublic() ).isFalse(); // no anonymous READ
        assertThat( vo.getIsShared() ).isFalse();
        assertThat( vo.getUserOwned() ).isTrue(); // sid was alice when ACL was created
    }

    @Test
    public void voWithoutAcl_throwsAccessDeniedException() {
        // The provider's javadoc claims it returns null on denial; in practice it doesn't
        // override decide(), so the base AclEntryAfterInvocationProvider's
        // throw-AccessDeniedException-on-denial behaviour wins. Document actual behaviour.
        ItemVO vo = new ItemVO( 1L );
        assertThatThrownBy( () -> provider.decide( auth, null, config, vo ) )
                .isInstanceOf( AccessDeniedException.class );
    }

    @Test
    public void nonVoSecurable_isPassedThroughIfPermissionGranted() {
        // The provider sets process-domain-object-class to ItemVO; non-VO instances should
        // bypass the AclProvider's decide entirely (returning the input unchanged).
        Object input = "not an Item";
        Object result = provider.decide( auth, null, config, input );
        assertThat( result ).isSameAs( input );
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        ItemVO vo = new ItemVO( 1L );
        aclService.grantToPrincipal( vo, USER, BasePermission.READ );
        Object result = provider.decide( auth, null, otherConfig, vo );
        assertThat( result ).isSameAs( vo );
    }

    /**
     * Minimal SecureValueObject with mutable security flags so we can assert population.
     */
    public static class ItemVO implements SecureValueObject {
        private final Long id;
        private boolean isPublic;
        private boolean isShared;
        private boolean userOwned;
        private boolean userCanWrite;

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
