/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security.authorization;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the SecurityService: making objects public or private and testing the permissions.
 * 
 * @author keshav
 * @version $Id$
 */
public class SecurityServiceTest extends BaseSpringContextTest {

    @Autowired
    private ArrayDesignService arrayDesignService;

    ArrayDesign arrayDesign;
    String arrayDesignName = "AD_" + RandomStringUtils.randomAlphabetic( 5 );
    String compositeSequenceName1 = "Design Element Bar1";
    String compositeSequenceName2 = "Design Element Bar2";

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AclService aclService;

    @Autowired
    private UserManager userManager;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    /*
     * (non-Javadoc)
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        // admin
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setShortName( arrayDesignName );
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

        arrayDesign = arrayDesignService.findOrCreate( arrayDesign );
    }

    @Test
    public void testUserCanEdit() {
        Collection<String> editableBy = securityService.editableBy( arrayDesign );
        assertTrue( editableBy.contains( "administrator" ) );
        assertTrue( !editableBy.contains( "gemmaAgent" ) );

        assertTrue( securityService.isEditableByUser( arrayDesign, "administrator" ) );
    }

    @Test
    public void testUserCanRead() {
        Collection<String> us = securityService.readableBy( arrayDesign );
        assertTrue( us.contains( "administrator" ) );
        assertTrue( us.contains( "gemmaAgent" ) );

        assertTrue( securityService.isViewableByUser( arrayDesign, "administrator" ) );
        assertTrue( securityService.isViewableByUser( arrayDesign, "gemmaAgent" ) );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testMakeExpressionExperimentPrivate() throws Exception {

        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();

        securityService.makePrivate( ee );

        assertTrue( "ExpressionExperiment not private, acl was: " + getAcl( ee ), securityService.isPrivate( ee ) );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertTrue( "BioAssay not private, acl was: " + getAcl( ba ), securityService.isPrivate( ba ) );
        }

        securityService.makePublic( ee );

        assertTrue( "ExpressionExperiment private, acl was: " + getAcl( ee ), securityService.isPublic( ee ) );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertTrue( "BioAssay not public, acl was: " + getAcl( ba ), securityService.isPublic( ba ) );
        }

    }

    private void makeUser( String username ) {
        try {
            userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", username, true, null, RandomStringUtils
                    .randomAlphabetic( 10 )
                    + "@gmail.com", "key", new Date() ) );
        }
    }

    @Test
    public void testMakeEEGroupReadWrite() throws Exception {

        ArrayDesign ee = super.getTestPersistentArrayDesign( 2, true );
        securityService.makePrivate( ee );

        String username = "first_" + randomName();
        String usertwo = "second_" + randomName();
        makeUser( username );
        makeUser( usertwo );

        securityService.makeOwnedByUser( ee, username );

        assertTrue( securityService.isEditableByUser( ee, username ) );

        this.runAsUser( username );

        /*
         * Create a group, do stuff...
         */
        String groupName = randomName();
        securityService.createGroup( groupName );
        securityService.makeWriteableByGroup( ee, groupName );

        /*
         * Add another user to the group.
         */

        securityService.addUserToGroup( usertwo, groupName );

        /*
         * Now, log in as another user.
         */
        this.runAsUser( usertwo );

        ee = arrayDesignService.load( ee.getId() );
        ee.setDescription( "woohoo, I can edit" );
        arrayDesignService.update( ee );
        // no exception == happy.

        this.runAsUser( username );
        securityService.makeUnreadableByGroup( ee, groupName );
        // should still work.
        ee = arrayDesignService.load( ee.getId() );

        this.runAsUser( usertwo );
        // should be locked out.
        try {
            ee = arrayDesignService.load( ee.getId() );
            fail( "Should have gotten 'access denied'" );
        } catch ( AccessDeniedException ok ) {

        }

    }

    /**
     * Tests changing object level security on the ArrayDesign from public to private WITHOUT the correct permission
     * (You need to be administrator).
     * 
     * @throws Exception
     */
    @Test
    public void testMakePrivateWithoutPermission() throws Exception {
        makeUser( "unauthorizedTestUser" );
        this.runAsUser( "unauthorizedTestUser" ); // test setup.

        ArrayDesign ad = arrayDesignService.findByName( arrayDesignName );

        try {
            securityService.makePrivate( ad );
            fail( "Should have gotten a unauthorized user exception" );
        } catch ( AccessDeniedException e ) {
            // ok.
        }
    }

    private MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            return ( MutableAcl ) aclService.readAclById( oi );
        } catch ( NotFoundException e ) {
            return null;
        }
    }

}
