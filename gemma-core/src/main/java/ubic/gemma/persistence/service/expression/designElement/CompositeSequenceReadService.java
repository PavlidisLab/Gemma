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
package ubic.gemma.persistence.service.expression.designElement;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Read-only retrieval service for {@link CompositeSequence}.
 * <p>
 * Phase 3 of the {@link CompositeSequenceService} decomposition (strangler fig). This
 * service houses the pure DAO-delegate read cluster previously implemented directly on the
 * {@code CompositeSequenceServiceImpl} facade: the {@code findByBioSequence*} /
 * {@code findByGene*} / {@code findByGenes*} / {@code findByName*} lookups,
 * {@code findByNamesInArrayDesigns}, the {@code getGenes*} variants,
 * {@code getGenesWithSpecificity}, the {@code getRawSummary} variants, and {@code thaw}.
 * All methods delegate to {@link CompositeSequenceDao} with no orchestration of other
 * collaborator services.
 * <p>
 * Methods that compose multiple read collaborators (gene-mapping summaries,
 * VO-enrichment with platform VOs) remain on the facade for now; they may be split into
 * their own slice later.
 * <p>
 * Callers should generally keep using {@link CompositeSequenceService} as the facade —
 * the facade delegates to this service. Direct injection is appropriate where a class
 * would otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link CompositeSequenceService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured.
 *
 * @see CompositeSequenceService
 */
public interface CompositeSequenceReadService {

    /**
     * @see CompositeSequenceDao#findByBioSequence(BioSequence)
     */
    Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    /**
     * @see CompositeSequenceDao#findByBioSequenceName(String)
     */
    Collection<CompositeSequence> findByBioSequenceName( String name );

    /**
     * @see CompositeSequenceDao#findByGene(Gene, boolean)
     */
    Collection<CompositeSequence> findByGene( Gene gene, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#findByGene(Gene, ArrayDesign, boolean)
     */
    Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#findByGenes(Collection, boolean)
     */
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#findByGenes(Collection, ArrayDesign, boolean)
     */
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, ArrayDesign arrayDesign, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#findByName(String)
     */
    Collection<CompositeSequence> findByName( String name );

    /**
     * @see CompositeSequenceDao#findByName(ArrayDesign, String)
     */
    CompositeSequence findByName( ArrayDesign arrayDesign, String name );

    /**
     * Resolve composite sequence names against a collection of array designs, preserving
     * insertion order and discarding names that don't match any sequence on any of the
     * supplied platforms.
     */
    Collection<CompositeSequence> findByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns );

    /**
     * @see CompositeSequenceDao#getGenes(Collection, boolean)
     */
    Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> sequences, boolean useGene2Cs );

    /**
     * Return all genes mapped to the given composite sequence.
     */
    Collection<Gene> getGenes( CompositeSequence compositeSequence, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#getGenes(CompositeSequence, int, int, boolean)
     */
    Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#getGenesByCursor(CompositeSequence, Cursor, int, boolean)
     */
    CursorPage<Gene> getGenesByCursor( CompositeSequence compositeSequence, @Nullable Cursor cursor, int limit, boolean useGene2Cs );

    /**
     * @see CompositeSequenceDao#getGenesWithSpecificity(Collection)
     */
    Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    /**
     * @see CompositeSequenceDao#getRawSummary(Collection)
     */
    Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences );

    /**
     * @see CompositeSequenceDao#getRawSummary(ArrayDesign, int)
     */
    Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults );

    /**
     * Thaw a collection of {@link CompositeSequence} for full traversal.
     */
    @CheckReturnValue
    Collection<CompositeSequence> thaw( Collection<CompositeSequence> compositeSequences );

    /**
     * Thaw a single {@link CompositeSequence} for full traversal.
     */
    @CheckReturnValue
    CompositeSequence thaw( CompositeSequence compositeSequence );
}
