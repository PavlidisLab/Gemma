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

import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.Gene2GeneProteinAssociationService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService
 */
public abstract class Gene2GeneProteinAssociationServiceBase implements
        ubic.gemma.model.association.Gene2GeneProteinAssociationService {

    @Autowired
    private ubic.gemma.model.association.Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao;

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#create(ubic.gemma.model.association.Gene2GeneProteinAssociation)
     */
    @Override
    public ubic.gemma.model.association.Gene2GeneProteinAssociation create(
            final ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleCreate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.create(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }
    
    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#create(ubic.gemma.model.association.Gene2GeneProteinAssociation)
     */
    @Override
    public ubic.gemma.model.association.Gene2GeneProteinAssociation createOrUpdate(
            final ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleCreateOrUpdate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.createOrUpdate(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }
    
    
    

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#update(ubic.gemma.model.association.Gene2GeneProteinAssociation)
     */
    @Override
    public void update( final ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            this.handleUpdate( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.create(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#find(ubic.gemma.model.association.Gene2GeneProteinAssociation)
     */
    @Override
    public Gene2GeneProteinAssociation find(
            final ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation ) {
        try {
            return this.handleFind( gene2GeneProteinAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.find(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#loadAll()
     */
    @Override
    public Collection<Gene2GeneProteinAssociation> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.loadAll(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#handleDeleteAll()
     */
    @Override
    public void deleteAll( Collection<Gene2GeneProteinAssociation> associations ) {
        try {
            this.handleDeleteAll( associations );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.deleteAll(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#handleDelete()
     */
    @Override
    public void delete( Gene2GeneProteinAssociation association ) {
        try {
            this.handleDelete( association );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.deleteAll(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }

    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#handleThaw()
     */
    @Override
    public void thaw( Gene2GeneProteinAssociation association ) {
        try {
            this.handleThaw( association );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.thaw(ubic.gemma.model.association.Gene2GeneProteinAssociation)' --> "
                            + th, th );
        }

    }
    
    /**
     * @see ubic.gemma.model.association.Gene2GeneProteinAssociationService#handleThaw()
     */
    @Override
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene ) {
        try {
            return this.handleFindProteinInteractionsForGene( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GeneProteinAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GeneProteinAssociationServiceBase.findProteinInteractionsForGene(ubic.gemma.model.association.Gene)' --> "
                            + th, th );
        }

    }       
    
    

    /**
     * Sets the reference to <code>gene2GeneProteinAssociation</code>'s DAO.
     */
    public void setGene2GeneProteinAssociationDao(
            ubic.gemma.model.association.Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao ) {
        this.gene2GeneProteinAssociationDao = gene2GeneProteinAssociationDao;
    }

    /**
     * Gets the reference to <code>gene2GeneProteinAssociation</code>'s DAO.
     */
    protected ubic.gemma.model.association.Gene2GeneProteinAssociationDao gene2GeneProteinAssociationDao() {
        return this.gene2GeneProteinAssociationDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract ubic.gemma.model.association.Gene2GeneProteinAssociation handleCreate(
            ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws java.lang.Exception;
    
    
    /**
     * Performs the core logic for {@link #createOUpdate(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract ubic.gemma.model.association.Gene2GeneProteinAssociation handleCreateOrUpdate(
            ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws java.lang.Exception;        

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract void handleUpdate(
            ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract Gene2GeneProteinAssociation handleFind(
            ubic.gemma.model.association.Gene2GeneProteinAssociation gene2GeneProteinAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract Collection<Gene2GeneProteinAssociation> handleLoadAll() throws java.lang.Exception;
    
    
    
    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract  void handleDeleteAll( Collection<Gene2GeneProteinAssociation> associations )  throws java.lang.Exception;
    
    
    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract  void handleDelete( Gene2GeneProteinAssociation associations )  throws java.lang.Exception;
    
    
    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.association.Gene2GeneProteinAssociation)}
     */
    protected abstract  void handleThaw( Gene2GeneProteinAssociation associations )  throws java.lang.Exception;
    
    /**
     * 
      Performs the core logic for {@link #findProteinInteractionsForGene(ubic.gemma.model.genome.Gene)}
      */
    protected abstract Collection<Gene2GeneProteinAssociation> handleFindProteinInteractionsForGene( Gene gene ) throws java.lang.Exception;
    
    
    

    

}