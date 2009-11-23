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
package ubic.gemma.grid.javaspaces.task.analysis.preprocess;

import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Command object for processing data vectors. Used by spaces.
 * 
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment = null;

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @param taskId
     */
    public ProcessedExpressionDataVectorCreateTaskCommand( String taskId, ExpressionExperiment expressionExperiment ) {
        super();
        this.setTaskId( taskId );
        this.expressionExperiment = expressionExperiment;
    }

    public ProcessedExpressionDataVectorCreateTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

}
