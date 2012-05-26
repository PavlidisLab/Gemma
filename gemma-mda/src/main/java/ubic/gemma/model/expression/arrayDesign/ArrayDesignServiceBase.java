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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.arrayDesign.ArrayDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
public abstract class ArrayDesignServiceBase implements ubic.gemma.model.expression.arrayDesign.ArrayDesignService {

    Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao;

    @Autowired
    private AuditEventDao auditEventDao;

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleCompositeSequenceWithoutGenes( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {

        return this.handleCountAll();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#create(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign create(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleCreate( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void deleteAlignmentData( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteAlignmentData( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void deleteGeneProductAssociations( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleDeleteGeneProductAssociations( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign find(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleFind( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByAlternateName(java.lang.String)
     */
    @Override
    public java.util.Collection<ArrayDesign> findByAlternateName( final java.lang.String queryString ) {

        return this.handleFindByAlternateName( queryString );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByName( final java.lang.String name ) {

        return this.handleFindByName( name );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByShortName(java.lang.String)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( final java.lang.String shortName ) {

        return this.handleFindByShortName( shortName );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findOrCreate(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleFindOrCreate( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getAllAssociatedBioAssays(java.lang.Long)
     */
    @Override
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( final java.lang.Long id ) {

        return this.handleGetAllAssociatedBioAssays( id );

    }

    /**
     * @return the auditEventDao
     */
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.lang.Integer getCompositeSequenceCount(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleGetCompositeSequenceCount( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleGetExpressionExperiments( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastAnnotationFile(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastAnnotationFile( final java.util.Collection<Long> ids ) {

        return this.handleGetLastAnnotationFile( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastGeneMapping(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastGeneMapping( final java.util.Collection<Long> ids ) {

        return this.handleGetLastGeneMapping( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastRepeatAnalysis(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastRepeatAnalysis( final java.util.Collection<Long> ids ) {

        return this.handleGetLastRepeatAnalysis( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastSequenceAnalysis(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastSequenceAnalysis( final java.util.Collection<Long> ids ) {

        return this.handleGetLastSequenceAnalysis( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastSequenceUpdate(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastSequenceUpdate( final java.util.Collection<Long> ids ) {

        return this.handleGetLastSequenceUpdate( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastTroubleEvent(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastTroubleEvent( final java.util.Collection<Long> ids ) {

        return this.handleGetLastTroubleEvent( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastValidationEvent(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, AuditEvent> getLastValidationEvent( final java.util.Collection<Long> ids ) {

        return this.handleGetLastValidationEvent( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getTaxa(java.lang.Long)
     */
    @Override
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( final java.lang.Long id ) {

        return this.handleGetTaxa( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getTaxon(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long id ) {

        return this.handleGetTaxon( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isMerged(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isMerged( final java.util.Collection<Long> ids ) {

        return this.handleIsMerged( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isMergee(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isMergee( final java.util.Collection<Long> ids ) {

        return this.handleIsMergee( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isSubsumed(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isSubsumed( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumed( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isSubsumer(java.util.Collection)
     */
    @Override
    public java.util.Map<Long, Boolean> isSubsumer( final java.util.Collection<Long> ids ) {

        return this.handleIsSubsumer( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#load(long)
     */
    @Override
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign load( final long id ) {

        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadAll()
     */
    @Override
    public java.util.Collection<ArrayDesign> loadAll() {

        return this.handleLoadAll();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadAllValueObjects()
     */
    @Override
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects() {

        return this.handleLoadAllValueObjects();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.util.Collection<CompositeSequence> loadCompositeSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleLoadCompositeSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<ArrayDesign> loadMultiple( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    @Override
    public ArrayDesignValueObject loadValueObject( final Long id ) {

        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ArrayDesignValueObject> advos = this.handleLoadValueObjects( ids );
        if ( advos == null || advos.size() < 1 )
            throw new IllegalArgumentException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadValueObject(Long id)' --> "
                            + "no entities found for id = " + id );
        if ( advos.size() > 1 ) {
            // this should never happen
            log.error( "Found more than one ArrayDesign for id = " + id );
        }
        return advos.iterator().next();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    @Override
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( final java.util.Collection<Long> ids ) {

        return this.handleLoadValueObjects( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBioSequences()
     */
    @Override
    public long numAllCompositeSequenceWithBioSequences() {

        return this.handleNumAllCompositeSequenceWithBioSequences();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithBioSequences( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithBioSequences( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBlatResults()
     */
    @Override
    public long numAllCompositeSequenceWithBlatResults() {

        return this.handleNumAllCompositeSequenceWithBlatResults();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithBlatResults( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithBlatResults( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithGenes()
     */
    @Override
    public long numAllCompositeSequenceWithGenes() {

        return this.handleNumAllCompositeSequenceWithGenes();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithGenes(java.util.Collection)
     */
    @Override
    public long numAllCompositeSequenceWithGenes( final java.util.Collection<Long> ids ) {

        return this.handleNumAllCompositeSequenceWithGenes( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllGenes()
     */
    @Override
    public long numAllGenes() {

        return this.handleNumAllGenes();

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllGenes(java.util.Collection)
     */
    @Override
    public long numAllGenes( final java.util.Collection<Long> ids ) {

        return this.handleNumAllGenes( ids );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numBioSequences( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numBlatResults( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumBlatResults( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numCompositeSequenceWithGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumCompositeSequenceWithGenes( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public long numGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleNumGenes( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleRemove( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void removeBiologicalCharacteristics( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleRemoveBiologicalCharacteristics( arrayDesign );

    }

    /**
     * Sets the reference to <code>arrayDesign</code>'s DAO.
     */
    public void setArrayDesignDao( ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param auditEventDao the auditEventDao to set
     */
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign thaw( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleThaw( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign thawLite( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleThawLite( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public void update( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        this.handleUpdate( arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public java.lang.Boolean updateSubsumingStatus(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee ) {

        return this.handleUpdateSubsumingStatus( candidateSubsumer, candidateSubsumee );

    }

    /**
     * Gets the reference to <code>arrayDesign</code>'s DAO.
     */
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesignDao getArrayDesignDao() {
        return this.arrayDesignDao;
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
    protected abstract void handleDeleteAlignmentData( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

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
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindByName( java.lang.String name );

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
    protected abstract java.lang.Integer handleGetCompositeSequenceCount(
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
     * Performs the core logic for {@link #getLastTroubleEvent(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastTroubleEvent( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getLastValidationEvent(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastValidationEvent( java.util.Collection<Long> ids );

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
     * Performs the core logic for {@link #loadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
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