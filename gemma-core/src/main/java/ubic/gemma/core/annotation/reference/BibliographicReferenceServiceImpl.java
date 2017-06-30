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
package ubic.gemma.core.annotation.reference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of BibliographicReferenceService.
 * Note: This is only in Core because it uses SearchService, but it could be refactored.
 *
 * @author keshav
 * @see BibliographicReferenceService
 */
@Service
public class BibliographicReferenceServiceImpl extends BibliographicReferenceServiceBase {

    private static final String PUB_MED_DATABASE_NAME = "PubMed";
    private final PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    @Autowired
    private SearchService searchService;

    @Autowired
    public BibliographicReferenceServiceImpl( BibliographicReferenceDao bibliographicReferenceDao,
            PhenotypeAssociationService phenotypeAssociationService ) {
        super( bibliographicReferenceDao, phenotypeAssociationService );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( Integer start, Integer limit ) {
        return this.bibliographicReferenceDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.bibliographicReferenceDao.browse( start, limit, orderField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.bibliographicReferenceDao.findByExternalId( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getRelatedExperiments( BibliographicReference bibRef ) {
        Collection<BibliographicReference> records = new ArrayList<BibliographicReference>();
        records.add( bibRef );
        Map<BibliographicReference, Collection<ExpressionExperiment>> map = this.bibliographicReferenceDao
                .getRelatedExperiments( records );
        if ( map.containsKey( bibRef ) ) {
            return map.get( bibRef );
        }
        return new ArrayList<ExpressionExperiment>();
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

        BibliographicReference existingBibRef = this.findByExternalId( pubMedId, PUB_MED_DATABASE_NAME );

        if ( existingBibRef == null ) {
            return null;
        }

        existingBibRef = thaw( existingBibRef );

        String oldAccession = existingBibRef.getPubAccession().getAccession();

        if ( StringUtils.isNotBlank( oldAccession ) && !oldAccession.equals( pubMedId ) ) {
            throw new IllegalArgumentException(
                    "The pubmed accession is already set and doesn't match the one provided" );
        }

        existingBibRef.getPubAccession().setAccession( pubMedId );
        BibliographicReference fresh = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

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

        update( existingBibRef );

        return existingBibRef;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) {
        SearchSettings ss = SearchSettingsImpl.bibliographicReferenceSearch( settings.getQuery() );

        //noinspection unchecked
        List<BibliographicReference> resultEntities = ( List<BibliographicReference> ) searchService
                .search( ss, BibliographicReference.class );

        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();

        // only return associations with the selected entity types.
        for ( BibliographicReference entity : resultEntities ) {
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
    public List<BibliographicReferenceValueObject> search( String query ) {
        //noinspection unchecked
        List<BibliographicReference> resultEntities = ( List<BibliographicReference> ) searchService
                .search( SearchSettingsImpl.bibliographicReferenceSearch( query ), BibliographicReference.class );
        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();
        for ( BibliographicReference entity : resultEntities ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );
            this.populateBibliographicPhenotypes( vo );
            this.populateRelatedExperiments( entity, vo );
            results.add( vo );
        }

        return results;

    }

    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) {
        bibliographicReference.setFullTextPdf( pdfFile );
        this.bibliographicReferenceDao.update( bibliographicReference );

    }

    /**
     * @see BibliographicReferenceService#findByExternalId(String)
     */
    @Override
    protected BibliographicReference handleFindByExternalId( String id ) {

        return this.bibliographicReferenceDao.findByExternalId( id, PUB_MED_DATABASE_NAME );

    }

    /**
     * @see BibliographicReferenceService#findByExternalId(String, String)
     */
    @Override
    protected BibliographicReference handleFindByExternalId( String id, String databaseName ) {

        return this.bibliographicReferenceDao.findByExternalId( id, databaseName );
    }

    @Override
    protected Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences() {
        return this.bibliographicReferenceDao.getAllExperimentLinkedReferences();
    }

    @Override
    protected Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference ) {
        return this.bibliographicReferenceDao.getRelatedExperiments( bibliographicReference );
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

}