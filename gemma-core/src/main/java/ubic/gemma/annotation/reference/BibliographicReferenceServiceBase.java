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
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceDao;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;

/**
 * Service base class for <code>BibliographicReferenceService</code>, provides access to all services and entities
 * referenced by this service.
 * 
 * @see ubic.gemma.annotation.reference.BibliographicReferenceService
 */
public abstract class BibliographicReferenceServiceBase implements
        ubic.gemma.annotation.reference.BibliographicReferenceService {

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    @Autowired
    private LocalFileDao localFileDao;

    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#addPDF(LocalFile, BibliographicReference)
     */
    @Override
    @Transactional
    public void addPDF( final LocalFile pdfFile, final BibliographicReference bibliographicReference ) {
        try {
            this.handleAddPDF( pdfFile, bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.addPDF(LocalFile pdfFile, BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#create(BibliographicReference)
     */
    @Override
    @Transactional
    public BibliographicReference create( final BibliographicReference bibliographicReference ) {
        try {
            return this.handleCreate( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.create(BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#find(BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference find( final BibliographicReference bibliographicReference ) {
        try {
            return this.handleFind( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.find(BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( final java.lang.String id ) {
        try {
            return this.handleFindByExternalId( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(java.lang.String id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( final java.lang.String id, final java.lang.String databaseName ) {
        try {
            return this.handleFindByExternalId( id, databaseName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(java.lang.String id, java.lang.String databaseName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findOrCreate(BibliographicReference)
     */
    @Override
    @Transactional
    public BibliographicReference findOrCreate( final BibliographicReference BibliographicReference ) {
        try {
            return this.handleFindOrCreate( BibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findOrCreate(BibliographicReference BibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findVOByExternalId(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findVOByExternalId( final java.lang.String id ) {
        try {
            BibliographicReference bibref = this.handleFindByExternalId( id );
            if ( bibref == null ) {
                return null;
            }
            BibliographicReferenceValueObject bibrefVO = new BibliographicReferenceValueObject( bibref );
            this.populateBibliographicPhenotypes( bibrefVO );
            this.populateRelatedExperiments( bibref, bibrefVO );
            return bibrefVO;
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(java.lang.String id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#getAllExperimentLinkedReferences()
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        try {
            return this.handleGetAllExperimentLinkedReferences();
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.getAllExperimentLinkedReferences()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#getRelatedExperiments(BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            final BibliographicReference bibliographicReference ) {
        try {
            return this.handleGetRelatedExperiments( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.getRelatedExperiments(BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.load(java.lang.Long id)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BibliographicReference> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.loadMultiple(java.util.Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BibliographicReferenceValueObject> loadMultipleValueObjects(
            final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultipleValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.loadMultiple(java.util.Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#remove(BibliographicReference)
     */
    @Override
    @Transactional
    public void remove( final BibliographicReference BibliographicReference ) {
        try {
            this.handleRemove( BibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.remove(BibliographicReference BibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bibliographicReference</code>'s DAO.
     */
    public void setBibliographicReferenceDao( BibliographicReferenceDao bibliographicReferenceDao ) {
        this.bibliographicReferenceDao = bibliographicReferenceDao;
    }

    /**
     * Sets the reference to <code>externalDatabase</code>'s DAO.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * Sets the reference to <code>localFile</code>'s DAO.
     */
    public void setLocalFileDao( LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#update(BibliographicReference)
     */
    @Override
    @Transactional
    public void update( final BibliographicReference bibliographicReference ) {
        try {
            this.handleUpdate( bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.annotation.reference.BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.update(BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>bibliographicReference</code>'s DAO.
     */
    protected BibliographicReferenceDao getBibliographicReferenceDao() {
        return this.bibliographicReferenceDao;
    }

    /**
     * Gets the reference to <code>externalDatabase</code>'s DAO.
     */
    protected ExternalDatabaseDao getExternalDatabaseDao() {
        return this.externalDatabaseDao;
    }

    /**
     * Gets the reference to <code>localFile</code>'s DAO.
     */
    protected LocalFileDao getLocalFileDao() {
        return this.localFileDao;
    }

    /**
     * Performs the core logic for {@link #addPDF(LocalFile, BibliographicReference)}
     */
    protected abstract void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(BibliographicReference)}
     */
    protected abstract BibliographicReference handleCreate( BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(BibliographicReference)}
     */
    protected abstract BibliographicReference handleFind( BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(java.lang.String)}
     */
    protected abstract BibliographicReference handleFindByExternalId( java.lang.String id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(java.lang.String, java.lang.String)}
     */
    protected abstract BibliographicReference handleFindByExternalId( java.lang.String id, java.lang.String databaseName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(BibliographicReference)}
     */
    protected abstract BibliographicReference handleFindOrCreate( BibliographicReference BibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences()
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getRelatedExperiments(BibliographicReference)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract BibliographicReference handleLoad( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BibliographicReference> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultipleValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<BibliographicReferenceValueObject> handleLoadMultipleValueObjects(
            java.util.Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(BibliographicReference)}
     */
    protected abstract void handleRemove( BibliographicReference BibliographicReference ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(BibliographicReference)}
     */
    protected abstract void handleUpdate( BibliographicReference bibliographicReference ) throws java.lang.Exception;

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

    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateBibliographicPhenotypes( Map<Long, BibliographicReferenceValueObject> idTobibRefVO ) {

        for ( BibliographicReferenceValueObject vo : idTobibRefVO.values() ) {
            this.populateBibliographicPhenotypes( vo );
        }
    }

    /**
     * @param bibRefs
     * @param idTobibRefVO
     */
    protected void populateRelatedExperiments( BibliographicReference bibRef, BibliographicReferenceValueObject bibRefVO ) {
        Collection<ExpressionExperiment> relatedExperiments = this.getRelatedExperiments( bibRef );
        if ( relatedExperiments.isEmpty() ) {
            bibRefVO.setExperiments( new ArrayList<ExpressionExperimentValueObject>() );
        } else {
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

}