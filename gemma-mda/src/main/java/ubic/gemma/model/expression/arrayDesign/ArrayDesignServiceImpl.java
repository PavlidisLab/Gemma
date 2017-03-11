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
package ubic.gemma.model.expression.arrayDesign;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author klc
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
@Service
public class ArrayDesignServiceImpl extends ArrayDesignServiceBase {

    @Override
    @Transactional
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes ) {
        this.getArrayDesignDao().addProbes( arrayDesign, newProbes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return this.getArrayDesignDao().findByManufacturer( searchString );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return this.getArrayDesignDao().findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().loadAlignments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().getBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.getArrayDesignDao().getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        return this.getArrayDesignDao().thawLite( arrayDesigns );
    }

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().compositeSequenceWithoutBioSequences( arrayDesign );
    }

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().compositeSequenceWithoutBlatResults( arrayDesign );
    }

    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().compositeSequenceWithoutGenes( arrayDesign );
    }

    @Override
    protected Integer handleCountAll() {
        return this.getArrayDesignDao().countAll();
    }

    @Override
    protected ArrayDesign handleCreate( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().create( arrayDesign );
    }

    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) {
        this.getArrayDesignDao().deleteAlignmentData( arrayDesign );
    }

    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        this.getArrayDesignDao().deleteGeneProductAssociations( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleFind( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().find( arrayDesign );
    }

    @Override
    protected Collection<ArrayDesign> handleFindByAlternateName( String queryString ) {
        return this.getArrayDesignDao().findByAlternateName( queryString );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    protected Collection<ArrayDesign> handleFindByName( String name ) {
        return this.getArrayDesignDao().findByName( name );
    }

    @Override
    protected ArrayDesign handleFindByShortName( String shortName ) {
        return this.getArrayDesignDao().findByShortName( shortName );
    }

    @Override
    protected ArrayDesign handleFindOrCreate( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().findOrCreate( arrayDesign );
    }

    @Override
    protected java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id ) {
        return this.getArrayDesignDao().getAllAssociatedBioAssays( id );

    }

    @Override
    protected Integer handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Array design cannot be null" );
        return this.getArrayDesignDao().numCompositeSequences( arrayDesign.getId() );
    }

    @Override
    protected Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().getExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public int numExperiments( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numExperiments( arrayDesign );
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastAnnotationFile( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );

        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not AnnotationFile events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignAnnotationFileEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastGeneMapping( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignGeneMappingEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastRepeatAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignRepeatAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceUpdate( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not Sequence update events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceUpdateEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    protected Collection<Taxon> handleGetTaxa( java.lang.Long id ) {
        return this.getArrayDesignDao().getTaxa( id );
    }

    @Override
    protected Taxon handleGetTaxon( java.lang.Long id ) {
        return this.getArrayDesignDao().load( id ).getPrimaryTaxon();
    }

    @Override
    protected Map<Long, Boolean> handleIsMerged( Collection<Long> ids ) {
        return this.getArrayDesignDao().isMerged( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsMergee( Collection<Long> ids ) {
        return this.getArrayDesignDao().isMergee( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumed( Collection<Long> ids ) {
        return this.getArrayDesignDao().isSubsumed( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids ) {
        return this.getArrayDesignDao().isSubsumer( ids );
    }

    @Override
    protected ArrayDesign handleLoad( long id ) {
        return this.getArrayDesignDao().load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<ArrayDesign> handleLoadAll() {
        return ( Collection<ArrayDesign> ) this.getArrayDesignDao().loadAll();
    }

    @Override
    protected Collection<ArrayDesignValueObject> handleLoadAllValueObjects() {
        return this.getArrayDesignDao().loadAllValueObjects();
    }

    @Override
    protected Collection<CompositeSequence> handleLoadCompositeSequences( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().loadCompositeSequences( arrayDesign.getId() );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ArrayDesign> handleLoadMultiple( Collection<Long> ids ) {
        return this.getArrayDesignDao().load( ids );
    }

    @Override
    protected Collection<ArrayDesignValueObject> handleLoadValueObjects( Collection<Long> ids ) {
        return this.getArrayDesignDao().loadValueObjects( ids );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences( ids );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBlatResults();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBlatResults( ids );
    }

    @Override
    protected long handleNumAllCompositeSequenceWithGenes() {
        return this.getArrayDesignDao().numAllCompositeSequenceWithGenes();
    }

    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection<Long> ids ) {
        return this.getArrayDesignDao().numAllCompositeSequenceWithGenes( ids );
    }

    @Override
    protected long handleNumAllGenes() {
        return this.getArrayDesignDao().numAllGenes();
    }

    @Override
    protected long handleNumAllGenes( Collection<Long> ids ) {
        return this.getArrayDesignDao().numAllGenes( ids );
    }

    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numBioSequences( arrayDesign );
    }

    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numBlatResults( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numCompositeSequenceWithBioSequences( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numCompositeSequenceWithBlatResults( arrayDesign );
    }

    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numCompositeSequenceWithGenes( arrayDesign );
    }

    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().numGenes( arrayDesign );
    }

    @Override
    protected void handleRemove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        this.getArrayDesignDao().remove( arrayDesign );
    }

    @Override
    protected void handleRemoveBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        this.getArrayDesignDao().removeBiologicalCharacteristics( arrayDesign );

    }

    @Override
    protected ArrayDesign handleThaw( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().thaw( arrayDesign );
    }

    @Override
    protected ArrayDesign handleThawLite( ArrayDesign arrayDesign ) {
        return this.getArrayDesignDao().thawLite( arrayDesign );
    }

    @Override
    protected void handleUpdate( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        this.getArrayDesignDao().update( arrayDesign );
    }

    @Override
    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {
        return this.getArrayDesignDao().updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

    private void checkForMoreRecentMethod( Map<Long, AuditEvent> lastEventMap,
            Class<? extends ArrayDesignAnalysisEvent> eventclass, Long arrayDesignId, ArrayDesign subsumedInto ) {
        AuditEvent lastSubsumerEvent = this.getAuditEventDao().getLastEvent( subsumedInto, eventclass );
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
                lastEvent = this.getAuditEventDao().getLastEvent( ad, eventclass );
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