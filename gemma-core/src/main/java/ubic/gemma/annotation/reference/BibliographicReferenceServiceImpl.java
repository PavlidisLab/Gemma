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
package ubic.gemma.annotation.reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.search.SearchService;

/**
 * Implementation of BibliographicReferenceService.
 * <p>
 * Note: This is only in Core because it uses SearchService, but it could be refactored.
 * 
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.annotation.reference.BibliographicReferenceService
 */
@Service
public class BibliographicReferenceServiceImpl extends BibliographicReferenceServiceBase {

    private static final String PUB_MED_DATABASE_NAME = "PubMed";
    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    @Autowired
    private SearchService searchService;

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceService#browse(java.lang.Integer, java.lang.Integer)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( Integer start, Integer limit ) {
        return this.getBibliographicReferenceDao().browse( start, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceService#browse(java.lang.Integer, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.getBibliographicReferenceDao().browse( start, limit, orderField, descending );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceService#count()
     */
    @Override
    @Transactional(readOnly = true)
    public Integer count() {
        return this.getBibliographicReferenceDao().count();
    }

    /**
     * 
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.getBibliographicReferenceDao().findByExternalId( accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceService#getRelatedExperiments(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getRelatedExperiments( BibliographicReference bibRef ) {
        Collection<BibliographicReference> records = new ArrayList<BibliographicReference>();
        records.add( bibRef );
        Map<BibliographicReference, Collection<ExpressionExperiment>> map = this.getBibliographicReferenceDao()
                .getRelatedExperiments( records );
        if ( map.containsKey( bibRef ) ) {
            return map.get( bibRef );
        }
        return new ArrayList<ExpressionExperiment>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceService#getRelatedExperiments(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return this.getBibliographicReferenceDao().getRelatedExperiments( records );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> listAll() {
        return getBibliographicReferenceDao().listAll();
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
            throw new IllegalStateException( "Unable to retrive record from pubmed for id=" + pubMedId );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#search(ubic.gemma.model.common.search.
     * SearchSettingsValueObject)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) {
        SearchSettings ss = SearchSettingsImpl.bibliographicReferenceSearch( settings.getQuery() );

        List<BibliographicReference> resultEntities = ( List<BibliographicReference> ) searchService.search( ss,
                BibliographicReference.class );

        List<BibliographicReferenceValueObject> results = new ArrayList<>();

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

    /*
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#search(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( String query ) {
        List<BibliographicReference> resultEntities = ( List<BibliographicReference> ) searchService.search(
                SearchSettingsImpl.bibliographicReferenceSearch( query ), BibliographicReference.class );
        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();
        for ( BibliographicReference entity : resultEntities ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );
            this.populateBibliographicPhenotypes( vo );
            this.populateRelatedExperiments( entity, vo );
            results.add( vo );
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @seeBibliographicReferenceService#thaw( BibliographicReference)
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * 
     * /* (non-Javadoc)
     * 
     * @see BibliographicReferenceService#thaw(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReferences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceServiceBase#handleAddDocument(byte[], BibliographicReference)
     */
    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) {
        bibliographicReference.setFullTextPdf( pdfFile );
        this.getBibliographicReferenceDao().update( bibliographicReference );

    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#saveBibliographicReference(BibliographicReference)
     */
    @Override
    protected BibliographicReference handleCreate( BibliographicReference bibliographicReference ) {
        return getBibliographicReferenceDao().create( bibliographicReference );
    }

    /**
     * Check to see if the reference already exists
     * 
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#alreadyExists(BibliographicReference)
     */
    @Override
    protected BibliographicReference handleFind( BibliographicReference bibliographicReference ) {

        return getBibliographicReferenceDao().find( bibliographicReference );
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    @Override
    protected BibliographicReference handleFindByExternalId( java.lang.String id ) {

        return this.getBibliographicReferenceDao().findByExternalId( id, PUB_MED_DATABASE_NAME );

    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected BibliographicReference handleFindByExternalId( java.lang.String id, java.lang.String databaseName ) {

        return this.getBibliographicReferenceDao().findByExternalId( id, databaseName );
    }

    @Override
    protected BibliographicReference handleFindOrCreate( BibliographicReference bibliographicReference ) {
        return this.getBibliographicReferenceDao().findOrCreate( bibliographicReference );
    }

    @Override
    protected Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences() {
        return this.getBibliographicReferenceDao().getAllExperimentLinkedReferences();
    }

    @Override
    protected Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference ) {
        return this.getBibliographicReferenceDao().getRelatedExperiments( bibliographicReference );
    }

    @Override
    protected BibliographicReference handleLoad( Long id ) {
        return this.getBibliographicReferenceDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang .Long)
     */
    protected BibliographicReference handleLoadBibliographicReference( Long id ) {
        return getBibliographicReferenceDao().load( id );
    }

    @Override
    protected Collection<BibliographicReference> handleLoadMultiple( Collection<Long> ids ) {
        return this.getBibliographicReferenceDao().load( ids );
    }

    @Override
    protected Collection<BibliographicReferenceValueObject> handleLoadMultipleValueObjects( Collection<Long> ids ) {
        Collection<BibliographicReference> bibRefs = this.getBibliographicReferenceDao().load( ids );
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

    @Override
    protected void handleRemove( BibliographicReference bibliographicReference ) {
        this.getBibliographicReferenceDao().remove( bibliographicReference );
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#saveBibliographicReference(BibliographicReference)
     */
    @Override
    protected void handleUpdate( BibliographicReference BibliographicReference ) {
        getBibliographicReferenceDao().update( BibliographicReference );
    }

}