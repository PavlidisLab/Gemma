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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.expression.experiment.*;
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
public class ExperimentalDesignControllerTest extends BaseSpringWebTest {

    @Autowired
    private ExperimentalDesignController experimentalDesignController;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Test
    public void testShowExperimentalDesign() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( true ); // readonly

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
    public void testGetExperimentalFactors() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( true ); // readonly.

        Collection<ExperimentalFactorValueObject> experimentalFactors = experimentalDesignController
                .getExperimentalFactors( new EntityDelegator( ee.getExperimentalDesign() ) );
        assertTrue( !experimentalFactors.isEmpty() );
    }

    @Test
    public void testGetExperimentalFactorValues() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ee = this.eeService.thawLite( ee );

        Collection<FactorValueValueObject> fvs = experimentalDesignController.getFactorValuesWithCharacteristics(
                new EntityDelegator( ee.getExperimentalDesign().getExperimentalFactors().iterator().next() ) );
        assertTrue( !fvs.isEmpty() );
    }

    @Test
    public void testCreateExperimentalFactor() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ee = this.eeService.thawLite( ee );

        ExperimentalFactorValueObject evvo = new ExperimentalFactorValueObject( -1L );
        evvo.setCategory( "foo" );
        experimentalDesignController
                .createExperimentalFactor( new EntityDelegator( ee.getExperimentalDesign() ), evvo );

    }

    @Test
    public void testAddCharacteristicToFactorValue() throws Exception {

        ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

        ee = this.eeService.thawLite( ee );

        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertNotNull( ef );
        EntityDelegator e = new EntityDelegator( ef.getFactorValues().iterator().next() );

        Statement vc = Statement.Factory.newInstance();
        vc.setValue( "foo" );
        vc.setCategory( "bar" );
        vc.setCategoryUri( "bar" );
        vc.setValueUri( "foo" );
        experimentalDesignController.createFactorValueCharacteristic( e, vc );
        assertEquals( 2, ef.getFactorValues().size() );

        // new empty
        experimentalDesignController.createFactorValue( new EntityDelegator( ef ) );
        experimentalDesignController.createFactorValue( new EntityDelegator( ef ) );
        experimentalDesignController.createFactorValue( new EntityDelegator( ef ) );

        ef = experimentalFactorService.load( ef.getId() );
        assertNotNull( ef );
        assertEquals( 5, ef.getFactorValues().size() );
    }

}
