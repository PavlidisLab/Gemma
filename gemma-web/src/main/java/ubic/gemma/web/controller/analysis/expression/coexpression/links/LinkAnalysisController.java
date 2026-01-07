/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.analysis.expression.coexpression.links;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilterConfig;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.coexp.LinkAnalysisTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

/**
 * A controller to pre-process expression data vectors.
 *
 * @author keshav
 */
@Controller
public class LinkAnalysisController {

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentReportService experimentReportService;

    public String run( Long id ) {
        /* this 'run' method is exported in the spring-beans.xml */

        ExpressionExperiment ee = expressionExperimentService.load( id );

        if ( ee == null ) {
            throw new EntityNotFoundException( "Cannot access experiment with id=" + id );
        }

        experimentReportService.evictFromCache( id );

        LinkAnalysisConfig lac = new LinkAnalysisConfig();
        ExpressionExperimentFilterConfig fc = new ExpressionExperimentFilterConfig();
        LinkAnalysisTaskCommand cmd = new LinkAnalysisTaskCommand( ee, lac, fc );

        return taskRunningService.submitTaskCommand( cmd );
    }
}
