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

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

/**
 * @author paul
 * @version $Id$
 */
public class SidValueObject implements Comparable<SidValueObject> {

    private static final String rolePrefix = "GROUP_";

    private String authority;

    private boolean principal;

    public SidValueObject() {
    }

    public SidValueObject( Sid owner ) {
        this.principal = owner instanceof PrincipalSid;
        this.authority = sidToString( owner );
    }

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

    public String getAuthority() {
        return authority;
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
