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
package ubic.gemma.security;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.security.acl.basic.BasicAclExtendedDao;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the SecurityService.
 * 
 * @author keshav
 * @version $Id$
 */
public class SecurityServiceTest extends BaseSpringContextTest {

    private ArrayDesignService arrayDesignService;

    ArrayDesign arrayDesign;
    String arrayDesignName = "Array Design Foo";
    String compositeSequenceName1 = "Design Element Bar1";
    String compositeSequenceName2 = "Design Element Bar2";

    private ExpressionExperimentService expressionExperimentService;

    private ExpressionExperimentSetService expressionExperimentSetService;

    private DifferentialExpressionAnalysisService diffAnalysisService;

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
        log.info( "Turn up the logging levels to DEBUG on the spring and gemma security packages" );

        super.onSetUpInTransaction(); // admin

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

    // /**
    // * Tests changing object level security on the ArrayDesign from public to private.
    // *
    // * @throws Exception
    // */
    // public void testMakePrivate() throws Exception {
    // TODO Fix this test - You need to add this back in - IS IMPORTANT
    // ArrayDesign ad = arrayDesignService.findByName( arrayDesignName );
    // SecurityService securityService = new SecurityService();
    //
    // securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
    // securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
    // securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
    // securityService.makePrivate( ad );
    // /*
    // * uncomment so you can see the acl permission has been changed in the database.
    // */
    // // this.setComplete();
    // }

    /**
     * Tests changing object level security on the ArrayDesign from public to private WITHOUT the correct permission
     * (You need to be administrator).
     * 
     * @throws Exception
     */
    public void testMakePrivateWithoutPermission() throws Exception {

        this.onSetUpInTransactionGrantingUserAuthority( "unauthorizedTestUser" );

        ArrayDesign ad = arrayDesignService.findByName( arrayDesignName );
        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );

        try {
            securityService.makePrivate( ad );
            fail( "Should have gotten a unauthorized user exception" );
        } catch ( Exception e ) {
            // ok.
        }
    }

    // /**
    // * @throws Exception
    // */
    // public void testMakeTestExpressionExperimentPrivate() throws Exception {
    // endTransaction();
    // ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false ); // not readonly.
    //
    // SecurityService securityService = new SecurityService();
    // securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
    // securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
    // securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
    // securityService.makePrivate( ee );
    // /*
    // * uncomment so you can see the acl permission has been changed in the database.
    // */
    // // this.setComplete();
    // }

    /**
     * @throws Exception
     */
    public void testMakeExpressionExperimentPrivate() throws Exception {
        String expName = "kottmann";// "GSE7480";

        ExpressionExperiment ee = getExperiment( expName );
        if ( ee == null ) return;

        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
        securityService.makePrivate( ee );
        /*
         * uncomment so you can see the acl permission has been changed in the database.
         */
        // this.setComplete();
    }

    /**
     * Tests makeing the {@link ExpressionExperimentSet} private. The underlying bioassay sets (experiments) are
     * untouched.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMakeExpressionExperimentSetPrivate() throws Exception {
        /*
         * This is more of an integration test and will be skipped as part of the test suite. To run this test, first
         * create an ExpressionExperimentSet in the DatasetChooserPanel with 2 experiments and call it test, then run
         * this.
         */
        String setName = "test";

        expressionExperimentSetService = ( ExpressionExperimentSetService ) this
                .getBean( "expressionExperimentSetService" );
        Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( setName );
        if ( eeSets == null || eeSets.isEmpty() ) return;

        ExpressionExperimentSet eeSet = eeSets.iterator().next();

        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
        securityService.makePrivate( eeSet );
        /*
         * uncomment this block so you can see the acl permission has been changed in the database. The underlying acl
         * permission of the ExpressionExperimentSet should be private (0), and the each ExpressionExperiment public
         * (6).
         */
        // this.setComplete();
        // assertTrue( securityService.isPrivate( eeSet ) );
        // Collection<BioAssaySet> baSets = eeSet.getExperiments();
        // for ( BioAssaySet bas : baSets ) {
        // assertFalse( securityService.isPrivate( bas ) );
        // }
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public void testMakeAnalysisPrivate() throws Exception {
        String expName = "GSE1077";

        ExpressionExperiment investigation = getExperiment( expName );
        if ( investigation == null ) return;

        diffAnalysisService = ( DifferentialExpressionAnalysisService ) this
                .getBean( "differentialExpressionAnalysisService" );

        Collection<DifferentialExpressionAnalysis> diffAnalyses = diffAnalysisService
                .findByInvestigation( investigation );

        if ( diffAnalyses == null || diffAnalyses.isEmpty() ) {
            log.error( "Cannot find analyses for experiment " + expName + " in database.  Skipping test." );
            return;
        } else {

            SecurityService securityService = new SecurityService();

            securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
            securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
            securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );
            securityService.makePrivate( diffAnalyses );

            /*
             * uncomment so you can see the acl permission has been changed in the database.
             */
            // this.setComplete();
        }
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public void testIsPrivate() throws Exception {
        String expName = "GSE1077";

        ExpressionExperiment investigation = getExperiment( expName );
        if ( investigation == null ) return;

        SecurityService securityService = new SecurityService();

        securityService.setBasicAclExtendedDao( ( BasicAclExtendedDao ) this.getBean( "basicAclExtendedDao" ) );
        securityService.setSecurableDao( ( SecurableDao ) this.getBean( "securableDao" ) );
        securityService.setCrudUtils( ( CrudUtils ) this.getBean( "crudUtils" ) );

        boolean priv = securityService.isPrivate( investigation );
        assertFalse( priv );
    }

    /**
     * @param expName
     * @return
     */
    private ExpressionExperiment getExperiment( String expName ) {

        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        ExpressionExperiment ee = expressionExperimentService.findByShortName( expName );

        if ( ee == null ) {
            log.error( "Cannot find experiment " + expName + " in database. Skipping test." );
        }
        return ee;
    }
}
