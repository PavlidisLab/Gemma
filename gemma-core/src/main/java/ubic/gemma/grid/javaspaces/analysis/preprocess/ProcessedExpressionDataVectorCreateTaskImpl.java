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
package ubic.gemma.grid.javaspaces.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A "processed expression data vector create" spaces task that can be passed into a space and executed by a worker.
 * 
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateTaskImpl extends BaseSpacesTask implements
        ProcessedExpressionDataVectorCreateTask {

    private Log log = LogFactory.getLog( ProcessedExpressionDataVectorCreateService.class.getName() );

    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService = null;

    private long counter = 0;

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.grid.javaspaces.analysis.preprocess.ProcessedExpressionDataVectorCreateTask#execute(ubic.gemma.grid
     * .javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand)
     */
    public TaskResult execute( ProcessedExpressionDataVectorCreateTaskCommand command ) {
        super.initProgressAppender( this.getClass() );

        ExpressionExperiment ee = command.getExpressionExperiment();

        TaskResult result = new TaskResult();
        Collection<ProcessedExpressionDataVector> processedVectors = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );
        result.setAnswer( processedVectors );

        counter++;
        result.setTaskID( counter );
        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
    }

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

}
