/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author klc
 * @see ArrayDesignService
 */
@Service
public class ArrayDesignServiceImpl extends ArrayDesignServiceBase {

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public ArrayDesignServiceImpl( ArrayDesignDao arrayDesignDao, AuditEventDao auditEventDao ) {
        super( arrayDesignDao, auditEventDao );
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    @Transactional
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes ) {
        this.arrayDesignDao.addProbes( arrayDesign, newProbes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return this.arrayDesignDao.findByManufacturer( searchString );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return this.arrayDesignDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.loadAlignments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.arrayDesignDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesignValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        return this.arrayDesignDao.loadValueObjectsByIds( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public void thawLite( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesignDao.thawLite( arrayDesigns );
    }

    @Override
    @Transactional(readOnly = true)
    public void thawLite( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.thawLite( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public int numExperiments( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numExperiments( arrayDesign );
    }

    /* ********************************
     * Protected methods
     * ********************************/

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.compositeSequenceWithoutBioSequences( arrayDesign );
    }

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.compositeSequenceWithoutBlatResults( arrayDesign );
    }

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.compositeSequenceWithoutGenes( arrayDesign );
    }

    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteAlignmentData( arrayDesign );
    }

    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteGeneProductAssociations( arrayDesign );
    }

    @Override
    protected Collection<ArrayDesign> handleFindByAlternateName( String queryString ) {
        return this.arrayDesignDao.findByAlternateName( queryString );
    }

    /**
     * @see ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    protected Collection<ArrayDesign> handleFindByName( String name ) {
        return this.arrayDesignDao.findByName( name );
    }

    @Override
    protected ArrayDesign handleFindByShortName( String shortName ) {
        return this.arrayDesignDao.findByShortName( shortName );
    }

    @Override
    protected java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id ) {
        return this.arrayDesignDao.getAllAssociatedBioAssays( id );

    }

    @Override
    protected Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getExpressionExperiments( arrayDesign );
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastAnnotationFile( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );

        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not AnnotationFile events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignAnnotationFileEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastGeneMapping( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignGeneMappingEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastRepeatAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignRepeatAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceUpdate( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not Sequence update events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceUpdateEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Collection<Taxon> handleGetTaxa( java.lang.Long id ) {
        return this.arrayDesignDao.getTaxa( id );
    }

    @Override
    protected Taxon handleGetTaxon( java.lang.Long id ) {
        return this.arrayDesignDao.load( id ).getPrimaryTaxon();
    }

    @Override
    protected Map<Long, Boolean> handleIsMerged( Collection<Long> ids ) {
        return this.arrayDesignDao.isMerged( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsMergee( Collection<Long> ids ) {
        return this.arrayDesignDao.isMergee( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumed( Collection<Long> ids ) {
        return this.arrayDesignDao.isSubsumed( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids ) {
        return this.arrayDesignDao.isSubsumer( ids );
    }

    @Override
    protected Collection<CompositeSequence> handleLoadCompositeSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.loadCompositeSequences( arrayDesign.getId() );
    }

    @Override
    protected Collection<ArrayDesignValueObject> handleLoadValueObjectsForEE( Long eeId ) {
        return this.arrayDesignDao.loadValueObjectsForEE( eeId );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() {
        return this.arrayDesignDao.numAllCompositeSequenceWithBioSequences();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {
        return this.arrayDesignDao.numAllCompositeSequenceWithBioSequences( ids );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() {
        return this.arrayDesignDao.numAllCompositeSequenceWithBlatResults();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        return this.arrayDesignDao.numAllCompositeSequenceWithBlatResults( ids );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithGenes() {
        return this.arrayDesignDao.numAllCompositeSequenceWithGenes();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection<Long> ids ) {
        return this.arrayDesignDao.numAllCompositeSequenceWithGenes( ids );
    }

    @Override
    protected long handleNumAllGenes() {
        return this.arrayDesignDao.numAllGenes();
    }

    @Override
    protected long handleNumAllGenes( Collection<Long> ids ) {
        return this.arrayDesignDao.numAllGenes( ids );
    }

    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numBioSequences( arrayDesign );
    }

    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numBlatResults( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numCompositeSequenceWithBioSequences( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numCompositeSequenceWithBlatResults( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numCompositeSequenceWithGenes( arrayDesign );
    }

    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.numGenes( arrayDesign );
    }

    @Override
    protected long handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Array design cannot be null" );
        return this.arrayDesignDao.numCompositeSequences( arrayDesign.getId() );
    }

    @Override
    protected void handleRemoveBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.removeBiologicalCharacteristics( arrayDesign );

    }

    @Override
    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {
        return this.arrayDesignDao.updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

    /* ********************************
     * Private methods
     * ********************************/

    private void checkForMoreRecentMethod( Map<Long, AuditEvent> lastEventMap,
            Class<? extends ArrayDesignAnalysisEvent> eventclass, Long arrayDesignId, ArrayDesign subsumedInto ) {
        AuditEvent lastSubsumerEvent = this.auditEventDao.getLastEvent( subsumedInto, eventclass );
        if ( lastSubsumerEvent != null && lastEventMap.containsKey( arrayDesignId )
                && lastEventMap.get( arrayDesignId ) != null && lastEventMap.get( arrayDesignId ).getDate()
                .before( lastSubsumerEvent.getDate() ) ) {
            lastEventMap.put( arrayDesignId, lastSubsumerEvent );
        }
    }

    private void getMostRecentEvents( Map<Long, Collection<AuditEvent>> eventMap, Map<Long, AuditEvent> lastEventMap,
            Set<Long> aaIds, Class<? extends ArrayDesignAnalysisEvent> eventclass ) {
        for ( Long arrayDesignId : aaIds ) {

            Collection<AuditEvent> events = eventMap.get( arrayDesignId );
            AuditEvent lastEvent;

            if ( events == null ) {
                lastEventMap.put( arrayDesignId, null );
            } else {
                ArrayDesign ad = this.load( arrayDesignId );
                lastEvent = this.auditEventDao.getLastEvent( ad, eventclass );
                lastEventMap.put( arrayDesignId, lastEvent );
            }

            /*
             * Check if the subsuming or merged array (if any) was updated more recently. To do this: 1) load the AA; 2)
             * check for merged; check for subsumed; check events for those.
             */
            ArrayDesign arrayDesign = this.load( arrayDesignId );
            if ( arrayDesign.getSubsumingArrayDesign() != null ) {
                ArrayDesign subsumedInto = arrayDesign.getSubsumingArrayDesign();
                checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, subsumedInto );
            }
            if ( arrayDesign.getMergedInto() != null ) {
                ArrayDesign mergedInto = arrayDesign.getMergedInto();
                checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, mergedInto );
            }

        }
    }
}