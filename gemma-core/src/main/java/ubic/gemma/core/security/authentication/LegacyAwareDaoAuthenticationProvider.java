/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link DaoAuthenticationProvider} that binds the authenticating user's username to
 * {@link GemmaLegacyAwarePasswordEncoder} via a {@link ThreadLocal} for the duration of
 * the password-match call, so the encoder can re-derive Gemma's legacy
 * {@code SHA-1(rawPassword + "{" + username + "}")} hash.
 *
 * <p>The ThreadLocal is set immediately before {@code super.additionalAuthenticationChecks}
 * and cleared in a {@code finally}, so the binding never outlives a single auth attempt
 * (no cross-request leakage).</p>
 *
 * @author Gemma
 */
public class LegacyAwareDaoAuthenticationProvider extends DaoAuthenticationProvider {

    @Override
    protected void additionalAuthenticationChecks( UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication ) throws AuthenticationException {
        GemmaLegacyAwarePasswordEncoder.setCurrentUsername( userDetails.getUsername() );
        try {
            super.additionalAuthenticationChecks( userDetails, authentication );
        } finally {
            GemmaLegacyAwarePasswordEncoder.clearCurrentUsername();
        }
    }
}
