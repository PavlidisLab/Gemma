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
package ubic.gemma.analysis.expression.coexpression.links;

import java.util.Collection;

import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @version $Id$
 */
public interface LinkAnalysisService {

    /**
     * Run a link analysis on an experiment, and persist the results if the configuration says to.
     * 
     * @param ee Experiment to be processed
     * @param filterConfig Configuration for filtering of the input data.
     * @param linkAnalysisConfig Configuration for the link analysis.
     * @throws Exception
     */
    public abstract LinkAnalysis process( Long eeId, FilterConfig filterConfig,
            LinkAnalysisConfig linkAnalysisConfig );

    /**
     * Used when the input is data vectors from another source, instead of from a DB-bound expressionExperiment. Example
     * would be vectors read from a file. Output is always 'text', and DB is not used. Intensity-level-based filtering
     * is not available, so the data should be pre-filtered if you need that.
     * 
     * @param t
     * @param dataVectors
     * @param filterConfig
     * @param linkAnalysisConfig - must include the array name.
     */
    public abstract LinkAnalysis process( Taxon t, Collection<ProcessedExpressionDataVector> dataVectors,
            FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig );

}