package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.core.security.audit.IgnoreAudit;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.AdditionalMetadata;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BrowsingDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.io.InputStream;
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
        extends CuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>,
        FilteringVoEnabledDao<ExpressionExperiment, ExpressionExperimentValueObject>, BrowsingDao<ExpressionExperiment> {

    String OBJECT_ALIAS = "ee";

    long countNotTroubled();

    Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon );

    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    Collection<ExpressionExperiment> findByAccession( String accession );

    Collection<ExpressionExperiment> findByBibliographicReference( Long bibRefID );

    ExpressionExperiment findByBioAssay( BioAssay ba );

    ExpressionExperiment findByBioMaterial( BioMaterial bm );

    Map<ExpressionExperiment, BioMaterial> findByBioMaterials( Collection<BioMaterial> bms );

    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank );

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

    Map<ArrayDesign, Long> getArrayDesignsUsageFrequency();

    Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( Collection<Long> eeIds );

    Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency();

    Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( Collection<Long> eeIds );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    long getBioMaterialCount( ExpressionExperiment expressionExperiment );

    long getDesignElementDataVectorCount( ExpressionExperiment ee );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    Date getLastArrayDesignUpdate( ExpressionExperiment ee );

    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Obtain the count of distinct experiments per taxon for experiments with the given IDs.
     */
    Map<Taxon, Long> getPerTaxonCount( List<Long> ids );

    Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment ee, ArrayDesign oldAd );

    /**
     * Obtain the preferred quantitation type, if available.
     */
    @Nullable
    QuantitationType getPreferredQuantitationType( ExpressionExperiment ee );

    /**
     * Obtain the masked preferred quantitation type, if available.
     */
    @Nullable
    QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee );

    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    Taxon getTaxon( BioAssaySet ee );

    /**
     * This is a specialized flavour of {@link #loadDetailsValueObjects(Filters, Sort, int, int)} for detailed EE VOs.
     */
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( Filters filters, Sort sort, int offset, int limit );

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
    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    /**
     * Like {@link #loadDetailsValueObjectsByIds(Collection, Taxon, Sort, int, int)}, but returning a list.
     */
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids );

    Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    Collection<ExpressionExperiment> loadLackingFactors();

    Collection<ExpressionExperiment> loadLackingTags();

    List<ExpressionExperimentValueObject> loadValueObjectsByIds( Collection<Long> ids );

    void thaw( ExpressionExperiment expressionExperiment );

    void thawWithoutVectors( ExpressionExperiment expressionExperiment );

    void thawBioAssays( ExpressionExperiment expressionExperiment );

    void thawForFrontEnd( ExpressionExperiment expressionExperiment );

    Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId );

    Collection<? extends AnnotationValueObject> getAnnotationsByFactorvalues( Long eeId );

    /**
     * Obtain all annotations, grouped by applicable level.
     */
    Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain sample-level annotations.
     */
    List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain experimental design-level annotations.
     */
    List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment );

    /**
     * Obtain annotations usage frequency for a set of given {@link ExpressionExperiment} IDs.
     * <p>
     * This is meant as a counterpart to {@link ubic.gemma.persistence.service.common.description.CharacteristicService#findExperimentsByUris(Collection, Taxon, int)}
     * to answer the reverse question: which annotations can be used to filter a given set of datasets?
     *
     * @param expressionExperimentIds IDs of {@link ExpressionExperiment} to use for restricting annotations, or null to
     *                                consider everything
     * @param level                   applicable annotation level, one of {@link ExpressionExperiment}, {@link ExperimentalDesign}
     *                                or {@link BioMaterial}
     * @param maxResults              maximum number of annotations to return
     * @param minFrequency            minimum usage frequency to be reported (0 effectively allows everything)
     */
    Map<Characteristic, Long> getAnnotationsUsageFrequency( @Nullable Collection<Long> expressionExperimentIds, @Nullable Class<? extends Identifiable> level, int maxResults, int minFrequency );

    Collection<ExpressionExperiment> getExperimentsLackingPublications();

    /**
     * Update the troubled status of all experiments using a given platform.
     * @return the number of expression experiments marked or unmarked as troubled
     */
    @IgnoreAudit
    int updateTroubledByArrayDesign( ArrayDesign arrayDesign, boolean troubled );

    /**
     * Count the number of distict platforms used that are troubled.
     */
    long countTroubledPlatforms( ExpressionExperiment ee );

    MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr );

    /**
     * Add metadata on a given dataset.
     */
    AdditionalMetadata addAdditionalMetadata( ExpressionExperiment ee, MetadataType type, InputStream additionalMetadata, long length, String mediaType );

    /**
     * Add metadata on a specific bioassay.
     * <p>
     * FIXME: this should probably be relocated in {@link BioAssayDao}.
     * @throws IllegalArgumentException if the bioassay does not belong to the expression experiment
     */
    AdditionalMetadata addAdditionalMetadata( ExpressionExperiment ee, BioAssay sample, MetadataType metadataType, InputStream stream, long length, String mediaType ) throws IllegalArgumentException;
}
