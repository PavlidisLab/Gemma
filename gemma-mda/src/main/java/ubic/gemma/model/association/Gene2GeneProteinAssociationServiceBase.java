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
package ubic.gemma.model.association;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.Gene2GeneProteinAssociationService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see Gene2GeneProteinAssociationService
 */
public abstract class Gene2GeneProteinAssociationServiceBase implements Gene2GeneProteinAssociationService {

    @Autowired
    private Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao;

    /**
     * @see Gene2GeneProteinAssociationService#create(Gene2GeneProteinAssociation)
     */
    @Override
    @Transactional
    public Gene2GeneProteinAssociation create( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleCreate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.create(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GeneProteinAssociationService#create(Gene2GeneProteinAssociation)
     */
    @Override
    @Transactional
    public Gene2GeneProteinAssociation createOrUpdate( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleCreateOrUpdate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.createOrUpdate(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GeneProteinAssociationService#update(Gene2GeneProteinAssociation)
     */
    @Override
    @Transactional
    public void update( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            this.handleUpdate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.create(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GeneProteinAssociationService#find(Gene2GeneProteinAssociation)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene2GeneProteinAssociation find( final Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleFind( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.find(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see Gene2GeneProteinAssociationService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GeneProteinAssociation> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.loadAll(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see Gene2GeneProteinAssociationService#handleDeleteAll()
     */
    @Override
    @Transactional
    public void deleteAll( Collection<Gene2GeneProteinAssociation> associations ) {
        try {
            this.handleDeleteAll( associations );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.deleteAll(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see Gene2GeneProteinAssociationService#handleDelete()
     */
    @Override
    @Transactional
    public void delete( Gene2GeneProteinAssociation association ) {
        try {
            this.handleDelete( association );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.deleteAll(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see Gene2GeneProteinAssociationService#handleThaw()
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( Gene2GeneProteinAssociation association ) {
        try {
            this.handleThaw( association );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.thaw(Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see Gene2GeneProteinAssociationService#handleThaw()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene ) {
        try {
            return this.handleFindProteinInteractionsForGene( gene );
        } catch ( Throwable th ) {
            throw new Gene2GeneProteinAssociationServiceException(
                    "Error performing 'Gene2GeneProteinAssociationServiceBase.findProteinInteractionsForGene(Gene)' --> "
                            + th, th );
        }

    }

    /**
     * Sets the reference to <code>gene2GeneProteinAssociation</code>'s DAO.
     */
    public void setGene2GeneProteinAssociationDao( Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao ) {
        this.gene2GeneProteinAssociationDao = gene2GeneProteinAssociationDao;
    }

    /**
     * Gets the reference to <code>gene2GeneProteinAssociation</code>'s DAO.
     */
    protected Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao() {
        return this.gene2GeneProteinAssociationDao;
    }

    /**
     * Performs the core logic for {@link #create(Gene2GeneProteinAssociation)}
     */
    protected abstract Gene2GeneProteinAssociation handleCreate( Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws Exception;

    /**
     * Performs the core logic for {@link #createOUpdate(Gene2GeneProteinAssociation)}
     */
    protected abstract Gene2GeneProteinAssociation handleCreateOrUpdate(
            Gene2GeneProteinAssociation gene2GeneProteinAssociation ) throws Exception;

    /**
     * Performs the core logic for {@link #update(Gene2GeneProteinAssociation)}
     */
    protected abstract void handleUpdate( Gene2GeneProteinAssociation gene2GeneProteinAssociation ) throws Exception;

    /**
     * Performs the core logic for {@link #update(Gene2GeneProteinAssociation)}
     */
    protected abstract Gene2GeneProteinAssociation handleFind( Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws Exception;

    /**
     * Performs the core logic for {@link #update(Gene2GeneProteinAssociation)}
     */
    protected abstract Collection<Gene2GeneProteinAssociation> handleLoadAll() throws Exception;

    /**
     * Performs the core logic for {@link #update(Gene2GeneProteinAssociation)}
     */
    protected abstract void handleDeleteAll( Collection<Gene2GeneProteinAssociation> associations ) throws Exception;

    /**
     * Performs the core logic for {@link #delete(Gene2GeneProteinAssociation)}
     */
    protected abstract void handleDelete( Gene2GeneProteinAssociation associations ) throws Exception;

    /**
     * Performs the core logic for {@link #thaw(Gene2GeneProteinAssociation)}
     */
    protected abstract void handleThaw( Gene2GeneProteinAssociation associations ) throws Exception;

    /**
     * 
     Performs the core logic for {@link #findProteinInteractionsForGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<Gene2GeneProteinAssociation> handleFindProteinInteractionsForGene( Gene gene )
            throws Exception;

}