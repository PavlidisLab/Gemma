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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 * @author keshav
 */
public class AclAuthorizationTest extends BaseSpringContextTest {

    private final String aDifferentUsername = "AclAuthTest_" + RandomStringUtils.insecure().nextAlphabetic( 5 );
    private final String arrayDesignName =
            "AD_" + RandomStringUtils.insecure().nextAlphabetic( 10 );
    private final String compositeSequenceName1 =
            "Design Element_" + RandomStringUtils.insecure().nextAlphabetic( 10 );
    private final String compositeSequenceName2 =
            "Design Element_" + RandomStringUtils.insecure().nextAlphabetic( 10 );

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MutableAclService aclService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;

    /* fixtures */
    private ArrayDesign arrayDesign;

    @Before
    public void setUp() throws Exception {

        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( arrayDesignName );
        arrayDesign.setShortName( arrayDesignName );
        arrayDesign.setDescription( "A test ArrayDesign from " + this.getClass().getName() );
        arrayDesign.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( compositeSequenceName1 );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( compositeSequenceName2 );

        Set<CompositeSequence> col = new HashSet<>();
        col.add( cs1 );
        col.add( cs2 );
        cs1.setArrayDesign( arrayDesign );
        cs2.setArrayDesign( arrayDesign );
        arrayDesign.setCompositeSequences( col );

        arrayDesign = ( ArrayDesign ) persisterHelper.persist( arrayDesign );// persister helper

        try {
            userManager.loadUserByUsername( aDifferentUsername );
        } catch ( UsernameNotFoundException e ) {
            userManager.createUser( new UserDetailsImpl( "foo", aDifferentUsername, true, null,
                    RandomStringUtils.insecure().nextAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }

    }

    /*
     * Tests getting composite sequences (target objects) with correct privileges on domain object (array design).
     */
    @Test
    public void testGetCompositeSequencesForArrayDesign() {

        Collection<CompositeSequence> col = compositeSequenceService.findByName( compositeSequenceName1 );

        assertTrue( "User should have been able to get the composite sequences", col.size() > 0 );

        securityService.makePrivate( arrayDesign );

        assertTrue( securityService.isPrivate( arrayDesign ) );

        /*
         * Exercises the AclAfterCollectionCompSeqByArrayDesignFilter
         */
        super.runAsUser( this.aDifferentUsername );
        col = compositeSequenceService.findByName( compositeSequenceName1 );

        assertEquals( "User should not be authorized to access composite sequences  for array design .", 0,
                col.size() );

        try {
            securityService.makePublic( arrayDesign );
            fail( "Should have gotten an access denied" );
        } catch ( AccessDeniedException ignored ) {

        }

        assertTrue( securityService.isPrivate( arrayDesign ) );
    }

    /*
     * Test modifying an arrayDesign with the correct authorization privileges. The security interceptor should be
     * called on this method, as should the AclInterceptor.
     *
     */
    @Test
    public void testEditArrayDesignDisallowed() {

        MutableAcl acl = this.getAcl( arrayDesign );

        securityService.makePrivate( arrayDesign );
        assertTrue( securityService.isPrivate( arrayDesign ) );

        super.runAsUser( this.aDifferentUsername );

        assertEquals( 1, SecurityContextHolder.getContext().getAuthentication().getAuthorities().size() );

        assertEquals( "GROUP_USER",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next()
                        .getAuthority() );

        try {
            arrayDesignService.update( arrayDesign );
            fail( "Should have gotten an access denied exception, acl was: " + acl );
        } catch ( AccessDeniedException ignored ) {

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
