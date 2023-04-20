package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.core.security.audit.IgnoreAudit;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
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

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Integer getBioAssayCountById( long Id );

    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    Integer getBioMaterialCount( ExpressionExperiment expressionExperiment );

    Integer getDesignElementDataVectorCountById( long Id );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    Date getLastArrayDesignUpdate( ExpressionExperiment ee );

    Map<Taxon, Long> getPerTaxonCount();

    Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<QuantitationType, Integer> getQuantitationTypeCountById( Long id );

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

    Collection<ExpressionExperiment> loadLackingFactors();

    Collection<ExpressionExperiment> loadLackingTags();

    List<ExpressionExperimentValueObject> loadValueObjectsByIds( Collection<Long> ids );

    void thaw( ExpressionExperiment expressionExperiment );

    void thawWithoutVectors( ExpressionExperiment expressionExperiment );

    void thawBioAssays( ExpressionExperiment expressionExperiment );

    void thawForFrontEnd( ExpressionExperiment expressionExperiment );

    Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId );

    Collection<? extends AnnotationValueObject> getAnnotationsByFactorvalues( Long eeId );

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
}
