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

import org.hibernate.Hibernate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author Paul
 * @version $Id: AclGrantedAuthoritySid.java,v 1.1 2013/09/14 16:55:18 paul Exp $
 */
public class AclGrantedAuthoritySid extends AclSid {
    /**
     *
     */
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
    public int hashCode() {
        return Objects.hash( grantedAuthority );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this == object ) return true;

        // Renovations Phase 2 (Hibernate 6): unwrap HibernateProxy before the instanceof check.
        // See the parallel comment in AclPrincipalSid.equals() — many-to-one AclSid references
        // can come back as proxies declared against the abstract base class even with lazy="false".
        Object unwrapped = ( object instanceof org.hibernate.proxy.HibernateProxy )
            ? Hibernate.unproxy( object )
            : object;

        if ( !( unwrapped instanceof AclGrantedAuthoritySid ) ) {
            return false;
        }

        // Delegate to getGrantedAuthority() to perform actual comparison (both should be identical)
        return Objects.equals( ( ( AclGrantedAuthoritySid ) unwrapped ).getGrantedAuthority(), this.getGrantedAuthority() );
    }

    @Override
    public String toString() {
        return "AclGrantedAuthoritySid[" + this.grantedAuthority + "]";
    }
}
