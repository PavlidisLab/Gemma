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
import org.springframework.security.AccessDeniedException;
import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.security.acl.basic.jdbc.CustomAclDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Use this to test acegi functionality.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class SecurityIntegrationTest extends BaseSpringContextTest {

    private ArrayDesignService arrayDesignService;

    String username = "test";
    String aDifferentUsername = "aDifferentTestUser";

    ArrayDesign arrayDesign;
    String arrayDesignName = "Array Design Foo";
    String compositeSequenceName1 = "Design Element Bar1";
    String compositeSequenceName2 = "Design Element Bar2";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        /*
         * Note: You will not see the acl_permission and acl_object_identity in the database unless you add the method
         * invocaction to setComplete() at the end of this onSetUpInTransaction.
         */
        log.info( "Turn up the logging levels to DEBUG on the acegi and gemma security packages" );

        super.onSetUpInTransaction(); // admin
        // super.onSetUpInTransactionGrantingUserAuthority( username ); // user

        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( arrayDesignName );
        arrayDesign.setDescription( "A test ArrayDesign from " + this.getClass().getName() );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( compositeSequenceName1 );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( compositeSequenceName2 );

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

        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        arrayDesign = arrayDesignService.findOrCreate( arrayDesign );
    }

    /**
     * Tests changing the permission of the ArrayDesign from 6 (READ_WRITE) to 1 (ADMINISTRATION).
     * 
     * @throws Exception
     */
    public void testMakePrivate() throws Exception {
        ArrayDesign ad = arrayDesignService.findByName( arrayDesignName );
        SecurityService securityService = new SecurityService();
        securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.setCustomAclDao( ( CustomAclDao ) this.getBean( "customAclDao" ) );
        securityService.makePrivate( ad );
        /*
         * uncomment so you can see the acl permission has been changed in the database.
         */
        this.setComplete();
    }

    /**
     * Test removing an arrayDesign with the correct authorization privileges. The security interceptor should be called
     * on this method, as should the AclInterceptor.
     * 
     * @throws Exception
     */
    public void testRemoveArrayDesign() throws Exception {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 10 ) + "_array" );
        ad = arrayDesignService.findOrCreate( arrayDesign );
        // ad = ( ArrayDesign ) persisterHelper.persist( ad );
        arrayDesignService.remove( ad );
    }

    /**
     * Test removing an arrayDesign without the correct authorization. The security interceptor should be called on this
     * method, as should the AclInterceptor. You should get an AccessDeniedException.
     * 
     * @throws Exception
     */
    public void testRemoveArrayDesignNotAuthorized() throws Exception {

        this.onSetUpInTransactionGrantingUserAuthority( aDifferentUsername );// use a non-admin user

        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info( "user is " + obj.toString() );

        try {
            arrayDesignService.remove( arrayDesign );
            fail( "Should have gotten an AccessDeniedException" );
        } catch ( AccessDeniedException okay ) {
            log.info( "Access successfully denied." );
        }
    }

    /**
     * Tests getting composite sequences (target objects) with correct privileges on domain object (array design).
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testGetCompositeSequencesForArrayDesign() throws Exception {

        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info( "user is: " + obj.toString() );

        assertNotNull( arrayDesign.getId() );

        CompositeSequenceService compositeSequenceService = ( CompositeSequenceService ) this
                .getBean( "compositeSequenceService" );
        Collection col = compositeSequenceService.findByName( "Design Element Bar1" );
        if ( col.size() == 0 ) {
            fail( "User not authorized to access at least one of the objects in the graph" );
        }
    }

    /**
     * Tests getting composite sequences (target objects) without correct privileges on domain object (array design).
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testGetCompositeSequencesForArrayDesignWithoutAuthorization() throws Exception {

        this.onSetUpInTransactionGrantingUserAuthority( aDifferentUsername );// a different user

        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info( "user is: " + obj.toString() );

        assertNotNull( arrayDesign.getId() );

        CompositeSequenceService compositeSequenceService = ( CompositeSequenceService ) this
                .getBean( "compositeSequenceService" );
        Collection col = compositeSequenceService.findByName( compositeSequenceName1 );

        /*
         * expection is to not have access to the composite sequences for this array design when authenticated as
         * 'aDifferentUser'
         */
        assertTrue(
                "User should not be authorized to access target objects (composite sequences) in the graph for this domain object (array design).",
                col.isEmpty() );

    }

}
