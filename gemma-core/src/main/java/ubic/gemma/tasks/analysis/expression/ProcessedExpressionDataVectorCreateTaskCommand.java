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

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Command object for processing data vectors. Has dual purpose for updating the correlation matrices.
 * 
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment = null;

    private boolean correlationMatrixOnly = false;

    public ProcessedExpressionDataVectorCreateTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    public ProcessedExpressionDataVectorCreateTaskCommand( ExpressionExperiment expressionExperiment,
            boolean correlationMatrixOnly ) {
        super();
        this.expressionExperiment = expressionExperiment;
        this.correlationMatrixOnly = correlationMatrixOnly;
    }

    /**
     * @param taskId
     */
    public ProcessedExpressionDataVectorCreateTaskCommand( String taskId, ExpressionExperiment expressionExperiment ) {
        super();
        this.setTaskId( taskId );
        this.expressionExperiment = expressionExperiment;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public boolean isCorrelationMatrixOnly() {
        return correlationMatrixOnly;
    }

    /**
     * If true, we'll try to just update the correlation matrix, without creating vectors unless they don't exist yet.
     * 
     * @param correlationMatrixOnly
     */
    public void setCorrelationMatrixOnly( boolean correlationMatrixOnly ) {
        this.correlationMatrixOnly = correlationMatrixOnly;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public Class getTaskClass() {
        return ProcessedExpressionDataVectorCreateTask.class;
    }
}
