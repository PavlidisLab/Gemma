/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.common.description.bibref;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.persistence.Persister;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list. Note: do not use parameterized collections as
 * parameters for ajax methods in this class! Type information is lost during proxy creation so DWR can't figure out
 * what type of collection the method should take. See bug 2756. Use arrays instead.
 * 
 * @author keshav
 */
@Controller
@RequestMapping("/bibRef")
public class BibliographicReferenceControllerImpl extends BaseController implements BibliographicReferenceController {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService = null;
    private final String messagePrefix = "Reference with PubMed Id";
    @Autowired
    private Persister persisterHelper;
    private PubMedXMLFetcher pubMedXmlFetcher = new PubMedXMLFetcher();

    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#add(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/bibRefAdd.html")
    public ModelAndView add( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" ); // FIXME: allow use of the primary key as well.

        if ( StringUtils.isBlank( pubMedId ) ) {
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );

        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }
            bibRef = ( BibliographicReference ) persisterHelper.persist( bibRef );
            saveMessage( request, "Added " + pubMedId + " to the system." );
        } else if ( StringUtils.isNotBlank( request.getParameter( "refresh" ) ) ) {
            bibRef = this.update( bibRef.getId(), pubMedId );
            saveMessage( request, "Updated record for pubmed id " + pubMedId );
        }

        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReferenceId", bibRef.getId() )
                .addObject( "existsInSystem", Boolean.TRUE ).addObject( "bibliographicReference", bibRef );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#browse(ubic.gemma.web.remote
     * .ListBatchCommand)
     */
    @Override
    public JsonReaderResponse<BibliographicReferenceValueObject> browse( ListBatchCommand batch ) {

        Integer count = this.bibliographicReferenceService.count();
        List<BibliographicReference> records = getBatch( batch );
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this.bibliographicReferenceService
                .getRelatedExperiments( records );

        List<BibliographicReferenceValueObject> valueObjects = new ArrayList<BibliographicReferenceValueObject>();

        for ( BibliographicReference ref : records ) {

            ref = this.bibliographicReferenceService.thaw( ref );
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( ref );

            if ( relatedExperiments.containsKey( ref ) ) {
                vo.setExperiments( ExpressionExperimentValueObject.convert2ValueObjects( relatedExperiments.get( ref ) ) );
            }
            valueObjects.add( vo );

            // adding phenotype informations to the Bibliographic Reference

            Collection<PhenotypeAssociation> phenotypeAssociations = this.phenotypeAssociationService
                    .findPhenotypesForBibliographicReference( vo.getPubAccession() );

            Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                    .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );
            vo.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );

        }

        JsonReaderResponse<BibliographicReferenceValueObject> returnVal = new JsonReaderResponse<BibliographicReferenceValueObject>(
                valueObjects, count.intValue() );
        return returnVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#delete(javax.servlet.http
     * .HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/deleteBibRef.html")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "acc" );

        if ( pubMedId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            String message = "There is no reference with accession=" + pubMedId + " in the system any more.";
            saveMessage( request, message );
            return new ModelAndView( "bibRefView" ).addObject( "errors", message );
        }

        return doDelete( request, bibRef );
    }

    @Override
    public BibliographicReferenceValueObject load( Long id ) {

        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        JsonReaderResponse<BibliographicReferenceValueObject> returnVal = this.loadMultiple( ids );
        if ( returnVal.getRecords() != null && !returnVal.getRecords().isEmpty() ) {
            return returnVal.getRecords().iterator().next();
        }
        throw new InvalidParameterException( "Error retrieving bibliographic reference for id = " + id );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#showAllForExperiments(javax
     * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public Collection<BibliographicReferenceValueObject> loadAllForExperiments() {
        Map<ExpressionExperiment, BibliographicReference> eeToBibRefs = bibliographicReferenceService
                .getAllExperimentLinkedReferences();
        Collection<BibliographicReference> bibRefs = eeToBibRefs.values();
        Collection<BibliographicReferenceValueObject> bibRefVOs = BibliographicReferenceValueObject
                .convert2ValueObjects( bibRefs );

        return bibRefVOs;
    }

    @Override
    public BibliographicReferenceValueObject loadFromPubmedID( String pubMedID ) {
        return bibliographicReferenceService.findVOByExternalId( pubMedID );
    }

    /*
     * public JsonReaderResponse<BibliographicReferenceValueObject> browseSearchResults( ListBatchCommand batch, String
     * query){
     * 
     * Collection<SearchResult> searchResults = (searchService.search( SearchSettings.bibliographicReferenceSearch(
     * query ), false)).get( BibliographicReference.class ); Collection<BibliographicReference>
     * 
     * }
     */

    @Override
    public JsonReaderResponse<BibliographicReferenceValueObject> loadMultiple( Collection<Long> ids ) {

        Collection<BibliographicReferenceValueObject> bibRefs = bibliographicReferenceService
                .loadMultipleValueObjects( ids );

        JsonReaderResponse<BibliographicReferenceValueObject> returnVal = new JsonReaderResponse<BibliographicReferenceValueObject>(
                new ArrayList<BibliographicReferenceValueObject>( bibRefs ), bibRefs.size() );
        return returnVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#search(ubic.gemma.web.remote
     * .ListBatchCommand)
     */
    @Override
    public JsonReaderResponse<BibliographicReferenceValueObject> search( String query ) {
        List<BibliographicReferenceValueObject> vos = bibliographicReferenceService.search( query );

        JsonReaderResponse<BibliographicReferenceValueObject> returnVal = new JsonReaderResponse<BibliographicReferenceValueObject>(
                vos, vos.size() );
        return returnVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#searchBibRefs(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/searchBibRefs.html")
    public ModelAndView searchBibRefs( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "bibRefList" );
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#show(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/bibRefView.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" );
        String gemmaId = request.getParameter( "id" );

        // FIXME: allow use of the primary key as well.

        if ( StringUtils.isBlank( pubMedId ) && StringUtils.isBlank( gemmaId ) ) {
            throw new EntityNotFoundException( "Must provide a gamma database id or a PubMed id" );
        }

        if ( !StringUtils.isBlank( gemmaId ) ) {
            return new ModelAndView( "bibRefView" ).addObject( "bibliographicReferenceId", gemmaId );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId
                        + ", either in Gemma or at NCBI" );
            }
        }

        // bibRef = bibliographicReferenceService.thaw( bibRef );
        // BibliographicReferenceValueObject bibRefVO = new BibliographicReferenceValueObject( bibRef );

        boolean isIncomplete = bibRef.getPublicationDate() == null;
        addMessage( request, "object.found", new Object[] { messagePrefix, pubMedId } );
        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReferenceId", bibRef.getId() )
                .addObject( "existsInSystem", Boolean.TRUE ).addObject( "incompleteEntry", isIncomplete )
                .addObject( "byAccession", Boolean.TRUE ).addObject( "accession", pubMedId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#showAllForExperiments(javax
     * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/showAllEeBibRefs.html")
    public ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response ) {
        Map<ExpressionExperiment, BibliographicReference> eeToBibRefs = bibliographicReferenceService
                .getAllExperimentLinkedReferences();

        // map sorted in natural order of the keys
        SortedMap<CitationValueObject, Collection<ExpressionExperimentValueObject>> citationToEEs = new TreeMap<CitationValueObject, Collection<ExpressionExperimentValueObject>>();
        for ( Entry<ExpressionExperiment, BibliographicReference> entry : eeToBibRefs.entrySet() ) {
            if ( entry.getValue().getTitle() == null || entry.getValue().getTitle().isEmpty()
                    || entry.getValue().getAuthorList() == null || entry.getValue().getAuthorList().isEmpty() ) {
                continue;
            }
            CitationValueObject cvo = CitationValueObject.convert2CitationValueObject( entry.getValue() );
            if ( !citationToEEs.containsKey( cvo ) ) {
                citationToEEs.put( cvo, new ArrayList<ExpressionExperimentValueObject>() );
            }
            citationToEEs.get( cvo ).add( new ExpressionExperimentValueObject( entry.getKey() ) );

        }

        return new ModelAndView( "bibRefAllExperiments" ).addObject( "citationToEEs", citationToEEs );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController#update(java.lang.Long)
     */
    @Override
    public BibliographicReference update( Long id, String pubMedId ) {
        BibliographicReference bibRef = bibliographicReferenceService.load( id );
        if ( bibRef == null ) {
            throw new EntityNotFoundException( "Could not locate reference with that id" );
        }
        bibRef = bibliographicReferenceService.thaw( bibRef );

        String oldAccession = bibRef.getPubAccession().getAccession();

        if ( StringUtils.isNotBlank( oldAccession ) && !oldAccession.equals( pubMedId ) ) {
            throw new IllegalArgumentException(
                    "The pubmed accession is already set and doesn't match the one provided" );
        }

        bibRef.getPubAccession().setAccession( pubMedId );
        BibliographicReference fresh = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        if ( fresh == null || fresh.getPublicationDate() == null ) {
            throw new IllegalStateException( "Unable to retrive record from pubmed for id=" + pubMedId );
        }

        assert fresh.getPubAccession().getAccession().equals( pubMedId );

        bibRef.setPublicationDate( fresh.getPublicationDate() );
        bibRef.setAuthorList( fresh.getAuthorList() );
        bibRef.setAbstractText( fresh.getAbstractText() );
        bibRef.setIssue( fresh.getIssue() );
        bibRef.setTitle( fresh.getTitle() );
        bibRef.setFullTextUri( fresh.getFullTextUri() );
        bibRef.setEditor( fresh.getEditor() );
        bibRef.setPublisher( fresh.getPublisher() );
        bibRef.setCitation( fresh.getCitation() );
        bibRef.setPublication( fresh.getPublication() );
        bibRef.setMeshTerms( fresh.getMeshTerms() );
        bibRef.setChemicals( fresh.getChemicals() );
        bibRef.setKeywords( fresh.getKeywords() );

        bibliographicReferenceService.update( bibRef );
        return bibRef;
    }

    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    private ModelAndView doDelete( HttpServletRequest request, BibliographicReference bibRef ) {
        bibliographicReferenceService.remove( bibRef );
        log.info( "Bibliographic reference with pubMedId: " + bibRef.getPubAccession().getAccession() + " deleted" );
        addMessage( request, "object.deleted", new Object[] { messagePrefix, bibRef.getPubAccession().getAccession() } );
        return new ModelAndView( "bibRefView", "bibliographicReference", bibRef );
    }

    /**
     * @param batch
     * @return
     */
    private List<BibliographicReference> getBatch( ListBatchCommand batch ) {
        List<BibliographicReference> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();

            String orderBy = "";
            if ( o.equals( "title" ) ) {
                orderBy = "title";
            } else if ( o.equals( "publicationDate" ) ) {
                orderBy = "publicationDate";
            } else if ( o.equals( "publication" ) ) {
                orderBy = "publication";
            } else if ( o.equals( "authorList" ) ) {
                orderBy = "authorList";
            } else {
                throw new IllegalArgumentException( "Unknown sort field: " + o );
            }

            boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );
            records = bibliographicReferenceService.browse( batch.getStart(), batch.getLimit(), orderBy, descending );

        } else {
            records = bibliographicReferenceService.browse( batch.getStart(), batch.getLimit() );
        }
        return records;
    }

}
