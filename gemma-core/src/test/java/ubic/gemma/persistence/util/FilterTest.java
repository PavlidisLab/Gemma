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

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilterTest {

    @Test
    public void testParseString() {
        Filter of = Filter.parse( "ee", "id", String.class, Filter.Operator.greaterOrEq, "just a string" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( "just a string" );
        assertThat( of ).hasToString( "ee.id >= just a string" );
    }

    @Test
    public void testParseInteger() {
        Filter of = Filter.parse( "ee", "id", Integer.class, Filter.Operator.greaterOrEq, "12321" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( 12321 );
        assertThat( of ).hasToString( "ee.id >= 12321" );
    }

    @Test
    public void testParseDouble() {
        Filter of = Filter.parse( "ee", "id", Double.class, Filter.Operator.greaterOrEq, "1.2" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( 1.2 );
    }

    @Test
    public void testParseBoolean() {
        Filter of = Filter.parse( "ee", "id", Boolean.class, Filter.Operator.greaterOrEq, "true" );
        assertThat( of.getRequiredValue() )
                .isEqualTo( true );
    }

    @Test
    public void testParseDate() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.greaterOrEq, "2021-10-01" );
        OffsetDateTime odf = OffsetDateTime.of( LocalDateTime.of( 2021, Month.OCTOBER, 1, 0, 0, 0 ), ZoneOffset.UTC );
        assertThat( ( Date ) of.getRequiredValue() ).isEqualTo( odf.toInstant() );
    }

    @Test
    public void testParseDateTime() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.greaterOrEq, "2021-10-01T22:00:03Z" );
        OffsetDateTime odf = OffsetDateTime.of( LocalDateTime.of( 2021, Month.OCTOBER, 1, 22, 0, 3 ), ZoneOffset.UTC );
        assertThat( ( Date ) of.getRequiredValue() ).isEqualTo( odf.toInstant() );
    }

    @Test
    public void testParseDateTimeWithoutZuluSuffix() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.greaterOrEq, "2021-10-01T22:00:03" );
        OffsetDateTime odf = OffsetDateTime.of( LocalDateTime.of( 2021, Month.OCTOBER, 1, 22, 0, 3 ), ZoneOffset.UTC );
        assertThat( ( Date ) of.getRequiredValue() ).isEqualTo( odf.toInstant() );
    }

    @Test
    public void testParseDateTimeWithNonUtcTimeZone() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.greaterOrEq, "2021-10-01T22:00:03+05:00" );
        OffsetDateTime odf = OffsetDateTime.of( LocalDateTime.of( 2021, Month.OCTOBER, 1, 22, 0, 3 ), ZoneOffset.ofHours( 5 ) );
        assertThat( ( Date ) of.getRequiredValue() )
                .isEqualTo( odf.toInstant() );
    }

    @Test
    public void testParseCollectionOfDates() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.in, "(2021-10-01, 2021-10-02)" );
        assertThat( of ).hasToString( "ee.lastUpdated in (2021-10-01T00:00:00.000+00:00, 2021-10-02T00:00:00.000+00:00)" );
    }

    @Test
    public void testParseCollectionOfDateTimes() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.in, "(2021-10-01T00:00:01, 2021-10-02T01:00:00Z)" );
        assertThat( of ).hasToString( "ee.lastUpdated in (2021-10-01T00:00:01.000+00:00, 2021-10-02T01:00:00.000+00:00)" );
    }

    @Test
    public void testParseMixtureOfDateAndDateTime() {
        Filter of = Filter.parse( "ee", "lastUpdated", Date.class, Filter.Operator.in, "(2021-10-01, 2021-10-02T01:00:00Z)" );
        assertThat( of ).hasToString( "ee.lastUpdated in (2021-10-01T00:00:00.000+00:00, 2021-10-02T01:00:00.000+00:00)" );
    }

    @Test
    public void testParseCollection() {
        Filter of = Filter.parse( "ee", "id", String.class, Filter.Operator.in, "(a, b, c)" );
        assertThat( of.getRequiredValue() )
                .isInstanceOf( Collection.class )
                .asList()
                .containsExactly( "a", "b", "c" );
    }

    @Test
    public void testParseInvalidCollection() {
        assertThatThrownBy( () -> Filter.parse( "ee", "id", Integer.class, Filter.Operator.in, "(1, 2, c)" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasCauseInstanceOf( ConversionFailedException.class );
    }

    @Test
    public void testParseUnsupportedType() {
        assertThatThrownBy( () -> Filter.parse( "ee", "id", Object.class, Filter.Operator.in, "unsupported type" ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testLike() {
        Filter of = Filter.by( "ee", "id", String.class, Filter.Operator.like, "abcd" );
        assertThat( of ).hasFieldOrPropertyWithValue( "requiredValue", "abcd" );
    }

    @Test
    public void testLikeWithInvalidPropertyType() {
        assertThatThrownBy( () -> Filter.by( "ee", "id", Integer.class, Filter.Operator.like, 1 ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testLikeWithInvalidRequiredValue() {
        assertThatThrownBy( () -> Filter.by( "ee", "id", String.class, Filter.Operator.like, 1 ) )
                .isInstanceOf( ( IllegalArgumentException.class ) );
    }

    @Test
    public void testGeqWithNullRequiredValue() {
        assertThatThrownBy( () -> Filter.by( "ee", "id", String.class, Filter.Operator.greaterOrEq, null ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testPrimitiveTypeToWrapperConversion() {
        Filter.by( "ee", "id", Integer.class, Filter.Operator.greaterOrEq, 1 );
    }

    @Test
    public void testWrapperToPrimitiveConversion() {
        Filter.by( "ee", "id", int.class, Filter.Operator.greaterOrEq, Integer.valueOf( 1 ) );
    }

    @Test
    public void testInvalidCollectionTypeConversion() {
        assertThatThrownBy( () -> Filter.by( "ee", "id", String.class, Filter.Operator.in, Arrays.asList( 1, 2, 3 ) ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}