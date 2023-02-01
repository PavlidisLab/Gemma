package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BrowsingDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;

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

    Map<Long, Integer> getAnnotationCounts( Collection<Long> ids );

    Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas );

    Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids );

    Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( Collection<Long> eeIds );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Integer getBioAssayCountById( long Id );

    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    Integer getBioMaterialCount( ExpressionExperiment expressionExperiment );

    Integer getDesignElementDataVectorCountById( long Id );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    Date getLastArrayDesignUpdate( ExpressionExperiment ee );

    QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee );

    Map<Taxon, Long> getPerTaxonCount();

    Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<QuantitationType, Integer> getQuantitationTypeCountById( Long id );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign );

    /**
     * Obtain the preferred quantitation type for a given data vector type.
     * <p>
     * If more than one preferred QT exists, a warning is emitted and the latest one according to their {@link DesignElementDataVector#getId()}
     * is returned.
     *
     * @return the data vector, or {@link Optional#empty()} if no preferred vector type can be found
     */
    Optional<QuantitationType> getPreferredQuantitationTypeForDataVectorType( ExpressionExperiment ee, Class<? extends DesignElementDataVector> vectorType );

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
     * Special method for front-end access. This is partly redundant with loadValueObjectsPreFilter; however, it fills
     * in more information, returns ExpressionExperimentDetailsValueObject
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

    ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawBioAssays( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawForFrontEnd( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawWithoutVectors( ExpressionExperiment expressionExperiment );

    Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId );

    Collection<? extends AnnotationValueObject> getAnnotationsByFactorvalues( Long eeId );

    /**
     * Obtain usage frequency for all annotations.
     * @param maxResults maximum number of characteristics to return
     */
    Map<Characteristic, Long> getAnnotationsFrequency( int maxResults );

    /**
     * Obtain a set of annotations for all the given {@link ExpressionExperiment} IDs.
     * <p>
     * This is meant as a counterpart to {@link ExpressionExperimentService#getAnnotationsFrequency(Long)} which return
     * annotations for a single EE.
     * This is meant as a counterpart to {@link ubic.gemma.persistence.service.common.description.CharacteristicService#findExperimentsByUris(Collection, Taxon, int)}
     * to answer the reverse question: which annotations can be used to.
     */
    Map<Characteristic, Long> getAnnotationsFrequency( Collection<Long> expressionExperimentIds, int maxResults );

    Collection<ExpressionExperiment> getExperimentsLackingPublications();
}
