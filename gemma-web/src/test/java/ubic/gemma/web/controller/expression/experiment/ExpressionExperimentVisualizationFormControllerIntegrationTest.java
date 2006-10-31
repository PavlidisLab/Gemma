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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.AbstractExpressionExperimentTest;
import ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationCommand;
import ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationFormController;

/**
 * Tests the expressionExperimentVisualizationController functionality. For this to work, data must be left in the
 * database, hence the integration test naming convention.
 * 
 * @author keshav
 * @version $Id: ExpressionExperimentVisualizationFormControllerIntegrationTest.java,v 1.1 2006/10/24 15:38:33 keshav
 *          Exp $
 */
public class ExpressionExperimentVisualizationFormControllerIntegrationTest extends AbstractExpressionExperimentTest {

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {
        /* put test data in database */
        ExpressionExperiment ee = this.getTestExpressionExperimentWithAllDependencies();

        /* leave data in database */
        setComplete();

        /* mimic the search */
        ExpressionExperimentVisualizationFormController controller = ( ExpressionExperimentVisualizationFormController ) this
                .getBean( "expressionExperimentVisualizationFormController" );

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        ExpressionExperimentVisualizationCommand command = new ExpressionExperimentVisualizationCommand();
        command.setSearchCriteria( "probe set id" );
        command.setSearchString( "probeset_0, probeset_1" );
        // command.setStandardQuantitationTypeName( StandardQuantitationType.MEASUREDSIGNAL.getValue() );

        log.debug( "expression experiment id " + ee.getId() );
        command.setExpressionExperimentId( ee.getId() );

        BindException errors = new BindException( command, "ExpressionExperimentSearchCommand" );
        controller.processFormSubmission( request, response, command, errors );
        ModelAndView mav = controller.onSubmit( request, response, command, errors );
        log.warn( mav.getViewName() );
        // assertEquals( "showExpressionExperimentVisualization", mav.getViewName() );
        assertEquals( "expressionExperimentVisualizationForm", mav.getViewName() );

    }
}
