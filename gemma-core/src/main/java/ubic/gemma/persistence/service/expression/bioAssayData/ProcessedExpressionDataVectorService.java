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
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Paul
 */
public interface ProcessedExpressionDataVectorService
        extends DesignElementDataVectorService<ProcessedExpressionDataVector> {

    /**
     * @see ProcessedExpressionDataVectorDao#createProcessedDataVectors(ExpressionExperiment, boolean)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int createProcessedDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * @see ProcessedExpressionDataVectorDao#createProcessedDataVectors(ExpressionExperiment, boolean)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException;

    /**
     * Create processed vectors and update ranks.
     * <p>
     * Mismatch between quantitation type and data is ignored.
     * @see #createProcessedDataVectors(ExpressionExperiment)
     * @see #updateRanks(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int computeProcessedExpressionData( ExpressionExperiment ee );

    /**
     * Create processed vectors and update ranks.
     * @see #createProcessedDataVectors(ExpressionExperiment, boolean)
     * @see #updateRanks(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int computeProcessedExpressionData( ExpressionExperiment ee, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException;

    /**
     * Replace the processed vectors of a EE with the given vectors.
     * <p>
     * Ranks are recomputed.
     *
     * @param ee      ee
     * @param vectors non-persistent, all of the same quantitationtype
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Creates new bioAssayDimensions to match the experimental design, reorders the data to match, updates.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void reorderByDesign( ExpressionExperiment ee );

    /**
     * Update the ranks of the processed vectors for the given experiment.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void updateRanks( ExpressionExperiment ee );

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
     * Retrieve expression levels by dataset IDs.
     * @see #getExpressionLevels(Collection, Collection, boolean, String)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<ExperimentExpressionLevelsValueObject> getExpressionLevelsByIds( Collection<Long> datasetIds,
            Collection<Gene> genes, boolean keepNonSpecific, @Nullable String consolidationMode );

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
    List<ExperimentExpressionLevelsValueObject> getExpressionLevelsDiffEx( Collection<ExpressionExperiment> ees,
            Long diffExResultSetId, double threshold, int max, boolean keepGeneNonSpecific, @Nullable String consolidateMode );

    /**
     * @see CachedProcessedExpressionDataVectorService#getProcessedDataArrays(Collection, Collection)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( Collection<ExpressionExperiment> expressionExperiments, Collection<Long> genes );

    /**
     * @see CachedProcessedExpressionDataVectorService#getProcessedDataArrays(BioAssaySet, Collection)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet bioAssaySet, Collection<Long> genes );

    /**
     * @see CachedProcessedExpressionDataVectorService#getProcessedDataArrays(BioAssaySet)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment );

    /**
     * @see CachedProcessedExpressionDataVectorService#getRandomProcessedDataArrays(BioAssaySet, int)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<DoubleVectorValueObject> getRandomProcessedDataArrays( ExpressionExperiment ee, int limit );

    /**
     * @see CachedProcessedExpressionDataVectorService#getProcessedDataArraysByProbe(Collection, Collection)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<CompositeSequence> compositeSequences );

    /**
     * @see ProcessedExpressionDataVectorDao#getProcessedVectors(ExpressionExperiment)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve and thaw a collection of vectors for a given experiment.
     * @see ProcessedExpressionDataVectorDao#getProcessedVectors(ExpressionExperiment)
     * @see ProcessedExpressionDataVectorDao#thaw(Collection)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<ProcessedExpressionDataVector> getProcessedDataVectorsAndThaw( ExpressionExperiment expressionExperiment );

    /**
     * @see ProcessedExpressionDataVectorDao#getRanks(ExpressionExperiment, Collection, ProcessedExpressionDataVectorDao.RankMethod)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes,
            ProcessedExpressionDataVectorDao.RankMethod method );

    List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, Double threshold, int maxNumberOfResults );
}
