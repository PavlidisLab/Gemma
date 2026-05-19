/*
 * The gemma-mda project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gemma.gsec.acl.domain;

import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Hibernate-mapped row for a principal entry in {@code acl_sid} (discriminator {@code principal=1}).
 * Phase B of the gsec absorption removed this class from Spring Security's {@code Sid} hierarchy
 * so the runtime security path now uses exactly one {@code Sid} type
 * ({@link org.springframework.security.acls.domain.PrincipalSid}). This class is a pure JPA
 * entity used by HQL queries (see {@code AclQueryUtils}) — call {@link #toSid()} at the boundary
 * when a Spring-typed sid is needed.
 *
 * @author Paul
 */
public class AclPrincipalSid extends AclSid {

    private static final long serialVersionUID = -4679911678447417301L;
    private String principal;

    public AclPrincipalSid() {
    }

    public AclPrincipalSid( Authentication authentication ) {
        Assert.notNull( authentication, "Authentication required" );
        Assert.notNull( authentication.getPrincipal(), "Principal required" );

        if ( authentication.getPrincipal() instanceof UserDetails ) {
            this.principal = ( ( UserDetails ) authentication.getPrincipal() ).getUsername();
        } else {
            this.principal = authentication.getPrincipal().toString();
        }
    }

    public AclPrincipalSid( String principal ) {
        super();
        this.principal = principal;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal( String principal ) {
        this.principal = principal;
    }

    @Override
    public Sid toSid() {
        return new org.springframework.security.acls.domain.PrincipalSid( principal );
    }

    @Override
    public int hashCode() {
        return Objects.hash( principal );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this == object ) return true;
        if ( !( object instanceof AclPrincipalSid ) ) return false;
        return Objects.equals( ( ( AclPrincipalSid ) object ).getPrincipal(), this.getPrincipal() );
    }

    @Override
    public String toString() {
        return "AclPrincipalSid[" + this.principal + "]";
    }
}
