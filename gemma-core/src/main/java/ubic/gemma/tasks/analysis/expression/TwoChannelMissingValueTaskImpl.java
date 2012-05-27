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
package ubic.gemma.tasks.analysis.expression;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Space task for computing two channel missing values.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class TwoChannelMissingValueTaskImpl implements TwoChannelMissingValueTask {

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValues;

    @Override
    @TaskMethod
    public TaskResult execute( TwoChannelMissingValueTaskCommand command ) {

        ExpressionExperiment ee = command.getExpressionExperiment();

        Collection<RawExpressionDataVector> missingValueVectors = twoChannelMissingValues.computeMissingValues( ee,
                command.getS2n(), command.getExtraMissingValueIndicators() );

        TaskResult result = new TaskResult( command, missingValueVectors.size() );

        return result;
    }

}
