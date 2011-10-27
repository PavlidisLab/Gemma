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
package ubic.gemma.model.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.gene.GeneProductService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.GeneProductService
 */
public abstract class GeneProductServiceBase implements ubic.gemma.model.genome.gene.GeneProductService {

    @Autowired
    private ubic.gemma.model.genome.gene.GeneProductDao geneProductDao;

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct create( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        try {
            return this.handleCreate( geneProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.create(ubic.gemma.model.genome.gene.GeneProduct geneProduct)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#delete(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public void delete( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        try {
            this.handleDelete( geneProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.delete(ubic.gemma.model.genome.gene.GeneProduct geneProduct)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#find(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct find( final ubic.gemma.model.genome.gene.GeneProduct gProduct ) {
        try {
            return this.handleFind( gProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.find(ubic.gemma.model.genome.gene.GeneProduct gProduct)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate(
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        try {
            return this.handleFindOrCreate( geneProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.findOrCreate(ubic.gemma.model.genome.gene.GeneProduct geneProduct)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#getGenesByName(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.getGenesByName(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#getGenesByNcbiId(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByNcbiId( final java.lang.String search ) {
        try {
            return this.handleGetGenesByNcbiId( search );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.getGenesByNcbiId(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<GeneProduct> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>geneProduct</code>'s DAO.
     */
    public void setGeneProductDao( ubic.gemma.model.genome.gene.GeneProductDao geneProductDao ) {
        this.geneProductDao = geneProductDao;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public void update( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        try {
            this.handleUpdate( geneProduct );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.GeneProductServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductService.update(ubic.gemma.model.genome.gene.GeneProduct geneProduct)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>geneProduct</code>'s DAO.
     */
    protected ubic.gemma.model.genome.gene.GeneProductDao getGeneProductDao() {
        return this.geneProductDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract void handleDelete( ubic.gemma.model.genome.gene.GeneProduct geneProduct )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleFind(
            ubic.gemma.model.genome.gene.GeneProduct gProduct ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleFindOrCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByNcbiId( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<GeneProduct> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.gene.GeneProduct geneProduct )
            throws java.lang.Exception;

}