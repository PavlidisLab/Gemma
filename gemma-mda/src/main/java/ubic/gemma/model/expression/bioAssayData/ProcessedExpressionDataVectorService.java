/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao.RankMethod;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorService {

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed.
     * 
     * @param expressionExperiment
     */
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiments
     * @param limit (null limit = default hibernate limit). 
     * @param boolean fullMap true returns pars, predicted genes and known genes, false just returns known genes
     * @return
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            ExpressionExperiment ee, int limit, boolean fullMap );

    /**
     * @param expressionExperiments
     * @param genes
     * @return
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes );

    
    /**
     * @param expressionExperiments
     * @param genes
     * @param fullMapping  if false only returns probe to known gene mappings, if true returns all (PARs, PGs, KGs), 
     * @return
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, Boolean fullMapping );

    /**
     * @param expressionExperiment
     * @return
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment
     * @param genes
     * @return
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes );

    /**
     * @param expressionExperiment
     * @return
     */
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment );

    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    /**
     * @param expressionExperiment
     * @param genes
     * @param method
     * @return
     */
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    /**
     * @param expressionExperiment
     * @param method
     * @return
     */
    public Map<DesignElement, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method );

    /**
     * @param vectors
     */
    public void thaw( Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Updates a collection of ProcessedExpressionDataVectors
     */
    public void update( java.util.Collection<ProcessedExpressionDataVector> dedvs );

    /**
     * Retrieve expression level information for genes in experiments.
     * 
     * @param expressionExperiments
     * @param genes
     * @return A map of experiment -> gene -> probe -> array of doubles holding the 1) mean and 2) max expression rank.
     */
    public Map<ExpressionExperiment, Map<Gene, Map<DesignElement, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> pars );

    
    /**
     * Retireves DEDV's by probes and experiments
     * 
     * @param expressionExperiments
     * @param compositeSequences
     * @param fullMap
     * @return DVVOs 
     */
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<CompositeSequence> compositeSequences, boolean fullMap );

}
