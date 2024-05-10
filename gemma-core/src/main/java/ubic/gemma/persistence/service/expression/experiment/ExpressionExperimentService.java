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
package ubic.gemma.persistence.service.expression.experiment;

import lombok.Value;
import org.springframework.security.access.annotation.Secured;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.CuratableService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public interface ExpressionExperimentService
        extends CuratableService<ExpressionExperiment, ExpressionExperimentValueObject> {

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor );

    /**
     * @param ee experiment.
     * @param fv must already have the experimental factor filled in.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv );

    /**
     * Intended with the case of a continuous factor being added.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void addFactorValues( ExpressionExperiment ee, Map<BioMaterial, FactorValue> fvs );

    /**
     * Used when we want to add data for a quantitation type. Does not remove any existing vectors.
     *
     * @param eeToUpdate experiment to be updated.
     * @param newVectors vectors to be added.
     * @return the number of added vectors
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int addRawVectors( ExpressionExperiment eeToUpdate, Collection<RawExpressionDataVector> newVectors );

    /**
     * @see ExpressionExperimentDao#replaceRawDataVectors(ExpressionExperiment, QuantitationType, Collection)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<RawExpressionDataVector> vectors );

    /**
     * Used when we are replacing data, such as when converting an experiment from one platform to another. Examples
     * would be exon array or RNA-seq data sets, or other situations where we are replacing data. Does not take care of
     * computing the processed data vectors, but it does clear them out.
     *
     * @param ee      experiment
     * @param vectors If they are from more than one platform, that will be dealt with.
     * @return the number of vectors replaced
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceAllRawDataVectors( ExpressionExperiment ee, Collection<RawExpressionDataVector> vectors );

    /**
     * @see ExpressionExperimentDao#removeAllRawDataVectors(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeAllRawDataVectors( ExpressionExperiment ee );

    /**
     * @see ExpressionExperimentDao#removeRawDataVectors(ExpressionExperiment, QuantitationType)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ExpressionExperimentDao#createProcessedDataVectors(ExpressionExperiment, Collection)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * @see ExpressionExperimentDao#replaceProcessedDataVectors(ExpressionExperiment, Collection)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * @see ExpressionExperimentDao#removeProcessedDataVectors(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeProcessedDataVectors( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    List<ExpressionExperiment> browse( int start, int limit );

    @Nullable
    BatchInformationEvent checkBatchFetchStatus( ExpressionExperiment ee );

    boolean checkHasBatchInfo( ExpressionExperiment ee );

    /**
     * returns ids of search results.
     *
     * @param searchString search string
     * @return collection of ids or an empty collection.
     */
    Collection<Long> filter( String searchString ) throws SearchException;

    /**
     * Remove IDs of Experiments that are not from the given taxon.
     *
     * @param ids   collection to purge.
     * @param taxon taxon to retain.
     * @return purged IDs.
     */
    Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment loadWithCharacteristics( Long id );

    /**
     * Load a {@link BioAssaySet} by ID which can be either a {@link ExpressionExperiment} or a {@link ExpressionExperimentSubSet}.
     */
    @Nullable
    BioAssaySet loadBioAssaySet( Long id );

    /**
     * Load an experiment and thaw it as per {@link #thawLite(ExpressionExperiment)} or fail with the supplied exception
     * and message.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    /**
     * Load an experiment and thaw it as per {@link #thaw(ExpressionExperiment)}.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment loadAndThaw( Long id );

    /**
     * Load an experiment and thaw it as per {@link #thawLite(ExpressionExperiment)} or fail with the supplied exception
     * and message.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    long countWithCache( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Slice<ExpressionExperimentValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment loadWithPrimaryPublication( Long id );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment loadWithMeanVarianceRelation( Long id );

    /**
     * @param accession accession
     * @return Experiments which have the given accession. There can be more than one, because one GEO
     * accession can result
     * in multiple experiments in Gemma.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByAccession( String accession );

    /**
     * @param bibRef bibliographic reference
     * @return a collection of EE that have that reference that BibliographicReference
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByBibliographicReference( BibliographicReference bibRef );

    /**
     * @param ba bio material
     * @return experiment the given bioassay is associated with
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByBioAssay( BioAssay ba );

    /**
     * @param bm bio material
     * @return experiment the given biomaterial is associated with
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByBioMaterial( BioMaterial bm );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
        // slight security overkill, if they got the biomaterial...
    Map<ExpressionExperiment, BioMaterial> findByBioMaterials( Collection<BioMaterial> bioMaterials );

    /**
     * @param gene gene
     * @param rank rank
     * @return a collection of expression experiment ids that express the given gene above the given expression
     * level
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, double rank );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByDesign( ExperimentalDesign ed );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactor( ExperimentalFactor factor );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactorValue( FactorValue factorValue );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactorValue( Long factorValueId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
        // slight security overkill, if they got the factorvalue...
    Map<ExpressionExperiment, FactorValue> findByFactorValues( Collection<FactorValue> factorValues );

    /**
     * @param gene gene
     * @return a collection of expression experiments that have an AD that detects the given Gene (ie a probe on
     * the AD
     * hybridizes to the given Gene)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByGene( Gene gene );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByQuantitationType( QuantitationType type );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment findByShortName( String shortName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByTaxon( Taxon taxon );

    @Secured({ "GROUP_AGENT", "AFTER_ACL_COLLECTION_READ" })
    List<ExpressionExperiment> findByUpdatedLimit( int limit );

    @Secured({ "GROUP_AGENT", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findUpdatedAfter( Date date );

    /**
     * @param ids ids
     * @return the map of ids to number of terms associated with each expression experiment.
     */
    Map<Long, Long> getAnnotationCountsByIds( Collection<Long> ids );

    /**
     * @param eeId experiment id.
     * @return the terms associated this expression experiment.
     */
    Set<AnnotationValueObject> getAnnotationsById( Long eeId );

    /**
     * Apply ontological inference to augment a filter with additional terms.
     *
     * @param mentionedTerms if non-null, all the terms explicitly mentioned in the filters are added to the collection.
     * @param inferredTerms  if non-null, all the terms inferred from those mentioned in the filters are added to the
     *                       collection
     */
    Filters getFiltersWithInferredAnnotations( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException;

    @Value
    class CharacteristicWithUsageStatisticsAndOntologyTerm {
        /**
         * Characteristic.
         */
        Characteristic characteristic;
        /**
         * The number of associated {@link ExpressionExperiment}.
         */
        Long numberOfExpressionExperiments;
        /**
         * An associated ontology term if available.
         * <p>
         * The {@link #characteristic} must have a non-null {@link Characteristic#getValueUri()} and must be retrievable
         * via {@link ubic.gemma.core.ontology.OntologyService#getTerm(String)} for this property to be filled.
         */
        @Nullable
        OntologyTerm term;
    }

    /**
     * Special indicator for free-text terms.
     * @see ExpressionExperimentDao#FREE_TEXT
     */
    String FREE_TEXT = ExpressionExperimentDao.FREE_TEXT;

    /**
     * Special indicator for uncategorized terms.
     * @see ExpressionExperimentDao#UNCATEGORIZED
     */
    String UNCATEGORIZED = ExpressionExperimentDao.UNCATEGORIZED;

    /**
     * Obtain category usage frequency for datasets matching the given filter.
     *
     * @param filters              filters restricting the terms to a given set of datasets
     * @param excludedCategoryUris ensure that the given category URIs are excluded
     * @param excludedTermUris     ensure that the given term URIs and their sub-terms (as per {@code subClassOf} relation)
     *                             are excluded; this requires relevant ontologies to be loaded in {@link ubic.gemma.core.ontology.OntologyService}.
     * @param retainedTermUris     ensure that the given terms are retained (overrides any exclusion from minFrequency and excludedTermUris)
     * @param maxResults           maximum number of results to return
     */
    Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults );

    /**
     * Obtain annotation usage frequency for datasets matching the given filters.
     * <p>
     * Terms may originate from the experiment tags, experimental design or samples.
     * <p>
     * The implementation uses a denormalized table for associating EEs to characteristics which is not always in sync
     * if new terms are attached.
     *
     * @param filters              filters restricting the terms to a given set of datasets
     * @param category             a category to restrict annotations to, or null to include all categories
     * @param excludedCategoryUris ensure that the given category URIs are excluded
     * @param excludedTermUris     ensure that the given term URIs and their sub-terms (as per {@code subClassOf} relation)
     *                             are excluded; this requires relevant ontologies to be loaded in {@link ubic.gemma.core.ontology.OntologyService}.
     * @param minFrequency         minimum occurrences of a term to be included in the results
     * @param retainedTermUris     ensure that the given terms are retained (overrides any exclusion from minFrequency and excludedTermUris)
     * @param maxResults           maximum number of results to return
     * @return mapping annotations grouped by category and term (URI or value if null) to their number of occurrences in
     * the matched datasets and ordered in descending number of associated experiments
     * @see ExpressionExperimentDao#getAnnotationsUsageFrequency(Collection, Class, int, int, String, Collection, Collection, Collection)
     */
    List<CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults );

    /**
     * @param expressionExperiment experiment
     * @return a collection of ArrayDesigns referenced by any of the BioAssays that make up the
     * given
     * ExpressionExperiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet expressionExperiment );

    Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    /**
     * Calculate the usage frequency of platforms by the datasets matching the provided filters.
     *
     * @param filters    a set of filters to be applied as per {@link #load(Filters, Sort, int, int)}
     * @param maxResults the maximum of results, or unlimited if less than 1
     */
    Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, int maxResults );

    /**
     * Calculate the usage frequency of taxa by the datasets matching the provided filters.
     * <p>
     * If no filters are supplied (either being null or empty), the {@link #getPerTaxonCount()} fast path is used.
     *
     * @see #getPerTaxonCount()
     */
    Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    /**
     * Checks the experiment for a batch confound.
     *
     * @param ee the experiment to check.
     * @return a string describing the batch confound, or null if there was no batch confound.[FIXME: String return value is unsafe]
     */
    @Nullable
    String getBatchConfound( ExpressionExperiment ee );

    /**
     * Obtain the full batch effect details of a given experiment.
     * @param ee experiment
     * @return details for the principal component most associated with batches (even if it isn't "significant"). Note
     * that we don't look at every component, just the first few.
     */
    BatchEffectDetails getBatchEffectDetails( ExpressionExperiment ee );

    /**
     * Obtain a {@link BatchEffectType} describing the batch effect state of the given experiment.
     * @param ee the experiment to get the batch effect for.
     */
    BatchEffectType getBatchEffect( ExpressionExperiment ee );

    /**
     * Obtain a string describing the summary statistics of a batch effect is present in the given experiment.
     * @return summary statistics or null if there is no batch effect
     */
    @Nullable
    String getBatchEffectStatistics( ExpressionExperiment ee );

    /**
     * @param expressionExperiment experiment
     * @return the BioAssayDimensions for the study.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment experiment
     * @return the amount of biomaterials associated with the given expression experiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getBioMaterialCount( ExpressionExperiment expressionExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getDesignElementDataVectorCount( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    /**
     * @param expressionExperiment experiment
     * @return the date of the last time any of the array designs associated with this experiment
     * were updated.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Date getLastArrayDesignUpdate( ExpressionExperiment expressionExperiment );

    /**
     * @param ids ids
     * @return AuditEvents of the latest link analyses for the specified expression experiment ids. This returns a
     * map
     * of id -&gt; AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastLinkAnalysis( Collection<Long> ids );

    /**
     * @param ids ids
     * @return AuditEvents of the latest missing value analysis for the specified expression experiment ids. This
     * returns a map of id -&gt; AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastMissingValueAnalysis( Collection<Long> ids );

    /**
     * @param ids ids
     * @return AuditEvents of the latest rank computation for the specified expression experiment ids. This returns
     * a
     * map of id -&gt; AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastProcessedDataUpdate( Collection<Long> ids );

    /**
     * @return counts of expression experiments grouped by taxon
     */
    Map<Taxon, Long> getPerTaxonCount();

    /**
     * @param ids ids
     * @return map of ids to how many factor values the experiment has, counting only factor values which are
     * associated
     * with biomaterials.
     */
    Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids );

    /**
     * @param ids ids
     * @return map of ids to how many factor values the experiment has, counting only factor values which are
     * associated
     * with biomaterials and only factors that aren't batch
     */
    Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    /**
     * Iterates over the quantitation types for a given expression experiment and returns the preferred quantitation
     * types.
     *
     * @param ee experiment
     * @return quantitation types
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    QuantitationType getPreferredQuantitationType( ExpressionExperiment ee );

    /**
     * Test if the given experiment has processed data vectors.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    boolean hasProcessedExpressionData( ExpressionExperiment ee );

    /**
     * @return counts design element data vectors grouped by quantitation type
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee );

    /**
     * @param expressionExperiment experiment
     * @return all the quantitation types used by the given expression experiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    /**
     * Load all {@link QuantitationType} associated to an expression experiment as VOs.
     * @see #getQuantitationTypes(ExpressionExperiment)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationTypeValueObject> getQuantitationTypeValueObjects( ExpressionExperiment expressionExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * @param expressionExperiment experiment
     * @return any ExpressionExperimentSubSets this Experiment might have.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    /**
     * Returns the taxon of the given expressionExperiment.
     *
     * @param bioAssaySet bioAssaySet.
     * @return taxon, or null if the experiment taxon cannot be determined (i.e., if it has no samples).
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Taxon getTaxon( BioAssaySet bioAssaySet );

    /**
     * @param expressionExperiment ee
     * @return true if this experiment was run on a sequencing-based platform.
     */
    boolean isRNASeq( ExpressionExperiment expressionExperiment );

    /**
     * Check if the dataset is either troubled or uses a troubled platform.
     */
    boolean isTroubled( ExpressionExperiment expressionExperiment );

    /**
     * @see ExpressionExperimentDao#loadDetailsValueObjects(Collection, Taxon, Sort, int, int)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsWithCache( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids );

    @Secured({ "GROUP_ADMIN", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadLackingFactors();

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadLackingTags();

    /**
     * Load VOs for the given dataset IDs and initialize their relations like {@link #load(Filters, Sort)}.
     * <p>
     * The order of VOs is preserved.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadValueObjectsByIdsWithRelationsAndCache( List<Long> ids );

    /**
     * Variant of {@link #loadValueObjectsByIds(Collection)} that preserve its input order.
     * @param ids           ids to load
     * @param maintainOrder If true, order of valueObjects returned will correspond to order of ids passed in.
     * @return value objects
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadValueObjectsByIds( List<Long> ids, boolean maintainOrder );

    /**
     * Will add the vocab characteristic to the expression experiment and persist the changes.
     *
     * @param ee the experiment to add the characteristics to.
     * @param vc If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     */
    void addCharacteristic( ExpressionExperiment ee, Characteristic vc );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thawBioAssays( ExpressionExperiment expressionExperiment );

    /**
     * Partially thaw the expression experiment given - do not thaw the raw data.
     *
     * @param expressionExperiment experiment
     * @return thawed experiment
     */
    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thawLite( ExpressionExperiment expressionExperiment );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thawLiter( ExpressionExperiment expressionExperiment );

    boolean isBlackListed( String geoAccession );

    /**
     * @return true if the experiment is not explicitly marked as unsuitable for DEA; false otherwise.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Boolean isSuitableForDEA( ExpressionExperiment ee );

    /**
     *
     * @return collection of GEO experiments which lack an association with a publication (non-GEO experiments will be ignored)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<ExpressionExperiment> getExperimentsLackingPublications();

    @Secured({ "GROUP_USER" })
    MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr );

    /**
     * @see ExpressionExperimentDao#countBioMaterials(Filters)
     */
    long countBioMaterials( @Nullable Filters filters );
}
