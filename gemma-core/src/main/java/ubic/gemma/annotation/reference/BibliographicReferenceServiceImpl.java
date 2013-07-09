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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class BibliographicReferenceServiceImpl extends
        ubic.gemma.annotation.reference.BibliographicReferenceServiceBase {

    private static final String PUB_MED_DATABASE_NAME = "PubMed";
    @Autowired
    private SearchService searchService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Integer,
     * java.lang.Integer)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit ) {
        return this.getBibliographicReferenceDao().browse( start, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Integer,
     * java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.getBibliographicReferenceDao().browse( start, limit, orderField, descending );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#count()
     */
    @Override
    public Integer count() {
        return this.getBibliographicReferenceDao().count();
    }

    /**
     * 
     */
    @Override
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.getBibliographicReferenceDao().findByExternalId( accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceService#getRelatedExperiments(java.util.Collection)
     */
    @Override
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
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceService#getRelatedExperiments(java.util.Collection)
     */
    @Override
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return this.getBibliographicReferenceDao().getRelatedExperiments( records );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#search(ubic.gemma.model.common.search.
     * SearchSettingsValueObject)
     */
    @Override
    public List<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) {
        SearchSettings ss = SearchSettingsImpl.bibliographicReferenceSearch( settings.getQuery() );

        List<BibliographicReference> resultEntities = ( List<BibliographicReference> ) searchService.search( ss,
                BibliographicReference.class );

        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();

        // only return associations with the selected entity types.
        for ( BibliographicReference entity : resultEntities ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );
            if ( settings.getSearchPhenotypes() ) {
                this.populateBibliographicPhenotypes( vo );
                if ( !vo.getBibliographicPhenotypes().isEmpty() ) {
                    results.add( vo );
                }
            }
            if ( settings.getSearchExperiments() ) {
                this.populateRelatedExperiments( entity, vo );
                if ( !vo.getExperiments().isEmpty() ) {
                    results.add( vo );
                }
            }
        }

        return results;
    }

    /*
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#search(java.lang.String)
     */
    @Override
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
     * @seeubic.gemma.model.common.description.BibliographicReferenceService#thaw(ubic.gemma.model.common.description.
     * BibliographicReference)
     */
    @Override
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * 
     * /* (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#thaw(java.util.Collection)
     */
    @Override
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReferences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleAddDocument(byte[],
     * ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) {
        bibliographicReference.setFullTextPdf( pdfFile );
        this.getBibliographicReferenceDao().update( bibliographicReference );

    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected BibliographicReference handleCreate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        return getBibliographicReferenceDao().create( bibliographicReference );
    }

    /**
     * Check to see if the reference already exists
     * 
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#alreadyExists(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected BibliographicReference handleFind(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {

        return getBibliographicReferenceDao().find( bibliographicReference );
    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId( java.lang.String id ) {

        return this.getBibliographicReferenceDao().findByExternalId( id, PUB_MED_DATABASE_NAME );

    }

    /**
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId( java.lang.String id,
            java.lang.String databaseName ) {

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
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang
     * .Long)
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
     * @see ubic.gemma.annotation.reference.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.description.BibliographicReference BibliographicReference ) {
        getBibliographicReferenceDao().update( BibliographicReference );
    }

}