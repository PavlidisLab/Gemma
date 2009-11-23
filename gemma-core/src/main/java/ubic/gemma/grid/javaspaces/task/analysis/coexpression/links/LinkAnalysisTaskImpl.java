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
package ubic.gemma.grid.javaspaces.task.analysis.coexpression.links;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * @author keshav
 * @version $Id$
 */
public class LinkAnalysisTaskImpl extends BaseSpacesTask implements LinkAnalysisTask {

    private LinkAnalysisService linkAnalysisService = null;

    public void setLinkAnalysisService( LinkAnalysisService linkAnalysisService ) {
        this.linkAnalysisService = linkAnalysisService;
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.grid.javaspaces.coexpression.LinkAnalysisTask#execute(ubic.gemma.grid.javaspaces.coexpression.
     * SpacesLinkAnalysisCommand)
     */
    public TaskResult execute( LinkAnalysisTaskCommand command ) {
        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        TaskResult result = new TaskResult();

        try {
            linkAnalysisService.process( command.getExpressionExperiment(), command.getFilterConfig(), command
                    .getLinkAnalysisConfig() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        result.setTaskID( super.taskId );

        super.tidyProgress( spacesProgressAppender );

        return result;

    }

}
