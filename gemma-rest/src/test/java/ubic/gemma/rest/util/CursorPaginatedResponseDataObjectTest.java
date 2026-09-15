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
package ubic.gemma.rest.util;

import org.junit.jupiter.api.Test;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class CursorPaginatedResponseDataObjectTest {

    @Test
    public void copiesFieldsFromCursorPage() {
        CursorPage<String> page = new CursorPage<>(
                Arrays.asList( "a", "b" ), null, 20, "next-tok", "prev-tok", null );

        CursorPaginatedResponseDataObject<String> resp =
                new CursorPaginatedResponseDataObject<>( page, new String[]{ "id" } );

        assertThat( resp.getData() ).containsExactly( "a", "b" );
        assertThat( resp.getLimit() ).isEqualTo( 20 );
        assertThat( resp.getNextCursor() ).isEqualTo( "next-tok" );
        assertThat( resp.getPrevCursor() ).isEqualTo( "prev-tok" );
        assertThat( resp.getTotalElements() ).isNull();
        assertThat( resp.getGroupBy() ).containsExactly( "id" );
        assertThat( resp.getSort() ).isNull();
    }

    @Test
    public void totalElementsForwardedWhenPresent() {
        CursorPage<String> page = new CursorPage<>(
                Arrays.asList( "a" ), null, 20, null, null, 7L );
        CursorPaginatedResponseDataObject<String> resp =
                new CursorPaginatedResponseDataObject<>( page, new String[]{ "id" } );
        assertThat( resp.getTotalElements() ).isEqualTo( 7L );
        assertThat( resp.getNextCursor() ).isNull();
        assertThat( resp.getPrevCursor() ).isNull();
    }
}
