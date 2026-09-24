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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for methods that perform operations on or with expressionExperiment sets
 *
 * @author tvrossum
 */
public class ExpressionExperimentSetServiceTest extends BaseSpringContextTest5 {

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

    @BeforeEach
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

    @AfterEach
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

    /**
     * A set that declares a taxon keeps the constraint: no mice in a rat set. The two tests below
     * pin that and must keep passing.
     * <p>
     * A set that declares NO taxon may span them. The constraint is a scope a set opts into, not a
     * property every set must have — a curation cohort is the counter-example that forced it: the
     * gold reference set is 179 human, 254 mouse and 16 rat, and there is nothing wrong with it.
     * Before this, creating one was impossible and the failure was an UnsupportedOperationException
     * reading "EESets with mixed taxa are not supported".
     */
    @Test
    public void testASetWithNoTaxonMaySpanThem() {
        ExpressionExperimentSet mixed = ExpressionExperimentSet.Factory.newInstance();
        mixed.setName( "Reference cohort, mixed taxa" );
        mixed.setDescription( "human and mouse together" );
        mixed.getExperiments().add( ee1 );
        mixed.getExperiments().add( eeMouse );
        mixed.setTaxon( null );

        ExpressionExperimentSet created = expressionExperimentSetService.create( mixed );
        assertNotNull( created.getId() );

        created.setDescription( "still mixed after an update" );
        expressionExperimentSetService.update( created );

        assertThat( expressionExperimentSetService.getExperimentsInSet( created.getId() ) )
                .as( "both taxa are in the set" )
                .hasSize( 2 );
        assertThat( created.getTaxon() ).isNull();

        expressionExperimentSetService.remove( created );
    }

    @Test
    public void testAddingExperimentOfWrongTaxonUpdate() {
        Set<ExpressionExperiment> newMembers = new HashSet<>();
        newMembers.add( ee1 );
        newMembers.add( eeMouse );
        eeSet.setExperiments( newMembers );

        assertThrows( Exception.class, () -> expressionExperimentSetService.update( eeSet ) );
    }

    @Test
    public void testAddingExperimentOfWrongTaxonUpdateDatabaseEntityMembers() {
        Collection<Long> newMemberIds = new LinkedList<>();
        newMemberIds.add( ee1.getId() );
        newMemberIds.add( eeMouse.getId() );
        assertThrows( IllegalArgumentException.class,
                () -> expressionExperimentSetValueObjectHelper.updateMembers( eeSet.getId(), newMemberIds ) );
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
