/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for methods that perform operations on or with expressionExperiment sets
 * 
 * @author tvrossum
 * @version $Id$
 */
public class ExpressionExperimentSetServiceTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    UserManager userManager;

    @Autowired
    ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    @Autowired
    BioMaterialService bioMaterialService;

    @Autowired
    BioAssayService bioAssayService;

    private Taxon tax1;
    private Taxon taxMouse;
    private ExpressionExperiment ee1 = null;
    private ExpressionExperiment ee2 = null;
    private ExpressionExperiment eeMouse = null;
    private ExpressionExperimentSet eeSet = null;
    private ExpressionExperimentSet eeSetAutoGen = null;

    @Before
    public void setUp() {

        // need persistent entities so that experiment's taxon can be
        // queried from database during methods being tested

        tax1 = this.getTaxon( "human" );
        taxMouse = this.getTaxon( "mouse" );

        ee1 = this.getTestPersistentExpressionExperiment( tax1 );
        ee2 = this.getTestPersistentExpressionExperiment( tax1 );
        eeMouse = this.getTestPersistentExpressionExperiment( taxMouse );

        // Make experiment set
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( ee1 );
        ees.add( ee2 );

        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setName( "CreateTest" );
        eeSet.setDescription( "CreateDesc" );
        eeSet.getExperiments().addAll( ees );
        eeSet.setTaxon( tax1 );

        eeSet = expressionExperimentSetService.create( eeSet );

        eeSetAutoGen = expressionExperimentSetService.initAutomaticallyGeneratedExperimentSet( ees, tax1 );
        eeSetAutoGen = expressionExperimentSetService.create( eeSetAutoGen );

    }

    @After
    public void tearDown() {

        // delete by id because otherwise get HibernateException: reassociated object has dirty collection reference
        expressionExperimentService.delete( ee1.getId() );
        expressionExperimentService.delete( ee2.getId() );
        expressionExperimentService.delete( eeMouse.getId() );

        // getting "access is denied" error here, even with this.runAsAdmin()
        // expressionExperimentSetService.delete( eeSet );
    }

    @Test
    public void testUpdate() {

        Long eeSetId = eeSet.getId();

        String newName = "newName";
        String newDesc = "newDesc";
        int newCount = 1;
        Collection<BioAssaySet> newMembers = new HashSet<BioAssaySet>();
        newMembers.add( ee1 );

        eeSet.setName( newName );
        eeSet.setDescription( newDesc );
        eeSet.setExperiments( newMembers );

        expressionExperimentSetService.update( eeSet );
        ExpressionExperimentSet updatedSet = expressionExperimentSetService.load( eeSetId );
        // need VO otherwise was getting lazy loading issues
        ExpressionExperimentSetValueObject setVO = expressionExperimentSetService.loadValueObject( updatedSet.getId() );

        assertEquals( newName, setVO.getName() );
        assertEquals( newDesc, setVO.getDescription() );
        assertEquals( newCount, setVO.getExpressionExperimentIds().size() );
        assertEquals( ee1.getId(), setVO.getExpressionExperimentIds().iterator().next() );

    }

    @Test(expected = Exception.class)
    public void testAddingExperimentOfWrongTaxonUpdate() {
        Collection<BioAssaySet> newMembers = new LinkedList<BioAssaySet>();
        newMembers.add( ee1 );
        newMembers.add( eeMouse );
        eeSet.setExperiments( newMembers );

        expressionExperimentSetService.update( eeSet );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddingExperimentOfWrongTaxonUpdateDatabaseEntityMembers() {
        Collection<Long> newMemberIds = new LinkedList<Long>();
        newMemberIds.add( ee1.getId() );
        newMemberIds.add( eeMouse.getId() );

        expressionExperimentSetService.updateDatabaseEntityMembers( eeSet.getId(), newMemberIds );
    }

    //
    // @Test
    // public void testUpdateDatabaseEntity() {
    //
    // // try to add an experiment of wrong taxon, should fail
    // }
    //
    // @Test
    // public void testUpdateDatabaseEntityMembers() {
    //
    // // try to add an experiment of wrong taxon, should fail
    // }

    @Test
    public void testIsAutomaticallyGenerated() {
        assertTrue( expressionExperimentSetService.isAutomaticallyGenerated( eeSetAutoGen.getDescription() ) );
        assertFalse( expressionExperimentSetService.isAutomaticallyGenerated( eeSet.getDescription() ) );

    }
}
