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
package ubic.gemma.annotation.reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;

/**
 * Service base class for <code>ubic.gemma.model.common.description.BibliographicReferenceService</code>, provides
 * access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.annotation.reference.BibliographicReferenceService
 */
public abstract class BibliographicReferenceServiceBase implements
        ubic.gemma.annotation.reference.BibliographicReferenceService {

    @Autowired
    private ubic.gemma.model.common.description.BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ubic.gemma.model.common.description.ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private ubic.gemma.model.common.description.LocalFileDao localFileDao;
    
    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;
    
    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#addPDF(ubic.gemma.model.common.description.LocalFile,
     *      ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public void addPDF( final ubic.gemma.model.common.description.LocalFile pdfFile,
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            this.handleAddPDF( pdfFile, bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.addPDF(ubic.gemma.model.common.description.LocalFile pdfFile, ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#create(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference create(
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            return this.handleCreate( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.create(ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#find(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference find(
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            return this.handleFind( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.find(ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }
    
    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( final java.lang.String id ) {
        try {
            return this.handleFindByExternalId( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.findByExternalId(java.lang.String id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findVOByExternalId(java.lang.String)
     */
    @Override
    public BibliographicReferenceValueObject findVOByExternalId( final java.lang.String id ) {
        try {
            BibliographicReference bibref = this.handleFindByExternalId( id );
            if (bibref == null){
                return null;
            }
            BibliographicReferenceValueObject bibrefVO = new BibliographicReferenceValueObject( bibref );
            this.populateBibliographicPhenotypes( bibrefVO );
            this.populateRelatedExperiments( bibref, bibrefVO );
            return bibrefVO;
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.findByExternalId(java.lang.String id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( final java.lang.String id,
            final java.lang.String databaseName ) {
        try {
            return this.handleFindByExternalId( id, databaseName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.findByExternalId(java.lang.String id, java.lang.String databaseName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findOrCreate(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference findOrCreate(
            final ubic.gemma.model.common.description.BibliographicReference BibliographicReference ) {
        try {
            return this.handleFindOrCreate( BibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.findOrCreate(ubic.gemma.model.common.description.BibliographicReference BibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#getAllExperimentLinkedReferences()
     */
    @Override
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        try {
            return this.handleGetAllExperimentLinkedReferences();
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.getAllExperimentLinkedReferences()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            return this.handleGetRelatedExperiments( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.BibliographicReference load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }
    
    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<BibliographicReference> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<BibliographicReferenceValueObject> loadMultipleValueObjects( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultipleValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#remove(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public void remove( final ubic.gemma.model.common.description.BibliographicReference BibliographicReference ) {
        try {
            this.handleRemove( BibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.remove(ubic.gemma.model.common.description.BibliographicReference BibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bibliographicReference</code>'s DAO.
     */
    public void setBibliographicReferenceDao(
            ubic.gemma.model.common.description.BibliographicReferenceDao bibliographicReferenceDao ) {
        this.bibliographicReferenceDao = bibliographicReferenceDao;
    }

    /**
     * Sets the reference to <code>externalDatabase</code>'s DAO.
     */
    public void setExternalDatabaseDao( ubic.gemma.model.common.description.ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * Sets the reference to <code>localFile</code>'s DAO.
     */
    public void setLocalFileDao( ubic.gemma.model.common.description.LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#update(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public void update( final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            this.handleUpdate( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceService.update(ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>bibliographicReference</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.BibliographicReferenceDao getBibliographicReferenceDao() {
        return this.bibliographicReferenceDao;
    }

    /**
     * Gets the reference to <code>externalDatabase</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.ExternalDatabaseDao getExternalDatabaseDao() {
        return this.externalDatabaseDao;
    }

    /**
     * Gets the reference to <code>localFile</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.LocalFileDao getLocalFileDao() {
        return this.localFileDao;
    }

    /**
     * Performs the core logic for
     * {@link #addPDF(ubic.gemma.model.common.description.LocalFile, ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract void handleAddPDF( ubic.gemma.model.common.description.LocalFile pdfFile,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleCreate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleFind(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId(
            java.lang.String id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(java.lang.String, java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId(
            java.lang.String id, java.lang.String databaseName ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleFindOrCreate(
            ubic.gemma.model.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences()
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetRelatedExperiments(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.description.BibliographicReference handleLoad( java.lang.Long id )
            throws java.lang.Exception;
    
    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BibliographicReference> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultipleValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<BibliographicReferenceValueObject> handleLoadMultipleValueObjects( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract void handleRemove(
            ubic.gemma.model.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract void handleUpdate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;
    

    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateRelatedExperiments( BibliographicReference bibRef,
            BibliographicReferenceValueObject bibRefVO ) {
        Collection<ExpressionExperiment> relatedExperiments = this.getRelatedExperiments( bibRef );
        if(relatedExperiments.isEmpty()){
            bibRefVO.setExperiments( new ArrayList<ExpressionExperimentValueObject>() );
        }else{
            bibRefVO.setExperiments( ExpressionExperimentValueObject.convert2ValueObjects( relatedExperiments ) );
        }
        
    }
    
    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateRelatedExperiments( Collection<BibliographicReference> bibRefs,
            Map<Long, BibliographicReferenceValueObject> idTobibRefVO ) {
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this
                .getRelatedExperiments( bibRefs );
        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = idTobibRefVO.get( bibref.getId() );
            if ( relatedExperiments.containsKey( bibref ) ) {
                vo.setExperiments( ExpressionExperimentValueObject.convert2ValueObjects( relatedExperiments
                        .get( bibref ) ) );
            }
        }
    }

    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateBibliographicPhenotypes( Map<Long, BibliographicReferenceValueObject> idTobibRefVO ) {

        for(BibliographicReferenceValueObject vo : idTobibRefVO.values()){
            this.populateBibliographicPhenotypes( vo );
        }
    }

    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateBibliographicPhenotypes( BibliographicReferenceValueObject bibRefVO ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenotypeAssociationService
                    .findPhenotypesForBibliographicReference( bibRefVO.getPubAccession() );
        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                    .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );
        bibRefVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );
    }

}