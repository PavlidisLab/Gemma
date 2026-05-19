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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link DaoAuthenticationProvider} that understands Gemma's pre-Phase-2 legacy password
 * format ({@code SHA-1(rawPassword + "{" + username + "}")}, bare 40-char hex digest, no
 * prefix — see {@code sql/init-data.sql}). For any other stored hash format ({@code {bcrypt}}
 * being the only other currently supported format) it falls through to the stock
 * {@code DaoAuthenticationProvider} machinery, which delegates to the configured
 * {@link org.springframework.security.crypto.password.PasswordEncoder}
 * ({@link GemmaLegacyAwarePasswordEncoder}).
 *
 * <h2>Why this exists (Phase 3 cloud-readiness)</h2>
 * Spring Security 6's {@code PasswordEncoder} interface is username-agnostic and the
 * username-as-salt scheme requires the username at verify time. The previous Phase-2
 * implementation pushed the username through a {@link ThreadLocal} on the encoder before
 * {@code super.additionalAuthenticationChecks} ran. That ThreadLocal is hostile to async /
 * reactive flows and per-request thread reuse, which is why it has been removed.
 *
 * <p>Now the legacy SHA-1 verification happens here, with the authoritative username sourced
 * from {@code userDetails.getUsername()} (loaded by
 * {@link org.springframework.security.core.userdetails.UserDetailsService}) — no
 * thread-bound state of any kind.</p>
 *
 * <h2>Password upgrade flow</h2>
 * After {@code additionalAuthenticationChecks} returns successfully, the stock
 * {@code DaoAuthenticationProvider.authenticate(...)} does:
 *
 * <pre>
 *   if (passwordEncoder.upgradeEncoding(user.getPassword())) {
 *       String newPassword = passwordEncoder.encode(presented);
 *       user = userDetailsPasswordService.updatePassword(user, newPassword);
 *   }
 * </pre>
 *
 * {@link GemmaLegacyAwarePasswordEncoder#upgradeEncoding} returns {@code true} for legacy
 * hashes, so a legacy match here triggers an automatic re-encode to {@code {bcrypt}} and a
 * write-back via {@code UserManagerImpl} (which implements {@code UserDetailsPasswordService}).
 *
 * @author Gemma
 */
public class LegacyAwareDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private static final Log log = LogFactory.getLog( LegacyAwareDaoAuthenticationProvider.class );

    @Override
    protected void additionalAuthenticationChecks( UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication ) throws AuthenticationException {

        String storedHash = userDetails.getPassword();

        if ( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( storedHash ) ) {
            // Legacy verification: super would delegate to the PasswordEncoder, which cannot
            // see the username, so we do the compare directly here.
            if ( authentication.getCredentials() == null ) {
                log.debug( "Authentication failed: no credentials provided" );
                throw new BadCredentialsException( messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials" ) );
            }
            String presented = authentication.getCredentials().toString();
            String computed = GemmaLegacyAwarePasswordEncoder
                    .sha1HexUsernameSalt( presented, userDetails.getUsername() );
            if ( !GemmaLegacyAwarePasswordEncoder.constantTimeHexEquals( computed, storedHash ) ) {
                log.debug( "Authentication failed: password does not match stored value (legacy SHA-1)" );
                throw new BadCredentialsException( messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials" ) );
            }
            // Match — fall out. The stock authenticate() will see
            // passwordEncoder.upgradeEncoding(legacyHash) == true and trigger the bcrypt
            // upgrade via UserDetailsPasswordService.
            return;
        }

        super.additionalAuthenticationChecks( userDetails, authentication );
    }
}
