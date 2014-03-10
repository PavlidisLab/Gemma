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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.testing.BaseSpringWebTest;

/**
 * 
 * 
 * @author ptan
 * @version $Id$
 */
public class ExpressionExperimentControllerTest extends BaseSpringWebTest {

    @Autowired
    private ExpressionExperimentController eeController;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadStatusSummariesLimit() {
        ArrayList<Long> ids = new ArrayList<Long>();
        int limit = -1;
        for ( int i = 0; i < 2; i++ ) {
            Long eeId = this.getTestPersistentCompleteExpressionExperiment( false ).getId();
            ids.add( eeId );
        }
        limit = 1;
        Collection<ExpressionExperimentValueObject> ret = eeController
                .loadStatusSummaries( -1L, ids, limit, null, true );
        assertEquals( 1, ret.size() );
        Iterator<Long> in = ids.iterator();
        Iterator<ExpressionExperimentValueObject> out = ret.iterator();
        assertEquals( in.next(), out.next().getId() );

        // Negative limit, assumes IDs have been sorted in decreasing order
        limit = -1;
        ArrayList<Long> idsRev = ( ArrayList<Long> ) ids.clone();
        Collections.reverse( ( List<?> ) idsRev );
        ret = eeController.loadStatusSummaries( -1L, idsRev, limit, null, true );
        out = ret.iterator();
        assertEquals( 1, ret.size() );
        assertEquals( in.next(), out.next().getId() );

    }

}
