/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.web.controller.analysis.preprocess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.expression.BatchInfoFetchTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

/**
 * For populating "batch" information about experiments.
 *
 * @author paul
 */
@Controller
public class BatchInfoFetchController {

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ExpressionExperimentReportService experimentReportService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    public String run( Long id ) {
        if ( id == null )
            throw new IllegalArgumentException( "ID cannot be null" );

        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "Could not load experiment with id=" + id );

        /*
         * Check preconditions.
         */
        if ( ee.getAccession() == null ) {
            throw new IllegalArgumentException(
                    "The experiment does not seem to be from an external source that would have batch information available." );
        }

        BatchInfoFetchTaskCommand cmd = new BatchInfoFetchTaskCommand( ee );

        experimentReportService.evictFromCache( id );
        return taskRunningService.submitTaskCommand( cmd );
    }

}
