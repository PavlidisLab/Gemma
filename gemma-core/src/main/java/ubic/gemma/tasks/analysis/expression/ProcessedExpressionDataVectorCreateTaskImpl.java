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
package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A "processed expression data vector create" task
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class ProcessedExpressionDataVectorCreateTaskImpl implements ProcessedExpressionDataVectorCreateTask {

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private SampleCoexpressionMatrixService coexpressionMatrixService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.grid.javaspaces.task.analysis.preprocess.ProcessedExpressionDataVectorCreateTask#execute(ubic.gemma
     * .grid .javaspaces.analysis.preprocess.SpacesProcessedExpressionDataVectorCreateCommand)
     */
    @TaskMethod
    public TaskResult execute( ProcessedExpressionDataVectorCreateTaskCommand command ) {

        ExpressionExperiment ee = command.getExpressionExperiment();
        Collection<ProcessedExpressionDataVector> processedVectors = null;
        if ( command.isCorrelationMatrixOnly() ) {
            // only create them if necessary. This is sort of stupid, it's just so I didn't have to create a whole other
            // task for the correlation matrix computation.
            processedVectors = processedExpressionDataVectorService.getProcessedDataVectors( ee );
            if ( processedVectors.isEmpty() ) {
                processedVectors = processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
            }
        } else {
            processedVectors = processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        }

        coexpressionMatrixService.create( ee, processedVectors );

        TaskResult result = new TaskResult( command, processedVectors.size() );
        return result;
    }

}
