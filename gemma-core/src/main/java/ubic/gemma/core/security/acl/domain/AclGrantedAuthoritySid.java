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
package ubic.gemma.core.security.acl.domain;

import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Hibernate-mapped row for a granted-authority entry in {@code acl_sid} (discriminator
 * {@code principal=0}). Phase B of the gsec absorption removed this class from Spring Security's
 * {@code Sid} hierarchy so the runtime security path now uses exactly one {@code Sid} type
 * ({@link org.springframework.security.acls.domain.GrantedAuthoritySid}). This class is a pure JPA
 * entity used by HQL queries (see {@code AclQueryUtils}) — call {@link #toSid()} at the boundary
 * when a Spring-typed sid is needed.
 *
 * @author Paul
 */
public class AclGrantedAuthoritySid extends AclSid {

    private static final long serialVersionUID = 7755206462003052441L;
    private String grantedAuthority;

    public AclGrantedAuthoritySid( GrantedAuthority grantedAuthority ) {
        Assert.notNull( grantedAuthority, "GrantedAuthority required" );
        Assert.notNull( grantedAuthority.getAuthority(),
            "This Sid is only compatible with GrantedAuthoritys that provide a non-null getAuthority()" );
        this.grantedAuthority = grantedAuthority.getAuthority();
    }

    @SuppressWarnings("unused")
    public AclGrantedAuthoritySid() {

    }

    public AclGrantedAuthoritySid( String grantedAuthority ) {
        this.grantedAuthority = grantedAuthority;
    }

    public String getGrantedAuthority() {
        return grantedAuthority;
    }

    @SuppressWarnings("unused")
    public void setGrantedAuthority( String grantedAuthority ) {
        this.grantedAuthority = grantedAuthority;
    }

    @Override
    public Sid toSid() {
        return new org.springframework.security.acls.domain.GrantedAuthoritySid( grantedAuthority );
    }

    @Override
    public int hashCode() {
        return Objects.hash( grantedAuthority );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this == object ) return true;
        if ( !( object instanceof AclGrantedAuthoritySid ) ) return false;
        return Objects.equals( ( ( AclGrantedAuthoritySid ) object ).getGrantedAuthority(), this.getGrantedAuthority() );
    }

    @Override
    public String toString() {
        return "AclGrantedAuthoritySid[" + this.grantedAuthority + "]";
    }
}
