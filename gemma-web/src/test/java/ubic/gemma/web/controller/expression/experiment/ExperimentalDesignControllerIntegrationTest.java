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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicBasicValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

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
    public void testShowExperimentalDesign() {
        MockHttpServletRequest req = super.newGet( "/experimentalDesign/showExperimentalDesign.html" );

        ExperimentalDesign ed = ee.getExperimentalDesign();

        assertTrue( ed != null && ee.getId() != null );

        req.addParameter( "name", "Experimental Design 0" );

        req.addParameter( "eeid", String.valueOf( ee.getId() ) );

        req.setRequestURI( "/experimentalDesign/showExperimentalDesign.html" );

        ModelAndView mav = experimentalDesignController.show( req, ( HttpServletResponse ) null );

        Map<String, Object> m = mav.getModel();
        assertNotNull( m.get( "expressionExperiment" ) );

        assertEquals( mav.getViewName(), "experimentalDesign.detail" );
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
        ExperimentalFactorValueObject evvo = new ExperimentalFactorValueObject( -1L );
        evvo.setCategory( "foo" );
        experimentalDesignController
                .createExperimentalFactor( new EntityDelegator<>( ee.getExperimentalDesign() ), evvo );
    }

    @Test
    public void testAddCharacteristicToFactorValue() {
        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        FactorValue fv2 = ef.getFactorValues().iterator().next();

        CharacteristicBasicValueObject vc = new CharacteristicBasicValueObject();
        vc.setValue( "foo" );
        vc.setCategory( "bar" );
        vc.setCategoryUri( "bar" );
        vc.setValueUri( "foo" );

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
                                assertEquals( "foo", c.getValue() );
                                assertEquals( "foo", c.getValueUri() );
                            } );
                } );
    }

    @Test
    public void testAddStatementToFactorValue() {
        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        EntityDelegator<FactorValue> e = new EntityDelegator<>( ef.getFactorValues().iterator().next() );

        CharacteristicBasicValueObject vc = new CharacteristicBasicValueObject();
        vc.setCategory( "bar" );
        vc.setCategoryUri( "bar" );
        vc.setValue( "foo" );
        vc.setValueUri( "foo" );
        vc.setPredicate( "has" );
        vc.setObject( new CharacteristicBasicValueObject() );
        vc.getObject().setValue( "bar2" );

        experimentalDesignController.createFactorValueCharacteristic( e, vc );

        ef = experimentalFactorService.load( ef.getId() );
        assertNotNull( ef );
        assertEquals( 2, ef.getFactorValues().size() );
        Assertions.assertThat( ef.getFactorValues() )
                .flatExtracting( FactorValue::getCharacteristics )
                .anySatisfy( c -> {
                    assertEquals( "bar", c.getCategory() );
                    assertEquals( "bar", c.getCategoryUri() );
                    assertEquals( "foo", c.getValue() );
                    assertEquals( "foo", c.getValueUri() );
                    assertEquals( "has", c.getPredicate() );
                    assertNull( c.getPredicateUri() );
                    assertNotNull( c.getObject() );
                    assertNull( c.getObject().getCategory() );
                    assertNull( c.getObject().getCategoryUri() );
                    assertEquals( "bar2", c.getObject().getValue() );
                    assertNull( c.getObject().getValueUri() );
                } );
    }
}
