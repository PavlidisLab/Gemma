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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionVoter;

/**
 * Wires {@link AclVoterAuthorizationManager} beans, one per active {@code ACL_SECURABLE_*} config
 * attribute, on top of the existing gsec voter beans. Phase X.1 of the ACL voter migration (see
 * {@code ACL_ENTRY_VOTER_MIGRATION.md} on {@code worktree-aclentryvoter-recce}).
 * <p>
 * The recce identified 4 active voter beans and 4 active config attributes plus 2
 * {@code _IGNORE_TRANSIENT} variants (281 call sites total). The 4 map/map-value voter beans are
 * dead (zero call sites) and are not wrapped here — they will be removed wholesale in Phase X.4.
 * <p>
 * <b>Active wrappers:</b>
 * <ul>
 *   <li>{@code aclSecurableReadAuthorizationManager} — wraps {@code securableReadVoter}
 *       ({@code AclEntryVoter}) for {@code ACL_SECURABLE_READ} (176 call sites).</li>
 *   <li>{@code aclSecurableEditAuthorizationManager} — wraps {@code securableEditVoter} for
 *       {@code ACL_SECURABLE_EDIT} (87 call sites).</li>
 *   <li>{@code aclSecurableEditIgnoreTransientAuthorizationManager} — same voter bean, bound to
 *       the {@code _IGNORE_TRANSIENT} suffix variant (1 call site in {@code SecurableBaseService}).</li>
 *   <li>{@code aclSecurableCollectionReadAuthorizationManager} — wraps
 *       {@code securableCollectionReadVoter} ({@code AclEntryCollectionVoter}) for
 *       {@code ACL_SECURABLE_COLLECTION_READ} (12 call sites).</li>
 *   <li>{@code aclSecurableCollectionEditAuthorizationManager} — wraps
 *       {@code securableCollectionEditVoter} for {@code ACL_SECURABLE_COLLECTION_EDIT} (2 call
 *       sites).</li>
 *   <li>{@code aclSecurableCollectionEditIgnoreTransientAuthorizationManager} — same voter bean,
 *       {@code _IGNORE_TRANSIENT} variant (1 call site in {@code SecurableBaseService}).</li>
 * </ul>
 * <p>
 * <b>Parallel-run posture.</b> These beans exist but are <em>not</em> yet wired into the
 * method-security interceptor chain. {@link ubic.gemma.core.security.MethodSecurityConfig} still
 * runs {@code @EnableGlobalMethodSecurity} on top of the legacy {@code accessDecisionManager}
 * bean. Phase X.2 will: (1) flip {@code MethodSecurityConfig} to {@code @EnableMethodSecurity},
 * (2) construct one {@code AuthorizationManagerBeforeMethodInterceptor} per bean here with a
 * pointcut matching {@code @Secured} annotations carrying the matching attribute string, (3) drop
 * the {@code accessDecisionManager()} / {@code runAsManager()} overrides. Until then the wrappers
 * are tested in isolation (see {@code AclVoterAuthorizationManagerTest}) but inactive in the
 * running call chain.
 *
 * @author claude
 */
@Configuration
public class AclVoterAuthorizationManagerConfig {

    static final String ATTR_SECURABLE_READ = "ACL_SECURABLE_READ";
    static final String ATTR_SECURABLE_EDIT = "ACL_SECURABLE_EDIT";
    static final String ATTR_SECURABLE_EDIT_IGNORE_TRANSIENT = "ACL_SECURABLE_EDIT_IGNORE_TRANSIENT";
    static final String ATTR_SECURABLE_COLLECTION_READ = "ACL_SECURABLE_COLLECTION_READ";
    static final String ATTR_SECURABLE_COLLECTION_EDIT = "ACL_SECURABLE_COLLECTION_EDIT";
    static final String ATTR_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT = "ACL_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT";

    @Bean
    public AclVoterAuthorizationManager aclSecurableReadAuthorizationManager(
        @Qualifier("securableReadVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_READ );
    }

    @Bean
    public AclVoterAuthorizationManager aclSecurableEditAuthorizationManager(
        @Qualifier("securableEditVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_EDIT );
    }

    @Bean
    public AclVoterAuthorizationManager aclSecurableEditIgnoreTransientAuthorizationManager(
        @Qualifier("securableEditVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_EDIT_IGNORE_TRANSIENT );
    }

    @Bean
    public AclVoterAuthorizationManager aclSecurableCollectionReadAuthorizationManager(
        @Qualifier("securableCollectionReadVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_COLLECTION_READ );
    }

    @Bean
    public AclVoterAuthorizationManager aclSecurableCollectionEditAuthorizationManager(
        @Qualifier("securableCollectionEditVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_COLLECTION_EDIT );
    }

    @Bean
    public AclVoterAuthorizationManager aclSecurableCollectionEditIgnoreTransientAuthorizationManager(
        @Qualifier("securableCollectionEditVoter") AccessDecisionVoter<MethodInvocation> voter ) {
        return new AclVoterAuthorizationManager( voter, ATTR_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT );
    }
}
