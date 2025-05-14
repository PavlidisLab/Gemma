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
package ubic.gemma.core.tasks.analysis.expression;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public class SvdTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private boolean postProcessOnly = false;

    private ExpressionExperiment expressionExperiment;

    public SvdTaskCommand( ExpressionExperiment expressionExperiment ) {
        super();
        this.setMaxRuntimeMillis( 30 );
        this.expressionExperiment = expressionExperiment;
    }

    public SvdTaskCommand( ExpressionExperiment expressionExperiment, boolean postProcessOnly ) {
        super();
        this.expressionExperiment = expressionExperiment;
        this.postProcessOnly = postProcessOnly;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return SvdTask.class;
    }

    public boolean isPostProcessOnly() {
        return postProcessOnly;
    }

    public void setPostProcessOnly( boolean postProcessOnly ) {
        this.postProcessOnly = postProcessOnly;
    }
}
