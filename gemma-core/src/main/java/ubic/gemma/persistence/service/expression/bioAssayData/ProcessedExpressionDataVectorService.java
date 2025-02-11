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
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Paul
 */
public interface ProcessedExpressionDataVectorService
        extends DesignElementDataVectorService<ProcessedExpressionDataVector> {

    /**
     * Create processed vectors and optionally update ranks.
     * <p>
     * Mismatch between quantitation type and data is ignored.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
     * @param updateRanks whether to update the rnaks of the vectors or not
     * @see #updateRanks(ExpressionExperiment)
     * @see ProcessedExpressionDataVectorDao#createProcessedDataVectors(ExpressionExperiment, boolean)
     * @throws QuantitationTypeConversionException if the data cannot be converted, generally to log2 scale
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks ) throws QuantitationTypeConversionException;

    /**
     * Create processed vectors and optionally update ranks.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
     * @see #createProcessedDataVectors(ExpressionExperiment, boolean)
     * @see #updateRanks(ExpressionExperiment)
     * @throws QuantitationTypeDetectionException if the QT caanot be detected from data, never raised if
     * ignoreQuantitationMismatch is set to true
     * @throws QuantitationTypeConversionException if the data cannot be converted, generally to log2 scale
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean updateRanks, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException;

    /**
     * Replace the processed vectors of a EE with the given vectors.
     * <p>
     * Ranks are recomputed, no conversion of QT is done.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
     *
     * @param ee      ee
     * @param vectors non-persistent, all of the same {@link QuantitationType}
     * @param updateRanks whether to update ranks or not
     * @see ProcessedExpressionDataVectorDao#createProcessedDataVectors(ExpressionExperiment, boolean)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors, boolean updateRanks );

    /**
     * Remove the processed vectors of an EE.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
     * @see ExpressionExperimentService#removeProcessedDataVectors(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Creates new bioAssayDimensions to match the experimental design, reorders the data to match, updates.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void reorderByDesign( ExpressionExperiment ee );

    /**
     * Update the ranks of the processed vectors for the given experiment.
     * <p>
     * This also adds an audit event and evict the vectors from the cache.
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

    Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( ExpressionExperiment ee, Collection<CompositeSequence> compositeSequences );

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

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Slice<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, int offset, int limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Slice<CompositeSequence> getProcessedDataVectorsDesignElements( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, int offset, int limit );

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

    List<DoubleVectorValueObject> getDiffExVectors( Long resultSetId, double threshold, int maxNumberOfResults );

    void evictFromCache( ExpressionExperiment ee );
}
