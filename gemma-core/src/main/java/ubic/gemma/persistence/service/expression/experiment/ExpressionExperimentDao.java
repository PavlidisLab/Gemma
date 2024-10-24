package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.CacheMode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

    @Nullable
    BioAssaySet loadBioAssaySet( Long id );

    Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon );

    ExpressionExperiment findByShortName( String shortName );

    Collection<ExpressionExperiment> findByName( String name );

    @Nullable
    ExpressionExperiment findOneByName( String name );

    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    Collection<ExpressionExperiment> findByAccession( String accession );

    @Nullable
    ExpressionExperiment findOneByAccession( String accession );

    Collection<ExpressionExperiment> findByBibliographicReference( Long bibRefID );

    ExpressionExperiment findByBioAssay( BioAssay ba );

    ExpressionExperiment findByBioMaterial( BioMaterial bm );

    Map<ExpressionExperiment, BioMaterial> findByBioMaterials( Collection<BioMaterial> bms );

    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank );

    ExpressionExperiment findByDesign( ExperimentalDesign ed );

    ExpressionExperiment findByFactor( ExperimentalFactor ef );

    ExpressionExperiment findByFactorValue( FactorValue fv );

    ExpressionExperiment findByFactorValue( Long factorValueId );

    Map<ExpressionExperiment, FactorValue> findByFactorValues( Collection<FactorValue> fvs );

    Collection<ExpressionExperiment> findByGene( Gene gene );

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

    long getBioMaterialCount( ExpressionExperiment expressionExperiment );

    long getDesignElementDataVectorCount( ExpressionExperiment ee );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

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
     * Obtain the preferred quantitation type, if available.
     */
    @Nullable
    QuantitationType getPreferredQuantitationType( ExpressionExperiment ee );

    /**
     * Test if the dataset has preferred expression data vectors.
     */
    boolean hasProcessedExpressionData( ExpressionExperiment ee );

    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

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

    void thaw( ExpressionExperiment expressionExperiment );

    void thawLite( ExpressionExperiment expressionExperiment );

    void thawBioAssays( ExpressionExperiment expressionExperiment );

    void thawForFrontEnd( ExpressionExperiment expressionExperiment );

    Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId );

    Collection<? extends AnnotationValueObject> getAnnotationsByFactorValues( Long eeId );

    /**
     * Obtain all annotations, grouped by applicable level.
     */
    Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain experiment-level annotations.
     */
    List<Characteristic> getExperimentAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain sample-level annotations.
     */
    List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain experimental design-level annotations.
     */
    List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment );

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
    String UNCATEGORIZED = "[uncategorized_" + RandomStringUtils.randomAlphanumeric( 10 ) + "]";

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

    Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

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
     * @return the number of removed raw vectors
     */
    int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Replace raw data vectors for a given quantitation type.
     * @return the number of replaced raw vectors
     */
    int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors );

    /**
     * Create processed data vectors
     * <p>
     * The QT is created if it doesn't exist and is attached to the experiment.
     * <p>
     * The number of vectors {@link ExpressionExperiment#getNumberOfDataVectors()} is updated.
     * @return the number of created processed vectors
     */
    int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Remove processed data vectors.
     * <p>
     * Their corresponding QT is detached from the experiment and removed. The number of vectors (i.e. {@link ExpressionExperiment#getNumberOfDataVectors()}
     * is set to zero.
     * @return the number of removed processed vectors
     */
    int removeProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Replace processed data vectors.
     * <p>
     * The QT is reused and the number of vectors {@link ExpressionExperiment#getNumberOfDataVectors()} is updated.
     * @return the number of vectors replaced
     */
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * Obtain all the single cell dimensions used by the single-cell vectors of a given experiment.
     */
    List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee );

    /**
     * Obtain the single-cell dimension used by a specific QT.
     */
    @Nullable
    SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Obtain the preferred single cell dimension, that is the dimension associated to the preferred set of single-cell vectors.
     */
    @Nullable
    SingleCellDimension getPreferredSingleCellDimension( ExpressionExperiment ee );

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

    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee );

    /**
     * Obtain the preferred assignment of the preferred single-cell vectors.
     *
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if there are multiple preferred cell-type
     *                                                                        labellings
     */
    @Nullable
    CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee );

    /**
     * Obtain a cell type assignment by ID.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId );

    /**
     * Obtain a cell type assignment by name.
     */
    @Nullable
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName );

    /**
     * Obtain all cell-level characteristics from all single cell dimensions.
     */
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee );

    /**
     * Obtain all cell-level characteristics from all single cell dimensions matching the given category.
     */
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee, Category category );

    List<Characteristic> getCellTypes( ExpressionExperiment ee );

    /**
     * Obtain a list of single-cell QTs.
     */
    List<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee );

    /**
     * Obtain a set of single-cell data vectors for the given quantitation type.
     */
    List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment expressionExperiment, QuantitationType quantitationType );

    /**
     * Obtain a stream over the vectors for a given QT.
     * <p>
     * Make absolutely sure that the resulting stream is closed because it is attached to a {@link org.hibernate.Session}
     * object.
     */
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize );

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
    Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize );

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
}
