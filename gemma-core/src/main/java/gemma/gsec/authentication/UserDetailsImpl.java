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
package gemma.gsec.authentication;

import gemma.gsec.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * Implementation for Spring Security.
 *
 * @author pavlidis
 * @version $Id: UserDetailsImpl.java,v 1.5 2014/05/29 02:11:59 ptan Exp $
 */
public class UserDetailsImpl implements UserDetails {

    /**
     *
     */
    private static final long serialVersionUID = 1650537135541038216L;

    private String email;
    private boolean enabled;
    private Collection<GrantedAuthority> grantedAuthorities = new HashSet<>();

    private final String password;

    private final String signupToken;
    private final Date signupTokenDatestamp;
    private final String userName;

    public UserDetailsImpl( String password, String userName, boolean enabled,
        @Nullable Collection<GrantedAuthority> grantedAuthorities, String email, String signupConfirmationKey,
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
     * Create a user details object from a {@link User} object.
     */
    public UserDetailsImpl( User user ) {
        this.password = user.getPassword();
        this.userName = user.getUserName();
        this.enabled = user.isEnabled();
        this.email = user.getEmail();
        this.signupToken = user.getSignupToken();
        this.signupTokenDatestamp = user.getSignupTokenDatestamp();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.grantedAuthorities;
    }

    public String getEmail() {
        return email;
    }

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

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

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
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return userName;
    }

}
