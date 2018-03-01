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
package ubic.gemma.core.tasks.analysis.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.Collection;

/**
 * @author Paul
 */
@Component
@Scope("prototype")
public class ProcessedExpressionDataVectorCreateTaskImpl
        extends AbstractTask<TaskResult, ProcessedExpressionDataVectorCreateTaskCommand>
        implements ProcessedExpressionDataVectorCreateTask {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private SampleCoexpressionMatrixService coexpressionMatrixService;

    @Override
    public TaskResult execute() {
        ExpressionExperiment ee = taskCommand.getExpressionExperiment();
        Collection<ProcessedExpressionDataVector> processedVectors;
        if ( taskCommand.isCorrelationMatrixOnly() ) {
            // only create them if necessary. This is sort of stupid, it's just so I didn't have to create a whole other
            // task for the correlation matrix computation.
            processedVectors = processedExpressionDataVectorService.getProcessedDataVectors( ee );
            if ( processedVectors.isEmpty() ) {
                processedVectors = processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            }
        } else {
            processedVectors = processedExpressionDataVectorService.computeProcessedExpressionData( ee );
        }

        coexpressionMatrixService.create( ee);

        return new TaskResult( taskCommand, processedVectors.size() );
    }
}
