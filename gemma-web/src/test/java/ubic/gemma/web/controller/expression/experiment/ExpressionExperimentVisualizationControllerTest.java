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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.Settings;
import ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationController;

/**
 * @author keshav
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ExpressionExperimentVisualizationControllerTest extends BaseSpringWebTest {

    @Autowired
    private ExpressionExperimentVisualizationController expressionExperimentVisualizationController;

    /**
     * @throws Exception
     */
    @Test
    public final void testSubmit() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newGet( "/expressionExperiment/visualizeDataMatrix.html" );

        request.setRemoteUser( Settings.getString( "gemma.admin.user" ) );

        ExpressionExperimentService service = this.getBean( ExpressionExperimentService.class );

        String shortName = "GSE3434";
        ExpressionExperiment ee = service.findByShortName( shortName );
        if ( ee == null ) {
            log.warn( "Could not find expression experiment with name " + shortName + ".  Skipping test ..." );
            return;
        }

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( ee.getRawExpressionDataVectors() );
        ExpressionDataDoubleMatrix matrix = builder.getPreferredData();

        int i = 1;

        request.setParameter( "id", String.valueOf( i ) );

        request.getSession().setAttribute( String.valueOf( i ), matrix );

        request.setParameter( "type", "heatmap" );

        ModelAndView mv = expressionExperimentVisualizationController.show( request, response );
        assertEquals( null, mv );
    }
}
