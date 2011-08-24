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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorDao extends DesignElementDataVectorDao<ProcessedExpressionDataVector> {

    public enum RankMethod {
        mean, max
    }

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed. If the values already exist, they will be re-written.
     * 
     * @param expressionExperiment
     * @return the updated expressionExperiment.
     */
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment expressionExperiment );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<? extends BioAssaySet> bioassaySets,
            Collection<Gene> genes, boolean fullMap );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Gene> genes );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, int limit, boolean fullMap );

    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<CompositeSequence> probes,
            boolean fullMap );

    /**
     * @param expressionExperiment
     * @return Processed data for the given experiment. NOTE the vectors are thawed before returning.
     */
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment );

    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment,
            Integer limit );

    public Collection<ProcessedExpressionDataVector> find( QuantitationType quantitationType );

    public Collection<ProcessedExpressionDataVector> find( Collection<QuantitationType> quantitationType );

    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method );

    /**
     * Retrieve expression level information for genes in experiments.
     * 
     * @param expressionExperiments
     * @param genes
     * @return A map of experiment -> gene -> probe -> array of doubles holding the 1) mean and 2) max expression rank.
     */
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes );

    public void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment );

    public Collection<? extends DesignElementDataVector> find( ArrayDesign arrayDesign,
            QuantitationType quantitationType );

}
