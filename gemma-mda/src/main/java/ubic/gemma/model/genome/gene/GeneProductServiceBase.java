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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequenceDao;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao;

/**
 * <p>
 * Spring Service base class for <code>GeneProductService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 * 
 * @see GeneProductService
 */
public abstract class GeneProductServiceBase implements GeneProductService {

    @Autowired
    private AnnotationAssociationDao annotationAssociationDao;

    @Autowired
    private BioSequenceDao bioSequenceDao;

    @Autowired
    private BlatAssociationDao blatAssociationDao;

    @Autowired
    private GeneProductDao geneProductDao;

    /**
     * @see GeneProductService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see GeneProductService#create(GeneProduct)
     */
    @Override
    @Transactional
    public GeneProduct create( final GeneProduct geneProduct ) {
        return this.handleCreate( geneProduct );

    }

    /**
     * @see GeneProductService#delete(GeneProduct)
     */
    @Override
    @Transactional
    public void delete( final GeneProduct geneProduct ) {
        this.handleDelete( geneProduct );

    }

    /**
     * @see GeneProductService#find(GeneProduct)
     */
    @Override
    @Transactional(readOnly = true)
    public GeneProduct find( final GeneProduct gProduct ) {
        return this.handleFind( gProduct );

    }

    /**
     * @see GeneProductService#findOrCreate(GeneProduct)
     */
    @Override
    @Transactional
    public GeneProduct findOrCreate( final GeneProduct geneProduct ) {
        return this.handleFindOrCreate( geneProduct );

    }

    public AnnotationAssociationDao getAnnotationAssociationDao() {
        return annotationAssociationDao;
    }

    public BioSequenceDao getBioSequenceDao() {
        return bioSequenceDao;
    }

    public BlatAssociationDao getBlatAssociationDao() {
        return blatAssociationDao;
    }

    /**
     * @see GeneProductService#getGenesByName(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );

    }

    /**
     * @see GeneProductService#getGenesByNcbiId(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByNcbiId( final java.lang.String search ) {
        return this.handleGetGenesByNcbiId( search );

    }

    /**
     * @see GeneProductService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public GeneProduct load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see GeneProductService#loadMultiple(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProduct> loadMultiple( final Collection<Long> ids ) {
        return this.handleLoadMultiple( ids );

    }

    /**
     * @see GeneProductService#update(GeneProduct)
     */
    @Override
    @Transactional
    public void update( final GeneProduct geneProduct ) {
        this.handleUpdate( geneProduct );

    }

    /**
     * Gets the reference to <code>geneProduct</code>'s DAO.
     */
    protected GeneProductDao getGeneProductDao() {
        return this.geneProductDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(GeneProduct)}
     */
    protected abstract GeneProduct handleCreate( GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #delete(GeneProduct)}
     */
    protected abstract void handleDelete( GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #find(GeneProduct)}
     */
    protected abstract GeneProduct handleFind( GeneProduct gProduct );

    /**
     * Performs the core logic for {@link #findOrCreate(GeneProduct)}
     */
    protected abstract GeneProduct handleFindOrCreate( GeneProduct geneProduct );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByNcbiId( java.lang.String search );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract GeneProduct handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadMultiple(Collection)}
     */
    protected abstract Collection<GeneProduct> handleLoadMultiple( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #update(GeneProduct)}
     */
    protected abstract void handleUpdate( GeneProduct geneProduct );

}