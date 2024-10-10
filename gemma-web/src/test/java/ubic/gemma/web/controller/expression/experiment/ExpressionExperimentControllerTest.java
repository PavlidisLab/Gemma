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
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ubic.gemma.web.util.dwr.MockDwrRequestBuilders.dwr;
import static ubic.gemma.web.util.dwr.MockDwrResultHandlers.getCallback;
import static ubic.gemma.web.util.dwr.MockDwrResultHandlers.getException;
import static ubic.gemma.web.util.dwr.MockDwrResultMatchers.callback;
import static ubic.gemma.web.util.dwr.MockDwrResultMatchers.exception;

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
    @Ignore("This test is failing on the CI (see https://github.com/PavlidisLab/Gemma/issues/1246 for details)")
    public void testUpdatePubMed() throws Exception {
        ExpressionExperiment ee = getTestPersistentExpressionExperiment();
        ees.add( ee );

        perform( dwr( ExpressionExperimentController.class, "updatePubMed", ee.getId(), "1" ) )
                .andExpect( callback().exist() )
                .andDo( getCallback( ( String taskId ) -> {
                    SubmittedTask st = taskRunningService.getSubmittedTask( taskId );
                    assertNotNull( st );
                    try {
                        st.getResult();
                    } catch ( ExecutionException | InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                    ExpressionExperiment ee1 = expressionExperimentService.thaw( ee );
                    assertEquals( "Biochem Med", ee1.getPrimaryPublication().getPublication() );
                } ) );


        perform( dwr( ExpressionExperimentController.class, "updatePubMed", ee.getId(), "2" ) )
                .andExpect( callback().exist() )
                .andDo( getCallback( ( String taskId ) -> {
                    SubmittedTask st = taskRunningService.getSubmittedTask( taskId );
                    assertNotNull( st );
                    try {
                        st.getResult();
                    } catch ( ExecutionException | InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                    ExpressionExperiment ee1 = expressionExperimentService.thaw( ee );
                    assertEquals( "Biochem Biophys Res Commun", ee1.getPrimaryPublication().getPublication() );
                } ) );
    }

    @Test
    public void testUpdatePubMedAsAnonymousUser() throws Exception {
        ExpressionExperiment ee = getTestPersistentExpressionExperiment();
        ees.add( ee );
        runAsAnonymous();
        try {
            perform( dwr( ExpressionExperimentController.class, "updatePubMed", ee.getId(), "1" ) )
                    .andExpect( status().isOk() )
                    .andExpect( exception().exist() )
                    .andExpect( callback().doesNotExist() )
                    .andDo( getException( e -> {
                        assertEquals( AccessDeniedException.class.getName(), e.getJavaClassName() );
                        assertEquals( "Access is denied", e.getMessage() );
                    } ) );
        } finally {
            runAsAdmin();
        }
    }
}
