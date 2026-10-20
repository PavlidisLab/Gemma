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
package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Argument representing an opaque cursor for keyset (cursor) pagination.
 * <p>
 * The wire format is a base64url-encoded JSON payload. Clients should treat the value
 * as opaque — pass back exactly what the server emitted in the previous response's
 * {@code nextCursor} / {@code prevCursor} field.
 *
 * @author phase3
 */
@Schema(type = "string",
        description = "An opaque cursor token returned in a previous response's nextCursor or prevCursor field, used for keyset pagination.")
public class CursorArg extends AbstractArg<Cursor> {

    private CursorArg( Cursor value ) {
        super( value );
    }

    public static CursorArg valueOf( String s ) throws MalformedArgException {
        if ( s == null ) {
            throw new MalformedArgException( "Cursor must not be null.", null );
        }
        try {
            return new CursorArg( Cursor.decode( s ) );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( "Cursor is malformed: " + e.getMessage(), e );
        }
    }
}
