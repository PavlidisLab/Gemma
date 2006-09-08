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
        arrayDesignDao = ( ArrayDesignDao ) getBean( "arrayDesignDao" );

        // FIXME for some reason the getTestPersistentArrayDesign method doesn't work - get empty collections.
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

        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        // ad = this.getTestPersistentArrayDesign( 2, true );

    }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numCompositeSequences(ArrayDesign)'
     */
    public void testNumCompositeSequencesArrayDesign() {
        Integer actualValue = arrayDesignDao.numCompositeSequences( ad.getId() );
        Integer expectedValue = 2;
        assertEquals( expectedValue, actualValue );
    }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numReporters(ArrayDesign)'
     */
    public void testNumReportersArrayDesign() {
        Integer actualValue = arrayDesignDao.numReporters( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    public void testLoadCompositeSequences() {
        Collection actualValue = arrayDesignDao.loadCompositeSequences( ad.getId() );
        assertEquals( 2, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof CompositeSequence );
    }

    public void testLoadReporters() {
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
