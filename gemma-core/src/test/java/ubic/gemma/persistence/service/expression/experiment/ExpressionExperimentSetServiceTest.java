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

package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for methods that perform operations on or with expressionExperiment sets
 *
 * @author tvrossum
 */
public class ExpressionExperimentSetServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    private ExpressionExperiment ee1 = null;
    private ExpressionExperiment ee2 = null;
    private ExpressionExperiment eeMouse = null;
    private ExpressionExperimentSet eeSet = null;
    private ExpressionExperimentSet eeSetAutoGen = null;

    @Before
    public void setUp() throws Exception {

        // need persistent entities so that experiment's taxon can be
        // queried from database during methods being tested

        Taxon tax1 = this.getTaxon( "human" );
        Taxon taxMouse = this.getTaxon( "mouse" );

        ee1 = this.getTestPersistentExpressionExperiment( tax1 );
        ee2 = this.getTestPersistentExpressionExperiment( tax1 );
        eeMouse = this.getTestPersistentExpressionExperiment( taxMouse );

        // Make experiment set
        Collection<ExpressionExperiment> ees = new HashSet<>();
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
        expressionExperimentService.remove( ee1 );
        expressionExperimentService.remove( ee2 );
        expressionExperimentService.remove( eeMouse );
    }

    @Test
    public void testUpdate() {

        Long eeSetId = eeSet.getId();

        String newName = "newName";
        String newDesc = "newDesc";
        Set<ExpressionExperiment> newMembers = new HashSet<>();
        newMembers.add( ee1 );

        eeSet.setName( newName );
        eeSet.setDescription( newDesc );
        eeSet.setExperiments( newMembers );

        expressionExperimentSetService.update( eeSet );
        ExpressionExperimentSet updatedSet = expressionExperimentSetService.load( eeSetId );
        assertNotNull( updatedSet );
        // need VO otherwise was getting lazy loading issues
        ExpressionExperimentSetValueObject setVO = expressionExperimentSetService.loadValueObject( updatedSet );

        assertNotNull( setVO );
        assertEquals( newName, setVO.getName() );
        assertEquals( newDesc, setVO.getDescription() );
        assertEquals( 1, setVO.getSize().intValue() ); // experiment IDs are not populated by default.

        Collection<ExpressionExperiment> eesInSet = expressionExperimentSetService.getExperimentsInSet( eeSet.getId() );

        assertEquals( 1, eesInSet.size() );
        assertTrue( eesInSet.contains( ee1 ) );

    }

    @Test(expected = Exception.class)
    public void testAddingExperimentOfWrongTaxonUpdate() {
        Set<ExpressionExperiment> newMembers = new HashSet<>();
        newMembers.add( ee1 );
        newMembers.add( eeMouse );
        eeSet.setExperiments( newMembers );

        expressionExperimentSetService.update( eeSet );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddingExperimentOfWrongTaxonUpdateDatabaseEntityMembers() {
        Collection<Long> newMemberIds = new LinkedList<>();
        newMemberIds.add( ee1.getId() );
        newMemberIds.add( eeMouse.getId() );
        expressionExperimentSetValueObjectHelper.updateMembers( eeSet.getId(), newMemberIds );
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
