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
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Spring Security 6 {@link AuthorizationManager} adapter that delegates to a legacy
 * {@link AccessDecisionVoter} bound to a single {@link ConfigAttribute} string.
 * <p>
 * Built for Phase X.1 of the ACL voter migration (see {@code ACL_ENTRY_VOTER_MIGRATION.md} on
 * {@code worktree-aclentryvoter-recce}). It lets the {@code @EnableMethodSecurity}-style
 * {@link AuthorizationManager} stack invoke the existing gsec {@code AclEntryVoter} /
 * {@code AclEntryCollectionVoter} beans without rewriting the 278 {@code @Secured("ACL_*")} call
 * sites. Each instance is bound to one config-attribute string (for example
 * {@code "ACL_SECURABLE_READ"}); register one bean per active attribute and use them as the
 * delegate inside {@code AuthorizationManagerBeforeMethodInterceptor} advisors.
 * <p>
 * <b>Phase X.1 status:</b> these wrapper beans are <em>not</em> yet wired into the method-security
 * interceptor chain. {@link ubic.gemma.core.security.MethodSecurityConfig} still uses
 * {@code @EnableGlobalMethodSecurity} with the legacy {@code accessDecisionManager} bean from gsec
 * XML, which invokes the same underlying voters directly. Phase X.2 will flip
 * {@code MethodSecurityConfig} to {@code @EnableMethodSecurity} and register these wrappers as
 * {@code AuthorizationManagerBeforeMethodInterceptor} beans pointcut on {@code @Secured}
 * annotations carrying the matching attribute string.
 * <p>
 * <b>Decision mapping.</b> The legacy voter returns three int constants — {@code ACCESS_GRANTED},
 * {@code ACCESS_DENIED}, {@code ACCESS_ABSTAIN}. The new model collapses GRANTED to
 * {@code new AuthorizationDecision(true)}, DENIED to {@code new AuthorizationDecision(false)}, and
 * ABSTAIN to {@code null} (the documented "no decision" signal in {@link AuthorizationManager}).
 * Returning null on abstain matters because the method-security stack consults multiple managers
 * and an abstaining wrapper must not deny — the framework's final decision (default-deny if every
 * manager abstains) is what enforces the {@code allowIfAllAbstainDecisions=false} semantic the
 * legacy {@code UnanimousBased} used.
 *
 * @author claude
 */
public class AclVoterAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final AccessDecisionVoter<MethodInvocation> voter;
    private final Collection<ConfigAttribute> attributes;
    private final String configAttribute;

    /**
     * @param voter           a legacy {@link AccessDecisionVoter} that operates on
     *                        {@link MethodInvocation} secure objects. In Gemma this is one of the
     *                        gsec voter beans ({@code securableReadVoter},
     *                        {@code securableEditVoter}, {@code securableCollectionReadVoter},
     *                        {@code securableCollectionEditVoter}). The voter must
     *                        {@link AccessDecisionVoter#supports(ConfigAttribute) support} the
     *                        configured attribute string; this is verified at construction.
     * @param configAttribute the single {@code ACL_SECURABLE_*} attribute string this manager
     *                        dispatches to the voter on. Each Phase X.1 wrapper bean binds one
     *                        attribute to one voter; the matching with {@code @Secured} annotation
     *                        values happens in the {@code BeforeMethodInterceptor} pointcut, not
     *                        here.
     */
    public AclVoterAuthorizationManager( AccessDecisionVoter<MethodInvocation> voter, String configAttribute ) {
        Assert.notNull( voter, "voter must not be null" );
        Assert.hasText( configAttribute, "configAttribute must be a non-empty string" );
        ConfigAttribute attr = new SecurityConfig( configAttribute );
        Assert.isTrue( voter.supports( attr ),
            "voter " + voter.getClass().getName() + " does not support config attribute " + configAttribute );
        this.voter = voter;
        this.configAttribute = configAttribute;
        this.attributes = Collections.singletonList( attr );
    }

    /**
     * @return the config-attribute string this wrapper dispatches on (for example
     * {@code "ACL_SECURABLE_READ"}). Useful for building advisor pointcuts that key off the
     * {@code @Secured} value.
     */
    public String getConfigAttribute() {
        return configAttribute;
    }

    @Override
    public AuthorizationDecision check( Supplier<Authentication> authentication, MethodInvocation invocation ) {
        Authentication auth = authentication.get();
        int decision = voter.vote( auth, invocation, attributes );
        switch ( decision ) {
            case AccessDecisionVoter.ACCESS_GRANTED:
                return new AuthorizationDecision( true );
            case AccessDecisionVoter.ACCESS_DENIED:
                return new AuthorizationDecision( false );
            case AccessDecisionVoter.ACCESS_ABSTAIN:
            default:
                // null signals "no decision" to the AuthorizationManager chain; the framework's
                // default-deny rule fires only if every manager abstains, which preserves the
                // legacy UnanimousBased(allowIfAllAbstainDecisions=false) semantic.
                return null;
        }
    }
}
