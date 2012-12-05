/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Service methods to do database-related work for differential expression analysis
 * 
 * @author Paul
 * @version $Id$
 */
public interface DifferentialExpressionAnalysisHelperService {

    public static final String FACTOR_NAME_MANGLING_DELIMITER = "__";

    /**
     * @param ee
     * @param differentialExpressionAnalysis
     */
    public void writeDistributions( BioAssaySet ee, DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Delete all the differential expression analyses for the experiment.
     * 
     * @param expressionExperiment
     * @return how many analyses were deleted.
     */
    public abstract int deleteAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * Delete an old analysis.
     * 
     * @param expressionExperiment
     * @param existingAnalysis
     */
    public abstract void deleteAnalysis( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysis existingAnalysis );

    /**
     * @param expressionExperiment
     * @param diffExpressionAnalyses
     * @param factors
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> persistAnalyses( ExpressionExperiment expressionExperiment,
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses, Collection<ExperimentalFactor> factors );

}
