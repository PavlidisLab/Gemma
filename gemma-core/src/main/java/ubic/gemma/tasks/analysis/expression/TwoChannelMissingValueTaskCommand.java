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
import java.util.HashSet;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValuesImpl;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.Task;

/**
 * @author paul
 * @version $Id$
 */
public class TwoChannelMissingValueTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment = null;

    private double s2n = TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;

    private Collection<Double> extraMissingValueIndicators = new HashSet<Double>();

    public TwoChannelMissingValueTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    /**
     * @param expressionExperiment
     * @param s2n
     * @param extraMissingValueIndictors
     * @see TwoChannelMissingValuesImpl for parameterization details.
     */
    public TwoChannelMissingValueTaskCommand( ExpressionExperiment expressionExperiment, double s2n,
            Collection<Double> extraMissingValueIndictors ) {
        super();
        this.s2n = s2n;
        this.extraMissingValueIndicators = extraMissingValueIndictors;
        this.expressionExperiment = expressionExperiment;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @return the extraMissingValueIndicators
     */
    public Collection<Double> getExtraMissingValueIndicators() {
        return extraMissingValueIndicators;
    }

    /**
     * @return the s2n
     */
    public double getS2n() {
        return s2n;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return TwoChannelMissingValueTask.class;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @param extraMissingValueIndicators the extraMissingValueIndicators to set
     */
    public void setExtraMissingValueIndicators( Collection<Double> extraMissingValueIndicators ) {
        this.extraMissingValueIndicators = extraMissingValueIndicators;
    }

    /**
     * @param s2n the s2n to set
     */
    public void setS2n( double s2n ) {
        this.s2n = s2n;
    }
}
