/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.util;

import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import ubic.gemma.persistence.service.ObjectFilterException;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ObjectFilterTest {

    @Test
    public void testParseString() throws ObjectFilterException {
        ObjectFilter of = ObjectFilter.parseObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.greaterOrEq, "just a string" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( "just a string" );
    }

    @Test
    public void testParseInteger() throws ObjectFilterException {
        ObjectFilter of = ObjectFilter.parseObjectFilter( "ee", "id", Integer.class, ObjectFilter.Operator.greaterOrEq, "12321" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( 12321 );
    }

    @Test
    public void testParseDouble() throws ObjectFilterException {
        ObjectFilter of = ObjectFilter.parseObjectFilter( "ee", "id", Double.class, ObjectFilter.Operator.greaterOrEq, "1.2" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( 1.2 );
    }

    @Test
    public void testParseBoolean() throws ObjectFilterException {
        ObjectFilter of = ObjectFilter.parseObjectFilter( "ee", "id", Boolean.class, ObjectFilter.Operator.greaterOrEq, "true" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( true );
    }

    @Test
    public void testParseCollection() throws ObjectFilterException {
        ObjectFilter of = ObjectFilter.parseObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.in, "(a, b, c)" );
        assertThat( of.getRequiredValue() )
                .isInstanceOf( Collection.class )
                .asList()
                .containsExactly( "a", "b", "c" );
    }

    @Test
    public void testParseInvalidCollection() {
        assertThatThrownBy( () -> ObjectFilter.parseObjectFilter( "ee", "id", Integer.class, ObjectFilter.Operator.in, "(1, 2, c)" ) )
                .isInstanceOf( ObjectFilterException.class )
                .hasCauseInstanceOf( ConversionFailedException.class );
    }

    @Test
    public void testParseUnsupportedType() {
        assertThatThrownBy( () -> ObjectFilter.parseObjectFilter( "ee", "id", Object.class, ObjectFilter.Operator.in, "unsupported type" ) )
                .isInstanceOf( ObjectFilterException.class )
                .hasCauseInstanceOf( ConversionFailedException.class );
    }

    @Test
    public void testLike() {
        ObjectFilter of = new ObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.like, "abcd" );
        assertThat( of ).hasFieldOrPropertyWithValue( "requiredValue", "abcd" );
    }

    @Test
    public void testLikeWithInvalidPropertyType() {
        assertThatThrownBy( () -> new ObjectFilter( "ee", "id", Integer.class, ObjectFilter.Operator.like, 1 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testLikeWithInvalidRequiredValue() {
        assertThatThrownBy( () -> new ObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.like, 1 ) )
                .isInstanceOf( ( IllegalArgumentException.class ) );
    }

    @Test
    public void testGeqWithNullRequiredValue() {
        assertThatThrownBy( () -> new ObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.greaterOrEq, null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testPrimitiveTypeToWrapperConversion() {
        new ObjectFilter( "ee", "id", Integer.class, ObjectFilter.Operator.greaterOrEq, 1 );
    }

    @Test
    public void testWrapperToPrimitiveConversion() {
        new ObjectFilter( "ee", "id", int.class, ObjectFilter.Operator.greaterOrEq, Integer.valueOf( 1 ) );
    }

    @Test
    public void testInvalidCollectionTypeConversion() {
        assertThatThrownBy( () -> new ObjectFilter( "ee", "id", String.class, ObjectFilter.Operator.in, Arrays.asList( 1, 2, 3 ) ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}
