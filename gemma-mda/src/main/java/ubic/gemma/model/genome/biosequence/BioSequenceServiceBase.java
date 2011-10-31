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
package ubic.gemma.model.genome.biosequence;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.biosequence.BioSequenceService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.biosequence.BioSequenceService
 */
public abstract class BioSequenceServiceBase implements ubic.gemma.model.genome.biosequence.BioSequenceService {

    @Autowired
    private ubic.gemma.model.genome.biosequence.BioSequenceDao bioSequenceDao;

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.countAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#create(java.util.Collection)
     */
    public java.util.Collection<BioSequence> create( final java.util.Collection<BioSequence> bioSequences ) {
        try {
            return this.handleCreate( bioSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.create(java.util.Collection bioSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#create(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence create(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleCreate( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.create(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence find(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleFind( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.find(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findByAccession(
            final ubic.gemma.model.common.description.DatabaseEntry accession ) {
        try {
            return this.handleFindByAccession( accession );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.findByAccession(ubic.gemma.model.common.description.DatabaseEntry accession)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findByGenes(java.util.Collection)
     */
    public java.util.Map<Gene, Collection<BioSequence>> findByGenes( final java.util.Collection<Gene> genes ) {
        try {
            return this.handleFindByGenes( genes );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.findByGenes(java.util.Collection genes)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findByName(java.lang.String)
     */
    public java.util.Collection<BioSequence> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findOrCreate(java.util.Collection)
     */
    public java.util.Collection findOrCreate( final java.util.Collection bioSequences ) {
        try {
            return this.handleFindOrCreate( bioSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.findOrCreate(java.util.Collection bioSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findOrCreate(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleFindOrCreate( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#getGenesByAccession(java.lang.String)
     */
    public java.util.Collection getGenesByAccession( final java.lang.String search ) {
        try {
            return this.handleGetGenesByAccession( search );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.getGenesByAccession(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#getGenesByName(java.lang.String)
     */
    public java.util.Collection getGenesByName( final java.lang.String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.getGenesByName(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#load(long)
     */
    public ubic.gemma.model.genome.biosequence.BioSequence load( final long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.load(long id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#loadMultiple(java.util.Collection)
     */
    public java.util.Collection loadMultiple( final java.util.Collection ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public void remove( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            this.handleRemove( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.remove(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bioSequence</code>'s DAO.
     */
    public void setBioSequenceDao( ubic.gemma.model.genome.biosequence.BioSequenceDao bioSequenceDao ) {
        this.bioSequenceDao = bioSequenceDao;
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#thaw(java.util.Collection)
     */
    public Collection<BioSequence> thaw( final java.util.Collection<BioSequence> bioSequences ) {
        try {
            return this.handleThaw( bioSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.thaw(java.util.Collection bioSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#thaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public BioSequence thaw( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleThaw( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.thaw(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(java.util.Collection)
     */
    public void update( final java.util.Collection bioSequences ) {
        try {
            this.handleUpdate( bioSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.update(java.util.Collection bioSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public void update( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            this.handleUpdate( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.biosequence.BioSequenceServiceException(
                    "Error performing 'ubic.gemma.model.genome.biosequence.BioSequenceService.update(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>bioSequence</code>'s DAO.
     */
    protected ubic.gemma.model.genome.biosequence.BioSequenceDao getBioSequenceDao() {
        return this.bioSequenceDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleCreate( java.util.Collection<BioSequence> bioSequences )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFind(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFindByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGenes(java.util.Collection)}
     */
    protected abstract java.util.Map handleFindByGenes( java.util.Collection<Gene> genes ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleFindOrCreate(
            java.util.Collection<BioSequence> bioSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleFindOrCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract java.util.Collection handleGetGenesByAccession( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection handleGetGenesByName( java.lang.String search ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(long)}
     */
    protected abstract ubic.gemma.model.genome.biosequence.BioSequence handleLoad( long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioSequence> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract void handleRemove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( java.util.Collection<BioSequence> bioSequences )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(java.util.Collection)}
     */
    protected abstract void handleUpdate( java.util.Collection<BioSequence> bioSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception;

}