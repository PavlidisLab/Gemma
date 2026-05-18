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

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies {@link GemmaLegacyAwarePasswordEncoder} against fixtures lifted from
 * {@code gemma-core/src/main/resources/sql/init-data.sql} — these are the actual hashes
 * shipped with Gemma so any production user row with the legacy SHA-1 format must match.
 */
public class GemmaLegacyAwarePasswordEncoderTest {

    private final GemmaLegacyAwarePasswordEncoder encoder = new GemmaLegacyAwarePasswordEncoder();

    @After
    public void clearTl() {
        GemmaLegacyAwarePasswordEncoder.clearCurrentUsername();
    }

    @Test
    public void legacyHash_administrator_matches() {
        // from init-data.sql line 11: user 'administrator' with password 'administrator'
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        GemmaLegacyAwarePasswordEncoder.setCurrentUsername( "administrator" );
        assertTrue( encoder.matches( "administrator", stored ) );
    }

    @Test
    public void legacyHash_gemmaAgent_matches() {
        // from init-data.sql line 11: user 'gemmaAgent' with password 'XXXXXXXX'
        String stored = "2db458c67b4b52bba0184611c302c9c174ce8de4";
        GemmaLegacyAwarePasswordEncoder.setCurrentUsername( "gemmaAgent" );
        assertTrue( encoder.matches( "XXXXXXXX", stored ) );
    }

    @Test
    public void legacyHash_wrongPassword_rejected() {
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        GemmaLegacyAwarePasswordEncoder.setCurrentUsername( "administrator" );
        assertFalse( encoder.matches( "not-the-password", stored ) );
    }

    @Test
    public void legacyHash_wrongUsername_rejected() {
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        GemmaLegacyAwarePasswordEncoder.setCurrentUsername( "someoneElse" );
        assertFalse( encoder.matches( "administrator", stored ) );
    }

    @Test
    public void legacyHash_withoutUsernameThreadLocal_failsClosed() {
        // No setCurrentUsername. Encoder must not accept anything — and must not throw.
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        assertFalse( encoder.matches( "administrator", stored ) );
    }

    @Test
    public void legacyHash_isFlaggedForUpgrade() {
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        assertTrue( "legacy hash should trigger re-encoding on next successful login",
                encoder.upgradeEncoding( stored ) );
    }

    @Test
    public void encode_producesBcryptPrefixed_andMatchesBack() {
        String raw = "hunter2";
        String encoded = encoder.encode( raw );
        assertTrue( "new encodings must carry the {bcrypt} prefix so the encoder can route them",
                encoded.startsWith( GemmaLegacyAwarePasswordEncoder.BCRYPT_PREFIX ) );
        assertTrue( encoder.matches( raw, encoded ) );
        assertFalse( encoder.matches( "wrong", encoded ) );
    }

    @Test
    public void bcryptHash_doesNotRequireUsernameThreadLocal() {
        // The username TL is only needed for legacy decode.
        String encoded = encoder.encode( "hunter2" );
        GemmaLegacyAwarePasswordEncoder.clearCurrentUsername();
        assertTrue( encoder.matches( "hunter2", encoded ) );
    }

    @Test
    public void bcryptHash_isNotFlaggedForUpgrade() {
        String encoded = encoder.encode( "hunter2" );
        assertFalse( encoder.upgradeEncoding( encoded ) );
    }

    @Test
    public void encode_isNotDeterministic() {
        // Sanity: BCrypt salts are random, so encoding the same password twice yields
        // different hashes — but both must verify.
        String a = encoder.encode( "samepass" );
        String b = encoder.encode( "samepass" );
        assertNotEquals( a, b );
        assertTrue( encoder.matches( "samepass", a ) );
        assertTrue( encoder.matches( "samepass", b ) );
    }

    @Test
    public void unknownFormat_failsClosed() {
        // Not 40 hex chars, not bcrypt-prefixed.
        assertFalse( encoder.matches( "anything", "not-a-known-format" ) );
        assertFalse( encoder.matches( "anything", "" ) );
    }

    @Test
    public void sha1HexUsernameSalt_helperMatchesFixtures() {
        // Document the salt format directly.
        assertEquals( "b7338dcc17d6b6c199a75540aab6d0506567b980",
                GemmaLegacyAwarePasswordEncoder.sha1HexUsernameSalt( "administrator", "administrator" ) );
        assertEquals( "2db458c67b4b52bba0184611c302c9c174ce8de4",
                GemmaLegacyAwarePasswordEncoder.sha1HexUsernameSalt( "XXXXXXXX", "gemmaAgent" ) );
    }
}
