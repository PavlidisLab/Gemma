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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class AclPrincipalSid extends AclSid {

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

    @Override
    public boolean equals( Object object ) {
        if ( ( object == null ) || !( object instanceof AclPrincipalSid ) ) {
            return false;
        }

        if ( this == object ) return true;

        // Delegate to getPrincipal() to perform actual comparison (both should be identical)
        return ( ( AclPrincipalSid ) object ).getPrincipal().equals( this.getPrincipal() );
    }

    public String getPrincipal() {
        return principal;
    }

    @Override
    public int hashCode() {
        return this.getPrincipal().hashCode();
    }

    /**
     * @param authentication
     */
    public void setPrincipal( Authentication authentication ) {
        if ( authentication.getPrincipal() instanceof UserDetails ) {
            this.principal = ( ( UserDetails ) authentication.getPrincipal() ).getUsername();
        } else {
            this.principal = authentication.getPrincipal().toString();
        }

    }

    /**
     * @param principal
     */
    public void setPrincipal( String principal ) {
        this.principal = principal;
    }

    @Override
    public String toString() {
        return "AclPrincipalSid[" + this.principal + "]";
    }
}
