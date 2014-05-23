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
import java.util.Map;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface GeneDifferentialExpressionService {

    /**
     * p values smaller than this will be treated as this value in a meta-analysis. The reason is to avoid extremely low
     * pvalues from driving meta-pvalues down too fast. This is suggested by the fact that very small pvalues presume an
     * extremely high precision in agreement between the tails of the true null distribution and the analytic
     * distribution used to compute the pvalues (e.g., F or t).
     */
    public static final double PVALUE_CLIP_THRESHOLD = 1e-8;

    /**
     * @param ef
     * @return
     */
    public abstract ExperimentalFactorValueObject configExperimentalFactorValueObject( ExperimentalFactor ef );

    /**
     * @param ee
     * @return
     */
    public abstract ExpressionExperimentValueObject configExpressionExperimentValueObject(
            ExpressionExperimentValueObject ee );

    /**
     * Get the differential expression results for the given gene that is in a specified set of experiments.
     * 
     * @param gene : gene of interest
     * @param Experiments : set of experiments to search
     * @return
     */
    public abstract Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            Collection<BioAssaySet> ees );

    /**
     * Get the differential expression results for the given gene that is in a specified set of experiments.
     * 
     * @param gene : gene of interest
     * @param Experiments : set of experiments to search
     * @param threshold : the cutoff to determine if diff expressed
     * @param limit : the maximum number of results to return (null for all)
     * @return
     */
    public abstract Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            Collection<BioAssaySet> ees, double threshold, Integer limit );

    /**
     * Get differential expression for a gene, constrained to a specific set of factors. Note that interactions are
     * ignored, only main effects (the factorMap can only have one factor per experiment)
     * 
     * @param gene
     * @param threshold
     * @param factorMap
     * @return
     */
    public abstract Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap );

    /**
     * Get the differential expression results for the given gene across all datasets.
     * 
     * @param gene
     * @param threshold
     * @return
     */
    public abstract Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            double threshold, Integer limit );

    /**
     * Get the differential expression analysis results for the gene in the activeExperiments.
     * 
     * @param threshold
     * @param g
     * @param eeFactorsMap
     * @param activeExperiments
     * @return
     */
    public abstract DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis(
            double threshold, Gene g, Map<Long, Long> eeFactorsMap, Collection<BioAssaySet> activeExperiments );

}