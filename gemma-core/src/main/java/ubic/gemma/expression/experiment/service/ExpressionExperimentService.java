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
package ubic.gemma.expression.experiment.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;

import ubic.gemma.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.monitor.Monitored;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ExpressionExperimentService {

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor );

    // @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    // public void addFactors( ExpressionExperiment ee, Collection<ExperimentalFactor> factors );

    /**
     * @param ee
     * @param fv must already have the experimental factor filled in
     * @return
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv );

    // /**
    // * @param ee
    // * @param fvs
    // * @return
    // */
    // @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    // public void addFactorValues( ExpressionExperiment ee, Collection<FactorValue> fvs );

    /**
     * Used when we want to add data for a quantitation type. Does not delete any existing vectors.
     * 
     * @param ee
     * @param vectors
     * @return
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public ExpressionExperiment addVectors( ExpressionExperiment eeToUpdate, ArrayDesign ad,
            Collection<RawExpressionDataVector> newVectors );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> browse( Integer start, Integer limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, String orderField,
            boolean descending, Collection<Long> ids );

    public Integer count();

    /**
     * Count how many ExpressionExperiments are in the database
     */
    public java.lang.Integer countAll();

    @Secured({ "GROUP_USER" })
    public ExpressionExperiment create( ExpressionExperiment expressionExperiment );

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically, though this behavior could be changed)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ExpressionExperiment expressionExperiment );

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically, though this behavior could be changed) Security handled with a check inside
     * the method.
     */
    public void delete( Long id );

    /**
     * returns ids of search results
     * 
     * @param searchString
     * @return collection of ids or an empty collection
     */
    public Collection<Long> filter( String searchString );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment find( ExpressionExperiment expressionExperiment );

    /**
     * @param accession
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByAccession( String accession );

    /**
     * @param accession
     * @return Expeirments which have this accession. There can be more than one, because one GEO accesssion can result
     *         in multiple experiments in Gemma.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByAccession( ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * given a bibliographicReference returns a collection of EE that have that reference that BibliographicReference
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByBibliographicReference(
            ubic.gemma.model.common.description.BibliographicReference bibRef );

    /**
     * Given a bioAssay returns an expressionExperiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByBioAssay( ubic.gemma.model.expression.bioAssay.BioAssay ba );

    /**
     * Given a bioMaterial returns an expressionExperiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bm );

    /**
     * @param bioMaterials
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByBioMaterials( Collection<BioMaterial> bioMaterials );

    /**
     * Returns a collection of expression experiment ids that express the given gene above the given expression level
     * 
     * @param gene
     * @param rank
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByExpressedGene( Gene gene, double rank );

    /**
     * @param factorValue
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByFactor( ExperimentalFactor factor );

    /**
     * @param factorValue
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByFactorValue( FactorValue factorValue );

    /**
     * @param factorValue
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByFactorValue( Long factorValueId );

    /**
     * @param factorValues
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> factorValues );

    /**
     * Returns a collection of expression experiments that have an AD that detects the given Gene (ie a probe on the AD
     * hybidizes to the given Gene)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByGene( Gene gene );

    /**
     * @param investigator
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByInvestigator(
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    /**
     * @param name
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByName( java.lang.String name );

    /**
     * gets all EE that match the given parent Taxon
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByParentTaxon( Taxon taxon );

    /**
     * @param type
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByQuantitationType( QuantitationType type );

    /**
     * @param shortName
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    public ExpressionExperiment findByShortName( java.lang.String shortName );

    /**
     * @param taxon
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByTaxon( Taxon taxon );

    /**
     * @param taxon
     * @param limit
     * @return in order of last update (most recent first)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit );

    /**
     * Return up to Math.abs(limit) experiments that were most recently updated (limit >0) or least recently updated
     * (limit < 0).
     * 
     * @param idsOfFetched
     * @param limit
     * @return EEs in order of decreasing last update event date.
     */
    @Secured({ "GROUP_AGENT", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> idsOfFetched, Integer limit );

    @Secured({ "GROUP_AGENT", "AFTER_ACL_COLLECTION_READ" })
    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment );

    /**
     * Get the map of ids to number of terms associated with each expression experiment.
     */
    public Map<Long, Integer> getAnnotationCounts( Collection<Long> ids );

    /**
     * Get the terms associated this expression experiment.
     */
    public Collection<AnnotationValueObject> getAnnotations( Long eeId );

    /**
     * Returns a collection of ArrayDesigns referenced by any of the BioAssays that make up the given
     * ExpressionExperiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet expressionExperiment );

    public String getBatchConfound( ExpressionExperiment ee );

    /**
     * TODO allow this for BioAssaySets.
     * 
     * @param ee
     * @return details for the principal component most associated with batches (even if it isn't "significant"), or
     *         null if there was no batch information available. Note that we don't look at every component, just the
     *         first few.
     */
    public BatchEffectDetails getBatchEffect( ExpressionExperiment ee );

    /**
     * Retrieve the BioAssayDimensions for the study.
     * 
     * @param expressionExperiment
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    /**
     * Counts the number of biomaterials associated with this expression experiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer getBioMaterialCount( ExpressionExperiment expressionExperiment );

    /**
     * @param id
     * @return
     */
    public Integer getDesignElementDataVectorCountById( Long id );

    /**
     * Find vectors constrained to the given quantitation type and design elements. Returns vectors for all experiments
     * the user has access to.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Get all the vectors for the given quantitation types.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes );

    /**
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsWithBatchEffect();

    /**
     * @param auditEventClass
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsWithEvent( Class<? extends AuditEventType> auditEventClass );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExperimentsWithOutliers();

    /**
     * @param expressionExperiments
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Get the date of the last time any of the array designs associated with this experiment were updated.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Date getLastArrayDesignUpdate( ExpressionExperiment expressionExperiment );

    /**
     * Gets the AuditEvents of the latest link analyses for the specified expression experiment ids. This returns a map
     * of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastLinkAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest missing value analysis for the specified expression experiment ids. This
     * returns a map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest rank computation for the specified expression experiment ids. This returns a
     * map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( Collection<Long> ids );

    /**
     * @param id
     * @return trouble event for the given id, if this event is still in effect. NUll if no such event is found.
     */
    public AuditEvent getOustandingTroubleEvent( Long id );

    /**
     * @param ids
     * @return
     */
    public Map<Long, AuditEvent> getLastTroubleEvent( Collection<Long> ids );

    /**
     * @param ids
     * @return
     */
    public Map<Long, AuditEvent> getLastValidationEvent( Collection<Long> ids );

    /**
     * Function to get a count of expression experiments, grouped by Taxon
     */
    public Map<Taxon, Long> getPerTaxonCount();

    /**
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials.
     */
    public Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    /**
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials and only factors that aren't batch
     */
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    /**
     * Iterates over the quantiation types for a given expression experiment and returns the preferred quantitation
     * types.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getPreferredQuantitationType( ExpressionExperiment EE );

    /**
     * @param ee
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Function to get a count of an expressionExperiment's designelementdatavectors, grouped by quantitation type
     */
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( java.lang.Long Id );

    /**
     * Return all the quantitation types used by the given expression experiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    /**
     * Get the quantitation types for the expression experiment, for the array design specified. This is really only
     * useful for expression experiments that use more than one array design.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * @param expressionExperiments
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Retrieve some of the vectors for the given expressionExperiment and quantitation type. Used for peeking at the
     * data without retrieving the whole data set.
     * <p>
     * To view processed data vectors, you should use ProcessedExpressionDataVectorService.getProcessedVectors instead.
     * 
     * @param quantitationType
     * @param limit
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit );

    /**
     * Return any ExpressionExperimentSubSets this Experiment might have.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    /**
     * @param publicEEs
     * @return
     */
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    /**
     * Returns the taxon of the given expressionExperiment.
     * 
     * @return taxon, or null if the experiment taxon cannot be determined (i.e., if it has no samples)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Taxon getTaxon( BioAssaySet bioAssaySet );

    /**
     * Of the given EE ids, get the ones which are troubled.
     * 
     * @param ids
     * @return
     */
    public Collection<Long> getTroubled( Collection<Long> ids );

    /**
     * Of the given EE ids, get the ones which are not troubled.
     * 
     * @param ids
     * @return
     */
    public Collection<Long> getUntroubled( Collection<Long> ids );

    @Monitored
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    public ExpressionExperiment load( java.lang.Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadAll();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( String orderField, boolean descending );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField,
            boolean descending, Taxon taxon );

    /**
     * @param id of a subset of an experiment
     * @return the experiment, or null if there isn't one.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment loadBySubsetId( Long id );

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadLackingFactors();

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadLackingTags();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadMultiple( Collection<Long> ids );

    /**
     * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific write permissions on (as opposed to data sets which are public). Important: This method
     * will return all experiments if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<ExpressionExperiment> loadMyExpressionExperiments();

    /**
     * * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific READ permissions on (as opposed to data sets which are public).
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyPrivateData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<ExpressionExperiment> loadMySharedExpressionExperiments();

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadTroubled();

    /**
     * Returns the {@link ExpressionExperiment}s owned by the {@link User} currently logged in. Note: this includes
     * public and private entities. Important: This method will return all experiments if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_USER_OWNED_DATA" })
    public Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments();

    /**
     * @param id
     * @return
     */
    public ExpressionExperimentValueObject loadValueObject( Long eeId );

    /**
     * @param ids
     * @param maintainOrder If true, order of valueObjects returned will correspond to order of ids passed in.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids, boolean maintainOrder );

    /**
     * @param orderField
     * @param descending
     * @param ids
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    public List<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids );

    /**
     * Remove raw vectors associated with the given quantitation type. It does not touch processed data.
     * 
     * @param ee
     * @param qt
     * @return number of vectors removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public int removeData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Used when we are converting an experiment from one platform to another. Examples would be exon array or MPSS data
     * sets. Does not take care of computing the processed data vectors, but it does clear them out.
     * 
     * @param ee
     * @param ad
     * @param vectors
     * @return the updated Experiment
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public ExpressionExperiment replaceVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> vectors );

    /**
     * @param expressionExperiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    /**
     * Partially thaw the expression experiment given - do not thaw the raw data.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionExperiment thawLite( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionExperiment thawLiter( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionExperiment expressionExperiment );

}
