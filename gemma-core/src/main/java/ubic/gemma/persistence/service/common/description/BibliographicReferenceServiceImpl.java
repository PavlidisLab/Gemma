/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.common.description;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.*;

/**
 * Implementation of BibliographicReferenceService.
 * Note: This is only in Core because it uses SearchService, but it could be refactored.
 *
 * @author keshav
 * @see    BibliographicReferenceService
 */
@Service
@ParametersAreNonnullByDefault
public class BibliographicReferenceServiceImpl
        extends AbstractVoEnabledService<BibliographicReference, BibliographicReferenceValueObject>
        implements BibliographicReferenceService {

    private static final String PUB_MED_DATABASE_NAME = "PubMed";
    private final PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();
    private final BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private SearchService searchService;

    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    public BibliographicReferenceServiceImpl( BibliographicReferenceDao bibliographicReferenceDao ) {
        super( bibliographicReferenceDao );
        this.bibliographicReferenceDao = bibliographicReferenceDao;
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject loadValueObject( BibliographicReference entity ) {
        return this.loadMultipleValueObjectsFromObjects( Collections.singleton( entity ) ).iterator().next();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> loadAllValueObjects() {
        return this.loadMultipleValueObjectsFromObjects( this.loadAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( int start, int limit ) {
        return this.bibliographicReferenceDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending ) {
        return this.bibliographicReferenceDao.browse( start, limit, orderField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.bibliographicReferenceDao.findByExternalId( accession );
    }

    @Override
    @Transactional
    public BibliographicReference findByExternalId( String id ) {

        return this.bibliographicReferenceDao
                .findByExternalId( id, BibliographicReferenceServiceImpl.PUB_MED_DATABASE_NAME );

    }

    @Override
    @Transactional
    public BibliographicReference findByExternalId( String id, String databaseName ) {

        return this.bibliographicReferenceDao.findByExternalId( id, databaseName );
    }

    /**
     * @see BibliographicReferenceService#findVOByExternalId(String)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findVOByExternalId( final String id ) {
        try {
            BibliographicReference bibref = this.findByExternalId( id );
            if ( bibref == null ) {
                return null;
            }
            BibliographicReferenceValueObject bibrefVO = new BibliographicReferenceValueObject( bibref );
            this.populateBibliographicPhenotypes( bibrefVO );
            this.populateRelatedExperiments( bibref, bibrefVO );
            return bibrefVO;
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(String id)' --> " + th, th );
        }
    }

    @Override
    @Transactional
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        return this.bibliographicReferenceDao.getAllExperimentLinkedReferences();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getRelatedExperiments( BibliographicReference bibRef ) {
        try {
            Collection<BibliographicReference> records = new ArrayList<>();
            records.add( bibRef );
            Map<BibliographicReference, Collection<ExpressionExperiment>> map = this.bibliographicReferenceDao
                    .getRelatedExperiments( records );
            if ( map.containsKey( bibRef ) ) {
                return map.get( bibRef );
            }
            return new ArrayList<>();
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BibliographicReferenceService.getRelatedExperiments(BibliographicReference bibliographicReference)' --> "
                            + th,
                    th );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return this.bibliographicReferenceDao.getRelatedExperiments( records );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> listAll() {
        return bibliographicReferenceDao.listAll();
    }

    @Override
    @Transactional
    public BibliographicReference refresh( String pubMedId ) {
        if ( StringUtils.isBlank( pubMedId ) ) {
            throw new IllegalArgumentException( "Must provide a pubmed ID" );
        }

        BibliographicReference existingBibRef = this
                .findByExternalId( pubMedId, BibliographicReferenceServiceImpl.PUB_MED_DATABASE_NAME );

        if ( existingBibRef == null ) {
            return null;
        }

        existingBibRef = this.thaw( existingBibRef );

        String oldAccession = existingBibRef.getPubAccession().getAccession();

        if ( StringUtils.isNotBlank( oldAccession ) && !oldAccession.equals( pubMedId ) ) {
            throw new IllegalArgumentException(
                    "The pubmed accession is already set and doesn't match the one provided" );
        }

        existingBibRef.getPubAccession().setAccession( pubMedId );
        BibliographicReference fresh;
        try {
            fresh = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
        } catch ( IOException e ) {
            throw new IllegalStateException( "Unable to retrieve record from pubmed for id=" + pubMedId, e );
        }

        if ( fresh == null || fresh.getPublicationDate() == null ) {
            throw new IllegalStateException( "Unable to retrieve record from pubmed for id=" + pubMedId );
        }

        assert fresh.getPubAccession().getAccession().equals( pubMedId );

        existingBibRef.setPublicationDate( fresh.getPublicationDate() );
        existingBibRef.setAuthorList( fresh.getAuthorList() );
        existingBibRef.setAbstractText( fresh.getAbstractText() );
        existingBibRef.setIssue( fresh.getIssue() );
        existingBibRef.setTitle( fresh.getTitle() );
        existingBibRef.setFullTextUri( fresh.getFullTextUri() );
        existingBibRef.setEditor( fresh.getEditor() );
        existingBibRef.setPublisher( fresh.getPublisher() );
        existingBibRef.setCitation( fresh.getCitation() );
        existingBibRef.setPublication( fresh.getPublication() );
        existingBibRef.setMeshTerms( fresh.getMeshTerms() );
        existingBibRef.setChemicals( fresh.getChemicals() );
        existingBibRef.setKeywords( fresh.getKeywords() );
        existingBibRef.setPages( fresh.getPages() );
        existingBibRef.setVolume( fresh.getVolume() );

        this.update( existingBibRef );

        return existingBibRef;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) throws SearchException {
        SearchSettings ss = SearchSettings.bibliographicReferenceSearch( settings.getQuery() );

        List<SearchResult<BibliographicReference>> resultEntities = searchService.search( ss )
                .getByResultObjectType( BibliographicReference.class );

        List<BibliographicReferenceValueObject> results = new ArrayList<>();

        // only return associations with the selected entity types.
        for ( SearchResult<BibliographicReference> sr : resultEntities ) {
            BibliographicReference entity = sr.getResultObject();
            if ( entity == null )
                continue; // might be a compass hit that is no longer valid
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );

            if ( settings.getSearchPhenotypes() || settings.getSearchBibrefs() ) {
                this.populateBibliographicPhenotypes( vo );
                if ( !vo.getBibliographicPhenotypes().isEmpty() || settings.getSearchBibrefs() ) {
                    results.add( vo );
                }
            }

            if ( settings.getSearchExperiments() || settings.getSearchBibrefs() ) {
                this.populateRelatedExperiments( entity, vo );
                if ( !vo.getExperiments().isEmpty() || settings.getSearchBibrefs() ) {
                    results.add( vo );
                }
            }

            if ( settings.getSearchBibrefs() && !settings.getSearchPhenotypes() && !settings.getSearchExperiments() ) {
                results.add( vo );
            }

        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( String query ) throws SearchException {
        List<SearchResult<BibliographicReference>> resultEntities = searchService
                .search( SearchSettings.bibliographicReferenceSearch( query ) )
                .getByResultObjectType( BibliographicReference.class );
        List<BibliographicReferenceValueObject> results = new ArrayList<>();
        for ( SearchResult<BibliographicReference> sr : resultEntities ) {
            BibliographicReference entity = sr.getResultObject();
            if ( entity == null ) {
                continue;
            }
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );
            this.populateBibliographicPhenotypes( vo );
            this.populateRelatedExperiments( entity, vo );
            results.add( vo );
        }

        return results;

    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return this.bibliographicReferenceDao.thaw( bibliographicReference );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return this.bibliographicReferenceDao.thaw( bibliographicReferences );
    }

    private List<BibliographicReferenceValueObject> loadMultipleValueObjectsFromObjects(
            Collection<BibliographicReference> bibRefs ) {
        if ( bibRefs.isEmpty() ) {
            return Collections.emptyList();
        }
        Map<Long, BibliographicReferenceValueObject> idToBibRefVO = new HashMap<>();

        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( bibref );
            idToBibRefVO.put( bibref.getId(), vo );
        }

        this.populateRelatedExperiments( bibRefs, idToBibRefVO );
        this.populateBibliographicPhenotypes( idToBibRefVO );

        return new ArrayList<>( idToBibRefVO.values() );
    }

    private void populateBibliographicPhenotypes( BibliographicReferenceValueObject bibRefVO ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenotypeAssociationService
                .findPhenotypesForBibliographicReference( bibRefVO.getPubAccession() );
        Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );
        bibRefVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );
    }

    private void populateBibliographicPhenotypes( Map<Long, BibliographicReferenceValueObject> idToBibRefVO ) {

        for ( BibliographicReferenceValueObject vo : idToBibRefVO.values() ) {
            this.populateBibliographicPhenotypes( vo );
        }
    }

    private void populateRelatedExperiments( BibliographicReference bibRef,
            BibliographicReferenceValueObject bibRefVO ) {
        Collection<ExpressionExperiment> relatedExperiments = this.getRelatedExperiments( bibRef );
        if ( relatedExperiments.isEmpty() ) {
            bibRefVO.setExperiments( new ArrayList<ExpressionExperimentValueObject>() );
        } else {
            bibRefVO.setExperiments( expressionExperimentService.loadValueObjects( relatedExperiments ) );
        }

    }

    private void populateRelatedExperiments( Collection<BibliographicReference> bibRefs,
            Map<Long, BibliographicReferenceValueObject> idToBibRefVO ) {
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this
                .getRelatedExperiments( bibRefs );
        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = idToBibRefVO.get( bibref.getId() );
            if ( relatedExperiments.containsKey( bibref ) ) {
                vo.setExperiments( expressionExperimentService.loadValueObjects( relatedExperiments.get( bibref ) ) );
            }
        }
    }

}