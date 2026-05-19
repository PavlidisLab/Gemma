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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
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
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author klc
 * @see ArrayDesignService
 */
@Service("arrayDesignService")
public class ArrayDesignServiceImpl extends AbstractFilteringVoEnabledService<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignService {

    private final ArrayDesignDao arrayDesignDao;

    @Autowired
    private ArrayDesignReadService readService;

    @Autowired
    public ArrayDesignServiceImpl( ArrayDesignDao arrayDesignDao ) {
        super( arrayDesignDao );
        this.arrayDesignDao = arrayDesignDao;
    }

    // =====================================================================
    // Read methods -- delegate to ArrayDesignReadService
    // ACL @Secured / @PostFilter / @PostAuthorize annotations live on the
    // ArrayDesignService interface and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public Collection<ArrayDesign> loadAllGenericGenePlatforms() {
        return readService.loadAllGenericGenePlatforms();
    }

    @Override
    public ArrayDesign loadAndThaw( Long id ) {
        return readService.loadAndThaw( id );
    }

    @Override
    public <T extends Exception> ArrayDesign loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        return readService.loadAndThawLiteOrFail( id, exceptionSupplier, message );
    }

    @Override
    public ArrayDesign loadWithAuditTrail( Long id ) {
        return readService.loadWithAuditTrail( id );
    }

    @Override
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        return readService.findByAlternateName( queryString );
    }

    @Override
    public ArrayDesign findOneByAlternateName( String name ) {
        return readService.findOneByAlternateName( name );
    }

    @Override
    public Collection<ArrayDesign> findByManufacturer( String searchString ) {
        return readService.findByManufacturer( searchString );
    }

    @Override
    public Collection<ArrayDesign> findByName( String name ) {
        return readService.findByName( name );
    }

    @Override
    public Collection<ArrayDesign> findByCompositeSequenceName( String name ) {
        return readService.findByCompositeSequenceName( name );
    }

    @Override
    public ArrayDesign findOneByName( String name ) {
        return readService.findOneByName( name );
    }

    @Override
    public ArrayDesign findByShortName( String shortName ) {
        return readService.findByShortName( shortName );
    }

    @Override
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return readService.findByTaxon( taxon );
    }

    @Override
    public Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign ) {
        return readService.getAlignments( arrayDesign );
    }

    @Override
    public Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign ) {
        return readService.getAllAssociatedBioAssays( arrayDesign );
    }

    @Override
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {
        return readService.getBioSequences( arrayDesign );
    }

    @Override
    public Collection<Gene> getGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return readService.getGenes( arrayDesign, useGene2Cs );
    }

    @Override
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return readService.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    public Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs ) {
        return readService.getGenesByCompositeSequence( arrayDesign, useGene2Cs );
    }

    @Override
    public long countCompositeSequences( ArrayDesign arrayDesign ) {
        return readService.countCompositeSequences( arrayDesign );
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign ) {
        return readService.getCompositeSequences( arrayDesign );
    }

    @Override
    public Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign, int limit, int offset ) {
        return readService.getCompositeSequences( arrayDesign, limit, offset );
    }

    @Override
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        return readService.getExpressionExperiments( arrayDesign );
    }

    @Override
    public long countExpressionExperiments( ArrayDesign arrayDesign ) {
        return readService.countExpressionExperiments( arrayDesign );
    }

    @Override
    public Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids ) {
        return readService.getLastGeneMapping( ids );
    }

    @Override
    public Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids ) {
        return readService.getLastRepeatAnalysis( ids );
    }

    @Override
    public Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids ) {
        return readService.getLastSequenceAnalysis( ids );
    }

    @Override
    public Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids ) {
        return readService.getLastSequenceUpdate( ids );
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        return readService.getPerTaxonCount();
    }

    @Override
    public Collection<ExpressionExperiment> getSwitchedExperiments( ArrayDesign arrayDesign ) {
        return readService.getSwitchedExperiments( arrayDesign );
    }

    @Override
    public long countSwitchedExpressionExperiments( ArrayDesign id ) {
        return readService.countSwitchedExpressionExperiments( id );
    }

    @Override
    public Collection<Taxon> getTaxaFromBioSequences( ArrayDesign arrayDesign ) {
        return readService.getTaxaFromBioSequences( arrayDesign );
    }

    @Override
    public Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return readService.loadBlacklistedValueObjects( filters, sort, offset, limit );
    }

    @Override
    public CursorPage<ArrayDesignValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        return readService.loadBlacklistedValueObjectsByCursor( filters, sort, cursor, limit );
    }

    @Override
    public Collection<ArrayDesignValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return readService.loadValueObjectsWithCache( filters, sort );
    }

    @Override
    public long countWithCache( @Nullable Filters filters ) {
        return readService.countWithCache( filters );
    }

    @Override
    public Map<Long, Boolean> isMerged( Collection<Long> ids ) {
        return readService.isMerged( ids );
    }

    @Override
    public Map<Long, Boolean> isMergee( Collection<Long> ids ) {
        return readService.isMergee( ids );
    }

    @Override
    public Map<Long, Boolean> isSubsumed( Collection<Long> ids ) {
        return readService.isSubsumed( ids );
    }

    @Override
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        return readService.isSubsumer( ids );
    }

    @Override
    public List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId ) {
        return readService.loadValueObjectsForEE( eeId );
    }

    @Override
    public long countCompositeSequencesWithBioSequences() {
        return readService.countCompositeSequencesWithBioSequences();
    }

    @Override
    public long countCompositeSequencesWithBlatResults() {
        return readService.countCompositeSequencesWithBlatResults();
    }

    @Override
    public long countCompositeSequencesWithGenes( boolean useGene2Cs ) {
        return readService.countCompositeSequencesWithGenes( useGene2Cs );
    }

    @Override
    public long countGenes( boolean useGene2Cs ) {
        return readService.countGenes( useGene2Cs );
    }

    @Override
    public long countBioSequences( ArrayDesign arrayDesign ) {
        return readService.countBioSequences( arrayDesign );
    }

    @Override
    public long countBlatResults( ArrayDesign arrayDesign ) {
        return readService.countBlatResults( arrayDesign );
    }

    @Override
    public long countCompositeSequencesWithBioSequences( ArrayDesign arrayDesign ) {
        return readService.countCompositeSequencesWithBioSequences( arrayDesign );
    }

    @Override
    public long countCompositeSequencesWithBlatResults( ArrayDesign arrayDesign ) {
        return readService.countCompositeSequencesWithBlatResults( arrayDesign );
    }

    @Override
    public long countCompositeSequencesWithGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return readService.countCompositeSequencesWithGenes( arrayDesign, useGene2Cs );
    }

    @Override
    public long countGenes( ArrayDesign arrayDesign, boolean useGene2Cs ) {
        return readService.countGenes( arrayDesign, useGene2Cs );
    }

    @Override
    public ArrayDesign thaw( ArrayDesign arrayDesign ) {
        return readService.thaw( arrayDesign );
    }

    @Override
    public Collection<ArrayDesign> thaw( Collection<ArrayDesign> aas ) {
        return readService.thaw( aas );
    }

    @Override
    public ArrayDesign thawCompositeSequences( ArrayDesign arrayDesign ) {
        return readService.thawCompositeSequences( arrayDesign );
    }

    @Override
    public Collection<ArrayDesign> thawCompositeSequences( Collection<ArrayDesign> ads ) {
        return readService.thawCompositeSequences( ads );
    }

    @Override
    public ArrayDesign thawLite( ArrayDesign arrayDesign ) {
        return readService.thawLite( arrayDesign );
    }

    @Override
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        return readService.thawLite( arrayDesigns );
    }

    // =====================================================================
    // Write methods -- still on the facade for now (Phase 3 follow-up slice
    // will extract these into ArrayDesignWriteService).
    // =====================================================================

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
    @Transactional
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        this.arrayDesignDao.removeBiologicalCharacteristics( arrayDesign );
    }

    @Override
    @Transactional
    public boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {
        return this.arrayDesignDao.updateSubsumingStatus( candidateSubsumer, candidateSubsumee );
    }
}
