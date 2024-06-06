/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.tasks.maintenance;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public class ExpressionExperimentReportTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;
    private ExpressionExperiment expressionExperiment = null;
    private boolean all = false;

    public ExpressionExperimentReportTaskCommand( Boolean all ) {
        super();
        this.setMaxRuntime( 300 ); /* 5 hours */
        this.all = all;
    }

    public ExpressionExperimentReportTaskCommand( ExpressionExperiment expressionExperiment ) {
        super();
        this.setMaxRuntime( 5 );
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return ExpressionExperimentReportTask.class;
    }

    public boolean doAll() {
        return all;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setAll( Boolean all ) {
        this.all = all;
    }

}
