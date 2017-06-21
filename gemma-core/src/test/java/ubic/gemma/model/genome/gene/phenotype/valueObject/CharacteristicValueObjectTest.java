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

package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

/**
 * 
 * @author mjacobson
 */
public class CharacteristicValueObjectTest extends TestCase {

    CharacteristicValueObject a;
    CharacteristicValueObject b;

    public void testEqualsA() {
        assertTrue( a.equals( b ) );
    }

    public void testEqualsB() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        assertFalse( a.equals( b ) );
    }

    public void testEqualsC() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        assertTrue( a.equals( b ) );
    }

    public void testEqualsD() {
        a.setValue( "foo" );
        b.setValue( "bar" );
        assertFalse( a.equals( b ) );
    }

    public void testEqualsE() {
        a.setValue( "foo" );
        b.setValue( "foo" );
        assertTrue( a.equals( b ) );
    }

    public void testEqualsF() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        a.setValue( "foo" );
        b.setValue( "foo" );
        assertFalse( a.equals( b ) );
    }

    public void testEqualsG() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        a.setValue( "foo" );
        b.setValue( "bar" );
        assertTrue( a.equals( b ) );
    }

    public void testCompareToNull() {
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );
    }

    public void testCompareToCategory() {
        a.setCategory( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) > 0 );

        b.setCategory( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setCategory( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

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

    public void testCompareToValue() {
        a.setValue( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) > 0 );

        b.setValue( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setValue( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    public void testCompareToValueUri() {
        a.setValueUri( "aaa" );
        b.setValueUri( "aaa" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertEquals( 0, a.compareTo( b ) );

        b.setValueUri( "zzz" );
        assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        assertTrue( a.compareTo( b ) < 0 );
    }

    public void testCompareToOrdering() {
        // Order is category, taxon, value, valueUri
        CharacteristicValueObject c = new CharacteristicValueObject(3L);
        CharacteristicValueObject d = new CharacteristicValueObject(4L);
        CharacteristicValueObject e = new CharacteristicValueObject(5L);

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
        List<CharacteristicValueObject> toSort = Lists.newArrayList( e, d, c, b, a );
        Collections.sort( toSort );

        Long i = 1L;
        for ( CharacteristicValueObject cvo : toSort ) {
            assertEquals( i, cvo.getId() );
            i++;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        a = new CharacteristicValueObject(1L);
        b = new CharacteristicValueObject(2L);
    }
}
