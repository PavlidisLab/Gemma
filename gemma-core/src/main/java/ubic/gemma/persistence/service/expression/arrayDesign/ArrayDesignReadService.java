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

import org.springframework.lang.Nullable;
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
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Thin read-only retrieval service for {@link ArrayDesign}.
 * <p>
 * Phase 3 of the {@link ArrayDesignService} decomposition (strangler fig). This service
 * houses the pure-retrieval methods: load/find/count/get/thaw/audit-event lookups that
 * delegate directly to {@link ArrayDesignDao} (and {@code AuditEventDao} for the
 * {@code getLastXxx} methods) with no orchestration of write-side collaborators.
 * <p>
 * Callers should generally keep using {@link ArrayDesignService} as the facade — the
 * facade delegates here. Direct injection is appropriate where a class would otherwise
 * create a Spring dependency cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ArrayDesignService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary.
 * Methods on this interface are unsecured at the AOP layer on purpose — intra-core
 * callers that hold an authenticated session can bypass duplicate ACL checks.
 *
 * @see ArrayDesignService
 */
public interface ArrayDesignReadService {

    Collection<ArrayDesign> loadAllGenericGenePlatforms();

    @Nullable
    ArrayDesign loadAndThaw( Long id );

    <T extends Exception> ArrayDesign loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    @Nullable
    ArrayDesign loadWithAuditTrail( Long id );

    Collection<ArrayDesign> findByAlternateName( String queryString );

    @Nullable
    ArrayDesign findOneByAlternateName( String name );

    Collection<ArrayDesign> findByManufacturer( String searchString );

    Collection<ArrayDesign> findByName( String name );

    Collection<ArrayDesign> findByCompositeSequenceName( String name );

    @Nullable
    ArrayDesign findOneByName( String name );

    @Nullable
    ArrayDesign findByShortName( String shortName );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    Map<CompositeSequence, Collection<BlatResult>> getAlignments( ArrayDesign arrayDesign );

    Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    Collection<Gene> getGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( ArrayDesign arrayDesign, boolean useGene2Cs );

    Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs );

    long countCompositeSequences( ArrayDesign arrayDesign );

    Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign );

    Collection<CompositeSequence> getCompositeSequences( ArrayDesign arrayDesign, int limit, int offset );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    long countExpressionExperiments( ArrayDesign arrayDesign );

    Map<Long, AuditEvent> getLastGeneMapping( Collection<Long> ids );

    Map<Long, AuditEvent> getLastRepeatAnalysis( Collection<Long> ids );

    Map<Long, AuditEvent> getLastSequenceAnalysis( Collection<Long> ids );

    Map<Long, AuditEvent> getLastSequenceUpdate( Collection<Long> ids );

    Map<Taxon, Long> getPerTaxonCount();

    Collection<ExpressionExperiment> getSwitchedExperiments( ArrayDesign arrayDesign );

    long countSwitchedExpressionExperiments( ArrayDesign id );

    Collection<Taxon> getTaxaFromBioSequences( ArrayDesign arrayDesign );

    Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Cursor-mode counterpart to {@link #loadBlacklistedValueObjects(Filters, Sort, int, int)}.
     * @see ArrayDesignDao#loadBlacklistedValueObjectsByCursor(Filters, Sort, Cursor, int)
     */
    CursorPage<ArrayDesignValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit );

    Collection<ArrayDesignValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    long countWithCache( @Nullable Filters filters );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId );

    long countCompositeSequencesWithBioSequences();

    long countCompositeSequencesWithBlatResults();

    long countCompositeSequencesWithGenes( boolean useGene2Cs );

    long countGenes( boolean useGene2Cs );

    long countBioSequences( ArrayDesign arrayDesign );

    long countBlatResults( ArrayDesign arrayDesign );

    long countCompositeSequencesWithBioSequences( ArrayDesign arrayDesign );

    long countCompositeSequencesWithBlatResults( ArrayDesign arrayDesign );

    long countCompositeSequencesWithGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    long countGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    @CheckReturnValue
    ArrayDesign thaw( ArrayDesign arrayDesign );

    @CheckReturnValue
    Collection<ArrayDesign> thaw( Collection<ArrayDesign> aas );

    @CheckReturnValue
    ArrayDesign thawCompositeSequences( ArrayDesign arrayDesign );

    @CheckReturnValue
    Collection<ArrayDesign> thawCompositeSequences( Collection<ArrayDesign> ads );

    @CheckReturnValue
    ArrayDesign thawLite( ArrayDesign arrayDesign );

    @CheckReturnValue
    Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );
}
