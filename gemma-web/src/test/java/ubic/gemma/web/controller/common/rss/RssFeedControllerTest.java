/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.common.rss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.util.RegressionTesting;
import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringWebTest;

/**
 * @author sshao
 * @version $Id$
 */
public class RssFeedControllerTest extends BaseSpringWebTest {

    @Autowired
    private WhatsNewService whatsNewService;

    @Autowired
    private RssFeedController rssFeedController;

    private int updateCount;
    private int newCount;

    private Map<ExpressionExperiment, String> experiments = new HashMap<ExpressionExperiment, String>();

    @Before
    public void setup() {

        WhatsNew wn = whatsNewService.retrieveReport();

        super.getTestPersistentExpressionExperiment();
        super.getTestPersistentExpressionExperiment();

        whatsNewService.generateWeeklyReport();
        wn = whatsNewService.retrieveReport();

        Collection<ExpressionExperiment> updatedExperiments = wn.getUpdatedExpressionExperiments();
        Collection<ExpressionExperiment> newExperiments = wn.getNewExpressionExperiments();

        for ( ExpressionExperiment e : updatedExperiments ) {
            experiments.put( e, "Updated" );
        }
        for ( ExpressionExperiment e : newExperiments ) {
            experiments.put( e, "New" );
        }

        updateCount = updatedExperiments.size();
        newCount = newExperiments.size();

    }

    @Test
    public void testGetLatestExperiments() {
        ModelAndView mav = rssFeedController.getLatestExperiments( null, null );
        assertNotNull( mav );

        Map<String, Object> model = mav.getModel();
        assertNotNull( model );

        @SuppressWarnings("unchecked")
        Map<ExpressionExperiment, String> retreivedExperiments = ( Map<ExpressionExperiment, String> ) model
                .get( "feedContent" );
        Integer retreivedUpdateCount = ( Integer ) model.get( "updateCount" );
        Integer retreivedNewCount = ( Integer ) model.get( "newCount" );

        assertNotNull( retreivedExperiments );
        assertNotNull( retreivedUpdateCount );
        assertNotNull( retreivedNewCount );

        assertTrue( RegressionTesting.containsSame( experiments.keySet(), retreivedExperiments.keySet() ) );
        assertEquals( updateCount, retreivedUpdateCount.intValue() );
        assertEquals( newCount, retreivedNewCount.intValue() );
    }

}
