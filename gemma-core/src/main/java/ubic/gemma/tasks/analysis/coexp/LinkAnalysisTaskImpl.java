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

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysis;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
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

    @TaskMethod
    public TaskResult execute( LinkAnalysisTaskCommand command ) {
        
        ExpressionExperiment ee = linkAnalysisService.loadDataForAnalysis( command.getExpressionExperiment().getId() );
        LinkAnalysis la = linkAnalysisService.doAnalysis( ee, command.getLinkAnalysisConfig(), command.getFilterConfig() );
        linkAnalysisService.saveResults( ee, la, command.getLinkAnalysisConfig(), command.getFilterConfig() );
        
        TaskResult result = new TaskResult( command, null );
        return result;

    }

}
