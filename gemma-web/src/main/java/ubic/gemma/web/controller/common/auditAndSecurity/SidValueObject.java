/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.Serializable;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

/**
 * @author paul
 * @version $Id$
 */
public class SidValueObject implements Comparable<SidValueObject>, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String rolePrefix = "GROUP_";

    private String authority;

    private boolean principal;

    public SidValueObject() {
    }

    public SidValueObject( Sid owner ) {
        this.principal = owner instanceof PrincipalSid;
        this.authority = sidToString( owner );
    }

    @Override
    public int compareTo( SidValueObject arg0 ) {

        /*
         * non-principals first.
         */

        if ( arg0.isPrincipal() && !this.isPrincipal() ) {
            return -1;
        }
        if ( !arg0.isPrincipal() && this.isPrincipal() ) {
            return 1;
        }

        return this.authority.compareTo( arg0.getAuthority() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        SidValueObject other = ( SidValueObject ) obj;
        if ( authority == null ) {
            if ( other.authority != null ) return false;
        } else if ( !authority.equals( other.authority ) ) return false;
        if ( principal != other.principal ) return false;
        return true;
    }

    public String getAuthority() {
        return authority;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( authority == null ) ? 0 : authority.hashCode() );
        result = prime * result + ( principal ? 1231 : 1237 );
        return result;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public void setAuthority( String authority ) {
        this.authority = authority;
    }

    public void setPrincipal( boolean principal ) {
        this.principal = principal;
    }

    private String sidToString( Sid s ) {
        if ( s instanceof PrincipalSid ) {
            return ( ( PrincipalSid ) s ).getPrincipal();
        } else if ( s instanceof GrantedAuthoritySid ) {
            String grantedAuthority = ( ( GrantedAuthoritySid ) s ).getGrantedAuthority();
            if ( !grantedAuthority.startsWith( rolePrefix ) ) {
                grantedAuthority = rolePrefix + grantedAuthority;
            }
            return grantedAuthority;
        }
        throw new IllegalArgumentException( "Don't know how to deal with " + s.getClass() );
    }

}
