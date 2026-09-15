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
import org.springframework.security.acls.model.AclService;
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
 * Fast unit tests for {@link AclEntryAfterInvocationByAssociationCollectionFilteringProvider}.
 * Filters a collection of Securables by checking each entry's ACL via an associated parent
 * (e.g. CompositeSequences filtered by the ArrayDesign they belong to).
 */
public class AclEntryAfterInvocationByAssociationCollectionFilteringProviderTest {

    private static final String USER = "alice";
    private static final String CONFIG_ATTR = "AFTER_ACL_BY_ASSOCIATION_COLLECTION_READ";

    private InMemoryAclService aclService;
    private ChildCollectionToParentByAssociation provider;
    private Authentication auth;
    private Collection<ConfigAttribute> config;

    @BeforeEach
    public void setUp() {
        aclService = new InMemoryAclService();
        provider = new ChildCollectionToParentByAssociation( aclService, CONFIG_ATTR,
                Collections.singletonList( BasePermission.READ ) );
        provider.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        provider.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        provider.setProcessDomainObjectClass( Child.class );

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
    public void filtersChildrenByParentPermission() {
        Parent allowedParent = new Parent( 1L );
        Parent deniedParent = new Parent( 2L );
        aclService.grantToPrincipal( allowedParent, USER, BasePermission.READ );
        aclService.ensureAcl( deniedParent );

        Child kept = new Child( 100L, allowedParent );
        Child dropped = new Child( 200L, deniedParent );
        List<Child> input = new ArrayList<>( Arrays.asList( kept, dropped ) );

        Object result = provider.decide( auth, null, config, input );

        assertThat( result ).isSameAs( input );
        assertThat( input ).containsExactly( kept );
    }

    @Test
    public void emptyCollection_passesThrough() {
        List<Child> input = new ArrayList<>();
        Object result = provider.decide( auth, null, config, input );
        assertThat( result ).isSameAs( input );
    }

    @Test
    public void unsupportedConfigAttribute_returnsInputUnchanged() {
        Collection<ConfigAttribute> otherConfig = Collections.singletonList( new SecurityConfig( "OTHER" ) );
        Parent p = new Parent( 1L );
        Child c = new Child( 100L, p );
        aclService.grantToPrincipal( p, USER, BasePermission.READ );
        List<Child> input = new ArrayList<>( Collections.singletonList( c ) );
        Object result = provider.decide( auth, null, otherConfig, input );
        assertThat( result ).isSameAs( input );
        assertThat( input ).hasSize( 1 );
    }

    /**
     * Test-only subclass: maps each Child → its Parent for the ACL check.
     */
    public static class ChildCollectionToParentByAssociation extends AclEntryAfterInvocationByAssociationCollectionFilteringProvider {
        public ChildCollectionToParentByAssociation( AclService aclService, String processConfigAttribute,
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
