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

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface CompositeSequenceService
        extends BaseService<CompositeSequence>, FilteringVoEnabledService<CompositeSequence, CompositeSequenceValueObject> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    CompositeSequence find( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    CompositeSequence findOrCreate( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    CompositeSequence create( CompositeSequence compositeSequence );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> load( Collection<Long> ids );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( CompositeSequence compositeSequence );

    @Override
    @Secured({ "GROUP_USER" })
    void update( CompositeSequence compositeSequence );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByBioSequenceName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByGene( Gene gene, boolean useGene2Cs );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign, boolean useGene2Cs );

    // FIXME: add ACL support for mapping of collections
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, boolean useGene3Cs );

    // FIXME: add ACL support for mapping of collections
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Gene, Collection<CompositeSequence>> findByGenes( Collection<Gene> genes, ArrayDesign arrayDesign, boolean useGene2Cs );

    /**
     * Include gene mapping summary in the {@link CompositeSequenceValueObject}.
     */
    @Nullable
    @Transactional(readOnly = true)
    CompositeSequenceValueObject loadValueObjectWithGeneMappingSummary( CompositeSequence cs );

    Slice<CompositeSequenceValueObject> loadValueObjectsForGene( Gene gene, int start, int limit, boolean useGene2Cs );

    /**
     * Cursor-mode counterpart to {@link #loadValueObjectsForGene(Gene, int, int, boolean)}
     * &mdash; see {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1m. Always sorts by ascending
     * {@code cs.id} (the primary key, indexed and unique); the cursor DAO restricts cursors
     * to single-component id sorts until the index audit lands.
     * <p>
     * Same value-object shape as the offset variant: the per-row {@link CompositeSequenceValueObject}
     * carries the {@link ArrayDesign} VO populated via a single {@code loadValueObjects}
     * call over the platforms touched by the page (matching the offset implementation).
     *
     * @see CompositeSequenceDao#findByGeneByCursor(Gene, Cursor, int, boolean)
     */
    CursorPage<CompositeSequenceValueObject> loadValueObjectsForGeneByCursor( Gene gene, @Nullable Cursor cursor, int limit, boolean useGene2Cs );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_READ" })
    CompositeSequence findByName( ArrayDesign arrayDesign, String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_COLLECTION_READ" })
    Collection<CompositeSequence> findByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns );

    Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> sequences, boolean useGene2Cs );

    Collection<Gene> getGenes( CompositeSequence compositeSequence, boolean useGene2Cs );

    Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit, boolean useGene2Cs );

    /**
     * Cursor-mode counterpart to {@link #getGenes(CompositeSequence, int, int, boolean)}
     * — see {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1l. Always sorts by ascending
     * {@code gene.id} (the primary key, indexed and unique); the cursor DAO restricts
     * cursors to single-component id sorts until the index audit lands.
     *
     * @see CompositeSequenceDao#getGenesByCursor(CompositeSequence, Cursor, int, boolean)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COMPOSITE_SEQUENCE_READ" })
    CursorPage<Gene> getGenesByCursor( CompositeSequence compositeSequence, @Nullable Cursor cursor, int limit, boolean useGene2Cs );

    /**
     * @param compositeSequences sequences
     * @return a map of CompositeSequences to collection of BioSequence2GeneProducts at each location.
     */
    Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences );

    Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults );

    Collection<GeneMappingSummary> getGeneMappingSummary( BioSequence biologicalCharacteristic,
            @Nullable CompositeSequenceValueObject cs );

    @CheckReturnValue
    Collection<CompositeSequence> thaw( Collection<CompositeSequence> compositeSequences );

    @CheckReturnValue
    CompositeSequence thaw( CompositeSequence compositeSequence );
}
