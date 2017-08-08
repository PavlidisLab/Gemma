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

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Service base class for <code>CompositeSequenceService</code>, provides access to all services and entities
 * referenced by this service.
 *
 * @see CompositeSequenceService
 */
public abstract class CompositeSequenceServiceBase extends VoEnabledService<CompositeSequence, CompositeSequenceValueObject>
        implements CompositeSequenceService {

    protected final CompositeSequenceDao compositeSequenceDao;

    public CompositeSequenceServiceBase( CompositeSequenceDao compositeSequenceDao ) {
        super( compositeSequenceDao );
        this.compositeSequenceDao = compositeSequenceDao;
    }

    /**
     * @see CompositeSequenceService#findByBioSequence(BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequence( final BioSequence bioSequence ) {
        return this.handleFindByBioSequence( bioSequence );
    }

    /**
     * @see CompositeSequenceService#findByBioSequenceName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequenceName( final String name ) {
        return this.handleFindByBioSequenceName( name );
    }

    /**
     * @see CompositeSequenceService#findByGene(Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( final Gene gene ) {
        return this.handleFindByGene( gene );
    }

    /**
     * @see CompositeSequenceService#findByGene(Gene, ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( final Gene gene, final ArrayDesign arrayDesign ) {
        return this.handleFindByGene( gene, arrayDesign );
    }

    /**
     * @see CompositeSequenceService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByName( final String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see CompositeSequenceService#findByName(ArrayDesign, String)
     */
    @Override
    @Transactional(readOnly = true)
    public CompositeSequence findByName( final ArrayDesign arrayDesign, final String name ) {
        return this.handleFindByName( arrayDesign, name );
    }

    /**
     * @see CompositeSequenceService#findByNamesInArrayDesigns(Collection, Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByNamesInArrayDesigns( final Collection<String> compositeSequenceNames,
            final Collection<ArrayDesign> arrayDesigns ) {
        return this.handleFindByNamesInArrayDesigns( compositeSequenceNames, arrayDesigns );
    }

    /**
     * @see CompositeSequenceService#getGenes(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<Gene>> getGenes( final Collection<CompositeSequence> sequences ) {
        return this.handleGetGenes( sequences );
    }

    /**
     * @see CompositeSequenceService#getGenes(CompositeSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenes( final CompositeSequence compositeSequence ) {
        return this.handleGetGenes( compositeSequence );
    }

    /**
     * @see CompositeSequenceService#getGenesWithSpecificity(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final Collection<CompositeSequence> compositeSequences ) {
        return this.handleGetGenesWithSpecificity( compositeSequences );

    }

    /**
     * @see CompositeSequenceService#getRawSummary(Collection, Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( final Collection<CompositeSequence> compositeSequences,
            final Integer numResults ) {
        return this.handleGetRawSummary( compositeSequences, numResults );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( final ArrayDesign arrayDesign, final Integer numResults ) {
        return this.handleGetRawSummary( arrayDesign, numResults );
    }

    /**
     * @see CompositeSequenceService#getRawSummary(CompositeSequence, Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( final CompositeSequence compositeSequence, final Integer numResults ) {
        return this.handleGetRawSummary( compositeSequence, numResults );
    }

    /**
     * @see CompositeSequenceService#remove(Collection)
     */
    @Override
    @Transactional
    public void remove( Collection<CompositeSequence> entities ) {
        this.handleRemove( entities );
    }

    /**
     * Performs the core logic for {@link #findByBioSequence(BioSequence)}
     */
    protected abstract Collection<CompositeSequence> handleFindByBioSequence( BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #findByBioSequenceName(String)}
     */
    protected abstract Collection<CompositeSequence> handleFindByBioSequenceName( String name );

    /**
     * Performs the core logic for {@link #findByGene(Gene)}
     */
    protected abstract Collection<CompositeSequence> handleFindByGene( Gene gene );

    /**
     * Performs the core logic for
     * {@link #findByGene(Gene, ArrayDesign)}
     */
    protected abstract Collection<CompositeSequence> handleFindByGene( Gene gene, ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #findByName(String)}
     */
    protected abstract Collection<CompositeSequence> handleFindByName( String name );

    /**
     * Performs the core logic for
     * {@link #findByName(ArrayDesign, String)}
     */
    protected abstract CompositeSequence handleFindByName( ArrayDesign arrayDesign, String name );

    /**
     * Performs the core logic for {@link #findByNamesInArrayDesigns(Collection, Collection)}
     */
    protected abstract Collection<CompositeSequence> handleFindByNamesInArrayDesigns(
            Collection<String> compositeSequenceNames, Collection<ArrayDesign> arrayDesigns );

    /**
     * Performs the core logic for {@link #getGenes(Collection)}
     */
    protected abstract Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            Collection<CompositeSequence> sequences );

    /**
     * Performs the core logic for {@link #getGenes(CompositeSequence)}
     */
    protected abstract Collection<Gene> handleGetGenes( CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(Collection)}
     */
    protected abstract Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getRawSummary(Collection, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary( Collection<CompositeSequence> compositeSequences,
            Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ArrayDesign, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary( ArrayDesign arrayDesign, Integer numResults );

    /**
     * Performs the core logic for {@link #getRawSummary(CompositeSequence, Integer)}
     */
    protected abstract Collection<Object[]> handleGetRawSummary( CompositeSequence compositeSequence,
            Integer numResults );

    /**
     * Performs the core logic for {@link #remove(Collection)}
     */
    protected abstract void handleRemove( Collection<CompositeSequence> sequencesToDelete );

}