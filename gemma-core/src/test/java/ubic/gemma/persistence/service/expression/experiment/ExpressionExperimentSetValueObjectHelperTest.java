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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for methods that create ExpressionExperimentSetValueObjects from expressionExperiment entities
 *
 * @author tvrossum
 */
public class ExpressionExperimentSetValueObjectHelperTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    private ExpressionExperiment ee = null;
    private ExpressionExperimentSet eeSet = null;

    @Before
    public void setUp() throws Exception {

        Taxon tax1 = this.getTaxon( "human" );
        ee = this.getTestPersistentExpressionExperiment( tax1 );

        Collection<ExpressionExperiment> ees = new HashSet<>();
        ees.add( ee );

        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setName( "CreateTest" );
        eeSet.setDescription( "CreateDesc" );
        eeSet.getExperiments().addAll( ees );
        eeSet.setTaxon( tax1 );

        eeSet = expressionExperimentSetService.create( eeSet );

    }

    @After
    public void tearDown() {
        expressionExperimentService.remove( ee );
    }

    @Test
    public void testConvertToEntity() {

        // create VO from entity
        Long id = eeSet.getId();
        assertNotNull( id );

        assertNotNull( expressionExperimentService.loadValueObject( ee ) );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetService.loadValueObject( eeSet );

        assertNotNull( eesvo );
        assertEquals( 1, eeSet.getExperiments().size() );

        Collection<ExpressionExperimentDetailsValueObject> experimentValueObjectsInSet = expressionExperimentSetService
                .getExperimentValueObjectsInSet( id );

        assertEquals( 1, experimentValueObjectsInSet.size() );

        eesvo.getExpressionExperimentIds().addAll( IdentifiableUtils.getIds( experimentValueObjectsInSet ) );

        // create entity from VO
        ExpressionExperimentSet remadeEE = expressionExperimentSetValueObjectHelper.convertToEntity( eesvo );

        // check that entity is valid
        expressionExperimentSetService.update( remadeEE );

        assertEquals( eeSet.getId(), remadeEE.getId() );
        assertEquals( eeSet.getExperiments().size(), remadeEE.getExperiments().size() );

        // check that experiment members are the same
        Set<BioAssaySet> set1 = new HashSet<>( eeSet.getExperiments() );
        Set<BioAssaySet> set2 = new HashSet<>( remadeEE.getExperiments() );
        //noinspection ResultOfMethodCallIgnored
        set1.equals( set2 );

        assertEquals( eeSet.getName(), remadeEE.getName() );
        assertEquals( eeSet.getDescription(), remadeEE.getDescription() );
        assertEquals( eeSet.getTaxon(), remadeEE.getTaxon() );

    }

    /**
     * Tests loading 'light' value objects - VOs without EEIds
     */
    @Test
    public void testConvertToLightValueObject() {

        Long id = eeSet.getId();
        ExpressionExperimentSetValueObject eesvo;
        assertNotNull( id );

        // test loading without EEIds.
        eesvo = expressionExperimentSetService.loadValueObjectById( id );
        assertNotNull( eesvo );
        assertEquals( 0, eesvo.getExpressionExperimentIds().size() );
        this.equalValuesCheck( eesvo );

        // test the same thing, without using the overloaded method.
        eesvo = expressionExperimentSetService.loadValueObjectById( id, false );
        assertNotNull( eesvo );
        assertEquals( 0, eesvo.getExpressionExperimentIds().size() );
        this.equalValuesCheck( eesvo );

    }

    /**
     * Tests loading value objects with EEIds.
     */
    @Test
    public void testConvertToValueObject() {

        Long id = eeSet.getId();
        ExpressionExperimentSetValueObject eesvo;
        assertNotNull( id );

        // test loading with EEIds.
        eesvo = expressionExperimentSetService.loadValueObjectById( id, true );
        assertNotNull( eesvo );
        assertEquals( 1, eesvo.getExpressionExperimentIds().size() );
        this.equalValuesCheck( eesvo );
    }

    private void equalValuesCheck( ExpressionExperimentSetValueObject eesvo ) {
        assertEquals( eeSet.getId(), eesvo.getId() );
        assertEquals( eeSet.getExperiments().size(), eesvo.getSize().intValue() );
        assertEquals( eeSet.getName(), eesvo.getName() );
        assertEquals( eeSet.getDescription(), eesvo.getDescription() );
        assertEquals( eesvo.getTaxonId(), eeSet.getTaxon().getId() );
    }
}
