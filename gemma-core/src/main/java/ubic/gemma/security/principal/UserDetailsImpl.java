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

import java.util.Collection;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.ConfigUtils;

/**
 * Implementation for Acegi.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserDetailsImpl implements UserDetails {

    /**
     * 
     */
    private static final long serialVersionUID = 1650537135541038216L;
    private Long id;
    private String password;
    private String userName;
    private Boolean enabled;
    private Collection<UserRole> roles;

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
    }

    /**
     * @param roles
     */
    private void setRoles( Collection<UserRole> roles ) {
        this.roles = roles;

    }

    /**
     * @param enabled
     */
    private void setEnabled( Boolean enabled ) {
        this.enabled = enabled;

    }

    /**
     * @param userName
     */
    private void setUserName( String userName ) {
        this.userName = userName;

    }

    /**
     * @param password
     */
    private void setPassword( String password ) {
        this.password = password;

    }

    /**
     * @param id
     */
    private void setId( Long id ) {
        this.id = id;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getAuthorities()
     */
    @SuppressWarnings("serial")
    public GrantedAuthority[] getAuthorities() {
        GrantedAuthority[] result = new GrantedAuthority[getRoles().size()];
        int i = 0;
        for ( UserRole role : getRoles() ) {
            final String name = role.getName();
            result[i] = new GrantedAuthorityImpl( name );
            i++;
        }
        return result;
    }

    /**
     * @return
     */
    private Collection<UserRole> getRoles() {
        return roles;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getUsername()
     */
    public String getUsername() {
        return userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isAccountNonExpired()
     */
    public boolean isAccountNonExpired() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isAccountNonLocked()
     */
    public boolean isAccountNonLocked() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isCredentialsNonExpired()
     */
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isEnabled()
     */
    public boolean isEnabled() {
        return enabled;
    }

    public String salt() {
        return ConfigUtils.getString( "gemma.salt" );
    }

    @Override
    public String toString() {
        return userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getPassword()
     */
    public String getPassword() {
        return password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    // /*
    // * These two methods are implemented to keep Tomcat from trying to serialize the session.
    // */
    // @SuppressWarnings("unused")
    // private void writeObject( ObjectOutputStream out ) throws IOException {
    // throw new NotSerializableException( "Not today!" );
    // }
    //
    // @SuppressWarnings("unused")
    // private void readObject( ObjectInputStream in ) throws IOException {
    // throw new NotSerializableException( "Not today!" );
    // }

}
