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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignDaoImplTest extends BaseTransactionalSpringContextTest {
    ArrayDesign ad;
    ArrayDesignDao arrayDesignDao;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        ad = ArrayDesign.Factory.newInstance();

        ad.setName( "foobly" );
        ad = ( ArrayDesign ) arrayDesignDao.create( ad );

        Reporter r1 = Reporter.Factory.newInstance();
        r1.setName( "rfoo" );
        Reporter r2 = Reporter.Factory.newInstance();
        r2.setName( "rbar" );
        Reporter r3 = Reporter.Factory.newInstance();
        r3.setName( "rfar" );

        r1.setArrayDesign( ad );
        r2.setArrayDesign( ad );
        r3.setArrayDesign( ad );

        ad.getReporters().add( r1 );
        ad.getReporters().add( r2 );
        ad.getReporters().add( r3 );

        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( "cfoo" );
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        c2.setName( "cbar" );
        CompositeSequence c3 = CompositeSequence.Factory.newInstance();
        c3.setName( "cbar" );

        c1.setArrayDesign( ad );
        c2.setArrayDesign( ad );
        c3.setArrayDesign( ad );

        c1.getComponentReporters().add( r1 );
        c2.getComponentReporters().add( r2 );
        c3.getComponentReporters().add( r3 );

        ad.getCompositeSequences().add( c1 );
        ad.getCompositeSequences().add( c2 );
        ad.getCompositeSequences().add( c3 );

    }

    public void testCascadeCreateCompositeSequences() {
        arrayDesignDao.update( ad ); // should cascade.
        flushSession(); // fails without this.
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();
        assertNotNull( cs.getId() );
        assertNotNull( cs.getArrayDesign().getId() );
    }

    public void testCascadeDeleteOrphanCompositeSequences() {
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();
        ad.getCompositeSequences().remove( cs );
        cs.setArrayDesign( null );
        arrayDesignDao.update( ad );
        assertEquals( 2, ad.getCompositeSequences().size() );
    }

    public void testCascadeDeleteOrphanReporters() {
        Reporter cs = ad.getReporters().iterator().next();
        ad.getReporters().remove( cs );
        cs.setArrayDesign( null );
        arrayDesignDao.update( ad );
        assertEquals( 2, ad.getReporters().size() );
    }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numCompositeSequences(ArrayDesign)'
     */
    public void testNumCompositeSequencesArrayDesign() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Integer actualValue = arrayDesignDao.numCompositeSequences( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numReporters(ArrayDesign)'
     */
    public void testNumReportersArrayDesign() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Integer actualValue = arrayDesignDao.numReporters( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    public void testLoadCompositeSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection actualValue = arrayDesignDao.loadCompositeSequences( ad.getId() );
        assertEquals( 3, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof CompositeSequence );
    }

    public void testLoadReporters() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection actualValue = arrayDesignDao.loadReporters( ad.getId() );
        assertEquals( 3, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof Reporter );
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

}
