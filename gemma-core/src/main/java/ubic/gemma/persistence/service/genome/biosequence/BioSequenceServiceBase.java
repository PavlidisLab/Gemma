/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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
package ubic.gemma.persistence.service.genome.biosequence;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Spring Service base class for <code>BioSequenceService</code>, provides access to
 * all services and entities referenced by this service.
 * 
 * @see BioSequenceService
 * @version $Id$
 */
public abstract class BioSequenceServiceBase implements BioSequenceService {

    @Autowired
    private BioSequenceDao bioSequenceDao;

    /**
     * @see BioSequenceService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see BioSequenceService#create(java.util.Collection)
     */
    @Override
    @Transactional
    public java.util.Collection<BioSequence> create( final java.util.Collection<BioSequence> bioSequences ) {
        return this.handleCreate( bioSequences );

    }

    /**
     * @see BioSequenceService#create(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional
    public ubic.gemma.model.genome.biosequence.BioSequence create(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleCreate( bioSequence );

    }

    /**
     * @see BioSequenceService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.genome.biosequence.BioSequence find(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFind( bioSequence );

    }

    /**
     * @see BioSequenceService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.genome.biosequence.BioSequence findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        return this.handleFindByAccession( accession );

    }

    /**
     * @see BioSequenceService#findByGenes(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Gene, Collection<BioSequence>> findByGenes( final java.util.Collection<Gene> genes ) {
        return this.handleFindByGenes( genes );

    }

    /**
     * @see BioSequenceService#findByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioSequence> findByName( final java.lang.String name ) {
        return this.handleFindByName( name );

    }

    /**
     * @see BioSequenceService#findOrCreate(java.util.Collection)
     */
    @Override
    @Transactional
    public java.util.Collection<BioSequence> findOrCreate( final java.util.Collection<BioSequence> bioSequences ) {
        return this.handleFindOrCreate( bioSequences );

    }

    /**
     * @see BioSequenceService#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional
    public ubic.gemma.model.genome.biosequence.BioSequence findOrCreate(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFindOrCreate( bioSequence );

    }

    /**
     * @see BioSequenceService#getGenesByAccession(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene> getGenesByAccession( final java.lang.String search ) {
        return this.handleGetGenesByAccession( search );

    }

    /**
     * @see BioSequenceService#getGenesByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );

    }

    /**
     * @see BioSequenceService#load(long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.genome.biosequence.BioSequence load( final long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see BioSequenceService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioSequence> loadMultiple( final java.util.Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );

    }

    /**
     * @see BioSequenceService#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        this.handleRemove( bioSequence );

    }

    /**
     * Sets the reference to <code>bioSequence</code>'s DAO.
     */
    public void setBioSequenceDao( BioSequenceDao bioSequenceDao ) {
        this.bioSequenceDao = bioSequenceDao;
    }

    /**
     * @see BioSequenceService#thaw(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BioSequence> thaw( final java.util.Collection<BioSequence> bioSequences ) {
        return this.handleThaw( bioSequences );

    }

    /**
     * @see BioSequenceService#thaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public BioSequence thaw( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleThaw( bioSequence );

    }

    /**
     * @see BioSequenceService#update(java.util.Collection)
     */
    @Override
    @Transactional
    public void update( final java.util.Collection<BioSequence> bioSequences ) {
        this.handleUpdate( bioSequences );

    }

    /**
     * @see BioSequenceService#update(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        this.handleUpdate( bioSequence );

    }

    /**
     * Gets the reference to <code>bioSequence</code>'s DAO.
     */
    protected BioSequenceDao getBioSequenceDao() {
        return this.bioSequenceDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleCreate( java.util.Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFind(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFindByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * Performs the core logic for {@link #findByGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<Gene, Collection<BioSequence>> handleFindByGenes( java.util.Collection<Gene> genes );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #findOrCreate(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindOrCreate(
            java.util.Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFindOrCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByAccession( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #load(long)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleLoad( long id );

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract void handleRemove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( java.util.Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #update(java.util.Collection)}
     */
    protected abstract void handleUpdate( java.util.Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

}