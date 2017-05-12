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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Spring Service base class for <code>ArrayDesignService</code>, provides
 * access to all services and entities referenced by this service.
 *
 * @see ArrayDesignService
 */
public abstract class ArrayDesignServiceBase implements ArrayDesignService {

    protected static final Log log = LogFactory.getLog( ArrayDesignServiceBase.class.getName() );

    @Autowired
    protected ArrayDesignDao arrayDesignDao;

    @Autowired
    protected AuditEventDao auditEventDao;

    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );
    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( final ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );

    }

    /**
     * @see ArrayDesignService#compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> compositeSequenceWithoutGenes( final ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutGenes( arrayDesign );

    }

    /**
     * @see ArrayDesignService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {

        return this.handleCountAll();

    }

    /**
     * @see ArrayDesignService#create(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public ArrayDesign create( final ArrayDesign arrayDesign ) {

        return this.handleCreate( arrayDesign );

    }

    /**
     * @see ArrayDesignService#deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void deleteAlignmentData( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteAlignmentData( arrayDesign );

    }

    /**
     * @see ArrayDesignService#deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void deleteGeneProductAssociations( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteGeneProductAssociations( arrayDesign );

    }

    /**
     * @see ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ArrayDesign find( final ArrayDesign arrayDesign ) {

        return this.handleFind( arrayDesign );

    }

    /**
     * @see ArrayDesignService#findByAlternateName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesign> findByAlternateName( final java.lang.String queryString ) {

        return this.handleFindByAlternateName( queryString );

    }

    /**
     * @see ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByName( final java.lang.String name ) {

        return this.handleFindByName( name );

    }

    /**
     * @see ArrayDesignService#findByShortName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( final java.lang.String shortName ) {

        return this.handleFindByShortName( shortName );

    }

    /**
     * @see ArrayDesignService#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public ArrayDesign findOrCreate( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleFindOrCreate( arrayDesign );

    }

    /**
     * @see ArrayDesignService#getAllAssociatedBioAssays(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( final java.lang.Long id ) {

        return this.handleGetAllAssociatedBioAssays( id );

    }

    /**
     * @see ArrayDesignService#getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Long getCompositeSequenceCount( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleGetCompositeSequenceCount( arrayDesign );

    }

    /**
     * @see ArrayDesignService#getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleGetExpressionExperiments( arrayDesign );

    }

    /**
     * @see ArrayDesignService#getLastAnnotationFile(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, AuditEvent> getLastAnnotationFile( final java.util.Collection<Long> ids ) {

        return this.handleGetLastAnnotationFile( ids );

    }

    /**
     * @see ArrayDesignService#getLastGeneMapping(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, AuditEvent> getLastGeneMapping( final java.util.Collection<Long> ids ) {

        return this.handleGetLastGeneMapping( ids );

    }

    /**
     * @see ArrayDesignService#getLastRepeatAnalysis(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, AuditEvent> getLastRepeatAnalysis( final java.util.Collection<Long> ids ) {

        return this.handleGetLastRepeatAnalysis( ids );

    }

    /**
     * @see ArrayDesignService#getLastSequenceAnalysis(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, AuditEvent> getLastSequenceAnalysis( final java.util.Collection<Long> ids ) {

        return this.handleGetLastSequenceAnalysis( ids );

    }

    /**
     * @see ArrayDesignService#getLastSequenceUpdate(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastSequenceUpdate( final java.util.Collection<Long> ids ) {

        return this.handleGetLastSequenceUpdate( ids );

    }

    /**
     * @see ArrayDesignService#getTaxa(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( final java.lang.Long id ) {

        return this.handleGetTaxa( id );

    }

    /**
     * @see ArrayDesignService#getTaxa(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long id ) {

        return this.handleGetTaxon( id );

    }

    /**
     * @see ArrayDesignService#isMerged(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Boolean> isMerged( final java.util.Collection<Long> ids ) {

        return this.handleIsMerged( ids );

    }

    /**
     * @see ArrayDesignService#isMergee(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Boolean> isMergee( final java.util.Collection<Long> ids ) {

        return this.handleIsMergee( ids );

    }

    /**
     * @see ArrayDesignService#isSubsumed(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Boolean> isSubsumed( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumed( ids );

    }

    /**
     * @see ArrayDesignService#isSubsumer(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Boolean> isSubsumer( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumer( ids );

    }

    /**
     * @see ArrayDesignService#load(long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign load( final long id ) {

        return this.handleLoad( id );

    }

    /**
     * @see ArrayDesignService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesign> loadAll() {

        return this.handleLoadAll();

    }

    /**
     * @see ArrayDesignService#loadAllValueObjects()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects() {

        return this.handleLoadAllValueObjects();

    }

    /**
     * @see ArrayDesignService#getCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> getCompositeSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleLoadCompositeSequences( arrayDesign );

    }

    /**
     * @see ArrayDesignService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesign> loadMultiple( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );

    }

    /**
     * @see ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public ArrayDesignValueObject loadValueObject( final Long id ) {

        Collection<Long> ids = new ArrayList<>();
        ids.add( id );
        Collection<ArrayDesignValueObject> advos = this.handleLoadValueObjects( ids );
        if ( advos == null || advos.size() < 1 )
            throw new IllegalArgumentException(
                    "Error performing 'ArrayDesignService.loadValueObject(Long id)' --> "
                            + "no entities found for id = " + id );
        if ( advos.size() > 1 ) {
            // this should never happen
            log.error( "Found more than one ArrayDesign for id = " + id );
        }
        return advos.iterator().next();

    }

    /**
     * @see ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( final java.util.Collection<Long> ids ) {

        return this.handleLoadValueObjects( ids );

    }

    /**
     * @see ArrayDesignService#loadValueObjectsForEE(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId  ) {

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
     * @see ArrayDesignService#numAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBioSequences( final java.util.Collection<Long> ids ) {

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
     * @see ArrayDesignService#numAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithBlatResults( final java.util.Collection<Long> ids ) {
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
     * @see ArrayDesignService#numAllCompositeSequenceWithGenes(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllCompositeSequenceWithGenes( final java.util.Collection<Long> ids ) {

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
     * @see ArrayDesignService#numAllGenes(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public long numAllGenes( final java.util.Collection<Long> ids ) {

        return this.handleNumAllGenes( ids );

    }

    /**
     * @see ArrayDesignService#numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numBioSequences( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBioSequences( arrayDesign );

    }

    /**
     * @see ArrayDesignService#numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numBlatResults( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBlatResults( arrayDesign );

    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numCompositeSequenceWithBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );

    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public long numCompositeSequenceWithBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );

    }

    /**
     * @see ArrayDesignService#numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithGenes( arrayDesign );

    }

    /**
     * @see ArrayDesignService#numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumGenes( arrayDesign );

    }

    /**
     * @see ArrayDesignService#remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        this.handleRemove( arrayDesign );
    }

    /**
     * @see ArrayDesignService#removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void removeBiologicalCharacteristics(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleRemoveBiologicalCharacteristics( arrayDesign );

    }

    /**
     * @see ArrayDesignService#thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ArrayDesign thaw( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleThaw( arrayDesign );

    }

    /**
     * @see ArrayDesignService#thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ArrayDesign thawLite( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.handleThawLite( arrayDesign );
    }

    /**
     * @see ArrayDesignService#update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        this.handleUpdate( arrayDesign );
    }

    /**
     * @see ArrayDesignService#updateSubsumingStatus(ArrayDesign, ArrayDesign)
     */
    @Override
    @Transactional
    public Boolean updateSubsumingStatus( final ArrayDesign candidateSubsumer, final ArrayDesign candidateSubsumee ) {
        return this.handleUpdateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteAlignmentData(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteGeneProductAssociations(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFind(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #findByAlternateName(java.lang.String)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleFindByAlternateName( java.lang.String queryString );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<ArrayDesign> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #findByShortName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindByShortName(
            java.lang.String shortName );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindOrCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getAllAssociatedBioAssays(java.lang.Long)}
     */
    protected abstract java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id );

    /**
     * Performs the core logic for
     * {@link #getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleGetCompositeSequenceCount(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getLastAnnotationFile(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastAnnotationFile( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastGeneMapping(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastGeneMapping( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastRepeatAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastRepeatAnalysis( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastSequenceAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastSequenceAnalysis( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastSequenceUpdate(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastSequenceUpdate( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getTaxa(java.lang.Long)} Lmd 29/07/09 Fishmanomics provide support multi
     * taxon arrays
     */
    protected abstract java.util.Collection<ubic.gemma.model.genome.Taxon> handleGetTaxa( java.lang.Long id );

    /**
     * Performs the core logic for {@link #getTaxon(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( java.lang.Long id );

    /**
     * Performs the core logic for {@link #isMerged(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMerged( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isMergee(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMergee( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isSubsumed(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumed( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #isSubsumer(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumer( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #load(long)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleLoad( long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ArrayDesign> handleLoadAll();

    /**
     * Performs the core logic for {@link #loadAllValueObjects()}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadAllValueObjects();

    /**
     * Performs the core logic for {@link #getCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoadCompositeSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #loadValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadValueObjects(
            java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #loadValueObjectsForEE(Long)}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadValueObjectsForEE(
            Long eeId );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes()}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes();

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numAllGenes()}
     */
    protected abstract long handleNumAllGenes();

    /**
     * Performs the core logic for {@link #numAllGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllGenes( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleRemoveBiologicalCharacteristics(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ArrayDesign handleThaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ArrayDesign handleThawLite( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for
     * {@link #updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.lang.Boolean handleUpdateSubsumingStatus(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee );

}