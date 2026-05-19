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
 * <h2>How the username is supplied</h2>
 * Spring Security 6's {@code PasswordEncoder} interface is username-agnostic
 * ({@code encode(rawPassword)} / {@code matches(rawPassword, encodedPassword)}). Salting with
 * the username therefore requires the username to arrive via a side channel. Earlier
 * Phase-2 versions of this class used a {@link ThreadLocal} bound by a custom auth provider,
 * but that pattern is hostile to async / reactive request flows and to per-request thread
 * reuse (Phase 3 cloud-readiness goal). The username is now supplied directly, with the
 * legacy-vs-bcrypt detection still done by inspecting the stored encoded string:
 *
 * <ul>
 *   <li>{@link LegacyAwareDaoAuthenticationProvider#additionalAuthenticationChecks} detects
 *       a legacy hash on {@code userDetails.getPassword()} and verifies it itself using the
 *       authoritative username from {@code userDetails.getUsername()}. {@link #matches} is
 *       never called for legacy hashes.</li>
 *   <li>{@link #upgradeEncoding(String)} returns {@code true} for legacy hashes, so Spring
 *       Security's {@code DaoAuthenticationProvider} re-encodes the verified plaintext via
 *       {@link #encode(CharSequence)} and writes it back via
 *       {@link org.springframework.security.core.userdetails.UserDetailsPasswordService}
 *       (implemented by {@code UserManagerImpl}).</li>
 *   <li>{@link #matches} only handles bcrypt; legacy hashes fail closed here.</li>
 * </ul>
 *
 * @author Gemma
 */
public class GemmaLegacyAwarePasswordEncoder implements PasswordEncoder {

    /** Prefix Spring Security's DelegatingPasswordEncoder uses for bcrypt hashes. */
    public static final String BCRYPT_PREFIX = "{bcrypt}";

    /** Length of a SHA-1 digest in hex characters. */
    private static final int SHA1_HEX_LEN = 40;

    private final BCryptPasswordEncoder bcrypt;

    public GemmaLegacyAwarePasswordEncoder() {
        this( new BCryptPasswordEncoder() );
    }

    public GemmaLegacyAwarePasswordEncoder( BCryptPasswordEncoder bcrypt ) {
        this.bcrypt = bcrypt;
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
        // Legacy hashes are intentionally not verified here — the username is not available
        // through the PasswordEncoder API. LegacyAwareDaoAuthenticationProvider intercepts
        // legacy stored hashes before super.additionalAuthenticationChecks delegates to this
        // encoder. Fail closed for defence-in-depth: anything that reaches matches() with a
        // non-bcrypt format is rejected.
        return false;
    }

    @Override
    public boolean upgradeEncoding( String encodedPassword ) {
        if ( encodedPassword == null ) {
            return false;
        }
        // Any non-bcrypt format should be re-encoded on next successful login. The framework
        // (DaoAuthenticationProvider) calls passwordEncoder.encode(presented) + then
        // userDetailsPasswordService.updatePassword(user, newHash).
        return !encodedPassword.startsWith( BCRYPT_PREFIX );
    }

    /**
     * @return {@code true} iff {@code encodedPassword} looks like a Gemma legacy SHA-1 hash
     *         (bare 40-char lowercase/uppercase hex, no prefix). Used by
     *         {@link LegacyAwareDaoAuthenticationProvider} to route auth checks.
     */
    public static boolean isLegacySha1Hex( String encodedPassword ) {
        if ( encodedPassword == null || encodedPassword.length() != SHA1_HEX_LEN ) {
            return false;
        }
        for ( int i = 0; i < encodedPassword.length(); i++ ) {
            char c = encodedPassword.charAt( i );
            boolean isHex = ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' ) || ( c >= 'A' && c <= 'F' );
            if ( !isHex ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute the legacy hash. Format:
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

    /**
     * Constant-time, case-insensitive comparison of two hex strings of equal length.
     */
    public static boolean constantTimeHexEquals( String a, String b ) {
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
