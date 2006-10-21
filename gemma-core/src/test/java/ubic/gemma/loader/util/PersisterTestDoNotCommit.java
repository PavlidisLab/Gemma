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
package ubic.gemma.loader.util;

import org.hibernate.SessionFactory;
import org.hibernate.TransientObjectException;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests meant to exercise and investigate hibernate persistence.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PersisterTestDoNotCommit extends BaseSpringContextTest {

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
    }

    /**
     * Within a transaction / session , create a new object, persist it, and then copy the id over to another object.
     */
    public void testSessionCacheIdTransfer() throws Exception {
        SessionFactory sessionFactory = ( SessionFactory ) this.getBean( "sessionFactory" );

        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setName( "goobly" );

        persisterHelper.persist( ba );

        int numArrayDesigns = 1;
        // assignment
        for ( int i = 0; i < numArrayDesigns; i++ ) {
            ArrayDesign a = ArrayDesign.Factory.newInstance();
            a.setName( "_" + i );

            try {
                a = ( ArrayDesign ) persisterHelper.persist( a );
                // fail( "Should have gotten a ArrayDesignServiceException" );
            } catch ( Exception e ) {
                log.error( "Got an exception when doing " + i, e );
                if ( !( e.getCause() instanceof TransientObjectException ) ) {
                    fail( "Cause should have been a TransientObjectException, got a "
                            + e.getCause().getClass().getName() );
                }
            }

            ba.setArrayDesignUsed( a );

            try {
                log.debug( sessionFactory.getCurrentSession().getIdentifier( a ) );
            } catch ( Exception e ) {
                fail( "Got an exception " + e.getMessage() );
            }
        }

        // id copying
        // for ( int i = 0; i < 10; i++ ) {
        // ArrayDesign a = ArrayDesign.Factory.newInstance();
        // a.setName( "_" + i );
        //
        // /*
        // * if we associate a transient object with the bioassay, and then try to find it, we get an error. Is this
        // * because the collection is persistent, but the instance in it is not?
        // */
        //
        // ba.getArrayDesignsUsed().add( a );
        //
        // ArrayDesign persistent = ( ( ArrayDesign ) persisterHelper.persist( a ) );
        //
        // a.setId( persistent.getId() );
        //
        // Session sess = sessionFactory.getCurrentSession();
        //
        // try {
        // log.debug( sess.getIdentifier( a ) );
        // fail( "Should have gotten a TransientObjectException because a is not in the session" );
        // } catch ( TransientObjectException e ) {
        // // ok
        // }
        //
        // }

    }
}
