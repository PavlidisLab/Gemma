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

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.designElement.CompositeSequenceService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.designElement.CompositeSequenceService
 */
public abstract class CompositeSequenceServiceBase implements
        ubic.gemma.model.expression.designElement.CompositeSequenceService {

    @Autowired
    private ubic.gemma.model.expression.designElement.CompositeSequenceDao compositeSequenceDao;

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#create(java.util.Collection)
     */
    public java.util.Collection<CompositeSequence> create(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            return this.handleCreate( compositeSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.create(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#create(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence create(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            return this.handleCreate( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.create(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#find(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence find(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            return this.handleFind( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.find(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    public java.util.Collection<CompositeSequence> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleFindByBioSequence( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByBioSequenceName(java.lang.String)
     */
    public java.util.Collection<CompositeSequence> findByBioSequenceName( final java.lang.String name ) {
        try {
            return this.handleFindByBioSequenceName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByBioSequenceName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection<CompositeSequence> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByGene(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> findByGene( final ubic.gemma.model.genome.Gene gene,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleFindByGene( gene, arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByGene(ubic.gemma.model.genome.Gene gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByName(java.lang.String)
     */
    public java.util.Collection<CompositeSequence> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.String)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findByName(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {
        try {
            return this.handleFindByName( arrayDesign, name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findByNamesInArrayDesigns(java.util.Collection,
     *      java.util.Collection)
     */
    public java.util.Collection<CompositeSequence> findByNamesInArrayDesigns(
            final java.util.Collection<String> compositeSequenceNames,
            final java.util.Collection<ArrayDesign> arrayDesigns ) {
        try {
            return this.handleFindByNamesInArrayDesigns( compositeSequenceNames, arrayDesigns );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findByNamesInArrayDesigns(java.util.Collection compositeSequenceNames, java.util.Collection arrayDesigns)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence findOrCreate(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            return this.handleFindOrCreate( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenes(java.util.Collection)
     */
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes(
            final java.util.Collection<CompositeSequence> sequences ) {
        try {
            return this.handleGetGenes( sequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getGenes(java.util.Collection sequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public java.util.Collection<Gene> getGenes(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            return this.handleGetGenes( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getGenes(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getGenesWithSpecificity(java.util.Collection)
     */
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            return this.handleGetGenesWithSpecificity( compositeSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getGenesWithSpecificity(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(java.util.Collection,
     *      java.lang.Integer)
     */
    public java.util.Collection<Object[]> getRawSummary(
            final java.util.Collection<CompositeSequence> compositeSequences, final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( compositeSequences, numResults );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getRawSummary(java.util.Collection compositeSequences, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.Integer)
     */
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( arrayDesign, numResults );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence,
     *      java.lang.Integer)
     * @Deprecated is this used anywhere?
     */
    @Deprecated
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence,
            final java.lang.Integer numResults ) {
        try {
            return this.handleGetRawSummary( compositeSequence, numResults );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#loadMultiple(java.util.Collection)
     */
    public java.util.Collection<CompositeSequence> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#remove(java.util.Collection)
     */
    public void remove( final java.util.Collection<CompositeSequence> sequencesToDelete ) {
        try {
            this.handleRemove( sequencesToDelete );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.remove(java.util.Collection sequencesToDelete)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#remove(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public void remove( final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            this.handleRemove( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.remove(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
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
    public void thaw( final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            this.handleThaw( compositeSequences );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.thaw(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#update(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public void update( final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            this.handleUpdate( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.designElement.CompositeSequenceServiceException(
                    "Error performing 'ubic.gemma.model.expression.designElement.CompositeSequenceService.update(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
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
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCreate(
            java.util.Collection<CompositeSequence> compositeSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFind(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByBioSequenceName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequenceName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #findByGene(ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByGene( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #findByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFindByName(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByNamesInArrayDesigns(java.util.Collection, java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByNamesInArrayDesigns(
            java.util.Collection<String> compositeSequenceNames, java.util.Collection<ArrayDesign> arrayDesigns )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleFindOrCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            java.util.Collection<CompositeSequence> sequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenes(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getRawSummary(java.util.Collection, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            java.util.Collection<CompositeSequence> compositeSequences, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.designElement.CompositeSequence handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(java.util.Collection)}
     */
    protected abstract void handleRemove( java.util.Collection<CompositeSequence> sequencesToDelete )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<CompositeSequence> compositeSequences )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence )
            throws java.lang.Exception;

}