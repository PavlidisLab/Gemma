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
package ubic.gemma.genome.taxon.service;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonDao;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.TaxonService</code>, provides access to all services and
 * entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.genome.taxon.service.TaxonService
 */
public abstract class TaxonServiceBase implements ubic.gemma.genome.taxon.service.TaxonService {

    @Autowired
    private ubic.gemma.model.genome.TaxonDao taxonDao;

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#find(ubic.gemma.model.genome.Taxon)
     */
    public ubic.gemma.model.genome.Taxon find( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFind( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.find(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByAbbreviation(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByAbbreviation( final java.lang.String abbreviation ) {
        try {
            return this.handleFindByAbbreviation( abbreviation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByAbbreviation(java.lang.String abbreviation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByCommonName(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByCommonName( final java.lang.String commonName ) {
        try {
            return this.handleFindByCommonName( commonName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByCommonName(java.lang.String commonName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByScientificName(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByScientificName( final java.lang.String scientificName ) {
        try {
            return this.handleFindByScientificName( scientificName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByScientificName(java.lang.String scientificName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findChildTaxaByParent(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> findChildTaxaByParent( Taxon parentTaxa ) {
        try {
            return this.handleFindChildTaxaByParent( parentTaxa );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByScientificName(java.lang.String scientificName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findOrCreate(ubic.gemma.model.genome.Taxon)
     */
    public ubic.gemma.model.genome.Taxon findOrCreate( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindOrCreate( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findOrCreate(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#load(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.load(java.lang.Long id)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#loadAll()
     */
    public java.util.Collection<Taxon> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#remove(ubic.gemma.model.genome.Taxon)
     */
    public void remove( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleRemove( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.remove(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>taxon</code>'s DAO.
     */
    public void setTaxonDao( ubic.gemma.model.genome.TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#update(ubic.gemma.model.genome.Taxon)
     */
    public void update( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleUpdate( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.update(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }
    
    /**
     * thaws taxon
     */
    public void thaw( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleThaw( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.thaw(ubic.gemma.model.genome.Taxon taxon)' -->' --> "
                            + th, th );
        }
    }    
    

    /**
     * Gets the reference to <code>taxon</code>'s DAO.
     */
    protected ubic.gemma.model.genome.TaxonDao getTaxonDao() {
        return this.taxonDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleFind( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByScientificName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleFindByAbbreviation( java.lang.String abbreviation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByCommonName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleFindByCommonName( java.lang.String commonName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByScientificName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleFindByScientificName( java.lang.String scientificName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<ubic.gemma.model.genome.Taxon> handleFindChildTaxaByParent(
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleFindOrCreate( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleLoad( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<Taxon> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract void handleRemove( ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;
    
    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract void handleThaw( ubic.gemma.model.genome.Taxon taxon ) throws Exception ;
    
    
    

}