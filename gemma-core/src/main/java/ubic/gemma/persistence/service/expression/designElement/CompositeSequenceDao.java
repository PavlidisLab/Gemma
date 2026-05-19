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
package ubic.gemma.persistence.service.expression.designElement;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @see CompositeSequence
 */
public interface CompositeSequenceDao extends FilteringVoEnabledDao<CompositeSequence, CompositeSequenceValueObject> {

    String OBJECT_ALIAS = "probe";

    Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    Collection<CompositeSequence> findByBioSequenceName( String name );

    /**
     * Find composite sequences mapped to a given gene.
     *
     * @param useGene2Cs whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                   accurate lookup
     */
    Collection<CompositeSequence> findByGene( Gene gene, boolean useGene2Cs );

    /**
     * Find a slice of composite sequences mapped to a given gene.
     *
     * @param useGene2Cs whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                   accurate lookup
     */
    Slice<CompositeSequence> findByGene( Gene gene, int start, int limit, boolean useGene2Cs );

    /**
     * Find composite sequences mapped to a given gene, restricted to a given platform.
     *
     * @param useGene2Cs whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                   accurate lookup
     */
    Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign, boolean useGene2Cs );

    /**
     * Find composite sequences mapped to the given genes.
     *
     * @param useGene2Cs whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                   accurate lookup
     */
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, boolean useGene2Cs );

    /**
     * Find composite sequences mapped to the given genes, restricted to a given platform.
     *
     * @param useGene2Cs whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                   accurate lookup
     */
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, ArrayDesign arrayDesign, boolean useGene2Cs );

    Collection<CompositeSequence> findByName( String name );

    CompositeSequence findByName( ArrayDesign arrayDesign, String name );

    /**
     * Given a collection of composite sequences returns a map of the given composite sequences to a collection of genes
     *
     * @param compositeSequences composite sequences
     * @param useGene2Cs         whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                           accurate lookup
     * @return map
     */
    Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> compositeSequences, boolean useGene2Cs );

    /**
     * given a composite sequence returns a collection of genes
     *
     * @param compositeSequence sequence
     * @param offset            offset
     * @param limit             limit
     * @param useGene2Cs        whether to use the {@code GENE2CS} mapping table for faster, but potentially less
     *                          accurate lookup
     * @return collection of genes
     */
    Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit, boolean useGene2Cs );

    /**
     * Cursor-paged listing of {@link Gene}s associated with a single
     * {@link CompositeSequence} — see {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1l.
     * Always sorted by ascending {@code gene.id} (the primary key, indexed and unique);
     * the cursor DAO currently restricts cursors to single-component id sorts until the
     * index audit lands.
     * <p>
     * Mirrors the structure of {@link #getGenes(CompositeSequence, int, int, boolean)}
     * but appends an {@code id > :cursorId} (ASC forward) / {@code id < :cursorId} (ASC
     * backward) predicate to the existing probe→gene join query and fetches
     * {@code limit + 1} rows to detect the next page; {@code totalElements} is left
     * {@code null} (cursor mode skips the {@code COUNT(*)} per request, matching the
     * rest of the cursor surface).
     *
     * @param useGene2Cs whether to use the denormalized {@code GENE2CS} mapping table for
     *                   a faster (but potentially less accurate) lookup, matching the
     *                   semantics of the offset variant.
     */
    CursorPage<Gene> getGenesByCursor( CompositeSequence compositeSequence, @Nullable Cursor cursor, int limit, boolean useGene2Cs );

    /**
     * @param compositeSequences sequences
     * @return a map of CompositeSequences to BlatAssociations.
     */
    Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults );

    void thaw( Collection<CompositeSequence> compositeSequences );

    void thaw( CompositeSequence compositeSequence );
}
