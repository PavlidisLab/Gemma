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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * @author ptan
 */
public class ExpressionExperimentControllerTest extends BaseSpringWebTest {

    @Autowired
    private ExpressionExperimentController eeController;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaskRunningService taskRunningService;

    private Collection<ExpressionExperiment> ees = new ArrayList<>();

    @After
    public void tearDown() {
        expressionExperimentService.remove( ees );
        ees.clear();
    }

    @Test
    public void testLoadStatusSummariesLimit() {
        List<Long> ids = new ArrayList<>();
        int limit;

        // Default ordering is by date last updated
        ExpressionExperiment lastUpdated = null;
        for ( int i = 0; i < 2; i++ ) {
            ExpressionExperiment ee = this.getTestPersistentCompleteExpressionExperiment( false );

            if ( lastUpdated == null || ( lastUpdated.getCurationDetails().getLastUpdated() != null && lastUpdated.getCurationDetails().getLastUpdated()
                    .before( ee.getCurationDetails().getLastUpdated() ) ) ) {
                lastUpdated = ee;
            }

            ids.add( ee.getId() );
            ees.add( ee );
        }
        limit = 1;
        Collection<ExpressionExperimentDetailsValueObject> ret = eeController
                .loadStatusSummaries( -1L, ids, limit, null, true );
        assertEquals( 1, ret.size() );
        ExpressionExperimentDetailsValueObject out = ret.iterator().next();

        assertEquals( lastUpdated.getId(), out.getId() );

    }

    @Test
    public void testUpdatePubMed() throws ExecutionException, InterruptedException {
        ExpressionExperiment ee = getTestPersistentExpressionExperiment();
        ees.add( ee );

        String taskId = eeController.updatePubMed( ee.getId(), "1" );
        taskRunningService.getSubmittedTask( taskId ).getResult();
        ee = expressionExperimentService.thaw( ee );
        assertEquals( "Biochem Med", ee.getPrimaryPublication().getPublication() );

        taskId = eeController.updatePubMed( ee.getId(), "2" );
        taskRunningService.getSubmittedTask( taskId ).getResult();
        ee = expressionExperimentService.thaw( ee );
        assertEquals( "Biochem Biophys Res Commun", ee.getPrimaryPublication().getPublication() );
    }
}
