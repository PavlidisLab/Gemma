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

import gemma.gsec.model.SecureValueObject;
import gemma.gsec.util.SecurityUtil;
import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

/**
 * Gemma-owned after-invocation provider for the {@code AFTER_ACL_VALUE_OBJECT_READ} config
 * attribute: single-object ACL check on a {@link SecureValueObject} return value.
 * <p>
 * Replaces gsec's {@code AclEntryAfterInvocationValueObjectProvider} as part of the Phase 3
 * AfterInvocation modernization (Phase B). Behaviorally identical to the gsec class — same
 * READ-permission check via {@link AclEntryAfterInvocationProvider#hasPermission}, same
 * post-check side-effect that populates the four security-metadata fields on the returned
 * {@link SecureValueObject} ({@code isPublic}, {@code isShared}, {@code userOwned},
 * {@code userCanWrite}) — but lives in gemma-core so the gsec class can be retired from the
 * after-invocation provider chain without touching the 4 {@code @Secured({...,
 * "AFTER_ACL_VALUE_OBJECT_READ"})} call sites.
 * <p>
 * Cannot be expressed as plain {@code @PostAuthorize}: the security metadata side-effect on the
 * returned VO is load-bearing — the web/REST layer renders the "private / shared / mine /
 * editable" UI affordances based on those flags. {@code @PostAuthorize} can only allow or
 * throw; it has no hook to mutate the result. An {@code @PostAuthorize} variant that called a
 * helper to populate the flags would duplicate the ACL fetch (once for the SpEL check, again
 * for the populate call). The after-invocation provider does both with one ACL read.
 *
 * @see AclEntryAfterInvocationValueObjectCollectionReadProvider the bulk variant for collections of VOs
 * @see ubic.gemma.core.security.MethodSecurityConfig wiring registration
 */
public class AclEntryAfterInvocationValueObjectReadProvider extends AclEntryAfterInvocationProvider {

    /**
     * The single config attribute string this provider responds to. Must match the value used
     * in {@code @Secured({..., "AFTER_ACL_VALUE_OBJECT_READ"})} annotations across the codebase.
     */
    public static final String ATTRIBUTE = "AFTER_ACL_VALUE_OBJECT_READ";

    public AclEntryAfterInvocationValueObjectReadProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, ATTRIBUTE, requirePermission );
    }

    @Override
    protected boolean hasPermission( Authentication authentication, Object domainObject ) {
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );
        try {
            Acl acl = aclService.readAclById( objectIdentity, sids );
            if ( domainObject instanceof SecureValueObject ) {
                populateValueObject( ( SecureValueObject ) domainObject, acl, sids, requirePermission,
                        SecurityUtil.getCurrentUsername(), SecurityUtil.isUserAdmin() );
            }
            return acl.isGranted( requirePermission, sids, false );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    /**
     * Populate the security-metadata fields on the given VO. Package-private so the collection
     * variant in this package can reuse it (same per-row logic, just driven by a bulk-fetched
     * ACL map instead of a single read).
     * <p>
     * Logic is a verbatim port of gsec's {@code AclEntryAfterInvocationValueObjectProvider
     * .populateValueObject} (commit 94c2b35f67 ACL recce documents the contract). The
     * "skip when not logged in" guard is intentional: anonymous users see VOs but the four
     * flags stay at their default false values (treated as "public / not shared / not mine /
     * read-only" by the UI), which matches the gsec behavior.
     */
    static void populateValueObject( SecureValueObject svo, Acl acl, List<Sid> sids,
            List<Permission> requirePermission, String currentUsername, boolean isAdmin ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            return;
        }
        svo.setIsPublic( !SecurityUtil.isPrivate( acl ) );
        svo.setIsShared( SecurityUtil.isShared( acl ) );
        svo.setUserOwned( SecurityUtil.isOwner( acl, currentUsername ) );
        if ( svo.getUserOwned() || isAdmin || requirePermission.contains( BasePermission.WRITE ) ) {
            svo.setUserCanWrite( true );
        } else {
            svo.setUserCanWrite( acl.isGranted( Collections.singletonList( BasePermission.WRITE ), sids, false ) );
        }
    }
}
