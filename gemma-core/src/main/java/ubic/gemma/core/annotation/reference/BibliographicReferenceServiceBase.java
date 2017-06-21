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
package ubic.gemma.core.annotation.reference;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;

import java.util.*;

/**
 * Service base class for <code>BibliographicReferenceService</code>, provides access to all services and entities
 * referenced by this service.
 *
 * @see BibliographicReferenceService
 */
public abstract class BibliographicReferenceServiceBase
        extends VoEnabledService<BibliographicReference, BibliographicReferenceValueObject>
        implements BibliographicReferenceService {

    final BibliographicReferenceDao bibliographicReferenceDao;
    private final PhenotypeAssociationService phenotypeAssociationService;

    public BibliographicReferenceServiceBase( BibliographicReferenceDao bibliographicReferenceDao,
            PhenotypeAssociationService phenotypeAssociationService ) {
        super( bibliographicReferenceDao );
        this.bibliographicReferenceDao = bibliographicReferenceDao;
        this.phenotypeAssociationService = phenotypeAssociationService;
    }

    /**
     * @see BibliographicReferenceService#addPDF(LocalFile, BibliographicReference)
     */
    @Override
    @Transactional
    public void addPDF( final LocalFile pdfFile, final BibliographicReference bibliographicReference ) {
        try {
            this.handleAddPDF( pdfFile, bibliographicReference );
        } catch ( Throwable th ) {
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.addPDF(LocalFile pdfFile, BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see BibliographicReferenceService#findByExternalId(String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( final String id ) {
        try {
            return this.handleFindByExternalId( id );
        } catch ( Throwable th ) {
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(String id)' --> " + th, th );
        }
    }

    /**
     * @see BibliographicReferenceService#findByExternalId(String, String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        try {
            return this.handleFindByExternalId( id, databaseName );
        } catch ( Throwable th ) {
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(String id, String databaseName)' --> "
                            + th, th );
        }
    }

    /**
     * @see BibliographicReferenceService#findVOByExternalId(String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findVOByExternalId( final String id ) {
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
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(String id)' --> " + th, th );
        }
    }

    /**
     * @see BibliographicReferenceService#getAllExperimentLinkedReferences()
     */
    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        try {
            return this.handleGetAllExperimentLinkedReferences();
        } catch ( Throwable th ) {
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.getAllExperimentLinkedReferences()' --> " + th,
                    th );
        }
    }

    /**
     * @see BibliographicReferenceService#getRelatedExperiments(BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            final BibliographicReference bibliographicReference ) {
        try {
            return this.handleGetRelatedExperiments( bibliographicReference );
        } catch ( Throwable th ) {
            throw new BibliographicReferenceServiceException(
                    "Error performing 'BibliographicReferenceService.getRelatedExperiments(BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    @Override
    public BibliographicReferenceValueObject loadValueObject( BibliographicReference entity ) {
        return this.loadMultipleValueObjectsFromObjects( Collections.singleton( entity ) ).iterator().next();
    }

    @Override
    public Collection<BibliographicReferenceValueObject> loadAllValueObjects() {
        return this.loadMultipleValueObjectsFromObjects( this.loadAll() );
    }

    /**
     * Performs the core logic for {@link #addPDF(LocalFile, BibliographicReference)}
     */
    protected abstract void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(String)}
     */
    protected abstract BibliographicReference handleFindByExternalId( String id ) throws Exception;

    /**
     * Performs the core logic for {@link #findByExternalId(String, String)}
     */
    protected abstract BibliographicReference handleFindByExternalId( String id, String databaseName ) throws Exception;

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences()
            throws Exception;

    /**
     * Performs the core logic for {@link #getRelatedExperiments(BibliographicReference)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference ) throws Exception;

    @Transactional(readOnly = true)
    private Collection<BibliographicReferenceValueObject> loadMultipleValueObjectsFromObjects(
            Collection<BibliographicReference> bibRefs ) {
        if ( bibRefs.isEmpty() ) {
            return new ArrayList<>();
        }
        Map<Long, BibliographicReferenceValueObject> idTobibRefVO = new HashMap<>();

        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( bibref );
            idTobibRefVO.put( bibref.getId(), vo );
        }

        this.populateRelatedExperiments( bibRefs, idTobibRefVO );
        this.populateBibliographicPhenotypes( idTobibRefVO );

        return idTobibRefVO.values();
    }

    void populateBibliographicPhenotypes( BibliographicReferenceValueObject bibRefVO ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenotypeAssociationService
                .findPhenotypesForBibliographicReference( bibRefVO.getPubAccession() );
        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );
        bibRefVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );
    }

    private void populateBibliographicPhenotypes( Map<Long, BibliographicReferenceValueObject> idTobibRefVO ) {

        for ( BibliographicReferenceValueObject vo : idTobibRefVO.values() ) {
            this.populateBibliographicPhenotypes( vo );
        }
    }

    void populateRelatedExperiments( BibliographicReference bibRef, BibliographicReferenceValueObject bibRefVO ) {
        Collection<ExpressionExperiment> relatedExperiments = this.getRelatedExperiments( bibRef );
        if ( relatedExperiments.isEmpty() ) {
            bibRefVO.setExperiments( new ArrayList<ExpressionExperimentValueObject>() );
        } else {
            bibRefVO.setExperiments( ExpressionExperimentValueObject.convert2ValueObjects( relatedExperiments ) );
        }

    }

    private void populateRelatedExperiments( Collection<BibliographicReference> bibRefs,
            Map<Long, BibliographicReferenceValueObject> idTobibRefVO ) {
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this
                .getRelatedExperiments( bibRefs );
        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = idTobibRefVO.get( bibref.getId() );
            if ( relatedExperiments.containsKey( bibref ) ) {
                vo.setExperiments(
                        ExpressionExperimentValueObject.convert2ValueObjects( relatedExperiments.get( bibref ) ) );
            }
        }
    }

}