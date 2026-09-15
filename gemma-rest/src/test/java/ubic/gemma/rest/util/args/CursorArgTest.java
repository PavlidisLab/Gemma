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
package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CursorArgTest {

    @Test
    public void decodesValidCursor() {
        String token = new Cursor( "+id", new Object[]{ 42L }, Cursor.Direction.FORWARD ).encode();
        CursorArg arg = CursorArg.valueOf( token );
        Cursor c = arg.getValue();
        assertThat( c.getSortSpec() ).isEqualTo( "+id" );
        assertThat( c.getDirection() ).isEqualTo( Cursor.Direction.FORWARD );
        assertThat( c.getKeyTuple() ).hasSize( 1 );
    }

    @Test
    public void rejectsNullAsMalformed() {
        assertThatThrownBy( () -> CursorArg.valueOf( null ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void rejectsEmptyAsMalformed() {
        assertThatThrownBy( () -> CursorArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void rejectsGarbageAsMalformed() {
        assertThatThrownBy( () -> CursorArg.valueOf( "not!a!cursor" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void wrapsIllegalArgumentInMalformedArg() {
        // base64url-decodes to "not json"
        String token = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString( "not json".getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
        assertThatThrownBy( () -> CursorArg.valueOf( token ) )
                .isInstanceOf( MalformedArgException.class )
                .hasMessageContaining( "Cursor is malformed" );
    }
}
