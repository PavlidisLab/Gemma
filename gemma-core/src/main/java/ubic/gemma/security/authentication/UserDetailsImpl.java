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
package ubic.gemma.security.authentication;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * Implementation for Spring Security.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserDetailsImpl implements UserDetails {

    /**
     * 
     */
    private static final long serialVersionUID = 1650537135541038216L;

    private String email;
    private Boolean enabled;
    private Collection<GrantedAuthority> grantedAuthorities = new HashSet<GrantedAuthority>();

    private String password;

    private String signupToken;
    private Date signupTokenDatestamp;
    private String userName;

    /**
     * @param password
     * @param userName
     * @param enabled
     * @param grantedAuthorities
     * @param email
     * @param signupConfirmationKey
     * @param signupConfirmationKeyDateStamp
     */
    public UserDetailsImpl( String password, String userName, Boolean enabled,
            Collection<GrantedAuthority> grantedAuthorities, String email, String signupConfirmationKey,
            Date signupConfirmationKeyDateStamp ) {
        super();
        this.password = password;
        this.userName = userName;
        this.enabled = enabled;

        if ( grantedAuthorities != null ) this.grantedAuthorities = grantedAuthorities;
        this.email = email;

        this.signupToken = signupConfirmationKey;
        this.signupTokenDatestamp = signupConfirmationKeyDateStamp;

    }

    /**
     * @param user
     */
    public UserDetailsImpl( User user ) {
        super();
        this.password = user.getPassword();
        this.userName = user.getUserName();
        this.enabled = user.getEnabled();
        this.email = user.getEmail();
        this.signupToken = user.getSignupToken();
        this.signupTokenDatestamp = user.getSignupTokenDatestamp();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getAuthorities()
     */
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.grantedAuthorities;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getPassword()
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * @return the signupToken
     */
    public String getSignupToken() {
        return signupToken;
    }

    /**
     * @return the signupTokenDatestamp
     */
    public Date getSignupTokenDatestamp() {
        return signupTokenDatestamp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#getUsername()
     */
    @Override
    public String getUsername() {
        return userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isAccountNonExpired()
     */
    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isAccountNonLocked()
     */
    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isCredentialsNonExpired()
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.userdetails.UserDetails#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param email the email to set
     */
    public void setEmail( String email ) {
        this.email = email;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return userName;
    }

}
