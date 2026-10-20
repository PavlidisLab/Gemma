/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.job.Task;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

/**
 * Async-task wrapper around {@link GeeqService#calculateScore(ExpressionExperiment, GeeqService.ScoreMode)}.
 *
 * @author paul
 */
@Getter
@Setter
public class GeeqTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment;
    private GeeqService.ScoreMode mode;

    public GeeqTaskCommand( ExpressionExperiment expressionExperiment, GeeqService.ScoreMode mode ) {
        super();
        this.expressionExperiment = expressionExperiment;
        this.mode = mode != null ? mode : GeeqService.ScoreMode.all;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return GeeqTask.class;
    }
}
