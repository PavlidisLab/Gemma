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

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Spring Security 6 {@link PasswordEncoder} that understands two formats:
 *
 * <ol>
 *   <li><b>Legacy SHA-1 + username-as-salt</b>: stored as a bare 40-char hex digest with no
 *       prefix, computed as {@code SHA-1(rawPassword + "{" + username + "}")} (the exact
 *       output of Spring Security 3/4's {@code ShaPasswordEncoder} configured with
 *       {@code ReflectionSaltSource(userPropertyToUse="username")} — see Gemma's pre-Phase-2
 *       {@code applicationContext-security.xml}). This is what is in the production database
 *       and in {@code sql/init-data.sql}.</li>
 *   <li><b>BCrypt</b>: stored with the {@code {bcrypt}} prefix per Spring Security 5/6's
 *       {@link org.springframework.security.crypto.password.DelegatingPasswordEncoder}
 *       convention.</li>
 * </ol>
 *
 * <h2>How the username gets here</h2>
 * Spring Security 6's {@code PasswordEncoder} interface is username-agnostic
 * ({@code encode(rawPassword)} / {@code matches(rawPassword, encodedPassword)}). Salting
 * with the username therefore requires the username to arrive via a side channel. We use a
 * {@link ThreadLocal}: {@link LegacyAwareDaoAuthenticationProvider} sets the current
 * username on this encoder before invoking {@code super.additionalAuthenticationChecks(...)}
 * and clears it afterwards. The ThreadLocal scope is the single auth call, so cross-request
 * leakage is impossible as long as the provider is the only caller and it always clears in
 * a {@code finally}.
 *
 * <p>Alternative considered: writing a custom {@code AuthenticationProvider} from scratch
 * that does the SHA itself. Rejected — it would duplicate a lot of {@code
 * AbstractUserDetailsAuthenticationProvider} machinery (caching, locking, credentials
 * erasure) and would still need to be told which password format the stored hash was in.
 * Encapsulating the format detection inside the encoder keeps the provider thin.</p>
 *
 * <h2>Migration</h2>
 * {@link #upgradeEncoding(String)} returns {@code true} for any non-bcrypt encoded form, so
 * Spring Security's {@code DaoAuthenticationProvider} will call {@link #encode(CharSequence)}
 * with the verified plaintext after a successful legacy match and the application is
 * expected to write the upgraded {@code {bcrypt}}-prefixed hash back to the user row.
 * (Gemma's {@code UserManagerImpl} does NOT currently observe the
 * {@code AuthenticationSuccessEvent} that would carry the upgraded hash, so this is "ready
 * for upgrade-on-login" but the wiring of the listener is a separate follow-up.)
 *
 * @author Gemma
 */
public class GemmaLegacyAwarePasswordEncoder implements PasswordEncoder {

    /** Prefix Spring Security's DelegatingPasswordEncoder uses for bcrypt hashes. */
    public static final String BCRYPT_PREFIX = "{bcrypt}";

    /** Length of a SHA-1 digest in hex characters. */
    private static final int SHA1_HEX_LEN = 40;

    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();

    private final BCryptPasswordEncoder bcrypt;

    public GemmaLegacyAwarePasswordEncoder() {
        this( new BCryptPasswordEncoder() );
    }

    public GemmaLegacyAwarePasswordEncoder( BCryptPasswordEncoder bcrypt ) {
        this.bcrypt = bcrypt;
    }

    /**
     * Bind the current authenticating user's username for the duration of one auth call.
     * Caller is responsible for {@link #clearCurrentUsername()} in a {@code finally}.
     */
    public static void setCurrentUsername( String username ) {
        CURRENT_USERNAME.set( username );
    }

    public static void clearCurrentUsername() {
        CURRENT_USERNAME.remove();
    }

    static String currentUsername() {
        return CURRENT_USERNAME.get();
    }

    @Override
    public String encode( CharSequence rawPassword ) {
        // Always encode new passwords as bcrypt — this is the forward direction.
        return BCRYPT_PREFIX + bcrypt.encode( rawPassword );
    }

    @Override
    public boolean matches( CharSequence rawPassword, String encodedPassword ) {
        if ( rawPassword == null || encodedPassword == null ) {
            return false;
        }
        if ( encodedPassword.startsWith( BCRYPT_PREFIX ) ) {
            return bcrypt.matches( rawPassword, encodedPassword.substring( BCRYPT_PREFIX.length() ) );
        }
        // Legacy: bare 40-char hex SHA-1.
        if ( isLikelySha1Hex( encodedPassword ) ) {
            String username = currentUsername();
            if ( username == null ) {
                // Without a username we cannot recompute the legacy salt. Fail closed —
                // returning false rather than throwing means the auth call reports
                // "bad credentials" instead of leaking the format to clients.
                return false;
            }
            String computed = sha1HexUsernameSalt( rawPassword, username );
            return constantTimeEquals( computed, encodedPassword );
        }
        // Unknown format. Fail closed.
        return false;
    }

    @Override
    public boolean upgradeEncoding( String encodedPassword ) {
        if ( encodedPassword == null ) {
            return false;
        }
        // Any non-bcrypt format should be re-encoded on next successful login.
        return !encodedPassword.startsWith( BCRYPT_PREFIX );
    }

    /**
     * Compute the legacy hash for direct use (e.g., tests). Format:
     * {@code lowercase-hex(SHA-1(UTF-8(rawPassword + "{" + username + "}")))}.
     */
    public static String sha1HexUsernameSalt( CharSequence rawPassword, String username ) {
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-1" );
            md.update( rawPassword.toString().getBytes( StandardCharsets.UTF_8 ) );
            md.update( ( byte ) '{' );
            md.update( username.getBytes( StandardCharsets.UTF_8 ) );
            md.update( ( byte ) '}' );
            return toHex( md.digest() );
        } catch ( NoSuchAlgorithmException e ) {
            // Every JRE ships SHA-1.
            throw new IllegalStateException( "SHA-1 not available", e );
        }
    }

    private static boolean isLikelySha1Hex( String s ) {
        if ( s.length() != SHA1_HEX_LEN ) {
            return false;
        }
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            boolean isHex = ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' ) || ( c >= 'A' && c <= 'F' );
            if ( !isHex ) {
                return false;
            }
        }
        return true;
    }

    private static boolean constantTimeEquals( String a, String b ) {
        if ( a == null || b == null || a.length() != b.length() ) {
            return false;
        }
        int diff = 0;
        for ( int i = 0; i < a.length(); i++ ) {
            diff |= Character.toLowerCase( a.charAt( i ) ) ^ Character.toLowerCase( b.charAt( i ) );
        }
        return diff == 0;
    }

    private static String toHex( byte[] bytes ) {
        char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for ( int i = 0; i < bytes.length; i++ ) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String( out );
    }
}
