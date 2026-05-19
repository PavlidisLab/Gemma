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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link GemmaLegacyAwarePasswordEncoder} against fixtures lifted from
 * {@code gemma-core/src/main/resources/sql/init-data.sql} — these are the actual hashes
 * shipped with Gemma so any production user row with the legacy SHA-1 format must still be
 * recognized as legacy (and trigger {@code upgradeEncoding}). Legacy verification itself
 * is the responsibility of {@link LegacyAwareDaoAuthenticationProvider}, which can see the
 * username from UserDetails — see {@code LegacyAwareDaoAuthenticationProviderTest}.
 */
public class GemmaLegacyAwarePasswordEncoderTest {

    private final GemmaLegacyAwarePasswordEncoder encoder = new GemmaLegacyAwarePasswordEncoder();

    @Test
    public void legacyHash_isRecognizedAsLegacyFormat() {
        // from init-data.sql line 11: user 'administrator' with password 'administrator'
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        assertTrue( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( stored ) );
    }

    @Test
    public void legacyHash_isFlaggedForUpgrade() {
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        assertTrue( encoder.upgradeEncoding( stored ),
                "legacy hash should trigger re-encoding on next successful login" );
    }

    @Test
    public void legacyHash_matchesIsFailClosed() {
        // matches() cannot verify legacy hashes (no username available) — fail closed.
        String stored = "b7338dcc17d6b6c199a75540aab6d0506567b980";
        assertFalse( encoder.matches( "administrator", stored ),
                "encoder.matches must not verify legacy hashes (no username channel) — "
                        + "LegacyAwareDaoAuthenticationProvider handles legacy verification before delegating" );
    }

    @Test
    public void encode_producesBcryptPrefixed_andMatchesBack() {
        String raw = "hunter2";
        String encoded = encoder.encode( raw );
        assertTrue( encoded.startsWith( GemmaLegacyAwarePasswordEncoder.BCRYPT_PREFIX ),
                "new encodings must carry the {bcrypt} prefix so the encoder can route them" );
        assertTrue( encoder.matches( raw, encoded ) );
        assertFalse( encoder.matches( "wrong", encoded ) );
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
        assertFalse( encoder.matches( "anything", null ) );
    }

    @Test
    public void isLegacySha1Hex_rejectsBcryptAndOther() {
        assertFalse( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( null ) );
        assertFalse( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( "" ) );
        assertFalse( GemmaLegacyAwarePasswordEncoder.isLegacySha1Hex( "too-short" ) );
        // 40 chars but contains non-hex
        assertFalse( GemmaLegacyAwarePasswordEncoder
                .isLegacySha1Hex( "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz" ) );
        // bcrypt: definitely not 40-char-hex
        assertFalse( GemmaLegacyAwarePasswordEncoder
                .isLegacySha1Hex( encoder.encode( "x" ) ) );
    }

    @Test
    public void sha1HexUsernameSalt_helperMatchesFixtures() {
        // Document the salt format directly — these are the exact stored hashes in
        // init-data.sql, so production rows can be re-derived from raw password + username.
        assertEquals( "b7338dcc17d6b6c199a75540aab6d0506567b980",
                GemmaLegacyAwarePasswordEncoder.sha1HexUsernameSalt( "administrator", "administrator" ) );
        assertEquals( "2db458c67b4b52bba0184611c302c9c174ce8de4",
                GemmaLegacyAwarePasswordEncoder.sha1HexUsernameSalt( "XXXXXXXX", "gemmaAgent" ) );
    }

    @Test
    public void constantTimeHexEquals_caseInsensitive() {
        assertTrue( GemmaLegacyAwarePasswordEncoder
                .constantTimeHexEquals( "abcdef", "ABCDEF" ) );
        assertFalse( GemmaLegacyAwarePasswordEncoder
                .constantTimeHexEquals( "abcdef", "abcde0" ) );
        assertFalse( GemmaLegacyAwarePasswordEncoder
                .constantTimeHexEquals( "abc", "abcd" ) );
        assertFalse( GemmaLegacyAwarePasswordEncoder
                .constantTimeHexEquals( null, "abc" ) );
    }
}
