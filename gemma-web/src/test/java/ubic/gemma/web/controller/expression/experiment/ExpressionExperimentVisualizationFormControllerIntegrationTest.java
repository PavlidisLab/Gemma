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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationFormController;

/**
 * Tests the expressionExperimentVisualizationController functionality.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id: ExpressionExperimentVisualizationFormControllerIntegrationTest.java,v 1.1 2006/10/24 15:38:33 keshav
 *          Exp $
 */
public class ExpressionExperimentVisualizationFormControllerIntegrationTest extends BaseSpringWebTest {
    ExpressionExperiment ee;
    QuantitationType qt;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ee = this.getTestPersistentCompleteExpressionExperiment( true );
        for ( DesignElementDataVector d : ee.getRawExpressionDataVectors() ) {
            if ( d.getQuantitationType().getIsPreferred() ) {
                qt = d.getQuantitationType();
            }
        }
        assertNotNull( qt );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ee != null ) {
            ExpressionExperimentService ees = ( ExpressionExperimentService ) this
                    .getBean( "expressionExperimentService" );
            ees.delete( ee );
        }
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {
        endTransaction();
        /* mimic the search */
        ExpressionExperimentVisualizationFormController controller = ( ExpressionExperimentVisualizationFormController ) this
                .getBean( "expressionExperimentVisualizationFormController" );

        MockHttpServletRequest request = newPost( "/expressionExperiment/visualizeDataMatrix.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter( "searchCriteria", ExpressionExperimentVisualizationFormController.SEARCH_BY_PROBE );
        request.setParameter( "searchString", "probeset_0, probeset_1" );
        request.setParameter( "quantitationType", qt.getName() );
        request.setParameter( "viewSampling", "false" );
        request.setParameter( "id", ee.getId().toString() );
        ModelAndView mav = controller.handleRequest( request, response );
        assertEquals( "showExpressionExperimentVisualization", mav.getViewName() );
    }

    /**
     * Tests the "view a sample of the data" functionality.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmitViewSampling() throws Exception {
        endTransaction();
        /* mimic the search */
        ExpressionExperimentVisualizationFormController controller = ( ExpressionExperimentVisualizationFormController ) this
                .getBean( "expressionExperimentVisualizationFormController" );

        MockHttpServletRequest request = newPost( "/expressionExperiment/visualizeDataMatrix.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter( "searchCriteria", ExpressionExperimentVisualizationFormController.SEARCH_BY_PROBE );
        request.setParameter( "quantitationType", qt.getName() );
        request.setParameter( "viewSampling", "true" );
        request.setParameter( "id", ee.getId().toString() );
        ModelAndView mav = controller.handleRequest( request, response );
        assertEquals( "showExpressionExperimentVisualization", mav.getViewName() );

    }
}
