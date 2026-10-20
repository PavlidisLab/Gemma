/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Opaque cursor for keyset pagination of REST collections.
 * <p>
 * A cursor encodes:
 * <ul>
 *     <li>{@code version} — payload format version, currently {@value #CURRENT_VERSION}</li>
 *     <li>{@code sortSpec} — the sort specification this cursor was emitted for, e.g.
 *         {@code "+id"} or {@code "+lastUpdated,+id"}. The server must validate this
 *         against the endpoint's allowed sorts before using the cursor.</li>
 *     <li>{@code keyTuple} — the last seen key values, one entry per sort component.
 *         Must always end in a unique tie-breaker (typically {@code id}).</li>
 *     <li>{@code direction} — {@link Direction#FORWARD} (next page) or
 *         {@link Direction#BACKWARD} (previous page).</li>
 * </ul>
 * <p>
 * The wire format is base64url-encoded JSON of the form:
 * <pre>
 *   {"v":1,"s":"+id","k":[12345],"d":"f"}
 * </pre>
 * with {@code "d":"f"} for forward and {@code "d":"b"} for backward. {@code "d"} is
 * omitted when the direction is forward (the default).
 * <p>
 * Cursors are not signed; this is acceptable because the {@code sortSpec} is
 * server-validated and access control is enforced by the ACL layer.
 *
 * @author phase3
 */
public final class Cursor {

    /**
     * Current cursor payload format version. Bump and add a migration when the JSON
     * shape changes incompatibly.
     */
    public static final int CURRENT_VERSION = 1;

    public enum Direction {
        FORWARD( "f" ),
        BACKWARD( "b" );

        private final String wire;

        Direction( String wire ) {
            this.wire = wire;
        }

        String wire() {
            return wire;
        }

        static Direction fromWire( @Nullable String wire ) {
            if ( wire == null || wire.equals( "f" ) ) {
                return FORWARD;
            }
            if ( wire.equals( "b" ) ) {
                return BACKWARD;
            }
            throw new IllegalArgumentException( "Unknown cursor direction: " + wire );
        }
    }

    private final int version;
    private final String sortSpec;
    private final Object[] keyTuple;
    private final Direction direction;

    /**
     * Build a cursor with the current payload format version.
     */
    public Cursor( String sortSpec, Object[] keyTuple, Direction direction ) {
        this( CURRENT_VERSION, sortSpec, keyTuple, direction );
    }

    /**
     * Build a cursor with an explicit payload format version. Intended for decoding;
     * callers constructing new cursors should use {@link #Cursor(String, Object[], Direction)}.
     */
    public Cursor( int version, String sortSpec, Object[] keyTuple, Direction direction ) {
        Objects.requireNonNull( sortSpec, "sortSpec" );
        Objects.requireNonNull( keyTuple, "keyTuple" );
        Objects.requireNonNull( direction, "direction" );
        if ( keyTuple.length == 0 ) {
            throw new IllegalArgumentException( "Cursor key tuple must contain at least one element." );
        }
        this.version = version;
        this.sortSpec = sortSpec;
        this.keyTuple = normalizeKeyTuple( keyTuple );
        this.direction = direction;
    }

    /**
     * Normalize integral {@link Number} key components to {@link Long} and
     * floating-point components to {@link Double}, so cursors compare equal across
     * encode/decode round-trips regardless of which numeric subtype the JSON parser
     * picked. Non-Number components pass through unchanged.
     */
    private static Object[] normalizeKeyTuple( Object[] in ) {
        Object[] out = new Object[in.length];
        for ( int i = 0; i < in.length; i++ ) {
            Object v = in[i];
            if ( v instanceof Number ) {
                Number n = ( Number ) v;
                if ( v instanceof Float || v instanceof Double ) {
                    out[i] = n.doubleValue();
                } else {
                    out[i] = n.longValue();
                }
            } else {
                out[i] = v;
            }
        }
        return out;
    }

    public int getVersion() {
        return version;
    }

    public String getSortSpec() {
        return sortSpec;
    }

    /**
     * @return a defensive copy of the key tuple; never null, never empty.
     */
    public Object[] getKeyTuple() {
        return keyTuple.clone();
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * Encode this cursor as a base64url-encoded JSON string (no padding).
     */
    public String encode() {
        JSONObject obj = new JSONObject();
        obj.put( "v", version );
        obj.put( "s", sortSpec );
        obj.put( "k", new JSONArray( keyTuple ) );
        if ( direction != Direction.FORWARD ) {
            obj.put( "d", direction.wire() );
        }
        byte[] bytes = obj.toString().getBytes( StandardCharsets.UTF_8 );
        return Base64.getUrlEncoder().withoutPadding().encodeToString( bytes );
    }

    /**
     * Decode a base64url-encoded cursor string.
     *
     * @throws IllegalArgumentException if the string is not valid base64url, not valid
     *                                  JSON, or is missing required fields. Callers in
     *                                  the REST layer should translate this into a
     *                                  {@code MalformedArgException} / 400 response.
     */
    public static Cursor decode( String encoded ) {
        Objects.requireNonNull( encoded, "encoded" );
        if ( encoded.isEmpty() ) {
            throw new IllegalArgumentException( "Cursor must not be empty." );
        }
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode( encoded );
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Cursor is not valid base64url.", e );
        }
        String json = new String( bytes, StandardCharsets.UTF_8 );
        JSONObject obj;
        try {
            obj = new JSONObject( json );
        } catch ( JSONException e ) {
            throw new IllegalArgumentException( "Cursor payload is not valid JSON.", e );
        }
        int version;
        String sortSpec;
        JSONArray keyArr;
        try {
            version = obj.getInt( "v" );
            sortSpec = obj.getString( "s" );
            keyArr = obj.getJSONArray( "k" );
        } catch ( JSONException e ) {
            throw new IllegalArgumentException( "Cursor payload missing required field (v/s/k).", e );
        }
        if ( version != CURRENT_VERSION ) {
            throw new IllegalArgumentException( "Unsupported cursor version: " + version
                    + " (expected " + CURRENT_VERSION + ")." );
        }
        if ( keyArr.length() == 0 ) {
            throw new IllegalArgumentException( "Cursor key tuple must contain at least one element." );
        }
        Object[] keyTuple = new Object[keyArr.length()];
        for ( int i = 0; i < keyArr.length(); i++ ) {
            keyTuple[i] = keyArr.get( i );
        }
        Direction direction;
        try {
            direction = Direction.fromWire( obj.optString( "d", null ) );
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Cursor has invalid direction.", e );
        }
        return new Cursor( version, sortSpec, keyTuple, direction );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof Cursor ) ) return false;
        Cursor other = ( Cursor ) o;
        return version == other.version
                && sortSpec.equals( other.sortSpec )
                && Arrays.equals( keyTuple, other.keyTuple )
                && direction == other.direction;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash( version, sortSpec, direction );
        result = 31 * result + Arrays.hashCode( keyTuple );
        return result;
    }

    @Override
    public String toString() {
        return "Cursor{v=" + version + ", s='" + sortSpec + "', k=" + Arrays.toString( keyTuple )
                + ", d=" + direction + "}";
    }
}
