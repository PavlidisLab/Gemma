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
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAclService;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
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
    MutableAclService aclService;

    @Autowired
    AclTestUtils aclTestUtils;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    ExperimentalDesignService experimentalDesignService;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionExperimentSetService expressionExperimentSetService;

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

        arrayDesignService.remove( ad );

        aclTestUtils.checkDeletedAcl( ad );

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
         * Now associate with ee set, delete the set and then the ee, make sure things are done correctly!
         */

        ExpressionExperimentSet ees = ExpressionExperimentSet.Factory.newInstance();
        ees.getExperiments().add( ee );
        ees.setName( randomName() );

        persisterHelper.persist( ees );

        // make sure the ACL for objects are there (throws an exception if not).

        Acl eeacl = aclService.readAclById( new ObjectIdentityImpl( ee ) );
        aclService.readAclById( new ObjectIdentityImpl( ees ) );

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
    public void testAnalysisAcl()   {

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis diffExpressionAnalysis = config.toAnalysis();

        /*
         * Create an analysis, add a result set, persist. Problem is: what if we do things in a funny order. Must call
         * update on the Analysis.
         */
        ExpressionExperiment ee = getTestPersistentCompleteExpressionExperiment( false );

        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( diffExpressionAnalysis );
        resultSet.setExperimentalFactors( ee.getExperimentalDesign().getExperimentalFactors() );

        diffExpressionAnalysis.getResultSets().add( resultSet );

        diffExpressionAnalysis.setExperimentAnalyzed( ee );

        diffExpressionAnalysis = ( DifferentialExpressionAnalysis ) persisterHelper.persist( diffExpressionAnalysis );

        aclTestUtils.checkHasAcl( ee );
        aclTestUtils.checkHasAcl( diffExpressionAnalysis );
        aclTestUtils.checkHasAcl( resultSet );

        aclTestUtils.checkHasAces( ee );
        aclTestUtils.checkHasAces( diffExpressionAnalysis );
        aclTestUtils.checkLacksAces( resultSet );

        aclTestUtils.checkHasAclParent( resultSet, diffExpressionAnalysis );

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
        ee.setShortName( randomName() );
        expressionExperimentService.update( ee );

        aclTestUtils.checkEEAcls( ee );

        expressionExperimentService.delete( ee );

        aclTestUtils.checkDeleteEEAcls( ee );
    }

}
