/*
 * The gemma-mda project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package gemma.gsec.acl.domain;

import org.springframework.security.acls.model.Sid;

import javax.annotation.Nullable;

/**
 * Utilities for inspecting {@link Sid} instances that may be either gsec's own
 * {@link AclPrincipalSid} / {@link AclGrantedAuthoritySid} or Spring Security's stock
 * {@link org.springframework.security.acls.domain.PrincipalSid} /
 * {@link org.springframework.security.acls.domain.GrantedAuthoritySid}.
 * <p>
 * After Renovations Phase 2, ACLs are read back by Spring Security's
 * {@code BasicLookupStrategy}, which builds Spring-typed sids. The legacy gsec-typed sids still
 * appear in code paths that construct sids from authentication tokens or build them inline (e.g.
 * {@code BaseAclAdvice}). Both flavours co-exist; tests like
 * {@code if ( sid instanceof AclPrincipalSid )} silently fail on Spring-typed sids and break ACL
 * predicates such as {@code isPrivate}, {@code isShared}, ownership checks, and group-membership
 * checks. Use these helpers in place of bare {@code instanceof}.
 */
public final class Sids {

    private Sids() {
    }

    /**
     * True if the sid identifies a principal (user) — either gsec's {@link AclPrincipalSid} or
     * Spring Security's {@link org.springframework.security.acls.domain.PrincipalSid}.
     */
    public static boolean isPrincipal( @Nullable Sid sid ) {
        return sid instanceof AclPrincipalSid
                || sid instanceof org.springframework.security.acls.domain.PrincipalSid;
    }

    /**
     * True if the sid identifies a granted authority (group/role) — either gsec's
     * {@link AclGrantedAuthoritySid} or Spring Security's
     * {@link org.springframework.security.acls.domain.GrantedAuthoritySid}.
     */
    public static boolean isGrantedAuthority( @Nullable Sid sid ) {
        return sid instanceof AclGrantedAuthoritySid
                || sid instanceof org.springframework.security.acls.domain.GrantedAuthoritySid;
    }

    /**
     * Principal name (username) carried by the sid, or {@code null} if {@code sid} is not a
     * principal sid.
     */
    @Nullable
    public static String principalName( @Nullable Sid sid ) {
        if ( sid instanceof AclPrincipalSid ) {
            return ( ( AclPrincipalSid ) sid ).getPrincipal();
        }
        if ( sid instanceof org.springframework.security.acls.domain.PrincipalSid ) {
            return ( ( org.springframework.security.acls.domain.PrincipalSid ) sid ).getPrincipal();
        }
        return null;
    }

    /**
     * Granted-authority name carried by the sid, or {@code null} if {@code sid} is not a
     * granted-authority sid.
     */
    @Nullable
    public static String grantedAuthority( @Nullable Sid sid ) {
        if ( sid instanceof AclGrantedAuthoritySid ) {
            return ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority();
        }
        if ( sid instanceof org.springframework.security.acls.domain.GrantedAuthoritySid ) {
            return ( ( org.springframework.security.acls.domain.GrantedAuthoritySid ) sid ).getGrantedAuthority();
        }
        return null;
    }
}
