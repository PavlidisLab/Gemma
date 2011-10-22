/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.expression.arrayDesign;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @author klc
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
@Service
public class ArrayDesignServiceImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByManufacturer(java.lang.String)
     */
    @Override
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return this.getArrayDesignDao().findByManufacturer( searchString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return this.getArrayDesignDao().findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getPerTaxonCount()
     */
    public Map<Taxon, Integer> getPerTaxonCount() {
        return this.getArrayDesignDao().getPerTaxonCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#thawLite(java.util.Collection)
     */
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        return this.getArrayDesignDao().thawLite( arrayDesigns );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutBioSequences(ubic
     * .gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign )
            throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutBioSequences( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutBlatResults(ubic
     * .gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign )
            throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutBlatResults( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutGenes(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign )
            throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutGenes( arrayDesign );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getArrayDesignDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCreate(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign)
     */
    @Override
    protected ArrayDesign handleCreate( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().create( arrayDesign );
    }

    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().deleteAlignmentData( arrayDesign );
    }

    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().deleteGeneProductAssociations( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleFind( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().find( arrayDesign );
    }

    @Override
    protected Collection<ArrayDesign> handleFindByAlternateName( String queryString ) throws Exception {
        return this.getArrayDesignDao().findByAlternateName( queryString );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindByName( String name ) throws Exception {
        return this.getArrayDesignDao().findByName( name );
    }

    @Override
    protected ArrayDesign handleFindByShortName( String shortName ) throws Exception {
        return this.getArrayDesignDao().findByShortName( shortName );
    }

    @Override
    protected ArrayDesign handleFindOrCreate( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().findOrCreate( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.handleGetAllAssociatedBioAssays(long)
     */
    @Override
    protected java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id ) {
        return this.getArrayDesignDao().getAllAssociatedBioAssays( id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetCompositeSequenceCount(ubic.gemma.model
     * .expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null ) throw new IllegalArgumentException( "Array design cannot be null" );
        return this.getArrayDesignDao().numCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetExpressionExperimentsById(long)
     */
    @Override
    protected Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign )
            throws Exception {
        return this.getArrayDesignDao().getExpressionExperiments( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastAnnotationFile(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, AuditEvent> handleGetLastAnnotationFile( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );

        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not AnnotationFile events
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignAnnotationFileEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastGeneMapping(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, AuditEvent> handleGetLastGeneMapping( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignGeneMappingEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, AuditEvent> handleGetLastRepeatAnalysis( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignRepeatAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastSequenceAnalysis(java.util.Collection
     * )
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceAnalysis( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignSequenceAnalysisEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastSequenceUpdate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, AuditEvent> handleGetLastSequenceUpdate( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        // remove all AuditEvents that are not Sequence update events
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignSequenceUpdateEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastTroubleEvent(java.util.Collection)
     */
    @Override
    protected java.util.Map<Long, AuditEvent> handleGetLastTroubleEvent( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();

        Set<Long> aaIds = eventMap.keySet();
        for ( Long arrayDesignId : aaIds ) {

            Collection<AuditEvent> events = eventMap.get( arrayDesignId );
            AuditEvent lastEvent = null;

            if ( events == null ) {
                // lastEventMap.put( arrayDesignId, null );
            } else {
                lastEvent = this.getAuditEventDao().getLastOutstandingTroubleEvent( events );
                if ( lastEvent != null ) {
                    lastEventMap.put( arrayDesignId, lastEvent );
                }

                /*
                 * TODO how to deal with merged/subsumed arrays in this case?
                 */
            }

        }

        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastValidationEvent(java.util.Collection)
     */
    @Override
    protected Map<Long, AuditEvent> handleGetLastValidationEvent( Collection<Long> ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();

        Set<Long> aaIds = eventMap.keySet();
        for ( Long arrayDesignId : aaIds ) {

            Collection<AuditEvent> events = eventMap.get( arrayDesignId );
            AuditEvent lastEvent = null;

            if ( events == null ) {
                // lastEventMap.put( arrayDesignId, null );
            } else {
                ArrayDesign ad = this.load( arrayDesignId );

                lastEvent = this.getAuditEventDao().getLastEvent( ad, ValidatedFlagEvent.class );
                if ( lastEvent != null ) lastEventMap.put( arrayDesignId, lastEvent );

                /*
                 * TODO how to deal with merged/subsumed arrays in this case?
                 */
            }

        }

        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.handleGetTaxa(long)
     */
    @Override
    protected Collection<Taxon> handleGetTaxa( java.lang.Long id ) {
        return this.getArrayDesignDao().getTaxa( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.handleGetTaxon(long)
     */
    @Override
    protected Taxon handleGetTaxon( java.lang.Long id ) {
        return this.getArrayDesignDao().getTaxon( id );
    }

    @Override
    protected Map<Long, Boolean> handleIsMerged( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().isMerged( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsMergee( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().isMergee( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumed( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().isSubsumed( ids );
    }

    @Override
    protected Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().isSubsumer( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoad(long)
     */
    @Override
    protected ArrayDesign handleLoad( long id ) throws Exception {
        return this.getArrayDesignDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getAllArrayDesigns()
     */
    @Override
    protected java.util.Collection<ArrayDesign> handleLoadAll() throws java.lang.Exception {
        return ( Collection<ArrayDesign> ) this.getArrayDesignDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection<ArrayDesignValueObject> handleLoadAllValueObjects() throws Exception {
        return this.getArrayDesignDao().loadAllValueObjects();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadCompositeSequences(ubic.gemma.model.
     * expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleLoadCompositeSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().loadCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadFully(java.lang.Long)
     */
    @Override
    protected ArrayDesign handleLoadFully( Long id ) throws Exception {
        return this.getArrayDesignDao().loadFully( id );
    }

    @Override
    protected Collection<ArrayDesign> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection<ArrayDesignValueObject> handleLoadValueObjects( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().loadValueObjects( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBioSequences()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBioSequences(
     * java.util.Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBlatResults();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBlatResults(java
     * .util.Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBlatResults( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithGenes()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithGenes() throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithGenes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithGenes(java.util
     * .Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithGenes( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllGenes()
     */
    @Override
    protected long handleNumAllGenes() throws Exception {
        return this.getArrayDesignDao().numAllGenes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllGenes(java.util.Collection)
     */
    @Override
    protected long handleNumAllGenes( Collection<Long> ids ) throws Exception {
        return this.getArrayDesignDao().numAllGenes( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumBioSequencesById(long)
     */
    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numBioSequences( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumBlatResultsById(long)
     */
    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numBlatResults( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithBioSequences(ubic
     * .gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithBioSequences( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithBlatResults(ubic
     * .gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithBlatResults( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithGenes(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithGenes( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithPredictedGenes(ubic
     * .gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithPredictedGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithPredictedGene( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithProbeAlignedRegion
     * (ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithProbeAlignedRegion( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithProbeAlignedRegion( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumGeneProductsById(long)
     */
    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numGenes( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#removeArrayDesign(java.lang.String)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception {
        this.getArrayDesignDao().remove( arrayDesign );
    }

    @Override
    protected void handleRemoveBiologicalCharacteristics( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().removeBiologicalCharacteristics( arrayDesign );

    }

    @Override
    protected ArrayDesign handleThaw( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().thaw( arrayDesign );
    }

    @Override
    protected ArrayDesign handleThawLite( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().thawLite( arrayDesign );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#updateArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().update( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleUpdateSubsumingStatus(ubic.gemma.model.
     * expression.arrayDesign.ArrayDesign, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee )
            throws Exception {
        return this.getArrayDesignDao().updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

    private void checkForMoreRecentMethod( Map<Long, AuditEvent> lastEventMap,
            Class<ArrayDesignAnalysisEvent> eventclass, Long arrayDesignId, ArrayDesign subsumedInto ) {
        AuditEvent lastSubsumerEvent = this.getAuditEventDao().getLastEvent( subsumedInto, eventclass );
        if ( lastSubsumerEvent != null && lastEventMap.containsKey( arrayDesignId )
                && lastEventMap.get( arrayDesignId ) != null
                && lastEventMap.get( arrayDesignId ).getDate().before( lastSubsumerEvent.getDate() ) ) {
            lastEventMap.put( arrayDesignId, lastSubsumerEvent );
        }
    }

    /**
     * @param eventMap
     * @param lastEventMap
     * @param aaIds
     * @param eventclass
     */
    private void getMostRecentEvents( Map<Long, Collection<AuditEvent>> eventMap, Map<Long, AuditEvent> lastEventMap,
            Set<Long> aaIds, Class<ArrayDesignAnalysisEvent> eventclass ) {
        for ( Long arrayDesignId : aaIds ) {

            Collection<AuditEvent> events = eventMap.get( arrayDesignId );
            AuditEvent lastEvent = null;

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