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

import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Paul
 */
public interface ProcessedExpressionDataVectorDao extends DesignElementDataVectorDao<ProcessedExpressionDataVector> {

    /**
     * @param expressionExperiment ee
     * @return Processed data for the given experiment. NOTE the vectors are thawed before returning.
     */
    Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve a slice of processed vectors.
     */
    List<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, int offset, int limit );

    /**
     * Obtain processed expression vectors with their associated genes.
     *
     * @param  cs2gene Map of probe to genes.
     * @param  ees     ees
     * @return map of vectors to genes.
     */
    Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectorsAndGenes( Collection<ExpressionExperiment> ees, Map<Long, Collection<Long>> cs2gene );

    /**
     * Retrieve the processed vectors for a set of design elements within a given quantitation type. Symmetric with
     * {@link RawExpressionDataVectorDao#find(Collection, QuantitationType)}. Returns the stored entities, whose data
     * has NOT had read-time outlier masking applied — used to recover un-masked values for a previously selected set
     * of probes.
     *
     * @param designElements   probes to retrieve; empty collection yields an empty result.
     * @param quantitationType the (processed) quantitation type; scopes the result to a single experiment.
     * @return the matching processed vectors, thawed for the bioAssayDimension / quantitationType / designElement.
     */
    Collection<ProcessedExpressionDataVector> find( Collection<CompositeSequence> designElements, QuantitationType quantitationType );

    Collection<ProcessedExpressionDataVector> getRandomProcessedVectors( ExpressionExperiment ee, int limit );

    /**
     * Only retrieve the design elements for a slice of vectors.
     */
    List<CompositeSequence> getProcessedVectorsDesignElements( ExpressionExperiment ee, BioAssayDimension dimension, int offset, int limit );

    Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    enum RankMethod {
        max, mean
    }

    /**
     * Obtain the genes associated to each vector.
     */
    Map<ProcessedExpressionDataVector, Collection<Long>> getGenes( Collection<ProcessedExpressionDataVector> vectors );
}
