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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Service base class for <code>ArrayDesignService</code>, provides
 * access to all services and entities referenced by this service.
 *
 * @see ArrayDesignService
 */
public abstract class ArrayDesignServiceBase extends VoEnabledService<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignService {

    final ArrayDesignDao arrayDesignDao;
    final AuditEventDao auditEventDao;

    @Autowired
    public ArrayDesignServiceBase( ArrayDesignDao arrayDesignDao, AuditEventDao auditEventDao ) {
        super( arrayDesignDao );
        this.arrayDesignDao = arrayDesignDao;
        this.auditEventDao = auditEventDao;
    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutBioSequences(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );
    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutBlatResults(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );
    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutGenes(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign ) {
        return this.handleCompositeSequenceWithoutGenes( arrayDesign );
    }

    /**
     * @see ArrayDesignService#deleteAlignmentData(ArrayDesign)
     */
    @Override
    @Transactional
    public void deleteAlignmentData( ArrayDesign arrayDesign ) {
        this.handleDeleteAlignmentData( arrayDesign );
    }

    /**
     * @see ArrayDesignService#deleteGeneProductAssociations(ArrayDesign)
     */
    @Override
    @Transactional
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        this.handleDeleteGeneProductAssociations( arrayDesign );
    }

    /**
     * @see ArrayDesignService#findByAlternateName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        return this.handleFindByAlternateName( queryString );
    }

    /**
     * @see ArrayDesignService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByName( String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see ArrayDesignService#findByShortName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public ArrayDesign findByShortName( String shortName ) {
        return this.handleFindByShortName( shortName );
    }

    /**
     * @see ArrayDesignService#getAllAssociatedBioAssays(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> getAllAssociatedBioAssays( Long id ) {
        return this.handleGetAllAssociatedBioAssays( id );
    }

    /**
     * @see ArrayDesignService#getCompositeSequenceCount(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Long getCompositeSequenceCount( ArrayDesign arrayDesign ) {
        return this.handleGetCompositeSequenceCount( arrayDesign );
    }

    /**
     * @see ArrayDesignService#getExpressionExperiments(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        return this.handleGetExpressionExperiments( arrayDesign );
    }

    /**
     * @see ArrayDesignService#getLastAnnotationFile(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastAnnotationFile( Collection<Long> ids ) {
        return this.handleGetLastAnnotationFile( ids );
    }

    /**
     * @see ArrayDesignService#getLastGeneMapping(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids ) {
        return this.handleGetLastGeneMapping( ids );
    }

    /**
     * @see ArrayDesignService#getLastRepeatAnalysis(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids ) {
        return this.handleGetLastRepeatAnalysis( ids );
    }

    /**
     * @see ArrayDesignService#getLastSequenceAnalysis(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids ) {
        return this.handleGetLastSequenceAnalysis( ids );
    }

    /**
     * @see ArrayDesignService#getLastSequenceUpdate(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids ) {
        return this.handleGetLastSequenceUpdate( ids );
    }

    /**
     * @see ArrayDesignService#getTaxa(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> getTaxa( Long id ) {
        return this.handleGetTaxa( id );
    }

    /**
     * @see ArrayDesignService#getTaxa(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( Long id ) {
        return this.handleGetTaxon( id );
    }

    /**
     * @see ArrayDesignService#isMerged(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isMerged( Collection<Long> ids ) {
        return this.handleIsMerged( ids );
    }

    /**
     * @see ArrayDesignService#isMergee(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isMergee( Collection<Long> ids ) {
        return this.handleIsMergee( ids );
    }

    /**
     * @see ArrayDesignService#isSubsumed(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumed( Collection<Long> ids ) {
        return this.handleIsSubsumed( ids );
    }

    /**
     * @see ArrayDesignService#isSubsumer(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        return this.handleIsSubsumer( ids );
    }

    /**
     * @see ArrayDesignService#getCompositeSequences(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign ) {
        return this.handleLoadCompositeSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesignValueObject> loadValueObjectsFilter( int offset, int limit, String orderBy, boolean asc ) {
        return this.arrayDesignDao.listFilter( offset, limit, orderBy, asc );
    }

    /**
     * @see ArrayDesignService#loadValueObjectsForEE(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId ) {
        return this.handleLoadValueObjectsForEE( eeId );
    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithBioSequences()
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBioSequences() {
        return this.handleNumAllCompositeSequenceWithBioSequences();
    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithBioSequences(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {
        return this.handleNumAllCompositeSequenceWithBioSequences( ids );
    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithBlatResults()
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBlatResults() {
        return this.handleNumAllCompositeSequenceWithBlatResults();
    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithBlatResults(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        return this.handleNumAllCompositeSequenceWithBlatResults( ids );

    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithGenes()
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithGenes() {
        return this.handleNumAllCompositeSequenceWithGenes();
    }

    /**
     * @see ArrayDesignService#numAllCompositeSequenceWithGenes(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithGenes( Collection<Long> ids ) {
        return this.handleNumAllCompositeSequenceWithGenes( ids );
    }

    /**
     * @see ArrayDesignService#numAllGenes()
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllGenes() {
        return this.handleNumAllGenes();
    }

    /**
     * @see ArrayDesignService#numAllGenes(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllGenes( Collection<Long> ids ) {
        return this.handleNumAllGenes( ids );
    }

    /**
     * @see ArrayDesignService#numBioSequences(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numBioSequences( ArrayDesign arrayDesign ) {
        return this.handleNumBioSequences( arrayDesign );
    }

    /**
     * @see ArrayDesignService#numBlatResults(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numBlatResults( ArrayDesign arrayDesign ) {
        return this.handleNumBlatResults( arrayDesign );
    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithBioSequences(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );
    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithBlatResults(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );
    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithGenes(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        return this.handleNumCompositeSequenceWithGenes( arrayDesign );
    }

    /**
     * @see ArrayDesignService#numGenes(ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numGenes( ArrayDesign arrayDesign ) {
        return this.handleNumGenes( arrayDesign );
    }

    /**
     * @see ArrayDesignService#removeBiologicalCharacteristics(ArrayDesign)
     */
    @Override
    @Transactional
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        this.handleRemoveBiologicalCharacteristics( arrayDesign );
    }

    /**
     * @see ArrayDesignService#updateSubsumingStatus(ArrayDesign, ArrayDesign)
     */
    @Override
    @Transactional
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {
        return this.handleUpdateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

    @Override
    @Transactional
    public abstract Collection<ArrayDesignValueObject> loadValueObjectsByIds( Collection<Long> ids );

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBioSequences(ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences(
            ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBlatResults(ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults(
            ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutGenes(ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #deleteAlignmentData(ArrayDesign)}
     */
    protected abstract void handleDeleteAlignmentData( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #deleteGeneProductAssociations(ArrayDesign)}
     */
    protected abstract void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #findByAlternateName(String)}
     */
    protected abstract Collection<ArrayDesign> handleFindByAlternateName( String queryString );

    /**
     * Performs the core logic for {@link #findByName(String)}
     */
    protected abstract Collection<ArrayDesign> handleFindByName( String name );

    /**
     * Performs the core logic for {@link #findByShortName(String)}
     */
    protected abstract ArrayDesign handleFindByShortName( String shortName );

    /**
     * Performs the core logic for {@link #getAllAssociatedBioAssays(Long)}
     */
    protected abstract Collection<BioAssay> handleGetAllAssociatedBioAssays( Long id );

    /**
     * Performs the core logic for
     * {@link #getCompositeSequenceCount(ArrayDesign)}
     */
    protected abstract long handleGetCompositeSequenceCount( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiments(ArrayDesign)}
     */
    protected abstract Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getLastAnnotationFile(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastAnnotationFile( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastGeneMapping(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastGeneMapping( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastRepeatAnalysis(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastRepeatAnalysis( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastSequenceAnalysis(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastSequenceAnalysis( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastSequenceUpdate(Collection)}
     */
    protected abstract Map<Long, AuditEvent> handleGetLastSequenceUpdate( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getTaxa(Long)} Lmd 29/07/09 FishManOmics provide support multi
     * taxon arrays
     */
    protected abstract Collection<Taxon> handleGetTaxa( Long id );

    /**
     * Performs the core logic for {@link #getTaxon(Long)}
     */
    protected abstract Taxon handleGetTaxon( Long id );

    /**
     * Performs the core logic for {@link #isMerged(Collection)}
     */
    protected abstract Map<Long, Boolean> handleIsMerged( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isMergee(Collection)}
     */
    protected abstract Map<Long, Boolean> handleIsMergee( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isSubsumed(Collection)}
     */
    protected abstract Map<Long, Boolean> handleIsSubsumed( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isSubsumer(Collection)}
     */
    protected abstract Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getCompositeSequences(ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleLoadCompositeSequences( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #loadValueObjectsForEE(Long)}
     */
    protected abstract Collection<ArrayDesignValueObject> handleLoadValueObjectsForEE( Long eeId );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences(Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults(Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes()}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes(Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllGenes()}
     */
    protected abstract long handleNumAllGenes();

    /**
     * Performs the core logic for {@link #numAllGenes(Collection)}
     */
    protected abstract long handleNumAllGenes( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numBioSequences(ArrayDesign)}
     */
    protected abstract long handleNumBioSequences( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #numBlatResults(ArrayDesign)}
     */
    protected abstract long handleNumBlatResults( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBioSequences(ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBlatResults(ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithGenes(ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #numGenes(ArrayDesign)}
     */
    protected abstract long handleNumGenes( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #removeBiologicalCharacteristics(ArrayDesign)}
     */
    protected abstract void handleRemoveBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #updateSubsumingStatus(ArrayDesign, ArrayDesign)}
     */
    protected abstract Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer,
            ArrayDesign candidateSubsumee );

}