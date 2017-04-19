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

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;
import ubic.gemma.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.monitor.Monitored;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author kelsey
 */
public interface ExpressionExperimentService {

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor );

    /**
     * @param fv must already have the experimental factor filled in
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv );

    /**
     * Used when we want to add data for a quantitation type. Does not delete any existing vectors.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    ExpressionExperiment addVectors( ExpressionExperiment eeToUpdate, ArrayDesign ad,
            Collection<RawExpressionDataVector> newVectors );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    List<ExpressionExperiment> browse( Integer start, Integer limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending );

    Integer count();

    /**
     * Count how many ExpressionExperiments are in the database
     */
    java.lang.Integer countAll();

    @Secured({ "GROUP_USER" })
    ExpressionExperiment create( ExpressionExperiment expressionExperiment );

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically, though this behavior could be changed)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void delete( ExpressionExperiment expressionExperiment );

    /**
     * returns ids of search results
     *
     * @return collection of ids or an empty collection
     */
    Collection<Long> filter( String searchString );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment find( ExpressionExperiment expressionExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByAccession( String accession );

    /**
     * @return Experiments which have this accession. There can be more than one, because one GEO accession can result
     * in multiple experiments in Gemma.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByAccession( ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * given a bibliographicReference returns a collection of EE that have that reference that BibliographicReference
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByBibliographicReference(
            ubic.gemma.model.common.description.BibliographicReference bibRef );

    /**
     * Given a bioAssay returns an expressionExperiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByBioAssay( ubic.gemma.model.expression.bioAssay.BioAssay ba );

    /**
     * Given a bioMaterial returns an expressionExperiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bm );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByBioMaterials( Collection<BioMaterial> bioMaterials );

    /**
     * Returns a collection of expression experiment ids that express the given gene above the given expression level
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, double rank );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactor( ExperimentalFactor factor );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactorValue( FactorValue factorValue );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByFactorValue( Long factorValueId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> factorValues );

    /**
     * Returns a collection of expression experiments that have an AD that detects the given Gene (ie a probe on the AD
     * hybridizes to the given Gene)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByGene( Gene gene );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByInvestigator(
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByName( java.lang.String name );

    /**
     * gets all EE that match the given parent Taxon
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByParentTaxon( Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment findByQuantitationType( QuantitationType type );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment findByShortName( java.lang.String shortName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> findByTaxon( Taxon taxon );

    @Secured({ "GROUP_AGENT", "AFTER_ACL_COLLECTION_READ" })
    List<ExpressionExperiment> findByUpdatedLimit( Integer limit );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment );

    /**
     * Get the map of ids to number of terms associated with each expression experiment.
     */
    Map<Long, Integer> getAnnotationCounts( Collection<Long> ids );

    /**
     * Get the terms associated this expression experiment.
     */
    Collection<AnnotationValueObject> getAnnotations( Long eeId );

    /**
     * Returns a collection of ArrayDesigns referenced by any of the BioAssays that make up the given
     * ExpressionExperiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet expressionExperiment );

    String getBatchConfound( ExpressionExperiment ee );

    /**
     * TODO allow this for BioAssaySets.
     *
     * @return details for the principal component most associated with batches (even if it isn't "significant"), or
     * null if there was no batch information available. Note that we don't look at every component, just the
     * first few.
     */
    BatchEffectDetails getBatchEffect( ExpressionExperiment ee );

    /**
     * Retrieve the BioAssayDimensions for the study.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    /**
     * Counts the number of biomaterials associated with this expression experiment.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Integer getBioMaterialCount( ExpressionExperiment expressionExperiment );

    Integer getDesignElementDataVectorCountById( Long id );

    /**
     * Find vectors constrained to the given quantitation type and design elements. Returns vectors for all experiments
     * the user has access to.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<DesignElementDataVector> getDesignElementDataVectors( Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Get all the vectors for the given quantitation types.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<DesignElementDataVector> getDesignElementDataVectors( Collection<QuantitationType> quantitationTypes );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Get the date of the last time any of the array designs associated with this experiment were updated.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Date getLastArrayDesignUpdate( ExpressionExperiment expressionExperiment );

    /**
     * Gets the AuditEvents of the latest link analyses for the specified expression experiment ids. This returns a map
     * of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastLinkAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest missing value analysis for the specified expression experiment ids. This
     * returns a map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastMissingValueAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest rank computation for the specified expression experiment ids. This returns a
     * map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    Map<Long, AuditEvent> getLastProcessedDataUpdate( Collection<Long> ids );

    /**
     * Function to get a count of expression experiments, grouped by Taxon
     */
    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials.
     */
    Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    /**
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials and only factors that aren't batch
     */
    Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    /**
     * Iterates over the quantitation types for a given expression experiment and returns the preferred quantitation
     * types.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationType> getPreferredQuantitationType( ExpressionExperiment EE );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Function to get a count of an expressionExperiment's design element data vectors, grouped by quantitation type
     */
    Map<QuantitationType, Integer> getQuantitationTypeCountById( java.lang.Long Id );

    /**
     * Return all the quantitation types used by the given expression experiment
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    /**
     * Get the quantitation types for the expression experiment, for the array design specified. This is really only
     * useful for expression experiments that use more than one array design.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Retrieve some of the vectors for the given expressionExperiment and quantitation type. Used for peeking at the
     * data without retrieving the whole data set.
     * To view processed data vectors, you should use ProcessedExpressionDataVectorService.getProcessedVectors instead.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<DesignElementDataVector> getSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit );

    /**
     * Return any ExpressionExperimentSubSets this Experiment might have.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets );

    /**
     * Returns the taxon of the given expressionExperiment.
     *
     * @return taxon, or null if the experiment taxon cannot be determined (i.e., if it has no samples)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Taxon getTaxon( BioAssaySet bioAssaySet );

    @Monitored
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperiment load( java.lang.Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadAll();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentValueObject> loadAllValueObjects();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( String orderField, boolean descending );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField, boolean descending,
            Taxon taxon );

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadLackingFactors();

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadLackingTags();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> loadMultiple( Collection<Long> ids );

    /**
     * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific write permissions on (as opposed to data sets which are public). Important: This method
     * will return all experiments if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<ExpressionExperiment> loadMyExpressionExperiments();

    /**
     * * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific READ permissions on (as opposed to data sets which are public).
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyPrivateData for
     * processConfigAttribute. (in Gemma-core)
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    Collection<ExpressionExperiment> loadMySharedExpressionExperiments();

    /**
     * Returns the {@link ExpressionExperiment}s owned by the {@link User} currently logged in. Note: this includes
     * public and private entities. Important: This method will return all experiments if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_USER_OWNED_DATA" })
    Collection<ExpressionExperiment> loadUserOwnedExpressionExperiments();

    ExpressionExperimentValueObject loadValueObject( Long eeId );

    /**
     * @param maintainOrder If true, order of valueObjects returned will correspond to order of ids passed in.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids, boolean maintainOrder );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids );

    /**
     * Remove raw vectors associated with the given quantitation type. It does not touch processed data.
     *
     * @return number of vectors removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Used when we are converting an experiment from one platform to another. Examples would be exon array or MPSS data
     * sets. Does not take care of computing the processed data vectors, but it does clear them out.
     *
     * @return the updated Experiment
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    ExpressionExperiment replaceVectors( ExpressionExperiment ee, ArrayDesign ad,
            Collection<RawExpressionDataVector> vectors );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    /**
     * Partially thaw the expression experiment given - do not thaw the raw data.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thawLite( ExpressionExperiment expressionExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperiment thawLiter( ExpressionExperiment expressionExperiment );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( ExpressionExperiment expressionExperiment );

    boolean isTroubled( ExpressionExperiment expressionExperiment);

}
