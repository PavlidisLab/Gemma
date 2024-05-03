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
package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused") // Possible external use
public interface ArrayDesignService extends CuratableService<ArrayDesign, ArrayDesignValueObject> {

    /**
     * Load a platform by ID and thaw it with {@link #thawLite(ArrayDesign)}
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> ArrayDesign loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    @Secured({ "GROUP_ADMIN" })
    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes );

    /**
     * remove sequence alignment results associated with the bioSequences for this array design. This can indirectly
     * * affect other platforms that use the same sequences.
     *
     * @param arrayDesign AD
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteAlignmentData( ArrayDesign arrayDesign );

    /**
     * deletes the gene product associations on the specified array design. If you only want to delete alignment-based or annotation-based
     * associations, use  deleteGeneProductAlignmentAssociation deleteGeneProductAnnotationAssociations.
     *
     * @param arrayDesign AD
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> findByAlternateName( String queryString );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> findByManufacturer( String searchString );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ArrayDesign findByShortName( String shortName );

    /**
     * Find by the primary taxon.
     *
     * @param taxon taxon
     * @return ADs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    /**
     * Retrieves alignments for the platform elements, limited to those which map to a gene product (so not all
     * blat results)
     *
     * @param arrayDesign AD
     * @return map of composite sequences to alignments, if available.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign );

    /**
     * Return all the (unique) biosequences associated with the array design. Composite sequences that don't have
     * sequences are also returned, so this can be used to do a thawRawAndProcessed, in effect.
     *
     * @param arrayDesign AD
     * @return map of composite seqs. to bio seqs.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Long getCompositeSequenceCount( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign, int limit, int offset );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getExpressionExperimentsCount( ArrayDesign arrayDesign );

    /**
     * Gets the AuditEvents of the latest gene mapping for the specified array design ids. This returns a map of id
     * -&gt;
     * AuditEvent. If the events do not exist, the map entry will point to null.
     *
     * @param ids ids
     * @return map of IDs to events
     */
    Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids );

    Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest sequence analyses for the specified array design ids. This returns a map of id
     * -&gt; AuditEvent. If the events do not exist, the map entry will point to null.
     *
     * @param ids ids
     * @return map of IDs to events
     */
    Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest sequence update for the specified array design ids. This returns a map of id
     * -&gt; AuditEvent. If the events do not exist, the map entry will point to null.
     *
     * @param ids ids
     * @return map of IDs to events
     */
    Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids );

    /**
     * Get the ids of experiments that "originally" used this platform, but which don't any more due to a platform
     * switch. Note that for some old platforms we may not have recorded this information.
     *
     * @param id id of the platform
     * @return collection of EE ids
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperiment> getSwitchedExperiments( ArrayDesign id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getSwitchedExpressionExperimentCount( ArrayDesign id );

    /**
     * @return a map of taxon -&gt; count of how many array designs there are for that taxon. Taxa with no arrays are
     * excluded.
     */
    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Return the taxa for the array design. This can be multiple, or zero if the array is not processed.
     *
     * @param arrayDesign The id of the array design
     * @return The Set of Taxons for array design.
     */
    Collection<Taxon> getTaxa( ArrayDesign arrayDesign );

    Taxon getTaxon( Long id );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    /**
     * Loads the Value Objects for array designs used by expression experiment with the given ID
     *
     * @param eeId the id of an expression experiment
     * @return AD VOs
     */
    List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId );

    /**
     * Function to return a count of all compositeSequences with bioSequence associations
     *
     * @return count
     */
    long numAllCompositeSequenceWithBioSequences();

    /**
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     *
     * @param ids ids
     * @return count
     */
    long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * Function to return all composite sequences with blat results
     *
     * @return count
     */
    long numAllCompositeSequenceWithBlatResults();

    /**
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     *
     * @param ids ids
     * @return count
     */
    long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * Function to return a count of all composite sequences with associated genes.
     *
     * @return count
     */
    long numAllCompositeSequenceWithGenes();

    /**
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     *
     * @param ids ids
     * @return count
     */
    long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * Returns a count of the number of genes associated with all arrayDesigns
     *
     * @return count
     */
    long numAllGenes();

    /**
     * Returns the number of unique Genes associated with the collection of ArrayDesign ids.
     *
     * @param ids ids
     * @return count
     */
    long numAllGenes( Collection<Long> ids );

    /**
     * returns the number of bioSequences associated with this ArrayDesign id
     *
     * @param arrayDesign AD
     * @return count
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numBioSequences( ArrayDesign arrayDesign );

    /**
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     *
     * @param arrayDesign AD
     * @return count
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numBlatResults( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * @param arrayDesign AD
     * @return how many experiments use this platform (not including experiment subsets) security filtered
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numExperiments( ArrayDesign arrayDesign );

    /**
     * Returns the number of unique Genes associated with this ArrayDesign id
     *
     * @param arrayDesign AD
     * @return count
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long numGenes( ArrayDesign arrayDesign );

    /**
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not remove the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     *
     * @param arrayDesign AD
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Thaw a given platform.
     * @see ArrayDesignDao#thaw(ArrayDesign)
     */
    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ArrayDesign thaw( ArrayDesign arrayDesign );

    /**
     * Thaw a collection of platforms.
     * @see ArrayDesignDao#thaw(ArrayDesign)
     */
    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> thaw( Collection<ArrayDesign> aas );

    /**
     * Perform a less intensive thaw of an array design: not the composite sequences.
     *
     * @param arrayDesign AD
     * @return AD
     */
    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ArrayDesign thawLite( ArrayDesign arrayDesign );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    /**
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     *
     * @param candidateSubsumee candidate subsumee
     * @param candidateSubsumer candidate subsumer
     * @return success
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

    /**
     * @param geoAccession for a GEO series or platform
     */
    boolean isBlackListed( String geoAccession );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteGeneProductAnnotationAssociations( ArrayDesign arrayDesign );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteGeneProductAlignmentAssociations( ArrayDesign arrayDesign );

    /**
     * No need for ACL_VALUE_OBJECT_COLLECTION_READ because the filtering is done in the query.
     * @see ArrayDesignDao#loadBlacklistedValueObjects(Filters, Sort, int, int)
     */
    @Secured("GROUP_ADMIN")
    Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Secured("IS_AUTHENTICATED_ANONYMOUSLY")
    Collection<ArrayDesignValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    long countWithCache( @Nullable Filters filters );
}
