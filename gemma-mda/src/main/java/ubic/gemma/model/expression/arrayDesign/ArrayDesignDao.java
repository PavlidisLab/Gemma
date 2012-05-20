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
import ubic.gemma.persistence.BaseDao;

/**
 * @see ArrayDesign
 */
@Repository
public interface ArrayDesignDao extends BaseDao<ArrayDesign> {

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have bioSequence associations.
     * </p>
     */
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have BLAT results.
     * </p>
     */
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign without gene associations.
     * </p>
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
     * <p>
     * deletes the gene product associations on the specified array design
     * </p>
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
     * 
     */
    public ArrayDesign findByName( String name );

    /**
     * 
     */
    public ArrayDesign findByShortName( String shortName );

    /**
     * 
     */
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Collection<BioAssay> getAllAssociatedBioAssays( Long id );

    /**
     * <p>
     * Get all audit events associated with the specified arrayDesign ids.
     * </p>
     */
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    /**
     * 
     */
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

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
     * <p>
     * Given a list of AD ids (longs) returns a collection of ArrayDesign Objects
     * </p>
     */
    public Collection<ArrayDesign> load( Collection<Long> ids );

    /**
     * <p>
     * loads all Array designs as value objects.
     * </p>
     */
    public Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * <p>
     * Needed because we want to lazy-load composite sequences
     * </p>
     */
    public Collection<CompositeSequence> loadCompositeSequences( Long id );

    /**
     * <p>
     * loads the Value Objects for the Array Designs specified by the input ids.
     * </p>
     */
    public Collection<ArrayDesignValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * <p>
     * Function to count all composite sequences with bioSequences.
     * </p>
     */
    public long numAllCompositeSequenceWithBioSequences();

    /**
     * <p>
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * <p>
     * Function to count all compositeSequences with blat results.
     * </p>
     */
    public long numAllCompositeSequenceWithBlatResults();

    /**
     * <p>
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * <p>
     * Function to count all compositeSequences with associated genes.
     * </p>
     */
    public long numAllCompositeSequenceWithGenes();

    /**
     * <p>
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * <p>
     * Returns a count of the genes associated with all composite Sequences
     * </p>
     */
    public long numAllGenes();

    /**
     * <p>
     * returns a count of the unique genes associated witht the given arrayDesigns
     * </p>
     */
    public long numAllGenes( Collection<Long> ids );

    /**
     * <p>
     * returns the number of bioSequences associated with this ArrayDesign
     * </p>
     */
    public long numBioSequences( ArrayDesign arrayDesign );

    /**
     * <p>
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     * </p>
     */
    public long numBlatResults( ArrayDesign arrayDesign );

    /**
     * 
     */
    public Integer numCompositeSequences( Long id );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have
     * bioSequences associated with them. The bioSequences matched have a non-null sequence.
     * </p>
     */
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have blat
     * results associated with them.
     * </p>
     */
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have genes
     * associated with them.
     * </p>
     */
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * <p>
     * Returns the number of Genes associated with this ArrayDesign
     * </p>
     */
    public long numGenes( ArrayDesign arrayDesign );

    /**
     * <p>
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     * </p>
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
     * <p>
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     * </p>
     */
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

    /**
     * @return a map of taxon -> count of how many array designs there are for that taxon. Taxa with no arrays are
     *         excluded.
     */
    public Map<Taxon, Integer> getPerTaxonCount();

    /**
     * @param searchString
     * @return
     */
    public Collection<ArrayDesign> findByManufacturer( String searchString );

    /**
     * @param taxon
     * @return
     */
    public Collection<ArrayDesign> findByTaxon( Taxon taxon );

    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newprobes );

}
