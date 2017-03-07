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

import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.BaseDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see ArrayDesign
 */
@Repository
public interface ArrayDesignDao extends BaseDao<ArrayDesign> {

    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newprobes );

    /**
     * returns all compositeSequences for the given arrayDesign that do not have bioSequence associations.
     */
    Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    /**
     * returns all compositeSequences for the given arrayDesign that do not have BLAT results.
     */
    Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    /**
     * returns all compositeSequences for the given arrayDesign without gene associations.
     */
    Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    /**
     *
     */
    Integer countAll();

    /**
     *
     */
    void deleteAlignmentData( ArrayDesign arrayDesign );

    /**
     * deletes the gene product associations on the specified array design
     */
    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    /**
     *
     */
    ArrayDesign find( ArrayDesign arrayDesign );

    /**
     *
     */
    Collection<ArrayDesign> findByAlternateName( String queryString );

    Collection<ArrayDesign> findByManufacturer( String searchString );

    Collection<ArrayDesign> findByName( String name );

    ArrayDesign findByShortName( String shortName );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    ArrayDesign findOrCreate( ArrayDesign arrayDesign );

    Collection<BioAssay> getAllAssociatedBioAssays( Long id );

    /**
     * Get all audit events associated with the specified arrayDesign ids.
     */
    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * @return a map of taxon -> count of how many array designs there are for that taxon. Taxa with no arrays are
     * excluded.
     */
    Map<Taxon, Integer> getPerTaxonCount();

    Collection<ubic.gemma.model.genome.Taxon> getTaxa( Long id );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    /**
     * Limited to those which map to a geneproduct FIXME rename this method to reflect that more obviously.
     */
    Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign );

    /**
     * loads all Array designs as value objects.
     */
    Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * Needed because we want to lazy-load composite sequences
     */
    Collection<CompositeSequence> loadCompositeSequences( Long id );

    /**
     * loads the Value Objects for the Array Designs specified by the input ids.
     */
    Collection<ArrayDesignValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * Function to count all composite sequences with bioSequences.
     */
    long numAllCompositeSequenceWithBioSequences();

    /**
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     */
    long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * Function to count all compositeSequences with blat results.
     */
    long numAllCompositeSequenceWithBlatResults();

    /**
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     */
    long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * Function to count all compositeSequences with associated genes.
     */
    long numAllCompositeSequenceWithGenes();

    /**
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     */
    long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * Returns a count of the genes associated with all composite Sequences
     */
    long numAllGenes();

    /**
     * returns a count of the unique genes associated witht the given arrayDesigns
     */
    long numAllGenes( Collection<Long> ids );

    /**
     * returns the number of bioSequences associated with this ArrayDesign
     */
    long numBioSequences( ArrayDesign arrayDesign );

    /**
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     */
    long numBlatResults( ArrayDesign arrayDesign );

    Integer numCompositeSequences( Long id );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have
     * bioSequences associated with them. The bioSequences matched have a non-null sequence.
     */
    long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have blat
     * results associated with them.
     */
    long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have genes
     * associated with them.
     */
    long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * @return how many experiments use this platform (not including experiment subsets), security filtered
     */
    int numExperiments( ArrayDesign arrayDesign );

    /**
     * Returns the number of Genes associated with this ArrayDesign
     */
    long numGenes( ArrayDesign arrayDesign );

    /**
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     */
    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Unlazify associations of this object including the composite sequences and associated biosequences.
     */
    ArrayDesign thaw( ArrayDesign arrayDesign );

    /**
     * Thaw just the basic information about the design, excluding the compositesequences.
     */
    ArrayDesign thawLite( ArrayDesign arrayDesign );

    Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    /**
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     */
    Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

}
