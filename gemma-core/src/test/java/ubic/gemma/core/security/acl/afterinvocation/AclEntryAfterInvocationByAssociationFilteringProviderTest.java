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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast unit tests for {@link AclEntryAfterInvocationByAssociationFilteringProvider}: the base
 * delegates the ACL check to a parent/associated Securable returned by
 * {@code getActualDomainObject}. Concrete subclass {@link ChildToParentByAssociation} is
 * defined here for test purposes (the production class is abstract).
 */
public class AclEntryAfterInvocationByAssociationFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_BY_ASSOCIATION_READ";

    private InMemoryAclService aclService;
    private ChildToParentByAssociation provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @Before
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new ChildToParentByAssociation( aclService, CONFIG_ATTR,
                Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Child.class );

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
    public void permissionOnAssociatedParent_lettsThroughChild() {
        Parent parent = new Parent( 1L );
        Child child = new Child( 100L, parent );
        aclService.grantToPrincipal( parent, USER, BasePermission.READ );

        Object result = provider.decide( auth, null, config, child );
        assertThat( result ).isSameAs( child );
    }

    @Test
    public void noPermissionOnParent_throwsAccessDenied() {
        Parent parent = new Parent( 1L );
        Child child = new Child( 100L, parent );
        aclService.ensureAcl( parent );

        // The base AclEntryAfterInvocationProvider throws AccessDeniedException on denial.
        // We accept any subtype of AccessDeniedException as a denial signal.
        try {
            Object result = provider.decide( auth, null, config, child );
            // Some configurations return null instead — also acceptable.
            assertThat( result ).isNull();
        } catch ( org.springframework.security.access.AccessDeniedException ok ) {
            // expected
        }
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        Parent parent = new Parent( 1L );
        Child child = new Child( 100L, parent );
        aclService.grantToPrincipal( parent, USER, BasePermission.READ );
        Object result = provider.decide( auth, null, otherConfig, child );
        assertThat( result ).isSameAs( child );
    }

    /**
     * Test-only concrete subclass that returns {@code Child.parent} as the associated
     * Securable. Mirrors how production code subclasses to define the association
     * (e.g. CompositeSequence → ArrayDesign).
     */
    public static class ChildToParentByAssociation extends AclEntryAfterInvocationByAssociationFilteringProvider {
        public ChildToParentByAssociation( AclService aclService, String processConfigAttribute,
                                            List<Permission> requirePermission ) {
            super( aclService, processConfigAttribute, requirePermission );
        }

        @Override
        protected Object getActualDomainObject( Object targetDomainObject ) {
            return ( ( Child ) targetDomainObject ).parent;
        }
    }

    public static class Parent implements Securable {
        private final Long id;
        public Parent( Long id ) { this.id = id; }
        @Override public Long getId() { return id; }
    }

    public static class Child implements Securable {
        private final Long id;
        public final Parent parent;
        public Child( Long id, Parent parent ) { this.id = id; this.parent = parent; }
        @Override public Long getId() { return id; }
    }
}
