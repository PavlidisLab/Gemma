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
package ubic.gemma.core.analysis.expression.coexpression.links;

import ubic.gemma.core.analysis.preprocess.SVDRelatedPreprocessingException;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilterConfig;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;

/**
 * @author paul
 */
public interface LinkAnalysisService {

    /**
     * Run a link analysis on an experiment, and persist the results if the configuration says to.
     *
     * @param ee                 Experiment to be processed
     * @param filterConfig       Configuration for filtering of the input data.
     * @param linkAnalysisConfig Configuration for the link analysis.
     * @return analysis
     */
    LinkAnalysis process( ExpressionExperiment ee, ExpressionExperimentFilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig );

    /**
     * Used when the input is data vectors from another source, instead of from a DB-bound expressionExperiment. Example
     * would be vectors read from a file. Output is always 'text', and DB is not used. Intensity-level-based filtering
     * is not available, so the data should be pre-filtered if you need that.
     *
     * @param linkAnalysisConfig - must include the array name.
     * @param dataVectors        data vectors
     * @param filterConfig       filter config
     * @param t                  taxon
     * @return analysis
     */
    @SuppressWarnings("UnusedReturnValue")
    // Possible external use
    LinkAnalysis processVectors( Taxon t, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionExperimentFilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig ) throws FilteringException, SVDRelatedPreprocessingException;

}