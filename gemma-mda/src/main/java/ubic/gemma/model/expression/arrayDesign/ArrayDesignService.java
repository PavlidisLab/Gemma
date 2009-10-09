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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @version $Id$
 */
public interface ArrayDesignService extends ubic.gemma.model.common.AuditableService {

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have any bioSequence associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have BLAT result associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have gene associations.
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
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign create(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * delete sequence alignment results associated with the bioSequences for this array design.
     * </p>
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
     * 
     */
    public java.lang.Integer getCompositeSequenceCount( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Gets the AuditEvents of the latest annotation file event for the specified array design ids. This returns a map
     * of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     * </p>
     */
    public java.util.Map<Long, AuditEvent> getLastAnnotationFile( java.util.Collection<Long> ids );

    /**
     * <p>
     * Gets the AuditEvents of the latest gene mapping for the specified array design ids. This returns a map of id ->
     * AuditEvent. If the events do not exist, the map entry will point to null.
     * </p>
     */
    public java.util.Map<Long, AuditEvent> getLastGeneMapping( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, AuditEvent> getLastRepeatAnalysis( java.util.Collection<Long> ids );

    /**
     * <p>
     * Gets the AuditEvents of the latest sequence analyses for the specified array design ids. This returns a map of id
     * -> AuditEvent. If the events do not exist, the map entry will point to null.
     * </p>
     */
    public java.util.Map<Long, AuditEvent> getLastSequenceAnalysis( java.util.Collection<Long> ids );

    /**
     * <p>
     * Gets the AuditEvents of the latest sequence update for the specified array design ids. This returns a map of id
     * -> AuditEvent. If the events do not exist, the map entry will point to null.
     * </p>
     */
    public java.util.Map<Long, AuditEvent> getLastSequenceUpdate( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, AuditEvent> getLastTroubleEvent( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, AuditEvent> getLastValidationEvent( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.lang.Integer getReporterCount( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Return the taxon for the array design. This can be misleading if the array uses multiple taxa: this method will
     * return the first one found.
     * 
     * @param id The id of the array design
     * @return The taxon
     */
    public ubic.gemma.model.genome.Taxon getTaxon( java.lang.Long id );

    /**
     * Return the taxa for the array design. This can be multiple, or zero if the array is not processed.
     * 
     * @param id The id of the array design
     * @return The Set of Taxons for array design.
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( java.lang.Long id );

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
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign load( long id );

    /**
     * 
     */
    public java.util.Collection<ArrayDesign> loadAll();

    /**
     * <p>
     * loads all Array designs as value objects.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> loadCompositeSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does a 'thaw' of an arrayDesign given an id. Returns the thawed arrayDesign.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign loadFully( java.lang.Long id );

    /**
     * <p>
     * Given a collection of ID (longs) will return a collection of ArrayDesigns
     * </p>
     */
    public java.util.Collection<ArrayDesign> loadMultiple( java.util.Collection<Long> ids );

    /**
     * <p>
     * loads the Value Objects for the Array Designs specified by the input ids.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( java.util.Collection<Long> ids );

    /**
     * <p>
     * Function to return a count of all compositeSequences with bioSequence associations
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
     * Function to return all composite sequences with blat results
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
     * Function to return a count of all composite sequences with associated genes.
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
     * Returns a count of the number of genes associated with all arrayDesigns
     * </p>
     */
    public long numAllGenes();

    /**
     * <p>
     * Returns the number of unique Genes associated with the collection of ArrayDesign ids.
     * </p>
     */
    public long numAllGenes( java.util.Collection<Long> ids );

    /**
     * <p>
     * returns the number of bioSequences associated with this ArrayDesign id
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
    public long numCompositeSequenceWithBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public long numCompositeSequenceWithBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public long numCompositeSequenceWithGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a predicted gene
     * </p>
     */
    public long numCompositeSequenceWithPredictedGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a probe-mapped region.
     * </p>
     */
    public long numCompositeSequenceWithProbeAlignedRegion(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Returns the number of unique Genes associated with this ArrayDesign id
     * </p>
     */
    public long numGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public void remove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

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
     * 
     */
    public void thaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Perform a less intensive thaw of an array design.
     * </p>
     */
    public void thawLite( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public void update( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

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
