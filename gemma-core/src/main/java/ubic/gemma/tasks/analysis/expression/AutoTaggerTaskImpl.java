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

package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotatorImpl;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class AutoTaggerTaskImpl implements AutoTaggerTask {

    @Autowired
    private ExpressionExperimentAnnotator expressionExperimentAnnotator;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.tasks.analysis.expression.AutoTaggerTask#execute(ubic.gemma.job.TaskCommand)
     */
    @Override
    @TaskMethod
    public TaskResult execute( TaskCommand command ) {

        if ( !ExpressionExperimentAnnotatorImpl.ready() ) {
            throw new RuntimeException( "Sorry, the auto-tagger is not available." );
        }

        ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + command.getEntityId() + " could be loaded" );
        }

        Collection<Characteristic> characteristics = expressionExperimentAnnotator.annotate( ee, true );

        return new TaskResult( command, characteristics );
    }

}
