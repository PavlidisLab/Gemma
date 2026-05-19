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

import ubic.gemma.core.security.gsec.acl.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider;
import ubic.gemma.core.security.gsec.model.SecureValueObject;
import ubic.gemma.core.security.gsec.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ubic.gemma.core.security.authorization.acl.AclEntryAfterInvocationValueObjectReadProvider.populateValueObject;

/**
 * Gemma-owned after-invocation provider for the {@code AFTER_ACL_VALUE_OBJECT_COLLECTION_READ}
 * config attribute: bulk ACL check + per-row {@link SecureValueObject} security-metadata
 * population over a returned collection.
 * <p>
 * Replaces gsec's {@code AclEntryAfterInvocationValueObjectCollectionFilteringProvider} as
 * part of the Phase 3 AfterInvocation modernization (Phase B). Behaviorally identical to the
 * gsec class — same bulk-fetch ACL pass (one {@code readAclsById} call for the whole
 * collection, not N reads), same per-row filtering, same side-effect that populates
 * {@code isPublic / isShared / userOwned / userCanWrite} on every retained VO. Lives in
 * gemma-core so gsec's class can be retired from the after-invocation provider chain without
 * touching the 20 {@code @Secured({..., "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ"})} call sites
 * (ExpressionExperimentService bulk loaders, GeneSetService, etc.).
 * <p>
 * Cannot be expressed as plain {@code @PostFilter}: the security metadata side-effect on every
 * retained VO is load-bearing — the catalog / dataset / gene-set listing UIs render lock /
 * share / edit icons per row based on those flags. {@code @PostFilter} can only retain or drop;
 * it has no hook to mutate retained elements. A {@code @PostFilter} variant that called a
 * helper inside the SpEL would re-fetch ACLs per row and lose the bulk optimization.
 * <p>
 * Extends gsec's {@link AclEntryAfterInvocationCollectionFilteringProvider} (the
 * bulk-optimized parent — same base used by the existing Gemma-owned association providers
 * for composite sequences and data vectors). That class manages the ThreadLocal-stashed
 * per-row permission results so the inherited {@code decide()} loop in Spring's stock
 * {@code AclEntryAfterInvocationCollectionFilteringProvider} can call back into the
 * single-object {@code hasPermission} cheaply.
 *
 * @see AclEntryAfterInvocationValueObjectReadProvider the single-object variant
 * @see ubic.gemma.core.security.MethodSecurityConfig wiring registration
 */
public class AclEntryAfterInvocationValueObjectCollectionReadProvider extends AclEntryAfterInvocationCollectionFilteringProvider {

    private static final Log log = LogFactory.getLog( AclEntryAfterInvocationValueObjectCollectionReadProvider.class );

    /**
     * The single config attribute string this provider responds to. Must match the value used
     * in {@code @Secured({..., "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ"})} annotations.
     */
    public static final String ATTRIBUTE = "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ";

    public AclEntryAfterInvocationValueObjectCollectionReadProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, ATTRIBUTE, requirePermission );
    }

    /**
     * Subclass constructor used by Map-shaped variants that delegate to this collection provider
     * with a different config attribute (e.g. the keys-only Map provider). Mirrors the
     * protected constructor on gsec's collection provider so subclasses can pass their own
     * {@code processConfigAttribute} while inheriting all the bulk-fetch + VO-populate logic.
     */
    protected AclEntryAfterInvocationValueObjectCollectionReadProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected boolean[] hasPermission( Authentication authentication, List<Object> domainObjects ) {
        boolean[] perms = new boolean[domainObjects.size()];
        if ( domainObjects.isEmpty() ) {
            return perms;
        }

        List<Sid> sids = this.sidRetrievalStrategy.getSids( authentication );
        List<ObjectIdentity> ois = getObjectIdentities( domainObjects );
        Map<ObjectIdentity, Acl> aclsById;
        try {
            aclsById = aclService.readAclsById( ois, sids );
        } catch ( NotFoundException e ) {
            aclsById = Collections.emptyMap();
        }

        String currentUsername = SecurityUtil.getCurrentUsername();
        boolean isAdmin = SecurityUtil.isUserAdmin();

        int i = 0;
        for ( ObjectIdentity oi : ois ) {
            Acl acl = aclsById.get( oi );
            if ( acl != null ) {
                perms[i] = acl.isGranted( requirePermission, sids, false );
                Object domainObject = domainObjects.get( i );
                if ( domainObject instanceof SecureValueObject ) {
                    populateValueObject( ( SecureValueObject ) domainObject, acl, sids, requirePermission, currentUsername, isAdmin );
                }
            } else {
                log.trace( String.format( "No ACL was found for %s.", oi ) );
                perms[i] = false;
            }
            i++;
        }

        return perms;
    }
}
