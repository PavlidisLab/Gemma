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

package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Paul
 */
public interface ProcessedExpressionDataVectorDao extends DesignElementDataVectorDao<ProcessedExpressionDataVector> {

    void clearCache();

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed. If the values already exist, they will be re-written. The data will be
     * quantile normalized (with some exceptions: ratios and count data will not be normalized).
     *
     * @param expressionExperiment       ee
     * @param ignoreQuantitationMismatch use raw data to infer scale type and the adequate transformation for producing
     *                                   processed EVs instead of relying on the QT
     * @return the created processed vectors
     */
    Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException;

    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment );

    @SuppressWarnings("unused")
        // Possible external use
    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes );

    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment, int limit );

    Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<? extends BioAssaySet> expressionExperiments,
            Collection<Long> genes );

    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<CompositeSequence> probes );

    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee, Collection<Long> probes );

    /**
     * @param expressionExperiment ee
     * @return Processed data for the given experiment. NOTE the vectors are thawed before returning.
     */
    Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment );

    Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method );

    /**
     * Retrieve expression level information for genes in experiments.
     *
     * @param genes                 genes
     * @param expressionExperiments expression experiments
     * @return A map of experiment -&gt; gene -&gt; probe -&gt; array of doubles holding the 1) mean and 2) max expression rank.
     */
    Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes );

    void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment );

    enum RankMethod {
        max, mean
    }

}
