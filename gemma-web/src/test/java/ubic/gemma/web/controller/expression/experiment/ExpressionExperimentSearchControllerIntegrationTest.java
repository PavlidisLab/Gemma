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
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.expression.experiment.ExpressionExperimentHelper;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests the expressionExperimentSearchController functionality. For this to work, data must be left in the database,
 * hence the integration test naming convention.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentSearchControllerIntegrationTest extends BaseTransactionalSpringContextTest {

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {

        onSetUpInTransaction();// set up in a transaction
        ExpressionExperimentHelper eeh = new ExpressionExperimentHelper();
        eeh.setExpressionExperimentDependencies();

        ExpressionExperimentSearchController searchController = ( ExpressionExperimentSearchController ) this
                .getBean( "expressionExperimentSearchController" );

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        ExpressionExperimentSearchCommand command = new ExpressionExperimentSearchCommand();
        command.setSearchCriteria( "probe set id" );
        command.setSearchString( "0_at, 1_at" );
        //command.setFilename( "build/Gemma/images/outImage.png" );
        command.setId( new Long( 0 ) );

        if ( ( ( ExpressionExperimentDao ) this.getBean( "expressionExperimentDao" ) ).loadAll().size() == 0 )
            setComplete();// leave data in database

        ModelAndView mav = searchController.onSubmit( request, response, command, null );
        assertEquals( "showExpressionExperimentSearchResults", mav.getViewName() );

    }
}
