/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.arrayDesign;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ArrayDesignReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link ArrayDesignService} interface — this class is
 * unsecured at the AOP boundary on purpose, so that intra-{@code gemma-core} callers
 * that hold an authenticated session can bypass duplicate ACL checks.
 *
 * @see ArrayDesignService
 */
@Service("arrayDesignReadService")
public class ArrayDesignReadServiceImpl implements ArrayDesignReadService {

    private final ArrayDesignDao arrayDesignDao;
    private final AuditEventDao auditEventDao;

    @Autowired
    public ArrayDesignReadServiceImpl( ArrayDesignDao arrayDesignDao, AuditEventDao auditEventDao ) {
        this.arrayDesignDao = arrayDesignDao;
        this.auditEventDao = auditEventDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> loadAllGenericGenePlatforms() {
        return arrayDesignDao.loadAllGenericGenePlatforms();
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign loadAndThaw( Long id ) {
        ArrayDesign ad = arrayDesignDao.load( id );
        if ( ad != null ) {
            arrayDesignDao.thaw( ad );
        }
        return ad;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ArrayDesign loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ArrayDesign ad = loadOrFail( id, exceptionSupplier, message );
        arrayDesignDao.thawLite( ad );
        return ad;
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign loadWithAuditTrail( Long id ) {
        ArrayDesign ad = arrayDesignDao.load( id );
        if ( ad != null ) {
            Hibernate.initialize( ad.getAuditTrail() );
        }
        return ad;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        return arrayDesignDao.findByAlternateName( queryString );
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign findOneByAlternateName( String name ) {
        return arrayDesignDao.findOneByAlternateName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return arrayDesignDao.findByManufacturer( searchString );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByName( String name ) {
        return arrayDesignDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByCompositeSequenceName( String name ) {
        return arrayDesignDao.findByCompositeSequenceName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign findOneByName( String name ) {
        return arrayDesignDao.findOneByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign findByShortName( String shortName ) {
        return arrayDesignDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return arrayDesignDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign ) {
        return arrayDesignDao.loadAlignments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign ) {
        return arrayDesignDao.getAllAssociatedBioAssays( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.getBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return arrayDesignDao.getGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return arrayDesignDao.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs ) {
        return arrayDesignDao.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countCompositeSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.loadCompositeSequences( arrayDesign, -1, 0 );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign, int limit, int offset ) {
        return arrayDesignDao.loadCompositeSequences( arrayDesign, limit, offset );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        return arrayDesignDao.getExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpressionExperiments( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        getMostRecentEvents( eventMap, lastEventMap, aaIds, ArrayDesignGeneMappingEvent.class );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        getMostRecentEvents( eventMap, lastEventMap, aaIds, ArrayDesignRepeatAnalysisEvent.class );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        getMostRecentEvents( eventMap, lastEventMap, aaIds, ArrayDesignSequenceAnalysisEvent.class );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        getMostRecentEvents( eventMap, lastEventMap, aaIds, ArrayDesignSequenceUpdateEvent.class );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return arrayDesignDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getSwitchedExperiments( ArrayDesign arrayDesign ) {
        return arrayDesignDao.getSwitchedExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countSwitchedExpressionExperiments( ArrayDesign id ) {
        return arrayDesignDao.countSwitchedExpressionExperiments( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> getTaxaFromBioSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.getTaxaFromBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return arrayDesignDao.loadBlacklistedValueObjects( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ArrayDesignValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        return arrayDesignDao.loadBlacklistedValueObjectsByCursor( filters, sort, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesignValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return arrayDesignDao.loadValueObjectsWithCache( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithCache( @Nullable Filters filters ) {
        return arrayDesignDao.countWithCache( filters );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isMerged( Collection<Long> ids ) {
        return arrayDesignDao.isMerged( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isMergee( Collection<Long> ids ) {
        return arrayDesignDao.isMergee( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumed( Collection<Long> ids ) {
        return arrayDesignDao.isSubsumed( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        return arrayDesignDao.isSubsumer( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId ) {
        return arrayDesignDao.loadValueObjectsForEE( eeId );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBioSequences() {
        return arrayDesignDao.countCompositeSequencesWithBioSequences();
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBlatResults() {
        return arrayDesignDao.countCompositeSequencesWithBlatResults();
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithGenes( boolean useGene2Cs ) {
        return arrayDesignDao.countCompositeSequencesWithGenes( useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countGenes( boolean useGene2Cs ) {
        return arrayDesignDao.countGenes( useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countBioSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countBlatResults( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countBlatResults( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBioSequences( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countCompositeSequencesWithBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBlatResults( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countCompositeSequencesWithBlatResults( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return arrayDesignDao.countCompositeSequencesWithGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return arrayDesignDao.countGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign thaw( ArrayDesign arrayDesign ) {
        arrayDesign = ensureInSession( arrayDesign );
        arrayDesignDao.thaw( arrayDesign );
        return arrayDesign;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> thaw( Collection<ArrayDesign> aas ) {
        aas = ensureInSession( aas );
        aas.forEach( arrayDesignDao::thaw );
        return aas;
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign thawCompositeSequences( ArrayDesign arrayDesign ) {
        arrayDesign = ensureInSession( arrayDesign );
        arrayDesignDao.thawCompositeSequences( arrayDesign );
        return arrayDesign;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> thawCompositeSequences( Collection<ArrayDesign> ads ) {
        ads = ensureInSession( ads );
        ads.forEach( arrayDesignDao::thawCompositeSequences );
        return ads;
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign thawLite( ArrayDesign arrayDesign ) {
        arrayDesign = ensureInSession( arrayDesign );
        arrayDesignDao.thawLite( arrayDesign );
        return arrayDesign;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        arrayDesigns = ensureInSession( arrayDesigns );
        arrayDesigns.forEach( arrayDesignDao::thawLite );
        return arrayDesigns;
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    /**
     * Re-implementation of {@code AbstractService.loadOrFail} so this service does not need
     * to extend the base service hierarchy.
     */
    private <T extends Exception> ArrayDesign loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ArrayDesign ad = arrayDesignDao.load( id );
        if ( ad == null ) {
            throw exceptionSupplier.apply( message );
        }
        return ad;
    }

    /**
     * Re-implementation of {@code AbstractService.ensureInSession} so this service does not
     * need to extend the base service hierarchy.
     */
    private ArrayDesign ensureInSession( ArrayDesign entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( arrayDesignDao.load( id ),
                String.format( "No %s with ID %d.", ArrayDesign.class.getName(), id ) );
    }

    private Collection<ArrayDesign> ensureInSession( Collection<ArrayDesign> entities ) {
        if ( entities == null || entities.isEmpty() ) {
            return entities;
        }
        // Simple per-entity reload mirroring the single-entity path; AbstractService has a
        // bulk-load fast path but this service intentionally avoids extending it.
        java.util.List<ArrayDesign> reloaded = new java.util.ArrayList<>( entities.size() );
        for ( ArrayDesign e : entities ) {
            reloaded.add( ensureInSession( e ) );
        }
        return reloaded;
    }

    private void checkForMoreRecentMethod( Map<Long, AuditEvent> lastEventMap,
            Class<? extends ArrayDesignAnalysisEvent> eventclass, Long arrayDesignId, ArrayDesign subsumedInto ) {
        AuditEvent lastSubsumerEvent = auditEventDao.getLastEvent( subsumedInto, eventclass );
        if ( lastSubsumerEvent != null && lastEventMap.containsKey( arrayDesignId )
                && lastEventMap.get( arrayDesignId ) != null && lastEventMap.get( arrayDesignId ).getDate()
                .before( lastSubsumerEvent.getDate() ) ) {
            lastEventMap.put( arrayDesignId, lastSubsumerEvent );
        }
    }

    private void getMostRecentEvents( Map<Long, Collection<AuditEvent>> eventMap, Map<Long, AuditEvent> lastEventMap,
            Set<Long> aaIds, Class<? extends ArrayDesignAnalysisEvent> eventclass ) {
        // Hoist the per-id load out of the loop: the original body did two
        // arrayDesignDao.load(id) calls per iteration (one for the audit-event
        // probe, one for the subsuming/mergedInto probe) — both returned the
        // same ArrayDesign. A single batched fetch turns 2*N round-trips into 1.
        Map<Long, ArrayDesign> arrayDesignsById = arrayDesignDao.loadAsMap( aaIds );
        for ( Long arrayDesignId : aaIds ) {

            Collection<AuditEvent> events = eventMap.get( arrayDesignId );
            ArrayDesign arrayDesign = arrayDesignsById.get( arrayDesignId );

            if ( events == null ) {
                lastEventMap.put( arrayDesignId, null );
            } else {
                if ( arrayDesign == null ) {
                    throw new NullPointerException( String.format( "No %s with ID %d.", ArrayDesign.class.getName(), arrayDesignId ) );
                }
                AuditEvent lastEvent = auditEventDao.getLastEvent( arrayDesign, eventclass );
                lastEventMap.put( arrayDesignId, lastEvent );
            }

            /*
             * Check if the subsuming or merged array (if any) was updated more recently. To do this: 1) load the AA; 2)
             * check for merged; check for subsumed; check events for those.
             */
            if ( arrayDesign != null && arrayDesign.getSubsumingArrayDesign() != null ) {
                ArrayDesign subsumedInto = arrayDesign.getSubsumingArrayDesign();
                checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, subsumedInto );
            }
            if ( arrayDesign != null && arrayDesign.getMergedInto() != null ) {
                ArrayDesign mergedInto = arrayDesign.getMergedInto();
                checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, mergedInto );
            }
        }
    }
}
