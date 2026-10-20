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
 * {@link DaoAuthenticationProvider} that understands Gemma's two pre-Phase-2 legacy
 * password formats, both bare 40-char hex SHA-1 with no prefix:
 *
 * <ol>
 *   <li><b>Username-salt</b> (post-2009-11-23): {@code SHA-1(rawPassword + "{" + username + "}")} —
 *       configured via {@code <s:salt-source user-property="username"/>}. See
 *       {@code sql/init-data.sql}.</li>
 *   <li><b>Fixed system-wide salt</b> (pre-2009-11-23): {@code SHA-1(rawPassword + "{gooblyfoobly}")} —
 *       configured via {@code SystemWideSaltSource} with {@code gemma.salt=gooblyfoobly} in
 *       build.properties (commit {@code 66f574e926}, 2008-10-02). Users created before the
 *       2009 salt-source switch (notably the original {@code administrator} account) still
 *       have hashes in this format in production gemd if their password hasn't been rotated.</li>
 * </ol>
 *
 * <p>The two are structurally indistinguishable (both 40-char SHA-1 hex), so verification
 * tries username-salt first, then fixed-salt; only if both miss is the password rejected.</p>
 *
 * <p>For any other stored hash format ({@code {bcrypt}}-prefixed or bare BCrypt are the
 * currently supported alternatives) verification falls through to the stock
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

    /** Historical SystemWideSaltSource value from {@code build.properties} (commit
     *  {@code 66f574e926}, 2008-10-02). Used by the {@code administrator} row and any other
     *  account whose password has not been rotated since the 2009-11-23 switch to
     *  username-salt. Default; override via {@link #setSystemWideSalt(String)} (or the
     *  {@code gemma.legacy.salt} property if Spring-wired) if the production salt has been
     *  rotated since 2008. */
    public static final String SYSTEM_WIDE_SALT = "gooblyfoobly";

    private String systemWideSalt = SYSTEM_WIDE_SALT;

    /** Override the system-wide salt used for the pre-2009 fixed-salt fallback. Setter form
     *  so Spring can inject from a property without restructuring the constructor. */
    public void setSystemWideSalt( String salt ) {
        this.systemWideSalt = salt == null ? SYSTEM_WIDE_SALT : salt;
    }

    public String getSystemWideSalt() {
        return systemWideSalt;
    }

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
            // Try username-salt first (post-2009 hashes — newer accounts and any rotated
            // since the switch).
            String usernameSaltHash = GemmaLegacyAwarePasswordEncoder
                    .sha1HexUsernameSalt( presented, userDetails.getUsername() );
            if ( GemmaLegacyAwarePasswordEncoder.constantTimeHexEquals( usernameSaltHash, storedHash ) ) {
                return;
            }
            // Fall back to the pre-2009 SystemWideSaltSource — used to be the only salt;
            // pre-2009 accounts (notably the original administrator) still carry these
            // hashes in production gemd.
            String fixedSaltHash = GemmaLegacyAwarePasswordEncoder
                    .sha1HexUsernameSalt( presented, systemWideSalt );
            if ( GemmaLegacyAwarePasswordEncoder.constantTimeHexEquals( fixedSaltHash, storedHash ) ) {
                return;
            }
            log.debug( "Authentication failed: password does not match stored value under either"
                    + " username-salt or fixed-salt (legacy SHA-1)" );
            throw new BadCredentialsException( messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials" ) );
        }

        super.additionalAuthenticationChecks( userDetails, authentication );
    }
}
