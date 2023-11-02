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

import junit.framework.TestCase;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author mjacobson
 */
public class CharacteristicValueObjectTest extends TestCase {

    private CharacteristicValueObject a;
    private CharacteristicValueObject b;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        a = new CharacteristicValueObject();
        b = new CharacteristicValueObject();
    }

    public void testEqualsA() {
        TestCase.assertTrue( a.equals( b ) );
    }

    public void testEqualsB() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        TestCase.assertFalse( a.equals( b ) );
    }

    public void testEqualsC() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        TestCase.assertTrue( a.equals( b ) );
    }

    public void testEqualsD() {
        a.setValue( "foo" );
        b.setValue( "bar" );
        TestCase.assertFalse( a.equals( b ) );
    }

    public void testEqualsE() {
        a.setValue( "foo" );
        b.setValue( "foo" );
        TestCase.assertTrue( a.equals( b ) );
    }

    public void testEqualsF() {
        a.setValueUri( "foo" );
        b.setValueUri( "bar" );
        a.setValue( "foo" );
        b.setValue( "foo" );
        TestCase.assertFalse( a.equals( b ) );
    }

    public void testEqualsG() {
        a.setValueUri( "foo" );
        b.setValueUri( "foo" );
        a.setValue( "foo" );
        b.setValue( "bar" );
        TestCase.assertTrue( a.equals( b ) );
    }

    public void testCompareToNull() {
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertEquals( 0, a.compareTo( b ) );
    }

    public void testCompareToCategory() {
        a.setCategory( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );

        b.setCategory( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertEquals( 0, a.compareTo( b ) );

        b.setCategory( "zzz" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );
    }

    public void testCompareToTaxon() {
        a.setTaxon( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) > 0 );

        b.setTaxon( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertEquals( 0, a.compareTo( b ) );

        b.setTaxon( "zzz" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );
    }

    public void testCompareToValue() {
        a.setValue( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );

        b.setValue( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertEquals( 0, a.compareTo( b ) );

        b.setValue( "zzz" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );
    }

    public void testCompareToValueUri() {
        a.setValueUri( "aaa" );
        b.setValueUri( "aaa" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertEquals( 0, a.compareTo( b ) );

        b.setValueUri( "zzz" );
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );
    }

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
        TestCase.assertEquals( b.compareTo( a ), -a.compareTo( b ) );
        TestCase.assertTrue( a.compareTo( b ) < 0 );

        TestCase.assertEquals( b.compareTo( c ), -c.compareTo( b ) );
        TestCase.assertTrue( b.compareTo( c ) < 0 );

        TestCase.assertEquals( d.compareTo( c ), -c.compareTo( d ) );
        TestCase.assertTrue( c.compareTo( d ) < 0 );

        TestCase.assertEquals( d.compareTo( e ), -e.compareTo( d ) );
        TestCase.assertTrue( d.compareTo( e ) < 0 );

        // sorting
        List<CharacteristicValueObject> toSort = Arrays.asList( e, d, c, b, a );
        List<CharacteristicValueObject> expectedOrder = Arrays.asList( a, b, c, d, e );
        Collections.sort( toSort );
        for ( int i = 0; i < toSort.size(); i++ ) {
            TestCase.assertSame( expectedOrder.get( i ), toSort.get( i ) );
        }
    }
}
