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
package ubic.gemma.security.authorization.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;
import gemma.gsec.authentication.UserService;

import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests of ACL management: adding and removing from objects during CRUD operations. (AclAdvice)
 * 
 * @author keshav
 * @author paul
 * @version $Id$
 */
public class AclAdviceTest extends BaseSpringContextTest {

    @Autowired
    private AclService aclService;

    @Autowired
    private AclTestUtils aclTestUtils;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @Test
    public void testSecuredNotChild() throws Exception {
        String groupName = randomName();
        securityService.createGroup( groupName );
        UserGroup g = ( UserGroup ) userService.findGroupByName( groupName );
        aclTestUtils.checkHasAcl( g );
        aclTestUtils.checkHasAces( g );
        userService.delete( g );
        aclTestUtils.checkDeletedAcl( g );
    }

    /**
     * Create Array design, check ACLs are put on correctly and removed when the design is removed. Array Designs are
     * _simple_ compared to EEs!
     * 
     * @throws Exception
     */
    @Test
    public void testArrayDesignAcls() throws Exception {
        ArrayDesign ad = this.getTestPersistentArrayDesign( 2, true, false, false ); // need to modify

        aclTestUtils.checkHasAcl( ad );
        aclTestUtils.checkHasAces( ad );

        Sid owner = securityService.getOwner( ad );
        assertEquals( "administrator", ( ( AclPrincipalSid ) owner ).getPrincipal() );

        arrayDesignService.remove( ad );

        aclTestUtils.checkDeletedAcl( ad );

    }

    /**
     * @throws Exception
     */
    @Test
    public void testSignup() throws Exception {
        try {
            this.runAsAnonymous();
            String userName = "testuser" + RandomStringUtils.randomAlphabetic( 3 );
            this.makeUser( userName );
            this.runAsUser( userName );
        } finally {
            this.runAsAdmin();
        }
    }

    @Test
    public void testArrayDesignAclsUser() throws Exception {

        String userName = "testuser" + RandomStringUtils.randomAlphabetic( 3 );
        this.makeUser( userName );
        this.runAsUser( userName );
        ArrayDesign ad = this.getTestPersistentArrayDesign( 2, true, false, false );

        aclTestUtils.checkHasAcl( ad );
        aclTestUtils.checkHasAces( ad );

        Sid owner = securityService.getOwner( ad );
        assertEquals( userName, ( ( AclPrincipalSid ) owner ).getPrincipal() );

        arrayDesignService.update( ad );
        assertEquals( userName, ( ( AclPrincipalSid ) owner ).getPrincipal() );

        arrayDesignService.remove( ad );

        aclTestUtils.checkDeletedAcl( ad );

    }

    @Test
    public void testNumExperiments() {

        this.runAsAdmin();
        ArrayDesign ad = super.getTestPersistentArrayDesign( 10, true );
        ExpressionExperiment ee = super.getTestPersistentBasicExpressionExperiment( ad );

        securityService.makePrivate( ee );

        ExpressionExperiment ee2 = super.getTestPersistentBasicExpressionExperiment( ad );

        securityService.makePublic( ee2 );

        // admin can see everything
        assertEquals( 2, arrayDesignService.getExpressionExperiments( ad ).size() );

        // anonymous can only see the public set
        this.runAsAnonymous();
        assertEquals( 1, arrayDesignService.numExperiments( ad ) );

        // make the other data set public too...
        this.runAsAdmin();
        securityService.makePublic( ee );

        // anonymous can see both
        this.runAsAnonymous();
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

        // logged-in user can also see both
        String user = RandomStringUtils.randomAlphabetic( 10 );
        makeUser( user );
        this.runAsUser( user );
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

        // make data set private
        this.runAsAdmin();
        securityService.makePrivate( ee );
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

        // user can't see data set that is now private
        this.runAsUser( user );
        assertEquals( 1, arrayDesignService.numExperiments( ad ) );

        // make the data set owned by user; now they can see both that one and the public one
        this.runAsAdmin();
        securityService.setOwner( ee, user );
        this.runAsUser( user );
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

        // anonymous can only see the public one.
        this.runAsAnonymous();
        assertEquals( 1, arrayDesignService.numExperiments( ad ) );

        // create a new user group, add user to it, make ee2 private to group
        this.runAsAdmin();
        String group = RandomStringUtils.randomAlphabetic( 10 );
        securityService.createGroup( group );
        securityService.addUserToGroup( user, group );
        securityService.makeReadableByGroup( ee2, group );
        securityService.makePrivate( ee2 );
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

        // anonymous can't see private groups
        this.runAsAnonymous();
        assertEquals( 0, arrayDesignService.numExperiments( ad ) );

        // user can view experiment he owns as well as one shared with him
        this.runAsUser( user );
        assertEquals( 2, arrayDesignService.numExperiments( ad ) );

    }

    private void makeUser( String username ) {
        try {
            this.userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", username, true, null, RandomStringUtils
                    .randomAlphabetic( 10 ) + "@gmail.com", "key", new Date() ) );
        }
    }

    /**
     * Test of EE ACLs and also SecurityNotInherited on EE set.
     * 
     * @throws Exception
     */
    @Test
    public void testExpressionExperimentAcls() throws Exception {

        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );

        aclTestUtils.checkEEAcls( ee );

        /*
         * Make public, and then add a factor and factorvalue.
         */
        securityService.makePublic( ee );

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        VocabCharacteristic cat = VocabCharacteristic.Factory.newInstance();
        cat.setCategory( "foo" );
        cat.setCategoryUri( "bar" );
        ef.setName( "TESTING ACLS" );
        ef.setCategory( cat );
        ef.setType( FactorType.CATEGORICAL );
        ef = expressionExperimentService.addFactor( ee, ef );

        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setValue( "ack" );
        fv = FactorValue.Factory.newInstance( ef );
        fv.setValue( "adddck" );
        expressionExperimentService.addFactorValue( ee, fv );

        securityService.makePrivate( ee );

        aclTestUtils.checkEEAcls( ee );

        /*
         * Now associate with ee set, delete the set and then the ee, make sure things are done correctly!
         */

        ExpressionExperimentSet ees = ExpressionExperimentSet.Factory.newInstance();
        ees.getExperiments().add( ee );
        ees.setName( randomName() );

        persisterHelper.persist( ees );

        // make sure the ACL for objects are there (throws an exception if not).

        Acl eeacl = aclService.readAclById( new AclObjectIdentity( ee ) );
        aclService.readAclById( new AclObjectIdentity( ees ) );

        assertNull( eeacl.getParentAcl() );

        expressionExperimentSetService.delete( ees );

        // make sure ACL for ees is gone
        aclTestUtils.checkDeletedAcl( ees );

        // make sure the ACL for ee is still there
        aclTestUtils.checkHasAcl( ee );

        expressionExperimentService.delete( ee );

        aclTestUtils.checkDeleteEEAcls( ee );

    }

    @Test
    public void testAnalysisAcl() {

        /*
         * Create an analysis, add a result set, persist. Problem is: what if we do things in a funny order. Must call
         * update on the Analysis.
         */
        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis diffExpressionAnalysis = config.toAnalysis();

        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( diffExpressionAnalysis );
        resultSet.setExperimentalFactors( ee.getExperimentalDesign().getExperimentalFactors() );

        diffExpressionAnalysis.getResultSets().add( resultSet );

        diffExpressionAnalysis.setExperimentAnalyzed( ee );

        diffExpressionAnalysis = differentialExpressionAnalyzerService.persistAnalysis( ee, diffExpressionAnalysis,
                config );

        aclTestUtils.checkHasAcl( ee );
        aclTestUtils.checkHasAcl( diffExpressionAnalysis );
        aclTestUtils.checkLacksAcl( resultSet );

        aclTestUtils.checkHasAces( ee );
        aclTestUtils.checkLacksAces( diffExpressionAnalysis );
        aclTestUtils.checkHasAclParent( diffExpressionAnalysis, ee );

    }

    /**
     * Test that when a new associated object is persisted by a cascade, it gets the correct permissions of the parent
     * object
     * 
     * @throws Exception
     */
    @Test
    public void testUpdateAcl() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        aclTestUtils.checkEEAcls( ee );

        ExperimentalDesign ed = ee.getExperimentalDesign();

        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ed );
        String efName = "acladdtest_" + randomName();
        ef.setName( efName );
        ef.setDescription( "I am a factor" );

        ed.getExperimentalFactors().add( ef );

        experimentalDesignService.update( ed );

        assertEquals( 3, ee.getExperimentalDesign().getExperimentalFactors().size() );

        aclTestUtils.checkEEAcls( ee );

        ee = expressionExperimentService.load( ee.getId() );

        assertNotNull( ee );

        ee.setShortName( randomName() );
        expressionExperimentService.update( ee );

        aclTestUtils.checkEEAcls( ee );

        expressionExperimentService.delete( ee );

        aclTestUtils.checkDeleteEEAcls( ee );
    }

}
