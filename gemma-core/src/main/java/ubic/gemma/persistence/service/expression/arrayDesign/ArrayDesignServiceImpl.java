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

import org.hibernate.Hibernate;
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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * @author klc
 * @see ArrayDesignService
 */
@Service("arrayDesignService")
public class ArrayDesignServiceImpl extends AbstractFilteringVoEnabledService<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignService {

    private final ArrayDesignDao arrayDesignDao;
    private final AuditEventDao auditEventDao;

    @Autowired
    public ArrayDesignServiceImpl( ArrayDesignDao arrayDesignDao, AuditEventDao auditEventDao ) {
        super( arrayDesignDao );
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
        ArrayDesign ad = load( id );
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
        ArrayDesign ad = load( id );
        if ( ad != null ) {
            Hibernate.initialize( ad.getAuditTrail() );
        }
        return ad;
    }

    @Override
    @Transactional
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes ) {
        this.arrayDesignDao.addProbes( arrayDesign, newProbes );
    }

    @Override
    @Transactional
    public void deleteAlignmentData( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteAlignmentData( arrayDesign );
    }

    @Override
    @Transactional
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteGeneProductAssociations( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        return this.arrayDesignDao.findByAlternateName( queryString );
    }

    @Override
    @Transactional(readOnly = true)
    public ArrayDesign findOneByAlternateName( String name ) {
        return arrayDesignDao.findOneByAlternateName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return this.arrayDesignDao.findByManufacturer( searchString );
    }

    /**
     * @see ArrayDesignService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> findByName( String name ) {
        return this.arrayDesignDao.findByName( name );
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
        return this.arrayDesignDao.findByShortName( shortName );
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
    public Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getAllAssociatedBioAssays( arrayDesign );

    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.arrayDesignDao.getGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.arrayDesignDao.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs ) {
        return this.arrayDesignDao.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.countCompositeSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.loadCompositeSequences( arrayDesign, -1, 0 );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign, int limit, int offset ) {
        return this.arrayDesignDao.loadCompositeSequences( arrayDesign, limit, offset );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countExpressionExperiments( ArrayDesign arrayDesign ) {
        return arrayDesignDao.countExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignGeneMappingEvent.class;
        this.getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignRepeatAnalysisEvent.class;
        this.getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not SequenceAnalysis events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceAnalysisEvent.class;
        this.getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids ) {
        Map<Long, Collection<AuditEvent>> eventMap = this.arrayDesignDao.getAuditEvents( ids );
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        // remove all AuditEvents that are not Sequence update events
        Set<Long> aaIds = eventMap.keySet();
        Class<? extends ArrayDesignAnalysisEvent> eventclass = ArrayDesignSequenceUpdateEvent.class;
        this.getMostRecentEvents( eventMap, lastEventMap, aaIds, eventclass );
        return lastEventMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.arrayDesignDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getSwitchedExperiments( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getSwitchedExpressionExperiments( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countSwitchedExpressionExperiments( ArrayDesign id ) {
        return this.arrayDesignDao.countSwitchedExpressionExperiments( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> getTaxaFromBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.getTaxaFromBioSequences( arrayDesign );
    }

    @Override
    @Transactional
    public void deleteGeneProductAnnotationAssociations( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteGeneProductAnnotationAssociations( arrayDesign );
    }

    @Override
    @Transactional
    public void deleteGeneProductAlignmentAssociations( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.deleteGeneProductAlignmentAssociations( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return arrayDesignDao.loadBlacklistedValueObjects( filters, sort, offset, limit );
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
        return this.arrayDesignDao.isMerged( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isMergee( Collection<Long> ids ) {
        return this.arrayDesignDao.isMergee( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumed( Collection<Long> ids ) {
        return this.arrayDesignDao.isSubsumed( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        return this.arrayDesignDao.isSubsumer( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId ) {
        return this.arrayDesignDao.loadValueObjectsForEE( eeId );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBioSequences() {
        return this.arrayDesignDao.countCompositeSequencesWithBioSequences();
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBlatResults() {
        return this.arrayDesignDao.countCompositeSequencesWithBlatResults();
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithGenes( boolean useGene2Cs ) {
        return this.arrayDesignDao.countCompositeSequencesWithGenes( useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countGenes( boolean useGene2Cs ) {
        return this.arrayDesignDao.countGenes( useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.countBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countBlatResults( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.countBlatResults( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBioSequences( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.countCompositeSequencesWithBioSequences( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithBlatResults( ArrayDesign arrayDesign ) {
        return this.arrayDesignDao.countCompositeSequencesWithBlatResults( arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompositeSequencesWithGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.arrayDesignDao.countCompositeSequencesWithGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional(readOnly = true)
    public long countGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return this.arrayDesignDao.countGenes( arrayDesign, useGene2Cs );
    }

    @Override
    @Transactional
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.removeBiologicalCharacteristics( arrayDesign );
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

    @Override
    @Transactional
    public boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {
        return this.arrayDesignDao.updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }

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
                ArrayDesign ad = this.loadOrFail( arrayDesignId );
                lastEvent = this.auditEventDao.getLastEvent( ad, eventclass );
                lastEventMap.put( arrayDesignId, lastEvent );
            }

            /*
             * Check if the subsuming or merged array (if any) was updated more recently. To do this: 1) load the AA; 2)
             * check for merged; check for subsumed; check events for those.
             */
            ArrayDesign arrayDesign = this.load( arrayDesignId );
            if ( arrayDesign != null && arrayDesign.getSubsumingArrayDesign() != null ) {
                ArrayDesign subsumedInto = arrayDesign.getSubsumingArrayDesign();
                this.checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, subsumedInto );
            }
            if ( arrayDesign != null && arrayDesign.getMergedInto() != null ) {
                ArrayDesign mergedInto = arrayDesign.getMergedInto();
                this.checkForMoreRecentMethod( lastEventMap, eventclass, arrayDesignId, mergedInto );
            }

        }
    }
}