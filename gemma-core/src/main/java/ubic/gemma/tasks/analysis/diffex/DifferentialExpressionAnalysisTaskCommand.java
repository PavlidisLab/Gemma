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
package ubic.gemma.tasks.analysis.diffex;

import java.util.Collection;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command object to be used by spaces.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    /**
     * Proposed analysis type. If null the system tries to figure it out.
     */
    private AnalysisType analysisType;

    private ExpressionExperiment expressionExperiment = null;

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

    private ExperimentalFactor subsetFactor;

    public DifferentialExpressionAnalysisTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    /**
     * @param taskId
     * @param forceAnalysis
     * @param expressionExperiment
     */
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

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public Collection<ExperimentalFactor> getFactors() {
        return factors;
    }

    /**
     * @return the subsetFactor
     */
    public ExperimentalFactor getSubsetFactor() {
        return subsetFactor;
    }

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public boolean isIncludeInteractions() {
        return includeInteractions;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setFactors( Collection<ExperimentalFactor> factors ) {
        this.factors = factors;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

    public void setIncludeInteractions( boolean includeInteractions ) {
        this.includeInteractions = includeInteractions;
    }

    /**
     * @param subsetFactor the subsetFactor to set
     */
    public void setSubsetFactor( ExperimentalFactor subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

}
