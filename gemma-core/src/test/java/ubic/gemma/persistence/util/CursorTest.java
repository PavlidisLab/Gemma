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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.persistence.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CursorTest {

    @Test
    public void roundTripSingleIdForward() {
        Cursor c = new Cursor( "+id", new Object[]{ 12345L }, Cursor.Direction.FORWARD );
        String token = c.encode();
        Cursor back = Cursor.decode( token );
        assertEquals( c, back );
        assertEquals( "+id", back.getSortSpec() );
        assertEquals( Cursor.Direction.FORWARD, back.getDirection() );
        assertEquals( 1, back.getKeyTuple().length );
        // org.json deserializes JSON integers as Integer when small enough, Long otherwise.
        // 12345 fits in int, so it comes back as Integer. Numeric equality is what matters.
        assertEquals( 12345, ( ( Number ) back.getKeyTuple()[0] ).longValue() );
    }

    @Test
    public void roundTripCompoundKeyForward() {
        Cursor c = new Cursor( "+lastUpdated,+id",
                new Object[]{ "2024-01-15T12:00:00Z", 42 },
                Cursor.Direction.FORWARD );
        Cursor back = Cursor.decode( c.encode() );
        assertEquals( c, back );
        assertEquals( 2, back.getKeyTuple().length );
        assertEquals( "2024-01-15T12:00:00Z", back.getKeyTuple()[0] );
    }

    @Test
    public void roundTripBackward() {
        Cursor c = new Cursor( "+id", new Object[]{ 7L }, Cursor.Direction.BACKWARD );
        Cursor back = Cursor.decode( c.encode() );
        assertEquals( Cursor.Direction.BACKWARD, back.getDirection() );
        assertEquals( c, back );
    }

    @Test
    public void forwardDirectionIsOmittedFromWire() {
        // Compactness: don't emit "d":"f" when forward is the default.
        Cursor c = new Cursor( "+id", new Object[]{ 1 }, Cursor.Direction.FORWARD );
        String token = c.encode();
        String decoded = new String( Base64.getUrlDecoder().decode( token ), StandardCharsets.UTF_8 );
        assertTrue( decoded.contains( "\"v\":1" ), "missing version: " + decoded );
        assertTrue( decoded.contains( "\"s\":\"+id\"" ), "missing sort: " + decoded );
        assertTrue( decoded.contains( "\"k\":[1]" ), "missing key: " + decoded );
        assertEquals( false, decoded.contains( "\"d\":" ), "forward direction should be omitted: " + decoded );
    }

    @Test
    public void backwardDirectionIsInWire() {
        Cursor c = new Cursor( "+id", new Object[]{ 1 }, Cursor.Direction.BACKWARD );
        String decoded = new String( Base64.getUrlDecoder().decode( c.encode() ), StandardCharsets.UTF_8 );
        assertTrue( decoded.contains( "\"d\":\"b\"" ), "missing backward direction: " + decoded );
    }

    @Test
    public void encodedTokenIsBase64UrlSafe() {
        Cursor c = new Cursor( "+id", new Object[]{ 1L }, Cursor.Direction.FORWARD );
        String token = c.encode();
        // base64url alphabet only: A-Z a-z 0-9 - _ (no +, no /, no =)
        assertTrue( token.matches( "[A-Za-z0-9_-]+" ),
                "token contains non-base64url characters: " + token );
    }

    @Test
    public void rejectsEmptyKeyTupleInConstructor() {
        assertThrows( IllegalArgumentException.class,
                () -> new Cursor( "+id", new Object[]{}, Cursor.Direction.FORWARD ) );
    }

    @Test
    public void rejectsNullArgumentsInConstructor() {
        assertThrows( NullPointerException.class,
                () -> new Cursor( null, new Object[]{ 1 }, Cursor.Direction.FORWARD ) );
        assertThrows( NullPointerException.class,
                () -> new Cursor( "+id", null, Cursor.Direction.FORWARD ) );
        assertThrows( NullPointerException.class,
                () -> new Cursor( "+id", new Object[]{ 1 }, null ) );
    }

    @Test
    public void decodeRejectsEmpty() {
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( "" ) );
    }

    @Test
    public void decodeRejectsNull() {
        assertThrows( NullPointerException.class, () -> Cursor.decode( null ) );
    }

    @Test
    public void decodeRejectsNonBase64() {
        // '!' is not in the base64url alphabet
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( "not!valid!base64" ) );
    }

    @Test
    public void decodeRejectsNonJson() {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "not json".getBytes( StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( token ) );
    }

    @Test
    public void decodeRejectsMissingFields() {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "{\"v\":1}".getBytes( StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( token ) );
    }

    @Test
    public void decodeRejectsUnsupportedVersion() {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "{\"v\":99,\"s\":\"+id\",\"k\":[1]}"
                        .getBytes( StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( token ) );
    }

    @Test
    public void decodeRejectsEmptyKeyTupleInPayload() {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "{\"v\":1,\"s\":\"+id\",\"k\":[]}"
                        .getBytes( StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( token ) );
    }

    @Test
    public void decodeRejectsInvalidDirection() {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "{\"v\":1,\"s\":\"+id\",\"k\":[1],\"d\":\"sideways\"}"
                        .getBytes( StandardCharsets.UTF_8 ) );
        assertThrows( IllegalArgumentException.class, () -> Cursor.decode( token ) );
    }

    @Test
    public void keyTupleIsDefensivelyCopied() {
        Object[] orig = { 1, 2, 3 };
        Cursor c = new Cursor( "+id", orig, Cursor.Direction.FORWARD );
        orig[0] = 999;
        assertEquals( 1, ( ( Number ) c.getKeyTuple()[0] ).intValue() );
        // Also ensure getKeyTuple returns a copy, not the internal array.
        Object[] returned = c.getKeyTuple();
        returned[0] = 999;
        assertEquals( 1, ( ( Number ) c.getKeyTuple()[0] ).intValue() );
    }

    @Test
    public void equalsAndHashCode() {
        Cursor a = new Cursor( "+id", new Object[]{ 1L }, Cursor.Direction.FORWARD );
        Cursor b = new Cursor( "+id", new Object[]{ 1L }, Cursor.Direction.FORWARD );
        Cursor diff = new Cursor( "+id", new Object[]{ 2L }, Cursor.Direction.FORWARD );
        assertEquals( a, b );
        assertEquals( a.hashCode(), b.hashCode() );
        assertNotEquals( a, diff );
    }

    @Test
    public void toStringIsNonNull() {
        Cursor c = new Cursor( "+id", new Object[]{ 1 }, Cursor.Direction.FORWARD );
        assertNotNull( c.toString() );
        assertTrue( c.toString().contains( "+id" ) );
    }
}
