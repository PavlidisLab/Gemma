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
package ubic.gemma.grid.javaspaces.analysis.coexpression.links;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author keshav
 * @version $Id$
 */
public class LinkAnalysisTaskImpl extends BaseSpacesTask implements LinkAnalysisTask {

    private long counter = 0;

    private LinkAnalysisService linkAnalysisService = null;

    public void setLinkAnalysisService( LinkAnalysisService linkAnalysisService ) {
        this.linkAnalysisService = linkAnalysisService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.coexpression.LinkAnalysisTask#execute(ubic.gemma.grid.javaspaces.coexpression.SpacesLinkAnalysisCommand)
     */
    public SpacesResult execute( SpacesLinkAnalysisCommand command ) {
        super.initProgressAppender( this.getClass() );
        SpacesResult result = new SpacesResult();

        try {
            linkAnalysisService.process( command.getExpressionExperiment(), command.getFilterConfig(), command
                    .getLinkAnalysisConfig() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        counter++;
        result.setTaskID( counter );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

}
