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
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests for methods that create ExpressionExperimentSetValueObjects from expressionExperiment entities
 * 
 * @author tvrossum
 * @version $Id$
 */
public class ExpressionExperimentSetValueObjectHelperTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    UserManager userManager;

    @Autowired
    ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    private Taxon tax1;
    private ExpressionExperiment ee = null;
    private ExpressionExperimentSet eeSet = null;

    @Before
    public void setUp() {

        tax1 = this.getTaxon( "human" );
        ee = this.getTestPersistentExpressionExperiment( tax1 );

        // needs to be a set
        Collection<BioAssaySet> ees = new HashSet<BioAssaySet>();
        ees.add( ee );

        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setName( "CreateTest" );
        eeSet.setDescription( "CreateDesc" );
        eeSet.setExperiments( ees );
        eeSet.setTaxon( tax1 );

        eeSet = expressionExperimentSetService.create( eeSet );

    }

    @After
    public void tearDown() {

        // delete by id because otherwise get HibernateException: reassociated object has dirty collection reference
        expressionExperimentService.delete( ee.getId() );

        // getting "access is denied" error here, even with this.runAsAdmin()
        // expressionExperimentSetService.delete( eeSet );
    }

    @Test
    public void testConvertToValueObject() {

        Long id = eeSet.getId();
        assertNotNull( id );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetService.loadValueObject( id );

        assertEquals( eesvo.getId(), eeSet.getId() );
        assertEquals( eesvo.getNumExperiments().intValue(), eeSet.getExperiments().size() );
        assertEquals( eesvo.getName(), eeSet.getName() );
        assertEquals( eesvo.getDescription(), eeSet.getDescription() );
        assertEquals( eesvo.getTaxonId(), eeSet.getTaxon().getId() );
    }

    @Test
    public void testConvertToLightValueObject() {

        Long id = eeSet.getId();
        assertNotNull( id );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetService.loadValueObject( id );

        assertEquals( 0, eesvo.getExpressionExperimentIds().size() );

        assertEquals( eeSet.getId(), eesvo.getId() );
        assertEquals( eeSet.getExperiments().size(), eesvo.getNumExperiments().intValue() );
        assertEquals( eeSet.getName(), eesvo.getName() );
        assertEquals( eeSet.getDescription(), eesvo.getDescription() );
        assertEquals( eesvo.getTaxonId(), eeSet.getTaxon().getId() );

    }

    @Test
    public void testConvertToEntity() {

        // create VO from entity
        Long id = eeSet.getId();
        assertNotNull( id );

        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetService.loadValueObject( id );

        // create entity from VO
        ExpressionExperimentSet remadeEE = expressionExperimentSetValueObjectHelper.convertToEntity( eesvo );

        // check that entity is valid
        expressionExperimentSetService.update( remadeEE );

        assertEquals( eeSet.getId(), remadeEE.getId() );
        assertEquals( eeSet.getExperiments().size(), remadeEE.getExperiments().size() );

        // check that experiment members are the same
        Set<Object> set1 = new HashSet<Object>();
        set1.addAll( eeSet.getExperiments() );
        Set<Object> set2 = new HashSet<Object>();
        set2.addAll( remadeEE.getExperiments() );
        set1.equals( set2 );

        assertEquals( eeSet.getName(), remadeEE.getName() );
        assertEquals( eeSet.getDescription(), remadeEE.getDescription() );
        assertEquals( eeSet.getStatus(), remadeEE.getStatus() );
        assertEquals( eeSet.getTaxon(), remadeEE.getTaxon() );

    }

}
