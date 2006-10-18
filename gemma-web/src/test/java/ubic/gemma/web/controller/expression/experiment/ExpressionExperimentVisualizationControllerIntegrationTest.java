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

/**
 * Tests the expressionExperimentVisualizationController functionality. For this to work, data must be left in the database,
 * hence the integration test naming convention.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentVisualizationControllerIntegrationTest extends AbstractExpressionExperimentTest {

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {
        ExpressionExperiment ee = this.getTestExpressionExperimentWithAllDependencies();

        ExpressionExperimentVisualizationFormController controller = ( ExpressionExperimentVisualizationFormController ) this
                .getBean( "expressionExperimentSearchController" );

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        ExpressionExperimentVisualizationCommand command = new ExpressionExperimentVisualizationCommand();
        command.setSearchCriteria( "probe set id" );
        command.setSearchString( "0_at, 1_at" );
        // command.setFilename( "build/Gemma/images/outImage.png" );
        log.debug( "expression experiment id " + ee.getId() );
        command.setExpressionExperimentId( ee.getId() );

        setComplete();// leave data in database

        BindException errors = new BindException( command, "ExpressionExperimentSearchCommand" );
        controller.processFormSubmission( request, response, command, errors );
        ModelAndView mav = controller.onSubmit( request, response, command, errors );
        assertEquals( "showExpressionExperimentSearchResults", mav.getViewName() );

    }
}
