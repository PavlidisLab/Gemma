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
package ubic.gemma.tasks.analysis.coexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
@Component
public class LinkAnalysisTaskImpl implements LinkAnalysisTask {

    @Autowired
    private LinkAnalysisService linkAnalysisService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @TaskMethod
    public TaskResult execute( LinkAnalysisTaskCommand command ) {

        ExpressionExperiment ee = expressionExperimentService.thawLite( command.getExpressionExperiment() );
        linkAnalysisService.process( ee, command.getFilterConfig(), command.getLinkAnalysisConfig() );
        TaskResult result = new TaskResult( command, null );
        return result;

    }

}
