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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.association.Gene2GOAssociationService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 */
public abstract class Gene2GOAssociationServiceBase implements ubic.gemma.model.association.Gene2GOAssociationService {

    private ubic.gemma.model.association.Gene2GOAssociationDao gene2GOAssociationDao;

    private ubic.gemma.model.common.description.VocabCharacteristicDao vocabCharacteristicDao;

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#create(ubic.gemma.model.association.Gene2GOAssociation)
     */
    public ubic.gemma.model.association.Gene2GOAssociation create(
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleCreate( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.create(ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    public ubic.gemma.model.association.Gene2GOAssociation find(
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleFind( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.find(ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection findAssociationByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindAssociationByGene( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.findAssociationByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findByGene(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findByGOTerm(java.lang.String,
     *      ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection findByGOTerm( final java.lang.String goID, final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByGOTerm( goID, taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.findByGOTerm(java.lang.String goID, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation)
     */
    public ubic.gemma.model.association.Gene2GOAssociation findOrCreate(
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleFindOrCreate( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.findOrCreate(ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#removeAll()
     */
    public void removeAll() {
        try {
            this.handleRemoveAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.Gene2GOAssociationServiceException(
                    "Error performing 'ubic.gemma.model.association.Gene2GOAssociationService.removeAll()' --> " + th,
                    th );
        }
    }

    /**
     * Sets the reference to <code>gene2GOAssociation</code>'s DAO.
     */
    public void setGene2GOAssociationDao( ubic.gemma.model.association.Gene2GOAssociationDao gene2GOAssociationDao ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
    }

    /**
     * Sets the reference to <code>vocabCharacteristic</code>'s DAO.
     */
    public void setVocabCharacteristicDao(
            ubic.gemma.model.common.description.VocabCharacteristicDao vocabCharacteristicDao ) {
        this.vocabCharacteristicDao = vocabCharacteristicDao;
    }

    /**
     * Gets the reference to <code>gene2GOAssociation</code>'s DAO.
     */
    protected ubic.gemma.model.association.Gene2GOAssociationDao getGene2GOAssociationDao() {
        return this.gene2GOAssociationDao;
    }

    /**
     * Gets the reference to <code>vocabCharacteristic</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.VocabCharacteristicDao getVocabCharacteristicDao() {
        return this.vocabCharacteristicDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.association.Gene2GOAssociation)}
     */
    protected abstract ubic.gemma.model.association.Gene2GOAssociation handleCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.association.Gene2GOAssociation)}
     */
    protected abstract ubic.gemma.model.association.Gene2GOAssociation handleFind(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findAssociationByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection handleFindAssociationByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGOTerm(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection handleFindByGOTerm( java.lang.String goID,
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.association.Gene2GOAssociation)}
     */
    protected abstract ubic.gemma.model.association.Gene2GOAssociation handleFindOrCreate(
            ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #removeAll()}
     */
    protected abstract void handleRemoveAll() throws java.lang.Exception;

}