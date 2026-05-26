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
package ubic.gemma.rest.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring regression tests for {@link RestSecurityConfig}.
 *
 * <p>The original failure mode this guards against: if
 * {@link RestSecurityConfig#restSecurityFilterChain} does not have
 * {@link AuthenticationManager} injected as a parameter (and does not call
 * {@code http.authenticationManager(...)} with it), Spring Security 6 builds a
 * <em>default</em> {@code DaoAuthenticationProvider} on the chain. That default provider goes
 * directly to {@code passwordEncoder.matches(presented, stored)}, bypassing
 * {@code LegacyAwareDaoAuthenticationProvider.additionalAuthenticationChecks} — so the
 * legacy SHA-1 + username-salt verify path is never invoked and every legacy-format password
 * row fails Basic-auth at {@code /rest/v2} regardless of password correctness.
 *
 * <p>These are reflection-based smoke tests (no Spring context boot, no Mockito on
 * {@code HttpSecurity}'s fluent builder). They catch the specific regression in proportion to
 * its blast radius without committing to a heavier MockMvc integration test.
 */
class RestSecurityConfigTest {

    @Test
    void restSecurityFilterChain_declaresAuthenticationManagerParameter() throws Exception {
        Method method = findFilterChainMethod();
        Parameter[] params = method.getParameters();

        Parameter authMgrParam = Arrays.stream( params )
                .filter( p -> p.getType().equals( AuthenticationManager.class ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError(
                        "restSecurityFilterChain must declare an AuthenticationManager parameter. "
                                + "Without it the chain falls back to Spring Security's default "
                                + "DaoAuthenticationProvider, which bypasses LegacyAwareDaoAuthenticationProvider "
                                + "and breaks Basic-auth for every legacy SHA-1 password row." ) );

        Qualifier qualifier = authMgrParam.getAnnotation( Qualifier.class );
        assertNotNull( qualifier,
                "AuthenticationManager parameter must be @Qualifier-bound to disambiguate from any "
                        + "Spring-provided default. Multiple AuthenticationManager candidates can exist "
                        + "in the context (esp. when spring-boot-starter-security is on the classpath)." );
        assertEquals( "authenticationManager", qualifier.value(),
                "must wire the bean named 'authenticationManager' — the ProviderManager configured "
                        + "by SecurityConfig with LegacyAwareDaoAuthenticationProvider at position 0." );
    }

    @Test
    void restSecurityFilterChain_returnsSecurityFilterChain() throws Exception {
        Method method = findFilterChainMethod();
        assertEquals( SecurityFilterChain.class, method.getReturnType(),
                "method must produce a SecurityFilterChain bean" );
    }

    @Test
    void restSecurityFilterChain_acceptsHttpSecurity() throws Exception {
        Method method = findFilterChainMethod();
        boolean hasHttpSec = Arrays.stream( method.getParameters() )
                .anyMatch( p -> p.getType().equals( HttpSecurity.class ) );
        assertTrue( hasHttpSec, "method must take HttpSecurity as a parameter" );
    }

    /** Resolves the bean method by name; tolerates parameter-list evolution. */
    private static Method findFilterChainMethod() {
        return Arrays.stream( RestSecurityConfig.class.getDeclaredMethods() )
                .filter( m -> "restSecurityFilterChain".equals( m.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError(
                        "RestSecurityConfig.restSecurityFilterChain method not found" ) );
    }
}
