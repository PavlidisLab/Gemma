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
package ubic.gemma.core.tasks.analysis.expression;

import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValuesImpl;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public class TwoChannelMissingValueTaskCommand extends TaskCommand {
    private static final long serialVersionUID = 1L;

    private ExpressionExperiment expressionExperiment;

    private double s2n = TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;

    private Collection<Double> extraMissingValueIndicators = new HashSet<>();

    public TwoChannelMissingValueTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    /**
     * @param expressionExperiment       experiment
     * @param s2n                        s2n
     * @param extraMissingValueIndictors extra missing values indicators
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

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @return the extraMissingValueIndicators
     */
    public Collection<Double> getExtraMissingValueIndicators() {
        return extraMissingValueIndicators;
    }

    /**
     * @param extraMissingValueIndicators the extraMissingValueIndicators to set
     */
    public void setExtraMissingValueIndicators( Collection<Double> extraMissingValueIndicators ) {
        this.extraMissingValueIndicators = extraMissingValueIndicators;
    }

    /**
     * @return the s2n
     */
    public double getS2n() {
        return s2n;
    }

    /**
     * @param s2n the s2n to set
     */
    public void setS2n( double s2n ) {
        this.s2n = s2n;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return TwoChannelMissingValueTask.class;
    }
}
