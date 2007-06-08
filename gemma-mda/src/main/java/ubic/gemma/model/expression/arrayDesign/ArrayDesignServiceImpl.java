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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.genome.Taxon;

/**
 * @author klc
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
public class ArrayDesignServiceImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase {

    @SuppressWarnings("unchecked")
    private void checkForMoreRecentMethod( Map<Long, AuditEvent> lastEventMap,
            Class<ArrayDesignAnalysisEvent> eventclass, Long arrayDesignId, ArrayDesign subsumedInto ) {
        Collection<AuditEvent> subsumerEvents = this.getEvents( subsumedInto );
        AuditEvent lastSubsumerEvent = getLastEvent( subsumerEvents, eventclass );
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
                lastEvent = getLastEvent( events, eventclass );
                lastEventMap.put( arrayDesignId, lastEvent );
            }

            /*
             * Check if the subsuming or merged array (if any) was updated more recently. To do this: 1) load the AA; 2)
             * check for merged; check for subsumed; check events for thos.
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutBioSequences( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutBlatResults( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCompositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().compositeSequenceWithoutGenes( arrayDesign );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getArrayDesignDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected ArrayDesign handleCreate( ArrayDesign arrayDesign ) throws Exception {
        return ( ArrayDesign ) this.getArrayDesignDao().create( arrayDesign );
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

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findArrayDesignByName(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindArrayDesignByName( String name )
            throws Exception {
        return this.getArrayDesignDao().findByName( name );
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadReporters(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
    // */
    // @Override
    // protected Collection handleLoadReporters( ArrayDesign arrayDesign ) throws Exception {
    // return this.getArrayDesignDao().loadReporters( arrayDesign.getId() );
    // }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleFindByGoId(String)
     */
    @Override
    protected Collection handleFindByGoId( String goId ) throws Exception {
        return this.getArrayDesignDao().findByGoId( goId );
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
    protected java.util.Collection handleGetAllAssociatedBioAssays( java.lang.Long id ) {
        return this.getArrayDesignDao().getAllAssociatedBioAssays( id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetCompositeSequenceCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequences( arrayDesign.getId() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetExpressionExperimentsById(long)
     */
    @Override
    protected Collection handleGetExpressionExperiments( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().getExpressionExperiments( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastAnnotationFile(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastAnnotationFile( Collection ids ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastGeneMapping(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastGeneMapping( Collection ids ) throws Exception {
        Map<Long, Collection<AuditEvent>> eventMap = this.getArrayDesignDao().getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<Long, AuditEvent>();
        Set<Long> aaIds = eventMap.keySet();
        Class eventclass = ArrayDesignGeneMappingEvent.class;
        getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastSequenceAnalysis(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastSequenceAnalysis( Collection ids ) throws Exception {
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
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastRepeatAnalysis( Collection ids ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetLastSequenceUpdate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetLastSequenceUpdate( Collection ids ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleGetReporterCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Integer handleGetReporterCount( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numReporters( arrayDesign.getId() );
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
    protected Map handleIsMerged( Collection ids ) throws Exception {
        return this.getArrayDesignDao().isMerged( ids );
    }

    @Override
    protected Map handleIsMergee( Collection ids ) throws Exception {
        return this.getArrayDesignDao().isMergee( ids );
    }

    @Override
    protected Map handleIsSubsumed( Collection ids ) throws Exception {
        return this.getArrayDesignDao().isSubsumed( ids );
    }

    @Override
    protected Map handleIsSubsumer( Collection ids ) throws Exception {
        return this.getArrayDesignDao().isSubsumer( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoad(long)
     */
    @Override
    protected ArrayDesign handleLoad( long id ) throws Exception {
        return ( ArrayDesign ) this.getArrayDesignDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getAllArrayDesigns()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getArrayDesignDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {
        return this.getArrayDesignDao().loadAllValueObjects();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleLoadCompositeSequences( ArrayDesign arrayDesign ) throws Exception {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        return this.getArrayDesignDao().loadValueObjects( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBioSequences()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection ids ) throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBioSequences( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() throws Exception {
        return this.getArrayDesignDao().numAllCompositeSequenceWithBlatResults();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection ids ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumAllCompositeSequenceWithGenes(java.util.Collection)
     */
    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection ids ) throws Exception {
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
    protected long handleNumAllGenes( Collection ids ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithBioSequences( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithBlatResults( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithGenes( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithPredictedGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithPredictedGenes( ArrayDesign arrayDesign ) throws Exception {
        return this.getArrayDesignDao().numCompositeSequenceWithPredictedGene( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleNumCompositeSequenceWithProbeAlignedRegion(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
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
    protected void handleThaw( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().thaw( arrayDesign );
    }

    @Override
    protected void handleThawLite( ArrayDesign arrayDesign ) throws Exception {
        this.getArrayDesignDao().thawLite( arrayDesign );
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceBase#handleUpdateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee )
            throws Exception {
        return this.getArrayDesignDao().updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

}