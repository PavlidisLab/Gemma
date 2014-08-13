/*
 * The gemma-mda project
 * 
 * Copyright (c) 2014 University of British Columbia
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
 *
 */

package ubic.gemma.model.association.coexpression;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;

/**
 * @author paul
 * @version $Id$
 */
public class CoexpressionValueObjectTest {

    @Test
    public void testEqualsDespiteGeneSwap() {

        CoexpressionValueObject c1 = new CoexpressionValueObject( 1L, 2L, true, 2, 12L, new HashSet<Long>() );
        CoexpressionValueObject c2 = new CoexpressionValueObject( 2L, 1L, true, 2, 12L, new HashSet<Long>() );

        assertEquals( c1, c2 );

        assertEquals( c1.hashCode(), c2.hashCode() );

    }

    @Test
    public void testEqualsAsLongAsGenesAreSame() {

        // this is a pathological condition, just ensuring equals is not considering other fields.
        CoexpressionValueObject c1 = new CoexpressionValueObject( 1L, 2L, true, 2, 14L, new HashSet<Long>() );
        CoexpressionValueObject c2 = new CoexpressionValueObject( 1L, 2L, true, 5, 12L, new HashSet<Long>() );

        assertEquals( c1, c2 );

        assertEquals( c1.hashCode(), c2.hashCode() );

    }

    @Test
    public void testNotEqualsAsLongAsGenesAreDifferent() {

        CoexpressionValueObject c1 = new CoexpressionValueObject( 1L, 2L, true, 2, 14L, new HashSet<Long>() );
        CoexpressionValueObject c2 = new CoexpressionValueObject( 1L, 3L, true, 2, 14L, new HashSet<Long>() );

        assertTrue( !c1.equals( c2 ) );

        assertTrue( c1.hashCode() != c2.hashCode() );

    }

}
