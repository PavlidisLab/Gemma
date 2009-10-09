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
import java.util.Map;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public interface DifferentialExpressionResultDao extends BaseDao<ProbeAnalysisResult> {

    /**
     * Creates a new instance of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult and adds
     * from the passed in <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
     *        instances to create.
     * @return the created instances.
     */
    public java.util.Collection<ProbeAnalysisResult> create( java.util.Collection<ProbeAnalysisResult> entities );

    /**
     * Creates an instance of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult and adds it
     * to the persistent store.
     */
    public ProbeAnalysisResult create( ProbeAnalysisResult differentialExpressionAnalysisResult );

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

    /**
     * Loads an instance of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult from the
     * persistent store.
     */
    public ProbeAnalysisResult load( java.lang.Long id );

    /**
     * Removes the instance of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult having the
     * given <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection<ProbeAnalysisResult> entities );

    /**
     * Removes the instance of ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult from the
     * persistent store.
     */
    public void remove( ProbeAnalysisResult differentialExpressionAnalysisResult );

    public void thaw( final ProbeAnalysisResult result ) throws Exception;

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection<ProbeAnalysisResult> entities );

    /**
     * Updates the <code>differentialExpressionAnalysisResult</code> instance in the persistent store.
     */
    public void update( ProbeAnalysisResult differentialExpressionAnalysisResult );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link ExpressionExperiment}.
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed );

    /**
     * Returns a map of a collection of {@link ProbeAnalysisResult}s keyed by {@link ExpressionExperiment}.
     * 
     * @param gene
     * @return Map<ExpressionExperiment, Collection<ProbeAnalysisResult>>
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene );

    /**
     * Find differential expression for a gene, exceeding a given significance level (using the corrected pvalue field)
     * 
     * @param gene
     * @param threshold
     * @param limit
     * @return
     */
    java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene, double threshold, Integer limit );

    /**
     * Find differential expression for a gene in given data sets, exceeding a given significance level (using the
     * corrected pvalue field)
     * 
     * @param gene
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold, Integer limit );

    /**
     * Given a list of experiments and a threshold value finds all the probes that met the cut off in the given
     * experiments
     * 
     * @param experimentsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> find(
            java.util.Collection<ubic.gemma.model.expression.experiment.ExpressionExperiment> experimentsAnalyzed,
            double threshold, Integer limit );

    /**
     * Given a list of result sets finds the diff expression results that met the given threshold
     * 
     * @param resultsAnalyzed
     * @param threshold
     * @return
     */
    public java.util.Map<ExpressionAnalysisResultSet, java.util.Collection<ProbeAnalysisResult>> findInResultSets(
            java.util.Collection<ExpressionAnalysisResultSet> resultsAnalyzed, double threshold, Integer limit );

}
