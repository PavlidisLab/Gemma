/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.tasks.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.AbstractTask;

/**
 * @author paul
 * @version $Id$
 */
@Component
@Scope("prototype")
public class SvdTaskImpl extends AbstractTask<TaskResult, SvdTaskCommand> implements SvdTask {

    private Log log = LogFactory.getLog( SvdTask.class.getName() );

    @Autowired
    private SVDService svdService;

    @Override
    public TaskResult execute() {
        TaskResult result = new TaskResult( taskCommand, null );

        if ( taskCommand.getExpressionExperiment() != null ) {
            svdService.svd( taskCommand.getExpressionExperiment().getId() );
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }
}
