package ubic.gemma.persistence.service.expression.experiment;

import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.CacheMode;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BrowsingDao;
import ubic.gemma.persistence.service.CachedFilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by tesarst on 13/03/17.
 *
 * @author tesarst
 */
@SuppressWarnings("unused") // Possible external use
public interface ExpressionExperimentDao
        extends CuratableDao<ExpressionExperiment>,
        CachedFilteringVoEnabledDao<ExpressionExperiment, ExpressionExperimentValueObject>, BrowsingDao<ExpressionExperiment> {

    String OBJECT_ALIAS = "ee";

    /**
     * Load an experiment by ID with a specific cache mode.
     * <p>
     * The cache mode will be effective for the remainder of the Hibernate session.
     */
    ExpressionExperiment load( Long id, CacheMode cacheMode );

    @Data
    class Identifiers {
        Long id;
        String shortName;
        String name;
        @Nullable
        String accession;
    }

    /**
     * Load all possible identifiers for all experiments.
     */
    List<Identifiers> loadAllIdentifiers();

    @Nullable
    BioAssaySet loadBioAssaySet( Long id );

    Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon );

    @Nullable
    ExpressionExperiment findByShortName( String shortName );

    Collection<ExpressionExperiment> findByName( String name );

    @Nullable
    ExpressionExperiment findOneByName( String name );

    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    Collection<ExpressionExperiment> findByAccession( String accession );

    @Nullable
    ExpressionExperiment findOneByAccession( String accession );

    Collection<ExpressionExperiment> findByBibliographicReference( BibliographicReference bibRef );

    @Nullable
    ExpressionExperiment findByBioAssay( BioAssay ba );

    Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm );

    Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> bms );

    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank );

    @Nullable
    ExpressionExperiment findByDesign( ExperimentalDesign ed );

    @Nullable
    ExpressionExperiment findByDesignId( Long designId );

    @Nullable
    ExpressionExperiment findByFactor( ExperimentalFactor ef );

    Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors );

    @Nullable
    ExpressionExperiment findByFactorValue( FactorValue fv );

    @Nullable
    ExpressionExperiment findByFactorValue( Long factorValueId );

    Map<ExpressionExperiment, FactorValue> findByFactorValues( Collection<FactorValue> fvs );

    Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds );

    Collection<ExpressionExperiment> findByGene( Gene gene );

    @Nullable
    ExpressionExperiment findByQuantitationType( QuantitationType quantitationType );

    Collection<ExpressionExperiment> findByTaxon( Taxon taxon );

    List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, int limit );

    List<ExpressionExperiment> findByUpdatedLimit( int limit );

    /**
     * Find experiments updated on or after a given date.
     */
    Collection<ExpressionExperiment> findUpdatedAfter( Date date );

    Map<Long, Long> getAnnotationCounts( Collection<Long> ids );

    Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas );

    Collection<ArrayDesign> getArrayDesignsUsed( Collection<? extends BioAssaySet> ees );

    Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt, Class<? extends DataVector> dataVectorType );

    /**
     * Obtain genes used by the processed vectors of this dataset.
     */
    Collection<Gene> getGenesUsedByPreferredVectors( ExpressionExperiment experimentConstraint );

    /**
     * Obtain the dataset usage frequency by technology type.
     * <p>
     * If a dataset was switched to a platform of a different technology type, it is counted toward both.
     */
    Map<TechnologyType, Long> getTechnologyTypeUsageFrequency();

    /**
     * Obtain the dataset usage frequency by technology type for the given dataset IDs.
     * <p>
     * Note: No ACL filtering is performed.
     *
     * @see #getTechnologyTypeUsageFrequency()
     */
    Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( Collection<Long> eeIds );

    /**
     * Obtain dataset usage frequency by platform currently used.
     * <p>
     * Note that a dataset counts toward all the platforms mentioned through its {@link BioAssay}.
     * <p>
     * This method uses ACLs and the troubled status to only displays the counts of datasets the current user is
     * entitled to see. Only administrator can see troubled platforms.
     */
    Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( int maxResults );

    /**
     * Obtain dataset usage frequency by platform currently for the given dataset IDs.
     * <p>
     * Note: no ACL filtering is performed. Only administrator can see troubled platforms.
     *
     * @see #getArrayDesignsUsageFrequency(int)
     */
    Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( Collection<Long> eeIds, int maxResults );

    /**
     * Obtain dataset usage frequency by original platforms.
     * <p>
     * Note that a dataset counts toward all the platforms mentioned through its {@link BioAssay}. Datasets whose
     * platform hasn't been switched (i.e. the original is the same as the current one) are ignored.
     * <p>
     * This method uses ACLs and the troubled status to only displays the counts of datasets the current user is
     * entitled to see. Only administrators can see troubled platforms.
     */
    Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( int maxResults );

    /**
     * Obtain dataset usage frequency by platform currently for the given dataset IDs.
     * <p>
     * Note: no ACL filtering is performed. Only administrators can see troubled platforms.
     *
     * @see #getOriginalPlatformsUsageFrequency(int)
     */
    Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( Collection<Long> eeIds, int maxResults );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve {@link BioAssayDimension} that are used by subsets of a given {@link ExpressionExperiment}.
     * <p>
     * This covers cases where BAs in a subset are not the same as the BAs in the experiment such as for single-cell
     * data where we use sub-assays.
     */
    Collection<BioAssayDimension> getBioAssayDimensionsFromSubSets( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve a dimension for a given experiment and quantitation type.
     * @param dataVectorType the type of data vectors to consider, this is necessary because otherwise all the vector
     *                       tables would have to be looked at. If you do nto know the type of vector, use {@link #getBioAssayDimension(ExpressionExperiment, QuantitationType)}.
     * @throws org.hibernate.NonUniqueResultException if there is more than one dimension for the given set of vectors
     */
    @Nullable
    BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType );

    /**
     * Retrieve a dimension for a given experiment and quantitation type.
     * @throws org.hibernate.NonUniqueResultException if there is more than one dimension for the given set of vectors
     */
    @Nullable
    BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain a bioassay dimension by ID.
     */
    @Nullable
    BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId, Class<? extends BulkExpressionDataVector> dataVectorType );

    long getBioMaterialCount( ExpressionExperiment expressionExperiment );

    long getRawDataVectorCount( ExpressionExperiment ee );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    @Nullable
    Date getLastArrayDesignUpdate( ExpressionExperiment ee );

    /**
     * Obtain the count of distinct experiments per taxon.
     * <p>
     * Experiments are filtered by ACLs and troubled experiments are only visible to administrators.
     */
    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Obtain the count of distinct experiments per taxon for experiments with the given IDs.
     * <p>
     * Experiments <b>are not</b> filtered by ACLs and toubled experiments are only visible to administrators.
     */
    Map<Taxon, Long> getPerTaxonCount( Collection<Long> ids );

    Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee );

    /**
     * Obtain the preferred quantitation type for single cell data, if available.
     */
    @Nullable
    QuantitationType getPreferredSingleCellQuantitationType( ExpressionExperiment ee );

    /**
     * Obtain the preferred quantitation type for the raw vectors, if available.
     */
    @Nullable
    QuantitationType getPreferredQuantitationType( ExpressionExperiment ee );

    /**
     * Obtain the quantitation type for the processed vectors, if available.
     * @throws org.hibernate.NonUniqueResultException if there is more than one set of processed vectors
     */
    @Nullable
    QuantitationType getProcessedQuantitationType( ExpressionExperiment ee );

    /**
     * Test if the dataset has preferred expression data vectors.
     */
    boolean hasProcessedExpressionData( ExpressionExperiment ee );

    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension bad );

    Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment );

    @Nullable
    ExpressionExperimentSubSet getSubSetById( ExpressionExperiment ee, Long subSetId );

    <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    /**
     * Determine the taxon for a given experiment or subset.
     * @return a unique taxon for the dataset, or null if no taxon could be determined
     */
    @Nullable
    Taxon getTaxon( BioAssaySet ee );

    /**
     * Load datasets by IDs with the same relation as {@link #loadWithCache(Filters, Sort)}.
     */
    List<ExpressionExperiment> loadWithRelationsAndCache( List<Long> ids );

    /**
     * Special method for front-end access. This is partly redundant with {@link #loadValueObjects(Filters, Sort, int, int)};
     * however, it fills in more information, returns ExpressionExperimentDetailsValueObject
     *
     * @param ids    only list specific ids, or null to ignore
     * @param taxon  only list EEs in the specified taxon, or null to ignore
     * @param sort   the field to order the results by.
     * @param offset offset
     * @param limit  maximum number of results to return
     * @return a list of EE details VOs representing experiments matching the given arguments.
     */
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    /**
     * Flavour of {@link #loadDetailsValueObjectsByIds(Collection)}, but using the query cache.
     */
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    /**
     * Like {@link #loadDetailsValueObjects(Collection, Taxon, Sort, int, int)}, but returning a list.
     */
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids );

    /**
     * Flavour of {@link #loadDetailsValueObjectsByIds(Collection)}, but using the query cache.
     */
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids );

    Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    Collection<ExpressionExperiment> loadLackingFactors();

    Collection<ExpressionExperiment> loadLackingTags();

    /**
     * Thaw everything.
     * <p>
     * Includes {@link #thawLite(ExpressionExperiment)} and raw/processed vectors.
     * <p>
     * Does not include single-cell vectors.
     */
    void thaw( ExpressionExperiment expressionExperiment );

    /**
     * Thaw experiment metadata.
     */
    void thawLiter( ExpressionExperiment expressionExperiment );

    /**
     * Thaw experiment metadata and bioassays.
     * <p>
     * Include {@link #thawLiter(ExpressionExperiment)} and bioassays.
     */
    void thawLite( ExpressionExperiment expressionExperiment );

    /**
     * Obtain all annotations, grouped by applicable level.
     * @param useEe2c use the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} table to retrieve annotations
     */
    Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c );

    /**
     * Obtain experiment-level annotations.
     *
     * @param useEe2c use the {@code EXPRESSION_EXPERIMENTE2CHARACTERISTIC} table, {@link ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil}
     */
    Collection<Characteristic> getExperimentAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c );

    /**
     * Obtain the subset annotations.
     */
    Collection<Characteristic> getExperimentSubSetAnnotations( ExpressionExperiment ee );

    /**
     * Obtain sample-level annotations.
     * <p>
     * This uses the {@code EE2C} table under the hood.
     */
    List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c );

    List<Characteristic> getBioMaterialAnnotations( ExpressionExperimentSubSet expressionExperiment );

    /**
     * Obtain experimental design-level annotations.
     * <p>
     * This is equivalent to the subject components of {@link #getFactorValueAnnotations(ExpressionExperiment)} for now,
     * but other annotations from the experimental design might be included in the future.
     */
    List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment, boolean useEe2c );

    /**
     * Obtain factor value-level annotations.
     */
    List<Statement> getFactorValueAnnotations( ExpressionExperiment ee );

    /**
     * Obtain factor value-level annotations for a given subset.
     */
    List<Statement> getFactorValueAnnotations( ExpressionExperimentSubSet ee );

    /**
     * Special indicator for free-text terms.
     * <p>
     * Free-text terms or categories have a null URI and a non-empty label.
     */
    String FREE_TEXT = null;

    /**
     * Special indicator for an uncategorized term.
     * <p>
     * Uncategorized terms (or the uncategorized category) has both null URI and label.
     */
    String UNCATEGORIZED = "[uncategorized_" + RandomStringUtils.insecure().nextAlphanumeric( 10 ) + "]";

    Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults );

    /**
     * Obtain annotations usage frequency for a set of given {@link ExpressionExperiment} IDs.
     * <p>
     * This is meant as a counterpart to {@link ubic.gemma.persistence.service.common.description.CharacteristicService#findExperimentsByUris(Collection, Taxon, int, boolean, boolean)}
     * to answer the reverse question: which annotations can be used to filter a given set of datasets?
     *
     * @param expressionExperimentIds IDs of {@link ExpressionExperiment} to use for restricting annotations, or null to
     *                                consider everything
     * @param level                   applicable annotation level, one of {@link ExpressionExperiment}, {@link ExperimentalDesign}
     *                                or {@link BioMaterial}
     * @param maxResults              maximum number of annotations to return, or -1 to return everything
     * @param minFrequency            minimum usage frequency to be reported (0 effectively allows everything)
     * @param category                a category URI or free text category to restrict the results to, or null to
     *                                consider everything, empty string to consider uncategorized terms
     * @param retainedTermUris        a collection of term to retain even if they don't meet the minimum frequency criteria
     */
    Map<Characteristic, Long> getAnnotationsUsageFrequency( @Nullable Collection<Long> expressionExperimentIds, @Nullable Class<? extends Identifiable> level, int maxResults, int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris );

    Collection<ExpressionExperiment> getExperimentsLackingPublications();

    MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr );

    /**
     * Count the number of biomaterials of datasets satisfying the given filters.
     * <p>
     * The result is stored in the standard query cache.
     */
    long countBioMaterials( @Nullable Filters filters );

    /**
     * Obtain raw vectors for a given experiment and QT.
     * <p>
     * This is preferable to using {@link ExpressionExperiment#getRawExpressionDataVectors()} as it only loads vectors
     * relevant to the given QT.
     */
    Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain a slice of the raw vectors for a given experiment and QT.
     */
    Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> assays, QuantitationType qt );

    /**
     * Add raw data vectors with the given quantitation type.
     * @return the number of raw data vectors created
     */
    int addRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> newVectors );

    /**
     * Remove all raw data vectors.
     * <p>
     * All affected QTs are removed.
     */
    int removeAllRawDataVectors( ExpressionExperiment ee );

    /**
     * Remove raw data vectors for a given quantitation type.
     * <p>
     * Unused {@link BioAssayDimension} are removed unless keepDimension is set to {@code true}.
     * @param keepDimension keep the {@link BioAssayDimension} if it is not used by any other vectors. Use this only if
     *                      you intend to reuse the dimension for another set of vectors. Alternatively,
     *                      {@link #replaceRawDataVectors(ExpressionExperiment, QuantitationType, Collection)} can be
     *                      used.
     * @return the number of removed raw vectors
     */
    int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension );

    /**
     * Replace raw data vectors for a given quantitation type.
     * @return the number of replaced raw vectors
     */
    int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors );

    /**
     * Retrieve the processed vector for an experiment.
     * <p>
     * Unlike {@link ExpressionExperiment#getProcessedExpressionDataVectors()}, this is guaranteed to return only one
     * set of vectors and will raise a {@link NonUniqueResultException} if there is more than one processed QTs.
     * @see #getProcessedQuantitationType(ExpressionExperiment)
     * @return the processed vectors, or null if there are no processed vectors
     */
    @Nullable
    Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Retrieve a slice of processed vectors for an experiment.
     * @return the processed vectors, or null if there are no processed vectors
     */
    @Nullable
    Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays );

    /**
     * Add processed data vectors
     * <p>
     * The number of vectors {@link ExpressionExperiment#getNumberOfDataVectors()} is updated.
     * @return the number of created processed vectors
     */
    int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Remove processed data vectors.
     * <p>
     * Their corresponding QT is detached from the experiment and removed. The number of vectors (i.e. {@link ExpressionExperiment#getNumberOfDataVectors()}
     * is set to zero. Unused dimensions are removed.
     * @return the number of removed processed vectors
     */
    int removeProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Replace processed data vectors.
     * <p>
     * The QT is reused and the number of vectors {@link ExpressionExperiment#getNumberOfDataVectors()} is updated.
     * Unused dimensions are removed.
     * @return the number of vectors replaced
     */
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Obtain all the single cell dimensions used by the single-cell vectors of a given experiment.
     */
    List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee );

    /**
     * Obtain all the single cell dimensions used by the single-cell vectors of a given experiment.
     * <p>
     * Cell IDs are not loaded.
     */
    List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee );

    List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices );

    /**
     * Obtain the single-cell dimension used by a specific QT.
     */
    @Nullable
    SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Load a single-cell dimension used by a specific QT without its cell IDs.
     */
    @Nullable
    SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt );

    @Nullable
    SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices );

    /**
     * Obtain the preferred single cell dimension, that is the dimension associated to the preferred set of single-cell vectors.
     */
    @Nullable
    SingleCellDimension getPreferredSingleCellDimension( ExpressionExperiment ee );

    /**
     * Load a single-cell dimension without its cell IDs.
     */
    @Nullable
    SingleCellDimension getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee );

    @Nullable
    SingleCellDimension getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, boolean includeBioAssays, boolean includeCtas, boolean includeClcs, boolean includeProtocol, boolean includeCharacteristics, boolean includeIndices );

    /**
     * Create a single-cell dimension for a given experiment.
     * @throws IllegalArgumentException if the single-cell dimension is invalid
     */
    void createSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension );

    /**
     * Update a single-cell dimensino for a given experiment.
     * @throws IllegalArgumentException if the single-cell dimension is invalid
     */
    void updateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension );

    /**
     * Delete the given single cell dimension.
     */
    void deleteSingleCellDimension( ExpressionExperiment ee, SingleCellDimension singleCellDimension );

    /**
     * Stream the cell IDs of a dimension.
     * @param createNewSession create a new session held by the stream, allowing to use the stream beyond the lifetime
     *                         current session. If you set this to true, make absolutely sure that the resulting stream
     *                         is closed.
     * @return a stream of cell IDs, or null if the dimension is not found
     */
    @Nullable
    Stream<String> streamCellIds( SingleCellDimension dimension, boolean createNewSession );

    @Nullable
    Stream<Characteristic> streamCellTypes( CellTypeAssignment cta, boolean createNewSession );

    /**
     * Obtain the category of a cell-level characteristic.
     * <p>
     * This handles the case where the characteristics were not loaded (i.e. using {@link #getSingleCellDimensionsWithoutCellIds(ExpressionExperiment, boolean, boolean, boolean, boolean, boolean, boolean)}).
     */
    @Nullable
    Category getCellLevelCharacteristicsCategory( CellLevelCharacteristics clc );

    @Nullable
    Stream<Characteristic> streamCellLevelCharacteristics( CellLevelCharacteristics clc, boolean createNewSession );

    /**
     * Obtain the cell type at a given cell index.
     */
    @Nullable
    Characteristic getCellTypeAt( CellTypeAssignment cta, int cellIndex );

    Characteristic[] getCellTypeAt( CellTypeAssignment cta, int startIndex, int endIndexExclusive );

    /**
     * Obtain the characteristic at a given cell index.
     */
    @Nullable
    Characteristic getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int cellIndex );

    Characteristic[] getCellLevelCharacteristicAt( CellLevelCharacteristics clc, int startIndex, int endIndexExclusive );

    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee );

    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment expressionExperiment, QuantitationType qt );

    /**
     * Obtain the preferred assignment of the preferred single-cell vectors.
     *
     * @throws org.hibernate.NonUniqueResultException if there are multiple preferred cell-type labellings
     */
    @Nullable
    CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee ) throws NonUniqueResultException;

    /**
     * Obtain a cell type assignment by ID.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId );

    /**
     * Obtain a cell type assignment by ID without loading the indices.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId );

    /**
     * Obtain a cell type assignment by name.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName );

    /**
     * Obtain all cell type assignment protocols currently used.
     */
    Collection<Protocol> getCellTypeAssignmentProtocols();

    @Nullable
    Collection<CellTypeAssignment> getCellTypeAssignmentByProtocol( ExpressionExperiment ee, QuantitationType qt, String protocolIdentifier );

    /**
     * Obtain a cell type assignment by name without loading the indices.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName );

    /**
     * Obtain all cell-level characteristics from all single cell dimensions.
     */
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee );

    /**
     * Obtain all cell-level characteristics from all single cell dimensions matching the given category.
     */
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee, Category category );

    /**
     * Obtain a specific cell-level characteristic by ID.
     * <p>
     * When using this method, no {@link CellTypeAssignment} can be returned as those are stored in a different table.
     */
    @Nullable
    CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, Long clcId );

    @Nullable
    CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, String clcName );

    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt );

    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, Category category );

    List<Characteristic> getCellTypes( ExpressionExperiment ee );

    /**
     * Obtain a list of single-cell QTs.
     */
    List<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee );

    /**
     * Indicate if the given experiment has single-cell quantitation types.
     */
    boolean hasSingleCellQuantitationTypes( ExpressionExperiment ee );

    /**
     * Obtain a set of single-cell data vectors for the given quantitation type.
     */
    List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment expressionExperiment, QuantitationType quantitationType );

    /**
     * Obtain a set of single-cell data vectors for the given quantitation type.
     */
    List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, boolean includeCellIds, boolean includeData, boolean includeDataIndices );

    /**
     * Obtain a stream over the vectors for a given QT.
     * <p>
     *
     * @param fetchSize                 number of vectors to fetch at once
     * @param createNewSession          create a new session held by the stream. If you set this to true, make absolutely sure
     *                                  that the resulting stream is closed because it is attached to a {@link org.hibernate.Session}
     *                                  object.
     * @see ubic.gemma.persistence.util.QueryUtils#stream(Query, Class, int, boolean, boolean)
     */
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession );

    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, boolean includeCellIds, boolean includeData, boolean includeDataIndices );

    SingleCellExpressionDataVector getSingleCellDataVectorWithoutCellIds( ExpressionExperiment ee, QuantitationType quantitationType, CompositeSequence designElement );

    /**
     * Obtain the number of single-cell vectors for a given QT.
     */
    long getNumberOfSingleCellDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain the number of non-zeroes.
     */
    long getNumberOfNonZeroes( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain the number of non-zeroes by sample.
     * <p>
     * This is quite costly because the indices of each vector has to be examined.
     */
    Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize, boolean useCursorFetchIfSupported );

    /**
     * Remove the given single-cell data vectors.
     * @param quantitationType quantitation to remove
     * @param deleteQt         if true, detach the QT from the experiment and delete it
     *                         TODO: add a replaceSingleCellDataVectors to avoid needing this
     */
    int removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, boolean deleteQt );

    /**
     * Remove all single-cell data vectors and their quantitation types.
     */
    int removeAllSingleCellDataVectors( ExpressionExperiment ee );

    Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment );
}
