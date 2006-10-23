/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.security.principal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.ConfigUtils;

/**
 * Implementation for Acegi.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserDetailsImpl extends User implements UserDetails {

    /**
     * This constructor is only to be used by the UserDetailsService.
     * 
     * @param user
     */
    protected UserDetailsImpl( User user ) {
        super();
        this.setId( user.getId() );
        this.setPassword( user.getPassword() );
        this.setUserName( user.getUserName() );
        this.setRoles( user.getRoles() );
        this.setEnabled( user.getEnabled() );

        // none of this other stuff matters...
        this.setAffiliations( user.getAffiliations() );
        this.setName( user.getName() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#getAuthorities()
     */
    public GrantedAuthority[] getAuthorities() {
        GrantedAuthority[] result = new GrantedAuthority[getRoles().size()];
        int i = 0;
        for ( UserRole role : getRoles() ) {
            final String name = role.getName();
            result[i] = new GrantedAuthority() {
                public String getAuthority() {
                    return name;
                }
            };
            i++;
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#getUsername()
     */
    public String getUsername() {
        return getUserName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#isAccountNonExpired()
     */
    public boolean isAccountNonExpired() {
        return getEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#isAccountNonLocked()
     */
    public boolean isAccountNonLocked() {
        return getEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#isCredentialsNonExpired()
     */
    public boolean isCredentialsNonExpired() {
        return getEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.acegisecurity.userdetails.UserDetails#isEnabled()
     */
    public boolean isEnabled() {
        return getEnabled();
    }

    public String salt() {
        return ConfigUtils.getString( "gemma.salt" );
    }

    @Override
    public String toString() {
        return this.getUserName();
    }

    /*
     * These two methods are implemented to keep Tomcat from trying to serialize the session.
     */
    @SuppressWarnings("unused")
    private void writeObject( ObjectOutputStream out ) throws IOException {
        throw new NotSerializableException( "Not today!" );
    }

    @SuppressWarnings("unused")
    private void readObject( ObjectInputStream in ) throws IOException {
        throw new NotSerializableException( "Not today!" );
    }

}
