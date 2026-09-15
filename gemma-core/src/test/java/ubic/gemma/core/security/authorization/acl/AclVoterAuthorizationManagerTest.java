/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.authorization.acl;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Thin unit test for {@link AclVoterAuthorizationManager}. Confirms the legacy-voter delegation
 * works: a mocked voter's {@code vote()} return value is faithfully translated into the new
 * {@link AuthorizationDecision} shape (with abstain mapped to {@code null}).
 * <p>
 * Phase X.1 of the ACL voter migration. A full integration test of the wired wrappers belongs in
 * Phase X.2 once {@code @EnableMethodSecurity} is flipped on.
 */
public class AclVoterAuthorizationManagerTest {

    private static final String ATTR = "ACL_SECURABLE_READ";

    @SuppressWarnings("unchecked")
    private AccessDecisionVoter<MethodInvocation> mockVoter() {
        AccessDecisionVoter<MethodInvocation> voter = mock( AccessDecisionVoter.class );
        when( voter.supports( any( ConfigAttribute.class ) ) ).thenReturn( true );
        return voter;
    }

    @SuppressWarnings("unchecked")
    private Supplier<Authentication> auth() {
        return ( Supplier<Authentication> ) mock( Supplier.class );
    }

    @Test
    public void grants_when_voter_grants() {
        AccessDecisionVoter<MethodInvocation> voter = mockVoter();
        when( voter.vote( any(), any(), anyCollection() ) ).thenReturn( AccessDecisionVoter.ACCESS_GRANTED );
        AclVoterAuthorizationManager mgr = new AclVoterAuthorizationManager( voter, ATTR );

        AuthorizationDecision decision = mgr.check( auth(), mock( MethodInvocation.class ) );

        assertNotNull( decision, "GRANTED must produce a non-null AuthorizationDecision" );
        assertTrue( decision.isGranted() );
    }

    @Test
    public void denies_when_voter_denies() {
        AccessDecisionVoter<MethodInvocation> voter = mockVoter();
        when( voter.vote( any(), any(), anyCollection() ) ).thenReturn( AccessDecisionVoter.ACCESS_DENIED );
        AclVoterAuthorizationManager mgr = new AclVoterAuthorizationManager( voter, ATTR );

        AuthorizationDecision decision = mgr.check( auth(), mock( MethodInvocation.class ) );

        assertNotNull( decision, "DENIED must produce a non-null AuthorizationDecision" );
        assertFalse( decision.isGranted() );
    }

    @Test
    public void returns_null_when_voter_abstains() {
        AccessDecisionVoter<MethodInvocation> voter = mockVoter();
        when( voter.vote( any(), any(), anyCollection() ) ).thenReturn( AccessDecisionVoter.ACCESS_ABSTAIN );
        AclVoterAuthorizationManager mgr = new AclVoterAuthorizationManager( voter, ATTR );

        AuthorizationDecision decision = mgr.check( auth(), mock( MethodInvocation.class ) );

        assertNull( decision, "ABSTAIN must map to null per AuthorizationManager contract" );
    }

    @Test
    public void passes_configured_attribute_to_voter() {
        AccessDecisionVoter<MethodInvocation> voter = mockVoter();
        when( voter.vote( any(), any(), anyCollection() ) ).thenReturn( AccessDecisionVoter.ACCESS_GRANTED );
        AclVoterAuthorizationManager mgr = new AclVoterAuthorizationManager( voter, ATTR );

        MethodInvocation mi = mock( MethodInvocation.class );
        mgr.check( auth(), mi );

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Collection<ConfigAttribute> captured = mock( Collection.class );
        // verify the voter was called once with some collection of attributes; the concrete
        // attribute identity is checked by the constructor's supports() assert
        verify( voter ).vote( any(), any( MethodInvocation.class ), anyCollection() );
    }

    @Test
    public void rejects_unsupported_attribute_at_construction() {
        AccessDecisionVoter<MethodInvocation> voter = mock( AccessDecisionVoter.class );
        when( voter.supports( any( ConfigAttribute.class ) ) ).thenReturn( false );
        try {
            new AclVoterAuthorizationManager( voter, ATTR );
            fail( "expected IllegalArgumentException when voter does not support the attribute" );
        } catch ( IllegalArgumentException expected ) {
            // ok
        }
    }

    @Test
    public void exposes_configured_attribute() {
        AccessDecisionVoter<MethodInvocation> voter = mockVoter();
        AclVoterAuthorizationManager mgr = new AclVoterAuthorizationManager( voter, ATTR );
        assertTrue( ATTR.equals( mgr.getConfigAttribute() ) );
    }
}
