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
package ubic.gemma.model.expression.designElement;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;

/**
 * Spring Service base class for <code>ubic.gemma.model.expression.designElement.CompositeSequenceService</code>,
 * provides access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.expression.designElement.CompositeSequenceService
 */
public abstract class CompositeSequenceServiceBase implements CompositeSequenceService {

    @Autowired
    private ubic.gemma.model.expression.designElement.CompositeSequenceDao compositeSequenceDao;

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {

        return this.handleCountAll();

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#create(java.util.Collection)
     */
    @Override
    @Transactional
    public java.util.Collection<CompositeSequence> create(
            final java.util.Collection<CompositeSequence> compositeSequences ) {

        return this.handleCreate( compositeSequences );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#create(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional
    public ubic.gemma.model.expression.designElement.CompositeSequence create(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        return this.handleCreate( compositeSequence );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#find(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.designElement.CompositeSequence find(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        return this.handleFind( compositeSequence );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {

        return this.handleFindByBioSequence( bioSequence );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByBioSequenceName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByBioSequenceName( final java.lang.String name ) {

        return this.handleFindByBioSequenceName( name );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByGene( final ubic.gemma.model.genome.Gene gene ) {

        return this.handleFindByGene( gene );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByGene(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByGene( final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {

        return this.handleFindByGene( gene, arrayDesign );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByName( final java.lang.String name ) {

        return this.handleFindByName( name );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.designElement.CompositeSequence findByName(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {

        return this.handleFindByName( arrayDesign, name );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByNamesInArrayDesigns(java.util.Collection,
     *      java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> findByNamesInArrayDesigns(
            final java.util.Collection<String> compositeSequenceNames,
            final java.util.Collection<ArrayDesign> arrayDesigns ) {

        return this.handleFindByNamesInArrayDesigns( compositeSequenceNames, arrayDesigns );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional
    public ubic.gemma.model.expression.designElement.CompositeSequence findOrCreate(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        return this.handleFindOrCreate( compositeSequence );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenes(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes(
            final java.util.Collection<CompositeSequence> sequences ) {

        return this.handleGetGenes( sequences );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene> getGenes(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        return this.handleGetGenes( compositeSequence );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenesWithSpecificity(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final java.util.Collection<CompositeSequence> compositeSequences ) {

        return this.handleGetGenesWithSpecificity( compositeSequences );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(java.util.Collection,
     *      java.lang.Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Object[]> getRawSummary(
            final java.util.Collection<CompositeSequence> compositeSequences, final java.lang.Integer numResults ) {

        return this.handleGetRawSummary( compositeSequences, numResults );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.Integer numResults ) {

        return this.handleGetRawSummary( arrayDesign, numResults );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence,
     *      java.lang.Integer)
     * @Deprecated is this used anywhere?
     */
    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence,
            final java.lang.Integer numResults ) {

        return this.handleGetRawSummary( compositeSequence, numResults );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.designElement.CompositeSequence load( final java.lang.Long id ) {

        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<CompositeSequence> loadMultiple( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#remove(java.util.Collection)
     */
    @Override
    @Transactional
    public void remove( final java.util.Collection<CompositeSequence> sequencesToDelete ) {

        this.handleRemove( sequencesToDelete );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#remove(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        this.handleRemove( compositeSequence );

    }

    /**
     * Sets the reference to <code>compositeSequence</code>'s DAO.
     */
    public void setCompositeSequenceDao(
            ubic.gemma.model.expression.designElement.CompositeSequenceDao compositeSequenceDao ) {
        this.compositeSequenceDao = compositeSequenceDao;
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#thaw(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final java.util.Collection<CompositeSequence> compositeSequences ) {

        this.handleThaw( compositeSequences );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#update(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {

        this.handleUpdate( compositeSequence );

    }

    /**
     * Gets the reference to <code>compositeSequence</code>'s DAO.
     */
    protected ubic.gemma.model.expression.designElement.CompositeSequenceDao getCompositeSequenceDao() {
        return this.compositeSequenceDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCreate(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFind(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #findByBioSequenceName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequenceName( java.lang.String name );

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for
     * {@link #findByGene(ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByGene( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for
     * {@link #findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFindByName(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.String name );

    /**
     * Performs the core logic for {@link #findByNamesInArrayDesigns(java.util.Collection, java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByNamesInArrayDesigns(
            java.util.Collection<String> compositeSequenceNames, java.util.Collection<ArrayDesign> arrayDesigns );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFindOrCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #getGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            java.util.Collection<CompositeSequence> sequences );

    /**
     * Performs the core logic for {@link #getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenes(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getRawSummary(java.util.Collection, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            java.util.Collection<CompositeSequence> compositeSequences, java.lang.Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(java.util.Collection)}
     */
    protected abstract void handleRemove( java.util.Collection<CompositeSequence> sequencesToDelete );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

}