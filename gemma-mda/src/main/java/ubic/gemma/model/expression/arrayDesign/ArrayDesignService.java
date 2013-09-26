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
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @version $Id$
 */
public interface ArrayDesignService {

    /**
     * @return all compositeSequences for the given arrayDesign that do not have any bioSequence associations.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    /**
     * @return all compositeSequences for the given arrayDesign that do not have BLAT result associations.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    /**
     * @return all compositeSequences for the given arrayDesign that do not have gene associations.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    /**
     * Return all the (unique) biosequences associated with the array design. Composite sequences that don't have
     * sequences are also returned, so this can be used to do a thaw, in effect.
     * 
     * @param arrayDesign
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    /**
     * @return global count of compositeSequences in the system.
     */
    public Integer countAll();

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public ArrayDesign create( ArrayDesign arrayDesign );

    /**
     * delete sequence alignment results associated with the bioSequences for this array design.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void deleteAlignmentData( ArrayDesign arrayDesign );

    /**
     * deletes the gene product associations on the specified array design F
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    public ArrayDesign find( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> findByAlternateName( String queryString );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> findByName( String name );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    public ArrayDesign findByShortName( String shortName );

    /**
     * Find by the primary taxon.
     * 
     * @param taxon
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> findByTaxon( Taxon taxon );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_READ_QUIET" })
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssay> getAllAssociatedBioAssays( Long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer getCompositeSequenceCount( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Gets the AuditEvents of the latest annotation file event for the specified array design ids. This returns a map
     * of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastAnnotationFile( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest gene mapping for the specified array design ids. This returns a map of id ->
     * AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest sequence analyses for the specified array design ids. This returns a map of id
     * -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest sequence update for the specified array design ids. This returns a map of id
     * -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, AuditEvent> getLastTroubleEvent( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, AuditEvent> getLastValidationEvent( Collection<Long> ids );

    /**
     * @return a map of taxon -> count of how many array designs there are for that taxon. Taxa with no arrays are
     *         excluded.
     */
    public Map<Taxon, Integer> getPerTaxonCount();

    /**
     * Return the taxa for the array design. This can be multiple, or zero if the array is not processed.
     * 
     * @param id The id of the array design
     * @return The Set of Taxons for array design.
     */
    public Collection<Taxon> getTaxa( Long id );

    /**
     * Return the taxon for the array design. This can be misleading if the array uses multiple taxa: this method will
     * return the first one found.
     * 
     * @param id The id of the array design
     * @return The taxon
     * @deprecated Use arrayDesign.getPrimaryTaxon if you want a single representative taxon for the array. Otherwise
     *             use getTaxa to get the comprehensive list.
     */
    @Deprecated
    public Taxon getTaxon( Long id );

    /**
     * 
     */
    public Map<Long, Boolean> isMerged( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, Boolean> isMergee( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    /**
     * 
     */
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    public ArrayDesign load( long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> loadAll();

    /**
     * loads all Array designs as value objects.
     */
    public Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign );

    /**
     * Retrieves alignments for the platform elements, limited to those which map to a gene product (so not all
     * blatresults)
     * 
     * @param arrayDesign
     * @return map of compositesequences to alignments, if available.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign );

    /**
     * Given a collection of ID (longs) will return a collection of ArrayDesigns
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> loadMultiple( Collection<Long> ids );

    /**
     * loads the Value Objects for the Array Designs specified by the input ids.
     */
    public ArrayDesignValueObject loadValueObject( Long id );

    /**
     * loads the Value Objects for the Array Designs specified by the input ids.
     */
    public Collection<ArrayDesignValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * Function to return a count of all compositeSequences with bioSequence associations
     */
    public long numAllCompositeSequenceWithBioSequences();

    /**
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * Function to return all composite sequences with blat results
     */
    public long numAllCompositeSequenceWithBlatResults();

    /**
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * Function to return a count of all composite sequences with associated genes.
     */
    public long numAllCompositeSequenceWithGenes();

    /**
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * Returns a count of the number of genes associated with all arrayDesigns
     */
    public long numAllGenes();

    /**
     * Returns the number of unique Genes associated with the collection of ArrayDesign ids.
     */
    public long numAllGenes( Collection<Long> ids );

    /**
     * returns the number of bioSequences associated with this ArrayDesign id
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numBioSequences( ArrayDesign arrayDesign );

    /**
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numBlatResults( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * Returns the number of unique Genes associated with this ArrayDesign id
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long numGenes( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( ArrayDesign arrayDesign );

    /**
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ArrayDesign thaw( ArrayDesign arrayDesign );

    /**
     * Perform a less intensive thaw of an array design: not the compositesequences.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ArrayDesign thawLite( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    /**
     * 
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ArrayDesign arrayDesign );

    /**
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

    /**
     * @param searchString
     * @return
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> findByManufacturer( String searchString );

    @Secured({ "GROUP_ADMIN" })
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newprobes );

}
