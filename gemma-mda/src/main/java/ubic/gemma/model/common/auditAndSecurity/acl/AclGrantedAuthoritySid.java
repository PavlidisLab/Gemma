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
package ubic.gemma.model.common.auditAndSecurity.acl;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class AclGrantedAuthoritySid extends AclSid {
    private String grantedAuthority;

    public AclGrantedAuthoritySid() {
    }

    public AclGrantedAuthoritySid( GrantedAuthority grantedAuthority ) {
        Assert.notNull( grantedAuthority, "GrantedAuthority required" );
        Assert.notNull( grantedAuthority.getAuthority(),
                "This Sid is only compatible with GrantedAuthoritys that provide a non-null getAuthority()" );
        this.grantedAuthority = grantedAuthority.getAuthority();
    }

    public AclGrantedAuthoritySid( String grantedAuthority ) {
        this.grantedAuthority = grantedAuthority;
    }

    @Override
    public boolean equals( Object object ) {
        if ( ( object == null ) || !( object instanceof AclGrantedAuthoritySid ) ) {
            return false;
        }

        if ( this == object ) return true;

        // Delegate to getGrantedAuthority() to perform actual comparison (both should be identical)
        return ( ( AclGrantedAuthoritySid ) object ).getGrantedAuthority().equals( this.getGrantedAuthority() );
    }

    public String getGrantedAuthority() {
        return grantedAuthority;
    }

    @Override
    public int hashCode() {
        return this.getGrantedAuthority().hashCode();
    }

    public void setGrantedAuthority( String grantedAuthority ) {
        this.grantedAuthority = grantedAuthority;
    }

    @Override
    public String toString() {
        return "AclGrantedAuthoritySid[" + this.grantedAuthority + "]";
    }
}
