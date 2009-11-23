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

import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
public interface ArrayDesignDao extends BaseDao<ArrayDesign> {

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have bioSequence associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have BLAT results.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign without gene associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public void deleteAlignmentData( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * deletes the gene product associations on the specified array design
     * </p>
     */
    public void deleteGeneProductAssociations( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign find(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<ArrayDesign> findByAlternateName( java.lang.String queryString );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByName( java.lang.String name );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( java.lang.String shortName );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findOrCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( java.lang.Long id );

    /**
     * <p>
     * Get all audit events associated with the specified arrayDesign ids.
     * </p>
     */
    public java.util.Map<Long, Collection<AuditEvent>> getAuditEvents( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( java.lang.Long id );

    /**
     * @deprecated Use getTaxa as array designs can have more than one associated taxon. This method will return only
     *             the first taxon found.
     */
    @Deprecated
    public ubic.gemma.model.genome.Taxon getTaxon( java.lang.Long id );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isMerged( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isMergee( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isSubsumed( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isSubsumer( java.util.Collection<Long> ids );

    /**
     * <p>
     * Given a list of AD ids (longs) returns a collection of ArrayDesign Objects
     * </p>
     */
    public java.util.Collection<ArrayDesign> load( java.util.Collection<Long> ids );

    /**
     * <p>
     * loads all Array designs as value objects.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * <p>
     * Needed because we want to lazy-load composite sequences
     * </p>
     */
    public java.util.Collection<CompositeSequence> loadCompositeSequences( java.lang.Long id );

    /**
     * <p>
     * does a 'thaw' of the given arrayDesign id, and returns the thawed arrayDesign.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign loadFully( java.lang.Long id );

    /**
     * <p>
     * loads the Value Objects for the Array Designs specified by the input ids.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( java.util.Collection<Long> ids );

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
    public long numAllCompositeSequenceWithBioSequences( java.util.Collection<Long> ids );

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
    public long numAllCompositeSequenceWithBlatResults( java.util.Collection<Long> ids );

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
    public long numAllCompositeSequenceWithGenes( java.util.Collection<Long> ids );

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
    public long numAllGenes( java.util.Collection<Long> ids );

    /**
     * <p>
     * returns the number of bioSequences associated with this ArrayDesign
     * </p>
     */
    public long numBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     * </p>
     */
    public long numBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer numCompositeSequences( java.lang.Long id );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have
     * bioSequences associated with them. The bioSequences matched have a non-null sequence.
     * </p>
     */
    public long numCompositeSequenceWithBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have blat
     * results associated with them.
     * </p>
     */
    public long numCompositeSequenceWithBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have genes
     * associated with them.
     * </p>
     */
    public long numCompositeSequenceWithGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a predicted gene.
     * </p>
     */
    public long numCompositeSequenceWithPredictedGene( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a probe-mapped region.
     * </p>
     */
    public long numCompositeSequenceWithProbeAlignedRegion(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Returns the number of Genes associated with this ArrayDesign
     * </p>
     */
    public long numGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer numReporters( java.lang.Long id );

    /**
     * <p>
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     * </p>
     */
    public void removeBiologicalCharacteristics( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Unlazify associations of this object.
     * </p>
     */
    public void thaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Perform a less intensive thaw of an array design.
     * </p>
     */
    public void thawLite( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     * </p>
     */
    public java.lang.Boolean updateSubsumingStatus(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee );

}
