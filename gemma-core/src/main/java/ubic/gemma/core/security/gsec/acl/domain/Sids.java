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
package ubic.gemma.core.security.gsec.acl.domain;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

import javax.annotation.Nullable;

/**
 * Utilities for inspecting {@link Sid} instances.
 * <p>
 * After Phase B of the gsec absorption there is exactly one {@code Sid} hierarchy at runtime —
 * Spring Security's stock {@link PrincipalSid} / {@link GrantedAuthoritySid}. gsec's Hibernate-
 * mapped {@link AclPrincipalSid} / {@link AclGrantedAuthoritySid} are NOT {@code Sid}
 * implementations any more (they're JPA entities backing HQL queries against {@code acl_sid}).
 * Callers that have an entity in hand should convert via {@link AclSid#toSid()} before reaching
 * these helpers; the helpers themselves only know about the Spring types.
 */
public final class Sids {

    private Sids() {
    }

    /**
     * True if the sid identifies a principal (user).
     */
    public static boolean isPrincipal( @Nullable Sid sid ) {
        return sid instanceof PrincipalSid;
    }

    /**
     * True if the sid identifies a granted authority (group/role).
     */
    public static boolean isGrantedAuthority( @Nullable Sid sid ) {
        return sid instanceof GrantedAuthoritySid;
    }

    /**
     * Principal name (username) carried by the sid, or {@code null} if {@code sid} is not a
     * principal sid.
     */
    @Nullable
    public static String principalName( @Nullable Sid sid ) {
        if ( sid instanceof PrincipalSid ) {
            return ( ( PrincipalSid ) sid ).getPrincipal();
        }
        return null;
    }

    /**
     * Granted-authority name carried by the sid, or {@code null} if {@code sid} is not a
     * granted-authority sid.
     */
    @Nullable
    public static String grantedAuthority( @Nullable Sid sid ) {
        if ( sid instanceof GrantedAuthoritySid ) {
            return ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority();
        }
        return null;
    }
}
