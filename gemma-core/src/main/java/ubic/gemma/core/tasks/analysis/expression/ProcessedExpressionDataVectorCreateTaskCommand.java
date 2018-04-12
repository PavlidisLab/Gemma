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
package ubic.gemma.core.tasks.analysis.expression;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Command object for processing data vectors. Has dual purpose for updating the correlation matrices.
 *
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ProcessedExpressionDataVectorCreateTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private boolean correlationMatrixOnly = false;

    private ExpressionExperiment expressionExperiment = null;

    private boolean restoreMissingSamples = true;

    public ProcessedExpressionDataVectorCreateTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    /**
     * @param expressionExperiment EE
     * @param restoreMissing       if any missing values should be restored (default = TRUE)
     */
    public ProcessedExpressionDataVectorCreateTaskCommand( ExpressionExperiment expressionExperiment,
            boolean restoreMissing ) {
        this( expressionExperiment );
        this.restoreMissingSamples = restoreMissing;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return ProcessedExpressionDataVectorCreateTask.class;
    }

    public boolean isCorrelationMatrixOnly() {
        return correlationMatrixOnly;
    }

    /**
     * If true, we'll try to just update the correlation matrix, without creating vectors unless they don't exist yet.
     *
     * @param correlationMatrixOnly correlation matrix only
     */
    public void setCorrelationMatrixOnly( boolean correlationMatrixOnly ) {
        this.correlationMatrixOnly = correlationMatrixOnly;
    }

    public boolean isRestoreMissingSamples() {
        return restoreMissingSamples;
    }

    public void setRestoreMissingSamples( boolean restoreMissingSamples ) {
        this.restoreMissingSamples = restoreMissingSamples;
    }
}