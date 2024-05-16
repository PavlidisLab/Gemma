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
package ubic.gemma.web.controller.expression.experiment;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Kiran Keshav
 */
public class ExperimentalDesignControllerIntegrationTest extends BaseSpringWebTest {

    @Autowired
    private ExperimentalDesignController experimentalDesignController;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService eeService;

    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        ee = this.getTestPersistentCompleteExpressionExperiment( false );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }

    @Test
    public void testShowExperimentalDesign() throws Exception {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        assertNotNull( ed );
        assertNotNull( ed.getId() );
        perform( get( "/experimentalDesign/showExperimentalDesign.html" )
                        .param( "edid", ed.getId().toString() ) )
                .andExpect( status().isOk() )
                .andExpect( request().attribute( "id", ed.getId() ) )
                .andExpect( view().name( "experimentalDesign.detail" ) )
                .andExpect( model().attribute( "needsAttention", equalTo( false ) ) )
                .andExpect( model().attribute( "randomExperimentalDesignThatNeedsAttention", nullValue() ) );
    }

    @Test
    public void testShowExperimentalDesignByExperimentId() throws Exception {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        assertTrue( ed != null && ee.getId() != null );
        perform( get( "/experimentalDesign/showExperimentalDesign.html" )
                        .param( "eeid", ee.getId().toString() ) )
                .andExpect( status().isOk() )
                .andExpect( request().attribute( "id", ed.getId() ) )
                .andExpect( view().name( "experimentalDesign.detail" ) );
    }

    @Test
    public void testShowExperimentalDesignByExperimentShortName() throws Exception {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        assertNotNull( ee.getShortName() );
        assertTrue( ed != null && ee.getId() != null );
        perform( get( "/experimentalDesign/showExperimentalDesign.html" )
                        .param( "shortName", ee.getShortName() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "experimentalDesign.detail" ) );
    }

    @Test
    public void testGetExperimentalFactors() {
        Collection<ExperimentalFactorValueObject> experimentalFactors = experimentalDesignController
                .getExperimentalFactors( new EntityDelegator<>( ee.getExperimentalDesign() ) );
        assertFalse( experimentalFactors.isEmpty() );
    }

    @Test
    public void testGetExperimentalFactorValues() {
        Collection<FactorValueValueObject> fvs = experimentalDesignController.getFactorValuesWithCharacteristics(
                new EntityDelegator<>( ee.getExperimentalDesign().getExperimentalFactors().iterator().next() ) );
        assertFalse( fvs.isEmpty() );
    }

    @Test
    public void testCreateExperimentalFactor() {
        ExperimentalFactorValueWebUIObject evvo = new ExperimentalFactorValueWebUIObject( -1L );
        evvo.setCategory( "foo" );
        experimentalDesignController
                .createExperimentalFactor( new EntityDelegator<>( ee.getExperimentalDesign() ), evvo );
    }

    @Test
    public void testAddCharacteristicToFactorValue() {
        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        FactorValue fv2 = ef.getFactorValues().iterator().next();

        CharacteristicValueObject vc = new CharacteristicValueObject();
        vc.setValue( "foo" );
        vc.setValueUri( "foo" );
        vc.setCategory( "bar" );
        vc.setCategoryUri( "bar" );

        experimentalDesignController.createFactorValueCharacteristic( new EntityDelegator<>( fv2 ), vc );
        assertEquals( 2, ef.getFactorValues().size() );

        // new empty
        experimentalDesignController.createFactorValue( new EntityDelegator<>( ef ) );
        experimentalDesignController.createFactorValue( new EntityDelegator<>( ef ) );
        experimentalDesignController.createFactorValue( new EntityDelegator<>( ef ) );

        ef = experimentalFactorService.load( ef.getId() );
        assertNotNull( ef );
        Assertions.assertThat( ef.getFactorValues() )
                .hasSize( 5 )
                .anySatisfy( fv -> {
                    assertEquals( fv2, fv );
                    Assertions.assertThat( fv.getCharacteristics() )
                            .anySatisfy( c -> {
                                assertEquals( "bar", c.getCategory() );
                                assertEquals( "bar", c.getCategoryUri() );
                                assertEquals( "foo", c.getSubject() );
                                assertEquals( "foo", c.getSubjectUri() );
                            } );
                } );
    }

    @Test
    public void testAddStatementToFactorValue() {
        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        EntityDelegator<FactorValue> e = new EntityDelegator<>( ef.getFactorValues().iterator().next() );

        CharacteristicValueObject vc = new CharacteristicValueObject();
        vc.setCategory( "bar" );
        vc.setCategoryUri( "bar" );
        vc.setValue( "foo" );
        vc.setValueUri( "foo" );

        experimentalDesignController.createFactorValueCharacteristic( e, vc );

        ef = experimentalFactorService.load( ef.getId() );
        assertNotNull( ef );
        assertEquals( 2, ef.getFactorValues().size() );
        Assertions.assertThat( ef.getFactorValues() )
                .flatExtracting( FactorValue::getCharacteristics )
                .anySatisfy( c -> {
                    assertEquals( "bar", c.getCategory() );
                    assertEquals( "bar", c.getCategoryUri() );
                    assertEquals( "foo", c.getSubject() );
                    assertEquals( "foo", c.getSubjectUri() );
                    assertNull( c.getPredicate() );
                    assertNull( c.getPredicateUri() );
                    assertNull( c.getObject() );
                    assertNull( c.getObjectUri() );
                } );
    }

    @Test
    public void testUpdateFactorValueStatement() {
        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        FactorValue fv = ef.getFactorValues().iterator().next();
        Statement stmt = fv.getCharacteristics().iterator().next();

        FactorValueValueObject fvvo = new FactorValueValueObject();
        fvvo.setId( fv.getId() );
        fvvo.setFactorId( ef.getId() );
        fvvo.setCharId( stmt.getId() );
        fvvo.setCategory( "bar" );
        fvvo.setCategoryUri( "bar" );
        fvvo.setValue( "foo" );
        fvvo.setValueUri( "foo" );
        fvvo.setPredicate( "has" );
        fvvo.setObject( "bar2" );

        experimentalDesignController.updateFactorValueCharacteristics( new FactorValueValueObject[] { fvvo } );

        ef = experimentalFactorService.load( ef.getId() );
        assertNotNull( ef );
        assertEquals( 2, ef.getFactorValues().size() );
        Assertions.assertThat( ef.getFactorValues() )
                .flatExtracting( FactorValue::getCharacteristics )
                .anySatisfy( c -> {
                    assertEquals( stmt.getId(), c.getId() );
                    assertEquals( "bar", c.getCategory() );
                    assertEquals( "bar", c.getCategoryUri() );
                    assertEquals( "foo", c.getSubject() );
                    assertEquals( "foo", c.getSubjectUri() );
                    assertEquals( "has", c.getPredicate() );
                    assertNull( c.getPredicateUri() );
                    assertNotNull( c.getObject() );
                    assertEquals( "bar2", c.getObject() );
                    assertNull( c.getObjectUri() );
                } );
    }
}
