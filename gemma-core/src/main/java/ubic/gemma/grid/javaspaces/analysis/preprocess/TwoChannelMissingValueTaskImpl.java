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
package ubic.gemma.grid.javaspaces.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Space task for computing two channel missing values.
 * 
 * @author paul
 * @version $Id$
 */
public class TwoChannelMissingValueTaskImpl extends BaseSpacesTask implements TwoChannelMissingValueTask {

    private static Log log = LogFactory.getLog( TwoChannelMissingValueTaskImpl.class.getName() );
    private TwoChannelMissingValues twoChannelMissingValues;

    /**
     * @param twoChannelMissingValues
     */
    public void setTwoChannelMissingValues( TwoChannelMissingValues twoChannelMissingValues ) {
        this.twoChannelMissingValues = twoChannelMissingValues;
    }

    public TaskResult execute( TwoChannelMissingValueTaskCommand command ) {
        super.initProgressAppender( this.getClass() );

        ExpressionExperiment ee = command.getExpressionExperiment();

        TaskResult result = new TaskResult();
        Collection<DesignElementDataVector> missingValueVectors = twoChannelMissingValues.computeMissingValues( ee,
                command.getS2n(), command.getExtraMissingValueIndicators() );
        result.setAnswer( missingValueVectors.size() );
        result.setTaskID( super.taskId );
        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );
        return result;
    }

}
