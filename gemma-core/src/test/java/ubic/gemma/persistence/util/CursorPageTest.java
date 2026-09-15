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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CursorPageTest {

    @Test
    public void carriesListAndMetadata() {
        Cursor next = new Cursor( "+id", new Object[]{ 3 }, Cursor.Direction.FORWARD );
        CursorPage<String> page = new CursorPage<>(
                Arrays.asList( "a", "b", "c" ),
                null, 20, next.encode(), null, null );

        assertEquals( 3, page.size() );
        assertEquals( "a", page.get( 0 ) );
        assertEquals( Integer.valueOf( 20 ), page.getLimit() );
        assertEquals( next.encode(), page.getNextCursor() );
        assertNull( page.getPrevCursor() );
        assertNull( page.getTotalElements() );
        assertNull( page.getSort() );
    }

    @Test
    public void carriesOptionalTotalElements() {
        CursorPage<String> page = new CursorPage<>(
                Collections.singletonList( "a" ), null, 20, null, null, 42L );
        assertEquals( Long.valueOf( 42L ), page.getTotalElements() );
    }

    @Test
    public void emptyPageWorks() {
        CursorPage<String> page = new CursorPage<>(
                Collections.emptyList(), null, 20, null, null, null );
        assertTrue( page.isEmpty() );
        assertNull( page.getNextCursor() );
    }

    @Test
    public void mapPreservesMetadata() {
        CursorPage<Integer> page = new CursorPage<>(
                Arrays.asList( 1, 2, 3 ), null, 20, "next-token", "prev-token", 99L );
        CursorPage<String> mapped = page.map( i -> "n=" + i );
        List<String> expected = Arrays.asList( "n=1", "n=2", "n=3" );
        assertEquals( expected, mapped );
        assertEquals( "next-token", mapped.getNextCursor() );
        assertEquals( "prev-token", mapped.getPrevCursor() );
        assertEquals( Long.valueOf( 99L ), mapped.getTotalElements() );
        assertEquals( Integer.valueOf( 20 ), mapped.getLimit() );
    }
}
