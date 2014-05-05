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

import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ArrayDesign
 */
@Repository
public interface ArrayDesignDao extends BaseDao<ArrayDesign> {

    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newprobes );

    /**
     * returns all compositeSequences for the given arrayDesign that do not have bioSequence associations.
     */
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    /**
     * returns all compositeSequences for the given arrayDesign that do not have BLAT results.
     */
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    /**
     * returns all compositeSequences for the given arrayDesign without gene associations.
     */
    public Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Integer countAll();

    /**
     * 
     */
    public void deleteAlignmentData( ArrayDesign arrayDesign );

    /**
     * deletes the gene product associations on the specified array design
     */
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    /**
     * 
     */
    public ArrayDesign find( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<ArrayDesign> findByAlternateName( String queryString );

    /**
     * @param searchString
     * @return
     */
    public Collection<ArrayDesign> findByManufacturer( String searchString );

    /**
     * 
     */
    public Collection<ArrayDesign> findByName( String name );

    /**
     * 
     */
    public ArrayDesign findByShortName( String shortName );

    /**
     * @param taxon
     * @return
     */
    public Collection<ArrayDesign> findByTaxon( Taxon taxon );

    /**
     * 
     */
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<BioAssay> getAllAssociatedBioAssays( Long id );

    /**
     * Get all audit events associated with the specified arrayDesign ids.
     */
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * @return a map of taxon -> count of how many array designs there are for that taxon. Taxa with no arrays are
     *         excluded.
     */
    public Map<Taxon, Integer> getPerTaxonCount();

    /**
     * 
     */
    public Collection<ubic.gemma.model.genome.Taxon> getTaxa( Long id );

    /**
     * @deprecated Use getTaxa as array designs can have more than one associated taxon. This method will return only
     *             the first taxon found.
     */
    @Deprecated
    public ubic.gemma.model.genome.Taxon getTaxon( Long id );

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
     * Limited to those which map to a geneproduct FIXME rename this method to reflect that more obviously.
     * 
     * @param arrayDesign
     * @return
     */
    public Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign );

    /**
     * loads all Array designs as value objects.
     */
    public Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * Needed because we want to lazy-load composite sequences
     */
    public Collection<CompositeSequence> loadCompositeSequences( Long id );

    /**
     * loads the Value Objects for the Array Designs specified by the input ids.
     */
    public Collection<ArrayDesignValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * Function to count all composite sequences with bioSequences.
     */
    public long numAllCompositeSequenceWithBioSequences();

    /**
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * Function to count all compositeSequences with blat results.
     */
    public long numAllCompositeSequenceWithBlatResults();

    /**
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * Function to count all compositeSequences with associated genes.
     */
    public long numAllCompositeSequenceWithGenes();

    /**
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     */
    public long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * Returns a count of the genes associated with all composite Sequences
     */
    public long numAllGenes();

    /**
     * returns a count of the unique genes associated witht the given arrayDesigns
     */
    public long numAllGenes( Collection<Long> ids );

    /**
     * returns the number of bioSequences associated with this ArrayDesign
     */
    public long numBioSequences( ArrayDesign arrayDesign );

    /**
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     */
    public long numBlatResults( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Integer numCompositeSequences( Long id );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have
     * bioSequences associated with them. The bioSequences matched have a non-null sequence.
     */
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have blat
     * results associated with them.
     */
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    /**
     * Given an array design, returns the number of unique composite sequences from that array design that have genes
     * associated with them.
     */
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * @param arrayDesign
     * @return how many experiments use this platform (not including experiment subsets)
     */
    public int numExperiments( ArrayDesign arrayDesign );

    /**
     * Returns the number of Genes associated with this ArrayDesign
     */
    public long numGenes( ArrayDesign arrayDesign );

    /**
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     */
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Unlazify associations of this object including the composite sequences and associated biosequences.
     */
    public ArrayDesign thaw( ArrayDesign arrayDesign );

    /**
     * Thaw just the basic information about the design, excluding the compositesequences.
     * 
     * @param arrayDesign
     * @return
     */
    public ArrayDesign thawLite( ArrayDesign arrayDesign );

    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    /**
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     */
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

}
