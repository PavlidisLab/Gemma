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
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Async-task wrapper around
 * {@link ExpressionExperimentPlatformSwitchService#switchExperimentToArrayDesign(ExpressionExperiment, ArrayDesign)}.
 * When {@code targetArrayDesign} is null the task delegates to
 * {@link ExpressionExperimentPlatformSwitchService#switchExperimentToMergedPlatform(ExpressionExperiment)}, mirroring
 * the {@code switchExperimentPlatform} CLI behaviour.
 *
 * @author paul
 */
@Getter
@Setter
public class ExpressionExperimentPlatformSwitchTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment;
    private ArrayDesign targetArrayDesign;

    public ExpressionExperimentPlatformSwitchTaskCommand( ExpressionExperiment expressionExperiment, ArrayDesign targetArrayDesign ) {
        super();
        this.expressionExperiment = expressionExperiment;
        this.targetArrayDesign = targetArrayDesign;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return ExpressionExperimentPlatformSwitchTask.class;
    }
}
