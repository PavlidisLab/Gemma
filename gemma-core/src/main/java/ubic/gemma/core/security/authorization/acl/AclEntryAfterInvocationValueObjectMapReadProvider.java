/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.authorization.acl;

import ubic.gemma.core.security.gsec.model.SecureValueObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Gemma-owned after-invocation provider for the {@code AFTER_ACL_VALUE_OBJECT_MAP_READ} config
 * attribute: bulk ACL check + per-key {@link SecureValueObject} security-metadata population over
 * a returned {@code Map} whose keys are {@code SecureValueObject}s. Map values are NOT checked
 * (matches the gsec semantics) — entries are filtered by whether the user has READ on the key.
 * <p>
 * Replaces gsec's {@code AclEntryAfterInvocationValueObjectMapFilteringProvider} as part of the
 * Phase 3 AfterInvocation modernization (Phase B). Behaviorally identical to the gsec class —
 * same Map unwrap + delegate-to-collection-provider over {@code map.keySet()}, same per-key ACL
 * filter, same side-effect that populates {@code isPublic / isShared / userOwned /
 * userCanWrite} on every retained key VO. Lives in gemma-core so gsec's class can be retired
 * from the after-invocation provider chain without touching the 6 {@code @Secured({...,
 * "AFTER_ACL_VALUE_OBJECT_MAP_READ"})} call sites (DifferentialExpressionResultService x5,
 * DifferentialExpressionAnalysisService x1).
 * <p>
 * Cannot be expressed as plain {@code @PostFilter}: the per-key security metadata side-effect
 * is load-bearing (DEA results UI renders dataset-level lock / share / mine flags from the
 * key VO), and {@code @PostFilter} can only retain or drop entries, not mutate retained keys.
 * <p>
 * Extends {@link AclEntryAfterInvocationValueObjectCollectionReadProvider} via its protected
 * constructor — same bulk readAclsById call, same populate logic — only the {@code decide()}
 * entry point differs (unwrap the Map and delegate over keySet, return the original Map so the
 * caller still gets a Map back, not a Collection of keys).
 *
 * @see AclEntryAfterInvocationValueObjectCollectionReadProvider the bulk-collection parent providing the actual ACL + populate logic
 * @see ubic.gemma.core.security.MethodSecurityConfig wiring registration
 */
public class AclEntryAfterInvocationValueObjectMapReadProvider extends AclEntryAfterInvocationValueObjectCollectionReadProvider {

    /**
     * The single config attribute string this provider responds to. Must match the value used
     * in {@code @Secured({..., "AFTER_ACL_VALUE_OBJECT_MAP_READ"})} annotations.
     */
    public static final String ATTRIBUTE = "AFTER_ACL_VALUE_OBJECT_MAP_READ";

    public AclEntryAfterInvocationValueObjectMapReadProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, ATTRIBUTE, requirePermission );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config,
            Object returnedObject ) throws AccessDeniedException {
        for ( ConfigAttribute configAttribute : config ) {
            if ( !supports( configAttribute ) ) {
                continue;
            }
            if ( returnedObject instanceof Map ) {
                Map<?, ?> map = ( Map<?, ?> ) returnedObject;
                // Delegate to the collection provider over the key set — keys are the
                // SecureValueObjects whose ACLs gate the entry. Per-key side-effect populates
                // the security-metadata fields on each retained key VO in place; the Map itself
                // is mutated by the inherited filter loop (removing denied keys). Return the
                // original Map (now filtered) so the caller gets a Map back, not a Collection.
                super.decide( authentication, object, config, map.keySet() );
                return returnedObject;
            } else {
                throw new AuthorizationServiceException( "A Map was required as the "
                        + "returnedObject, but the returnedObject was: " + returnedObject );
            }
        }
        return returnedObject;
    }
}
