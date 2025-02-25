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
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * @author Paul
 */
@Component
@Scope("prototype")
public class PreprocessTaskImpl
        extends AbstractTask<PreprocessTaskCommand>
        implements PreprocessExperimentTask {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Override
    public TaskResult call() {
        ExpressionExperiment ee = taskCommand.getExpressionExperiment();
        ee = expressionExperimentService.thaw( ee );
        if ( taskCommand.diagnosticsOnly() ) {
            preprocessorService.processDiagnostics( ee );
            return new TaskResult( taskCommand, "Diagnostics updated" );
        }
        try {
            preprocessorService.process( ee );
            return new TaskResult( taskCommand, "Preprocessing completed" );
        } catch ( PreprocessingException e ) {
            return new TaskResult( taskCommand, "Failed: " + e.getMessage() );
        }
    }
}
