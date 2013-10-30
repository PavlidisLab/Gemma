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
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.security.authentication.UserService;
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
        UserGroup g = userService.findGroupByName( groupName );
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

        aclTestUtils.checkEEAcls( ee );

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
