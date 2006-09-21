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
package ubic.gemma.security;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Use this to test acegi functionality.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class SecurityIntegrationTest extends BaseTransactionalSpringContextTest {

    private ArrayDesignService arrayDesignService;

    ArrayDesign notYourArrayDesign;
    User testUser;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction(); // so we have authority to add a user.

        notYourArrayDesign = ArrayDesign.Factory.newInstance();
        notYourArrayDesign.setName( "deleteme" );
        notYourArrayDesign = ( ArrayDesign ) persisterHelper.persist( notYourArrayDesign );

        // // log in as a user who has limited permissions.
        // testUser = getTestPersistentUser( "foobly", "doobly" );
        // if ( this. ) {
        // throw new RuntimeException( "Failed to authenticate" );
        // }
    }

    /**
     * Test removing an arrayDesign with the correct authorization privileges. The security interceptor should be called
     * on this method, as should the AddOrRemoveFromACLInterceptor.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testRemoveArrayDesign() throws Exception {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 10 ) + "_array" );
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        arrayDesignService.remove( ad );
    }

    /**
     * Test removing an arrayDesign without the correct authorization. The security interceptor should be called on this
     * method, as should the AddOrRemoveFromACLInterceptor. You should get an AccessDeniedException.
     * 
     * @throws Exception
     */
    // FIXME - the superclass onSetUpInTransaction grants admin rights to the user.
    // public void testRemoveArrayDesignNotAuthorized() throws Exception {
    //
    // try {
    // arrayDesignService.remove( notYourArrayDesign );
    // fail( "Should have gotten an AccessDeniedException" );
    // } catch ( AccessDeniedException okay ) {
    // //
    // }
    // }
    /**
     * Save an array design.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testSaveArrayDesign() throws Exception {

        this.setFlushModeCommit();

        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "AD Foo1" );
        arrayDesign.setDescription( "a test ArrayDesign" );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( "DE Bar10" );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( "DE Bar20" );

        Collection<CompositeSequence> col = new HashSet<CompositeSequence>();
        col.add( cs1 );
        col.add( cs2 );

        /*
         * Note this sequence. Remember, inverse="true" if using this. If you do not make an explicit call to
         * cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
         */
        cs1.setArrayDesign( arrayDesign );
        cs2.setArrayDesign( arrayDesign );
        arrayDesign.setCompositeSequences( col );

        arrayDesign = arrayDesignService.findOrCreate( arrayDesign );

        assertNotNull( arrayDesign.getId() );

        //  
        // col = compositeSequenceService.getAllCompositeSequences();
        // if ( col.size() == 0 ) {
        // fail( "User not authorized for to access at least one of the objects in the graph" );
        // }
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}
