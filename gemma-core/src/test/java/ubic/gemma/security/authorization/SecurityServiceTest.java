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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclPrincipalSid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.AclTestUtils;
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

    private ArrayDesign arrayDesign;
    private String arrayDesignName;
    private String compositeSequenceName1 = "Design Element Bar1";
    private String compositeSequenceName2 = "Design Element Bar2";

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private AclTestUtils aclTestUtils;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() {
        this.arrayDesignName = "AD_" + RandomStringUtils.randomAlphabetic( 5 );

        // admin
        this.arrayDesign = ArrayDesign.Factory.newInstance();
        this.arrayDesign.setShortName( this.arrayDesignName );
        this.arrayDesign.setName( this.arrayDesignName );
        this.arrayDesign.setDescription( "A test ArrayDesign from " + this.getClass().getName() );
        this.arrayDesign.setPrimaryTaxon( this.getTaxon( "human" ) );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( this.compositeSequenceName1 );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( this.compositeSequenceName2 );

        Collection<CompositeSequence> col = new HashSet<CompositeSequence>();
        col.add( cs1 );
        col.add( cs2 );

        /*
         * Note this sequence. Remember, inverse="true" if using this. If you do not make an explicit call to
         * cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
         */
        cs1.setArrayDesign( this.arrayDesign );
        cs2.setArrayDesign( this.arrayDesign );
        this.arrayDesign.setCompositeSequences( col );

        this.arrayDesign = this.arrayDesignService.create( this.arrayDesign );

    }

    /**
     * Tests that the same ACE can not be added twice to a securable object.
     * 
     * @throws Exception
     */
    @Test
    public void testDuplicateAcesNotAddedOnPrivateExpressionExperiment() throws Exception {
        // make private experiment
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        this.securityService.makePrivate( ee );
        // add user and add the user to the group
        String username = "salmonid" + randomName();
        String groupName = "fish" + randomName();
        makeUser( username );
        this.securityService.makeOwnedByUser( ee, username );
        assertTrue( this.securityService.isEditableByUser( ee, username ) );
        this.runAsUser( username );

        this.securityService.createGroup( groupName );

        MutableAcl acl = aclTestUtils.getAcl( ee );
        int numberOfAces = acl.getEntries().size();

        this.securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAdded = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 1, aclAfterReadableAdded.getEntries().size() );

        this.securityService.makeWriteableByGroup( ee, groupName );
        MutableAcl aclAfterWritableAdded = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterWritableAdded.getEntries().size() );

        // this time the acl there and should not be added again
        this.securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAddedAgain = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterReadableAddedAgain.getEntries().size() );

        // check writable too
        this.securityService.makeWriteableByGroup( ee, groupName );
        MutableAcl aclAfterWritableAddedAgain = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterWritableAddedAgain.getEntries().size() );

    }

    @Test
    public void testMakeEEGroupReadWrite() throws Exception {

        ArrayDesign entity = super.getTestPersistentArrayDesign( 2, true );
        this.securityService.makePrivate( entity );

        String username = "first_" + randomName();
        String usertwo = "second_" + randomName();
        makeUser( username );
        makeUser( usertwo );

        this.securityService.makeOwnedByUser( entity, username );

        assertTrue( this.securityService.isEditableByUser( entity, username ) );

        this.runAsUser( username );

        /*
         * Create a group, do stuff...
         */
        String groupName = randomName();
        this.securityService.createGroup( groupName );
        this.securityService.makeWriteableByGroup( entity, groupName );

        /*
         * Add another user to the group, which is owned by username
         */

        this.securityService.addUserToGroup( usertwo, groupName );

        /*
         * Now, log in as another user.
         */
        this.runAsUser( usertwo );

        entity = this.arrayDesignService.load( entity.getId() );
        entity.setDescription( "woohoo, I can edit" );
        this.arrayDesignService.update( entity );
        // no exception == happy.

        this.runAsUser( username );
        this.securityService.makeUnreadableByGroup( entity, groupName );
        // should still work.
        entity = this.arrayDesignService.load( entity.getId() );

        this.runAsUser( usertwo );
        // should be locked out.

        entity = this.arrayDesignService.load( entity.getId() );
        assertNull( entity );

        try {
            this.userManager.deleteGroup( groupName );
            fail( "Should have gotten 'access denied'" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }
        this.runAsUser( username );
        this.userManager.deleteGroup( groupName );

    }

    /**
     * @throws Exception
     */
    @Test
    public void testMakeExpressionExperimentPrivate() throws Exception {

        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();

        for ( int i = 0; i < 5; i++ ) {
            this.securityService.makePrivate( ee );

            assertTrue( "ExpressionExperiment not private, acl was: " + aclTestUtils.getAcl( ee ),
                    this.securityService.isPrivate( ee ) );

            for ( BioAssay ba : ee.getBioAssays() ) {
                assertTrue( "BioAssay not private, acl of ee was: " + aclTestUtils.getAcl( ee )
                        + "\nacl of bioassay was: " + aclTestUtils.getAcl( ba ), this.securityService.isPrivate( ba ) );
            }

            this.securityService.makePublic( ee );

            assertTrue( "ExpressionExperiment still private, acl was: " + aclTestUtils.getAcl( ee ),
                    this.securityService.isPublic( ee ) );

            for ( BioAssay ba : ee.getBioAssays() ) {
                assertTrue( "BioAssay not public, acl of ee was: " + aclTestUtils.getAcl( ee ),
                        this.securityService.isPublic( ba ) );
            }
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

        ArrayDesign ad = this.arrayDesignService.findByName( this.arrayDesignName ).iterator().next();

        try {
            this.securityService.makePrivate( ad );
            fail( "Should have gotten a unauthorized user exception" );
        } catch ( AccessDeniedException e ) {
            // ok.
        }
    }

    /**
     * Tests an unlikely scenario?? but if there is an acl that was duplicated with same principal, permission and
     * object then both acls can be deleted.
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveMultipleAcesFromPrivateExpressionExperiment() throws Exception {
        // make private experiment
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        this.securityService.makePrivate( ee );

        // add user and add the user to a group
        String username = "salmonid";
        String groupName = "fish" + randomName();
        makeUser( username );
        this.securityService.makeOwnedByUser( ee, username );
        assertTrue( this.securityService.isEditableByUser( ee, username ) );
        this.runAsUser( username );
        this.securityService.createGroup( groupName );

        // get the basic acls
        MutableAcl acl = aclTestUtils.getAcl( ee );
        int numberOfAces = acl.getEntries().size();

        // make readable by group add first ACE read for group and check added
        this.securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAdded = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 1, aclAfterReadableAdded.getEntries().size() );

        // force the addition of duplicate ACE read, fish group on the same experiment. Note that in the current
        // implementation this only adds one - we already avoid duplicates.
        List<GrantedAuthority> groupAuthorities = this.userManager.findGroupAuthorities( groupName );
        GrantedAuthority ga = groupAuthorities.get( 0 );
        aclAfterReadableAdded.insertAce( aclAfterReadableAdded.getEntries().size(), BasePermission.READ,
                new AclGrantedAuthoritySid( this.userManager.getRolePrefix() + ga ), true );
        this.aclTestUtils.update( aclAfterReadableAdded );
        MutableAcl aclAfterReadableAddedDuplicate = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces + 1, aclAfterReadableAddedDuplicate.getEntries().size() );

        // delete the ace now and check removed permission completely.
        this.securityService.makeUnreadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAddedDuplicateRemoval = aclTestUtils.getAcl( ee );
        assertEquals( numberOfAces, aclAfterReadableAddedDuplicateRemoval.getEntries().size() );
        List<AccessControlEntry> entriesAfterDelete = aclAfterReadableAddedDuplicateRemoval.getEntries();
        assertEquals( numberOfAces, entriesAfterDelete.size() );

        // also check that the right ACE check the principals
        Collection<String> principals = new ArrayList<String>();
        principals.add( "AclGrantedAuthoritySid[GROUP_ADMIN]" );
        principals.add( "AclGrantedAuthoritySid[GROUP_AGENT]" );
        principals.add( "AclPrincipalSid[salmonid]" );
        principals.add( "AclPrincipalSid[salmonid]" );

        for ( AccessControlEntry accessControl : entriesAfterDelete ) {
            Sid sid = accessControl.getSid();
            assertTrue( principals.contains( sid.toString() ) );
            // remove it once in case found in case of duplicates
            principals.remove( sid.toString() );
        }
        // clean up the groups
        this.userManager.deleteGroup( groupName );
        // userManager.deleteUser( username );
    }

    @Test
    public void testSetOwner() {
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        this.securityService.makePrivate( ee );

        String username = "first_" + randomName();
        makeUser( username );

        this.securityService.setOwner( ee, username );

        Sid owner = this.securityService.getOwner( ee );
        assertTrue( owner instanceof AclPrincipalSid );
        assertEquals( username, ( ( AclPrincipalSid ) owner ).getPrincipal() );

    }

    /**
     * Test to ensure that on creation of principal using a username that does not exist in system exception is thrown.
     * Principal ids are created in these method calls on SecurityService.
     */
    @Test
    public void testSetPrincialSID() {
        String username = "first_" + randomName();
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        this.securityService.makePrivate( ee );

        try {
            this.securityService.setOwner( ee, username );
            fail();
        } catch ( Exception e ) {

        }

        try {
            this.securityService.makeOwnedByUser( ee, username );
            fail();
        } catch ( Exception e ) {

        }

    }

    @Test
    public void testUserCanEdit() {
        Collection<String> editableBy = this.securityService.editableBy( this.arrayDesign );
        assertTrue( editableBy.contains( "administrator" ) );
        assertTrue( !editableBy.contains( "gemmaAgent" ) );

        assertTrue( this.securityService.isEditableByUser( this.arrayDesign, "administrator" ) );
    }

    @Test
    public void testUserCanRead() {
        Collection<String> us = this.securityService.readableBy( this.arrayDesign );
        assertTrue( us.contains( "administrator" ) );
        assertTrue( us.contains( "gemmaAgent" ) );

        assertTrue( this.securityService.isViewableByUser( this.arrayDesign, "administrator" ) );
        assertTrue( this.securityService.isViewableByUser( this.arrayDesign, "gemmaAgent" ) );
    }

    private void makeUser( String username ) {
        try {
            this.userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", username, true, null, RandomStringUtils
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }
}
