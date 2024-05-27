package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.CacheMode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
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

    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    Collection<ExpressionExperiment> findByAccession( String accession );

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

    Collection<ExpressionExperiment> findUpdatedAfter( Date date );

    Map<Long, Long> getAnnotationCounts( Collection<Long> ids );

    Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas );

    Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids );

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

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

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

    Taxon getTaxon( BioAssaySet ee );

    /**
     * Load datasets by IDs with the same relation as {@link #loadWithCache(Filters, Sort)}.
     */
    List<ExpressionExperiment> loadWithRelationsAndCache( List<Long> ids );

    /**
     * Special method for front-end access. This is partly redundant with {@link #loadValueObjects(Filters, Sort, int, int)};
     * however, it fills in more information, returns ExpressionExperimentDetailsValueObject
     *
     * @param ids        only list specific ids, or null to ignore
     * @param taxon      only list EEs in the specified taxon, or null to ignore
     * @param sort       the field to order the results by.
     * @param offset     offset
     * @param limit      maximum number of results to return
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

    void thawWithoutVectors( ExpressionExperiment expressionExperiment );

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
}
