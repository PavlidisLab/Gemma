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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
    private MutableAclService mutableAclService;

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.BaseDependencyInjectionSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        // admin
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setShortName( arrayDesignName );
        arrayDesign.setName( arrayDesignName );
        arrayDesign.setDescription( "A test ArrayDesign from " + this.getClass().getName() );
        arrayDesign.setPrimaryTaxon( this.getTaxon( "human" ) );

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

    @Test
    public void testSetOwner() {
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        securityService.makePrivate( ee );

        String username = "first_" + randomName();
        makeUser( username );

        securityService.setOwner( ee, username );

        Sid owner = securityService.getOwner( ee );
        assertTrue( owner instanceof PrincipalSid );
        assertEquals( username, ( ( PrincipalSid ) owner ).getPrincipal() );

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
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }

    @Test
    public void testMakeEEGroupReadWrite() throws Exception {

        ArrayDesign entity = super.getTestPersistentArrayDesign( 2, true );
        securityService.makePrivate( entity );

        String username = "first_" + randomName();
        String usertwo = "second_" + randomName();
        makeUser( username );
        makeUser( usertwo );

        securityService.makeOwnedByUser( entity, username );

        assertTrue( securityService.isEditableByUser( entity, username ) );

        this.runAsUser( username );

        /*
         * Create a group, do stuff...
         */
        String groupName = randomName();
        securityService.createGroup( groupName );
        securityService.makeWriteableByGroup( entity, groupName );

        /*
         * Add another user to the group.
         */

        securityService.addUserToGroup( usertwo, groupName );

        /*
         * Now, log in as another user.
         */
        this.runAsUser( usertwo );

        entity = arrayDesignService.load( entity.getId() );
        entity.setDescription( "woohoo, I can edit" );
        arrayDesignService.update( entity );
        // no exception == happy.

        this.runAsUser( username );
        securityService.makeUnreadableByGroup( entity, groupName );
        // should still work.
        entity = arrayDesignService.load( entity.getId() );

        this.runAsUser( usertwo );
        // should be locked out.

        entity = arrayDesignService.load( entity.getId() );
        assertNull( entity );

        try {
            securityService.deleteGroup( groupName );
            fail( "Should have gotten 'access denied'" );
        } catch ( AccessDeniedException ok ) {

        }
        this.runAsUser( username );
        securityService.deleteGroup( groupName );

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

    /**
     * Tests that the same ACL can not be added to a securable object.
     * 
     * @throws Exception
     */
    @Test
    public void testDuplicateAcesNotAddedOnPrivateExpressionExperiment() throws Exception {
        // make private experiment
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        securityService.makePrivate( ee );
        // add user and add the user to the group
        String username = "salmonid" + randomName();
        String groupName = "fish" + randomName();
        makeUser( username );
        securityService.makeOwnedByUser( ee, username );
        assertTrue( securityService.isEditableByUser( ee, username ) );
        this.runAsUser( username );

        securityService.createGroup( groupName );

        MutableAcl acl = getAcl( ee );
        int numberOfAces = acl.getEntries().size();

        securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAdded = getAcl( ee );
        assertEquals( numberOfAces + 1, aclAfterReadableAdded.getEntries().size() );

        securityService.makeWriteableByGroup( ee, groupName );
        MutableAcl aclAfterWritableAdded = getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterWritableAdded.getEntries().size() );

        // this time the acl there and should not be added again
        securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAddedAgain = getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterReadableAddedAgain.getEntries().size() );

        // check writable too
        securityService.makeWriteableByGroup( ee, groupName );
        MutableAcl aclAfterWritableAddedAgain = getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterWritableAddedAgain.getEntries().size() );

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
        securityService.makePrivate( ee );

        // add user and add the user to a group
        String username = "salmonid";
        String groupName = "fish" + randomName();
        makeUser( username );
        securityService.makeOwnedByUser( ee, username );
        assertTrue( securityService.isEditableByUser( ee, username ) );
        this.runAsUser( username );
        securityService.createGroup( groupName );

        // get the basic acls
        MutableAcl acl = getAcl( ee );
        int numberOfAces = acl.getEntries().size();

        // make readable by group add first acl read for grouo and check added
        securityService.makeReadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAdded = getAcl( ee );
        assertEquals( numberOfAces + 1, aclAfterReadableAdded.getEntries().size() );

        // force the addition of duplicate ACL read, fish group on the same experiment
        List<GrantedAuthority> groupAuthorities = userManager.findGroupAuthorities( groupName );
        GrantedAuthority ga = groupAuthorities.get( 0 );
        aclAfterReadableAdded.insertAce( aclAfterReadableAdded.getEntries().size(), BasePermission.READ,
                new GrantedAuthoritySid( userManager.getRolePrefix() + ga ), true );
        mutableAclService.updateAcl( aclAfterReadableAdded );
        MutableAcl aclAfterReadableAddedDuplicate = getAcl( ee );
        assertEquals( numberOfAces + 2, aclAfterReadableAddedDuplicate.getEntries().size() );

        // delete the acl now and check removed both
        securityService.makeUnreadableByGroup( ee, groupName );
        MutableAcl aclAfterReadableAddedDuplicateRemoval = getAcl( ee );
        assertEquals( numberOfAces, aclAfterReadableAddedDuplicateRemoval.getEntries().size() );
        List<AccessControlEntry> entriesAfterDelete = aclAfterReadableAddedDuplicateRemoval.getEntries();
        assertEquals( numberOfAces, entriesAfterDelete.size() );

        // also check that the right acls check the principals
        Collection<String> principals = new ArrayList<String>();
        principals.add( "GrantedAuthoritySid[GROUP_ADMIN]" );
        principals.add( "GrantedAuthoritySid[GROUP_AGENT]" );
        principals.add( "PrincipalSid[salmonid]" );
        principals.add( "PrincipalSid[salmonid]" );

        for ( AccessControlEntry accessControl : entriesAfterDelete ) {
            Sid sid = accessControl.getSid();
            assertTrue( principals.contains( sid.toString() ) );
            // remove it once in case found in case of duplicates
            principals.remove( sid.toString() );
        }
        // clean up the groups
        userManager.deleteGroup( groupName );
        // userManager.deleteUser( username );
    }

    /**
     * Test to ensure that on creation of principal using a username that does not exist in system exception is thrown.
     * Principal ids are created in these method calls on SecurityService.
     */
    @Test
    public void testSetPrincialSID() {
        String username = "first_" + randomName();
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment();
        securityService.makePrivate( ee );

        try {
            securityService.setOwner( ee, username );
            fail();
        } catch ( Exception e ) {

        }

        try {
            securityService.makeOwnedByUser( ee, username );
            fail();
        } catch ( Exception e ) {

        }

    }

}
