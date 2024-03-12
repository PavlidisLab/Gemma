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

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Paul
 */
@SuppressWarnings("unused") // Possible external use
public interface ProcessedExpressionDataVectorService
        extends DesignElementDataVectorService<ProcessedExpressionDataVector> {

    /**
     * Replace the processed vectors of a EE with the given vectors.
     * <p>
     * Ranks are recomputed.
     *
     * @param ee      ee
     * @param vectors non-persistent, all of the same quantitationtype
     */
    @Secured({ "GROUP_USER" })
    void replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    @Transactional
    @Secured({ "GROUP_USER" })
    void updateRanks( ExpressionExperiment ee );

    @Secured({ "GROUP_ADMIN" })
    void clearCache();

    @Secured({ "GROUP_USER" })
    Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed.
     *
     * @param expressionExperiment ee
     * @return updated expressionExperiment
     */
    @Secured({ "GROUP_USER" })
    Set<ProcessedExpressionDataVector> createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreInferredScale ) throws QuantitationMismatchException;

    /**
     * @param expressionExperiments - expressionExperiments or expressionExperimentSubSets
     * @param genes                 genes
     * @return vectors, which will be subsetted if the bioassayset is a subset.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<ExpressionExperiment> expressionExperiments,
            Collection<Long> genes );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, Collection<Long> genes );

    /**
     * @param ees                 expressionExperiments
     * @param genes               genes
     * @param consolidateMode     how to consolidate the vectors when there is more than one
     * @param keepGeneNonSpecific whether to keep vectors that are not specific to the gene
     * @return value objects containing structured information about the expression levels of given genes
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    List<ExperimentExpressionLevelsValueObject> getExpressionLevels( Collection<ExpressionExperiment> ees,
            Collection<Gene> genes, boolean keepGeneNonSpecific, @Nullable String consolidateMode );

    /**
     * @param ees                 expressionExperiments
     * @param component           the principal component
     * @param threshold           threshold
     * @param consolidateMode     how to consolidate the vectors when there is more than one
     * @param keepGeneNonSpecific whether to keep vectors that are not specific to the gene
     * @return value objects containing structured information about the expression levels of genes highly loaded in
     * the given principal component.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    @Transactional(readOnly = true)
    List<ExperimentExpressionLevelsValueObject> getExpressionLevelsPca( Collection<ExpressionExperiment> ees,
            int threshold, int component, boolean keepGeneNonSpecific, @Nullable String consolidateMode );

    /**
     * @param diffExResultSetId   the differential expression result set to access
     * @param threshold           threshold
     * @param consolidateMode     how to consolidate the vectors when there is more than one
     * @param keepGeneNonSpecific whether to keep vectors that are not specific to the gene
     * @param ees                 ees
     * @param max                 max level
     * @return value objects containing structured information about the expression levels of genes highly loaded in
     * the given principal component.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    @Transactional(readOnly = true)
    List<ExperimentExpressionLevelsValueObject> getExpressionLevelsDiffEx( Collection<ExpressionExperiment> ees,
            Long diffExResultSetId, double threshold, int max, boolean keepGeneNonSpecific, @Nullable String consolidateMode );

    /**
     * @param expressionExperiment ee
     * @return double vector vos
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment );

    /**
     * @param limit (null limit = default hibernate limit).
     * @param ee    ee
     * @return double vector vos
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment ee, int limit );

    /**
     * Retrieves DEDV's by probes and experiments
     *
     * @param expressionExperiments EEs
     * @param compositeSequences    composite sequences
     * @return double vector vos
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<CompositeSequence> compositeSequences );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet analyzedSet,
            Collection<Long> probes );

    Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment );

    Collection<ProcessedExpressionDataVector> getProcessedDataVectorsAndThaw( ExpressionExperiment expressionExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_AFTER_MAP_READ", "ACL_SECURABLE_COLLECTION_READ" })
    Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes,
            ProcessedExpressionDataVectorDao.RankMethod method );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            ProcessedExpressionDataVectorDao.RankMethod method );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment,
            ProcessedExpressionDataVectorDao.RankMethod method );

    /**
     * Retrieve expression level information for genes in experiments.
     *
     * @param eeCol ees
     * @param pars  genes
     * @return A map of experiment -&gt; gene -&gt; probe -&gt; array of doubles holding the 1) mean and 2) max expression rank.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> eeCol, Collection<Gene> pars );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment );

    List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, Double threshold, int maxNumberOfResults );

    /**
     * Compute processed expression data, ignoring mismatched with the inferred scale.
     */
    @Secured({ "GROUP_ADMIN" })
    void computeProcessedExpressionData( ExpressionExperiment ee );

    /**
     * This method should not be called on its own, if possible. Use the PreprocessorService to do all necessary
     * refreshing.
     */
    @Secured({ "GROUP_ADMIN" })
    void computeProcessedExpressionData( ExpressionExperiment ee, boolean ignoreInferredScale ) throws QuantitationMismatchException;


    /**
     * Creates new bioAssayDimensions to match the experimental design, reorders the data to match, updates.
     *
     * @param eeId the experiment id
     */
    @Secured({ "GROUP_ADMIN" })
    void reorderByDesign( ExpressionExperiment ee );

    @Secured({ "GROUP_USER" })
    void update( Collection<ProcessedExpressionDataVector> updatedVectors );
}
