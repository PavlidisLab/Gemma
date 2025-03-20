/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.tasks.analysis.diffex;

import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * A command object to be used by spaces.
 *
 * @author keshav
 */
@SuppressWarnings("unused") // Possible external use
public class DifferentialExpressionAnalysisTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    /**
     * Proposed analysis type. If null the system tries to figure it out.
     */
    private AnalysisType analysisType;

    private ExpressionExperiment expressionExperiment;

    /**
     * The factors to actually use in the analysis. If null the system tries to figure it out.
     */
    private Collection<ExperimentalFactor> factors;

    private boolean forceAnalysis = false;

    /**
     * Whether interactions among the factors should be included. The implementation may limit this to two-way
     * interactions for only up to two factors, so this may not have the effect desired.
     */
    private boolean includeInteractions = false;

    /**
     * Whether to moderate test statistics via empirical Bayes
     */
    private boolean moderateStatistics = false;

    private ExperimentalFactor subsetFactor;

    private DifferentialExpressionAnalysis toRedo;

    /**
     * Whether to use weighted regression based on mean-variance relationships (voom)
     */
    private boolean useWeights = false;

    public DifferentialExpressionAnalysisTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    public DifferentialExpressionAnalysisTaskCommand( ExpressionExperiment ee, DifferentialExpressionAnalysis toRedo ) {
        super();
        this.expressionExperiment = ee;
        this.toRedo = toRedo;
    }

    public DifferentialExpressionAnalysisTaskCommand( String taskId, boolean forceAnalysis,
            ExpressionExperiment expressionExperiment ) {
        super();
        this.setTaskId( taskId );
        this.forceAnalysis = forceAnalysis;
        this.expressionExperiment = expressionExperiment;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public Collection<ExperimentalFactor> getFactors() {
        return factors;
    }

    public void setFactors( Collection<ExperimentalFactor> factors ) {
        this.factors = factors;
    }

    /**
     * @return the subsetFactor
     */
    public ExperimentalFactor getSubsetFactor() {
        return subsetFactor;
    }

    /**
     * @param subsetFactor the subsetFactor to set
     */
    public void setSubsetFactor( ExperimentalFactor subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return DifferentialExpressionAnalysisTask.class;
    }

    public DifferentialExpressionAnalysis getToRedo() {
        return toRedo;
    }

    public void setToRedo( DifferentialExpressionAnalysis toRedo ) {
        this.toRedo = toRedo;
    }

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

    public boolean isIncludeInteractions() {
        return includeInteractions;
    }

    /**
     * @param includeInteractions Sets preference for interactions to be included.
     */
    public void setIncludeInteractions( boolean includeInteractions ) {
        this.includeInteractions = includeInteractions;
    }

    public boolean isModerateStatistics() {
        return moderateStatistics;
    }

    public void setModerateStatistics( boolean moderateStatistics ) {
        this.moderateStatistics = moderateStatistics;
    }

    public boolean isUseWeights() {
        return useWeights;
    }

    public void setUseWeights( boolean useWeights ) {
        this.useWeights = useWeights;
    }
}
