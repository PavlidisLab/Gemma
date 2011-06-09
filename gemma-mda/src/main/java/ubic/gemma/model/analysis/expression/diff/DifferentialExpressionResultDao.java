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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
@Repository
public interface DifferentialExpressionResultDao extends BaseDao<ProbeAnalysisResult> {

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     * 
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experimentsAnalyzed,
            double threshold, Integer limit );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link BioAssaySet}.
     * 
     * @param gene
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link BioAssaySet}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experimentsAnalyzed );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experimentsAnalyzed,
            double threshold, Integer limit );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    public Map<ExpressionAnalysisResultSet, List<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );
    
    public List<Double> findGeneInResultSets(Gene gene, ExpressionAnalysisResultSet resultSet, Collection<Long> arrayDesignIds, Integer limit );

    public List<Long> findProbeAnalysisResultIdsInResultSet( Long geneId, Long resultSetId, Integer limit );
    
    public Integer countNumberOfDifferentiallyExpressedProbes ( long resultSetId, double threshold );
    
    /**
     * 
     */
    public Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            java.util.Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults );

    /**
     * 
     */
    public java.util.Collection<ExperimentalFactor> getExperimentalFactors(
            ProbeAnalysisResult differentialExpressionAnalysisResult );

    public ProbeAnalysisResult load (Long id); 
    
    public void thaw( final ProbeAnalysisResult result );

    public void thaw( Collection<ProbeAnalysisResult> results );
      
    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     * 
     * @param gene
     * @param threshold
     * @param limit
     * @return
     */
    java.util.Map<ubic.gemma.model.expression.experiment.BioAssaySet, java.util.List<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene, double threshold, Integer limit );

}
