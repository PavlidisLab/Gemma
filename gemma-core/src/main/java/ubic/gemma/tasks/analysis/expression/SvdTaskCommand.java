/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.Task;

/**
 * @author paul
 * @version $Id$
 */
public class SvdTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private boolean postprocessOnly = false;

    public boolean isPostprocessOnly() {
        return postprocessOnly;
    }

    public void setPostprocessOnly( boolean postprocessOnly ) {
        this.postprocessOnly = postprocessOnly;
    }

    public SvdTaskCommand( ExpressionExperiment expressionExperiment, boolean postprocessOnly ) {
        super();
        this.expressionExperiment = expressionExperiment;
        this.postprocessOnly = postprocessOnly;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    private ExpressionExperiment expressionExperiment = null;

    public SvdTaskCommand( ExpressionExperiment expressionExperiment ) {
        super();
        this.setMaxRuntime( 30 );
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return SvdTask.class;
    }
}
