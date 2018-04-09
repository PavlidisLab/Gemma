/*
 * The gemma-web project
 *
 * Copyright (c) 2014 University of British Columbia
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author ptan
 */
public class ExpressionExperimentControllerTest extends BaseSpringWebTest {

    @Autowired
    private ExpressionExperimentController eeController;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadStatusSummariesLimit() {
        ArrayList<Long> ids = new ArrayList<>();
        int limit;

        // Default ordering is by date last updated
        ExpressionExperiment lastUpdated = null;
        for ( int i = 0; i < 2; i++ ) {
            ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

            if ( lastUpdated == null || lastUpdated.getCurationDetails().getLastUpdated()
                    .before( ee.getCurationDetails().getLastUpdated() ) ) {
                lastUpdated = ee;
            }

            ids.add( ee.getId() );
        }
        limit = 1;
        Collection<ExpressionExperimentDetailsValueObject> ret = eeController
                .loadStatusSummaries( -1L, ids, limit, null, true );
        assertEquals( 1, ret.size() );
        ExpressionExperimentDetailsValueObject out = ret.iterator().next();

        assertEquals( lastUpdated.getId(), out.getId() );

    }

}
