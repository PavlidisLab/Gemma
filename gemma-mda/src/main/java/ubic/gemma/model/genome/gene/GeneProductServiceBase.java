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
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao;

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

    @Autowired
    private BlatAssociationDao blatAssociationDao;

    @Autowired
    private AnnotationAssociationDao annotationAssociationDao;

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct create( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this.handleCreate( geneProduct );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#delete(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public void delete( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        this.handleDelete( geneProduct );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#find(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct find( final ubic.gemma.model.genome.gene.GeneProduct gProduct ) {
        return this.handleFind( gProduct );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate(
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this.handleFindOrCreate( geneProduct );

    }

    public AnnotationAssociationDao getAnnotationAssociationDao() {
        return annotationAssociationDao;
    }

    public BlatAssociationDao getBlatAssociationDao() {
        return blatAssociationDao;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#getGenesByName(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#getGenesByNcbiId(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByNcbiId( final java.lang.String search ) {
        return this.handleGetGenesByNcbiId( search );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.genome.gene.GeneProduct load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<GeneProduct> loadMultiple( final java.util.Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );

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
        this.handleUpdate( geneProduct );

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
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract void handleDelete( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleFind(
            ubic.gemma.model.genome.gene.GeneProduct gProduct );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleFindOrCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByNcbiId( java.lang.String search );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.gene.GeneProduct handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<GeneProduct> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.gene.GeneProduct)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

}