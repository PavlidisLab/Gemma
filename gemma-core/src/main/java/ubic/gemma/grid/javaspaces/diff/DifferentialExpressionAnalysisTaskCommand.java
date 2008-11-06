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
package ubic.gemma.grid.javaspaces.diff;

import java.util.Collection;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.grid.javaspaces.TaskCommand;
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

    public Collection<ExperimentalFactor> getFactors() {
        return factors;
    }

    public void setFactors( Collection<ExperimentalFactor> factors ) {
        this.factors = factors;
    }

    private boolean forceAnalysis = false;

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

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

}
