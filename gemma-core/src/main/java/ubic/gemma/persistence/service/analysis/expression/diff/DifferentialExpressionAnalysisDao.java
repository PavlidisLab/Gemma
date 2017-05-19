/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.persistence.service.analysis.AnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @version $Id$
 * @see DifferentialExpressionAnalysis
 */
public interface DifferentialExpressionAnalysisDao extends AnalysisDao<DifferentialExpressionAnalysis> {

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param ears
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param gene
     * @param resultSet
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold );

    /**
     * @param ef
     * @return analyses associated with the factor, either through the subsetfactor or as factors for resultsets.
     */
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * 
     */
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            Collection<Long> investigationIds );

    /**
     * @param gene
     * @return
     */
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene );

    /**
     * @param expressionExperiments
     * @return
     */
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments );

    /**
     * @param idsToFilter
     * @return
     */
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * @param taxon
     * @return
     */
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    /**
     * 
     */
    public void thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * 
     */
    public void thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Given a set of ids, find experiments or experimentsubsets that have differential expression analyses. Subsets are
     * handled two ways: if the ID given is of a subset, or if the ID is of an experiment that has subsets. In the
     * latter case, the return value will contain experiments that were not explicitly queried for. Note that this has
     * to be called from a method that replaces the keys with experiments or valueobjects for security filtering.
     * 
     * @param ids
     * @return
     */
    Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds( Collection<Long> ids );
}