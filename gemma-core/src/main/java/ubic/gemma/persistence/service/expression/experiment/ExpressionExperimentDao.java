package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.InitializingBean;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.ObjectFilter;

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
        extends InitializingBean, CuratableDao<ExpressionExperiment, ExpressionExperimentValueObject> {

    List<ExpressionExperiment> browse( Integer start, Integer limit );

    Integer countNotTroubled();

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

    List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit );

    List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit );

    List<ExpressionExperiment> findByUpdatedLimit( Integer limit );

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

    QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee );

    Map<Taxon, Long> getPerTaxonCount();

    Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<QuantitationType, Integer> getQuantitationTypeCountById( Long id );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign );

    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    Taxon getTaxon( BioAssaySet ee );

    Collection<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( String orderField, boolean descending );

    Collection<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( Taxon taxon );

    Collection<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField, boolean descending,
            Taxon taxon );

    /**
     * Special method for front-end access. This is partly redundant with loadValueObjectsPreFilter; however, it fills
     * in more information, returns ExpressionExperimentDetailsValueObject
     *
     * @param  orderField the field to order the results by.
     * @param  descending whether the ordering by the orderField should be descending.
     * @param  ids        only list specific ids.
     * @param  taxon      only list experiments within specific taxon.
     * @param  limit      max to return
     * @param  start      offset
     * @return            a list of EE details VOs representing experiments matching the given arguments.
     */
    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( String orderField, boolean descending,
            Collection<Long> ids, Taxon taxon, int limit, int start );

    Collection<ExpressionExperiment> loadLackingFactors();

    Collection<ExpressionExperiment> loadLackingTags();

    ExpressionExperimentValueObject loadValueObject( Long eeId );

    Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids, boolean maintainOrder );

    Collection<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids );

    /**
     * @param  offset  offset
     * @param  limit   limit
     * @param  asc     order ascending
     * @param  filter  filters
     * @param  orderBy order by property
     * @return         collection of value objects
     * @see            ExpressionExperimentDaoImpl#loadValueObjectsPreFilter(int, int, String, boolean, List) for
     *                 description (no but seriously do look it might not work as you would expect).
     */
    @Override
    Collection<ExpressionExperimentValueObject> loadValueObjectsPreFilter( int offset, int limit, String orderBy,
            boolean asc, List<ObjectFilter[]> filter );

    ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawBioAssays( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawForFrontEnd( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawWithoutVectors( ExpressionExperiment expressionExperiment );

    Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId );

    Collection<? extends AnnotationValueObject> getAnnotationsByFactorvalues( Long eeId );
}
