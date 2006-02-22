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
package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignDaoImplTest extends BaseDAOTestCase {
    ArrayDesign ad;
    ArrayDesignDao dao;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        dao = ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" );
        ad = ArrayDesign.Factory.newInstance();

        ad.setName( "foobly" );

        Reporter r1 = Reporter.Factory.newInstance();
        r1.setName( "rfoo" );
        Reporter r2 = Reporter.Factory.newInstance();
        r2.setName( "rbar" );
        Reporter r3 = Reporter.Factory.newInstance();
        r3.setName( "rfar" );
        ad.getReporters().add( r1 );
        ad.getReporters().add( r2 );
        ad.getReporters().add( r3 );

        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( "cfoo" );
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        c2.setName( "cbar" );
        ad.getCompositeSequences().add( c1 );
        ad.getCompositeSequences().add( c2 );

        ad = ( ArrayDesign ) this.getPersisterHelper().persist( ad );

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        dao.remove( ad );
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoImpl.numCompositeSequences(ArrayDesign)'
     */
    public void testNumCompositeSequencesArrayDesign() {
        Integer actualValue = dao.numCompositeSequences( ad.getId() );
        Integer expectedValue = 2;
        assertEquals( expectedValue, actualValue );
    }

    /*
     * Test method for 'edu.columbia.gemma.expression.arrayDesign.ArrayDesignDaoImpl.numReporters(ArrayDesign)'
     */
    public void testNumReportersArrayDesign() {
        Integer actualValue = dao.numReporters( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    public void testLoadCompositeSequences() {
        Collection actualValue = dao.loadCompositeSequences( ad.getId() );
        assertEquals( 2, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof CompositeSequence );
    }

    public void testLoadReporters() {
        Collection actualValue = dao.loadReporters( ad.getId() );
        assertEquals( 3, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof Reporter );
    }

}
