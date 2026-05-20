/*
 * The gemma project
 *
 * Copyright (c) 2017 University of British Columbia
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

package ubic.gemma.model.common.description;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mjacobson
 */
public class CharacteristicValueObjectTest {

    private CharacteristicValueObject a;
    private CharacteristicValueObject b;

    @BeforeEach
    protected void setUp() throws Exception {
        a = new CharacteristicValueObject();
        b = new CharacteristicValueObject();
    }

    @Test
    public void testEqualsA() {
        assertTrue( a.equals( b ) );
    }

    @Test
    public void testEqualsB() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        assertFalse( a.equals( b ) );
    }

    @Test
    public void testEqualsC() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        assertTrue( a.equals( b ) );
    }

    @Test
    public void testEqualsD() {
        a.setValue( "foo" );
        b.setValue( "bar" );
        assertFalse( a.equals( b ) );
    }

    @Test
    public void testEqualsE() {
        a.setValue( "foo" );
        b.setValue( "foo" );
        assertTrue( a.equals( b ) );
    }

    @Test
    public void testEqualsF() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        a.setValue( "foo" );
        b.setValue( "foo" );
        assertFalse( a.equals( b ) );
    }

    @Test
    public void testEqualsG() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        a.setValue( "foo" );
        b.setValue( "bar" );
        assertTrue( a.equals( b ) );
    }

    @Test
    public void testCompareToNull() {
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );
    }

    @Test
    public void testCompareToCategory() {
        a.setCategory( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );

        b.setCategory( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setCategory( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    @Test
    public void testCompareToTaxon() {
        a.setTaxon( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) > 0 );

        b.setTaxon( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setTaxon( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    @Test
    public void testCompareToValue() {
        a.setValue( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );

        b.setValue( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setValue( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    @Test
    public void testCompareToValueUri() {
        a.setValueUri( "aaa" );
        b.setValueUri( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setValueUri( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    @Test
    public void testCompareToOrdering() {
        // Order is category, taxon, value, valueUri
        CharacteristicValueObject c = new CharacteristicValueObject( 3L );
        CharacteristicValueObject d = new CharacteristicValueObject( 4L );
        CharacteristicValueObject e = new CharacteristicValueObject( 5L );

        a.setCategory( "aaa" );
        b.setCategory( "zzz" );
        c.setCategory( "zzz" );
        d.setCategory( "zzz" );
        e.setCategory( "zzz" );

        b.setTaxon( "aaa" );
        c.setTaxon( "zzz" );
        d.setTaxon( "zzz" );
        e.setTaxon( "zzz" );

        c.setValue( "aaa" );
        d.setValue( "zzz" );
        e.setValue( "zzz" );

        d.setValueUri( "aaa" );
        e.setValueUri( "zzz" );

        // order should be a, b, c, d, e
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );

        assertEquals( b.compareTo( c ), -c.compareTo( b ) );
        assertTrue( b.compareTo( c ) < 0 );

        assertEquals( d.compareTo( c ), -c.compareTo( d ) );
        assertTrue( c.compareTo( d ) < 0 );

        assertEquals( d.compareTo( e ), -e.compareTo( d ) );
        assertTrue( d.compareTo( e ) < 0 );

        // sorting
        List<CharacteristicValueObject> toSort = Arrays.asList( e, d, c, b, a );
        List<CharacteristicValueObject> expectedOrder = Arrays.asList( a, b, c, d, e );
        Collections.sort( toSort );
        for ( int i = 0; i < toSort.size(); i++ ) {
            assertSame( expectedOrder.get( i ), toSort.get( i ) );
        }
    }
}
