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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
@Component
@Scope("prototype")
public class ExpressionExperimentPlatformSwitchTaskImpl extends AbstractTask<ExpressionExperimentPlatformSwitchTaskCommand>
        implements ExpressionExperimentPlatformSwitchTask {

    private final Log log = LogFactory.getLog( ExpressionExperimentPlatformSwitchTask.class.getName() );

    @Autowired
    private ExpressionExperimentPlatformSwitchService platformSwitchService;

    @Override
    public TaskResult call() {
        TaskResult result = newTaskResult( null );

        ExpressionExperiment ee = getTaskCommand().getExpressionExperiment();
        ArrayDesign target = getTaskCommand().getTargetArrayDesign();

        if ( ee == null ) {
            log.warn( "TaskCommand was not valid, nothing being done" );
            return result;
        }

        if ( target != null ) {
            platformSwitchService.switchExperimentToArrayDesign( ee, target );
        } else {
            platformSwitchService.switchExperimentToMergedPlatform( ee );
        }

        return result;
    }
}
