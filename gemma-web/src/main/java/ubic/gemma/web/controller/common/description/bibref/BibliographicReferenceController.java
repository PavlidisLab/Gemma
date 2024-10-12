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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list.
 *
 * @author keshav
 */
@Controller
public class BibliographicReferenceController extends BaseController {

    private static final String messagePrefix = "Reference with PubMed Id";

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService = null;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    private PhenotypeAssociationService phenotypeAssociationService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private PubMedXMLFetcher pubMedXmlFetcher;

    @RequestMapping(value = "/bibRefAdd.html", method = RequestMethod.POST)
    public ModelAndView add( @RequestParam("accession") String pubMedId, HttpServletRequest request ) {
        // FIXME: allow use of the primary key as well.

        if ( StringUtils.isBlank( pubMedId ) ) {
            throw new IllegalArgumentException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        BibliographicReferenceValueObject vo;
        if ( bibRef == null ) {
            try {
                bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to retrieve publication with PubMed ID: " + pubMedId, e );
            }
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }
            vo = new BibliographicReferenceValueObject( ( BibliographicReference ) persisterHelper.persist( bibRef ) );
            this.saveMessage( request, "Added " + pubMedId + " to the system." );
        } else if ( StringUtils.isNotBlank( request.getParameter( "refresh" ) ) ) {
            vo = this.update( pubMedId );
            this.saveMessage( request, "Updated record for pubmed id " + pubMedId );
        } else {
            throw new IllegalArgumentException( "Action not understood" );
        }

        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReferenceId", vo.getId() )
                .addObject( "existsInSystem", Boolean.TRUE ).addObject( "bibliographicReference", vo );
    }

    @SuppressWarnings("unused")
    public JsonReaderResponse<BibliographicReferenceValueObject> browse( ListBatchCommand batch ) {

        long count = this.bibliographicReferenceService.countAll();
        List<BibliographicReference> records = this.getBatch( batch );
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this.bibliographicReferenceService
                .getRelatedExperiments( records );

        List<BibliographicReferenceValueObject> valueObjects = new ArrayList<>();

        for ( BibliographicReference ref : records ) {

            ref = this.bibliographicReferenceService.thaw( ref );
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( ref );

            if ( relatedExperiments.containsKey( ref ) ) {
                vo.setExperiments( expressionExperimentService.loadValueObjects( relatedExperiments.get( ref ) ) );
            }
            valueObjects.add( vo );

            // adding phenotype information to the Bibliographic Reference

            Collection<PhenotypeAssociation> phenotypeAssociations = this.phenotypeAssociationService
                    .findPhenotypesForBibliographicReference( vo.getPubAccession() );

            Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                    .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );
            vo.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );

        }

        return new JsonReaderResponse<>( valueObjects, ( int ) count );
    }

    @RequestMapping(value = "/deleteBibRef.html", method = RequestMethod.POST)
    public ModelAndView delete( @RequestParam("acc") String pubMedId, HttpServletRequest request ) {
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            String message = "There is no reference with PubMed ID " + pubMedId + " in the system any more.";
            this.saveMessage( request, message );
            throw new EntityNotFoundException( message );
        }
        bibliographicReferenceService.remove( bibRef );
        log.info( "Bibliographic reference with pubMedId: " + bibRef.getPubAccession().getAccession() + " deleted" );
        this.addMessage( request, "object.deleted",
                new Object[] { messagePrefix, bibRef.getPubAccession().getAccession() } );
        return new ModelAndView( "bibRefView", "bibliographicReference", bibRef );
    }

    public BibliographicReferenceValueObject load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ID cannot be null" );
        }
        Collection<Long> ids = new ArrayList<>();
        ids.add( id );
        JsonReaderResponse<BibliographicReferenceValueObject> returnVal = this.loadMultiple( ids );
        if ( returnVal.getRecords() != null && !returnVal.getRecords().isEmpty() ) {
            return returnVal.getRecords().iterator().next();
        }
        throw new EntityNotFoundException( "Error retrieving bibliographic reference for id = " + id );

    }

    @SuppressWarnings("unused")
    public Collection<BibliographicReferenceValueObject> loadAllForExperiments() {
        Map<ExpressionExperiment, BibliographicReference> eeToBibRefs = bibliographicReferenceService
                .getAllExperimentLinkedReferences();
        Collection<BibliographicReference> bibRefs = eeToBibRefs.values();

        return BibliographicReferenceValueObject.convert2ValueObjects( bibRefs );
    }

    @SuppressWarnings("unused")
    public BibliographicReferenceValueObject loadFromPubmedID( String pubMedID ) {
        return bibliographicReferenceService.findVOByExternalId( pubMedID );
    }

    public JsonReaderResponse<BibliographicReferenceValueObject> loadMultiple( Collection<Long> ids ) {

        Collection<BibliographicReference> bss = bibliographicReferenceService.load( ids );
        bss = bibliographicReferenceService.thaw( bss );
        Collection<BibliographicReferenceValueObject> bibRefs = bibliographicReferenceService.loadValueObjects( bss );

        return new JsonReaderResponse<>( new ArrayList<>( bibRefs ), bibRefs.size() );
    }

    public JsonReaderResponse<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) {
        try {
            List<BibliographicReferenceValueObject> vos = bibliographicReferenceService.search( settings );
            return new JsonReaderResponse<>( vos, vos.size() );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
    }

    @RequestMapping(value = "/searchBibRefs.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView searchBibRefs() {
        return new ModelAndView( "bibRefList" );
    }

    @RequestMapping(value = "/bibRefView.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = { "id" })
    public ModelAndView showById( @RequestParam(value = "id") Long id, HttpServletRequest request ) {
        BibliographicReference bibRef = bibliographicReferenceService.loadOrFail( id, EntityNotFoundException::new );
        return show( bibRef, request, true )
                .addObject( "byAccession", Boolean.FALSE );
    }

    @RequestMapping(value = "/bibRefView.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = { "accession" })
    public ModelAndView showByAccession( @RequestParam(value = "accession") String accession, HttpServletRequest request ) {
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( accession, ExternalDatabases.PUBMED );
        boolean existsInSystem = bibRef != null;
        if ( bibRef == null ) {
            // attempt to fetch it from PubMed
            try {
                bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( accession ) );
            } catch ( NumberFormatException e ) {
                // ignore, this will be treated as an EntityNotFoundException
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to retrieve publication with PubMed ID: " + accession, e );
            }
        }
        if ( bibRef == null ) {
            throw new EntityNotFoundException( "Could not locate reference with PubMwd ID " + accession + ", either in Gemma or by querying NCBI." );
        }
        return show( bibRef, request, existsInSystem )
                .addObject( "byAccession", Boolean.TRUE )
                .addObject( "accession", accession );
    }

    private ModelAndView show( BibliographicReference bibRef, HttpServletRequest request, boolean existsInSystem ) {
        this.addMessage( request, "object.found", new Object[] { messagePrefix, } );
        return new ModelAndView( "bibRefView" )
                .addObject( "bibliographicReferenceId", bibRef.getId() )
                .addObject( "existsInSystem", existsInSystem )
                .addObject( "incompleteEntry", bibRef.getPublicationDate() == null );
    }

    @RequestMapping(value = "/showAllEeBibRefs.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showAllForExperiments() {
        Map<ExpressionExperiment, BibliographicReference> eeToBibRefs = bibliographicReferenceService
                .getAllExperimentLinkedReferences();

        // map sorted in natural order of the keys
        SortedMap<CitationValueObject, Collection<ExpressionExperimentValueObject>> citationToEEs = new TreeMap<>();
        for ( Entry<ExpressionExperiment, BibliographicReference> entry : eeToBibRefs.entrySet() ) {
            if ( entry.getValue().getTitle() == null || entry.getValue().getTitle().isEmpty()
                    || entry.getValue().getAuthorList() == null || entry.getValue().getAuthorList().isEmpty() ) {
                continue;
            }
            CitationValueObject cvo = CitationValueObject.convert2CitationValueObject( entry.getValue() );
            if ( !citationToEEs.containsKey( cvo ) ) {
                citationToEEs.put( cvo, new ArrayList<>() );
            }
            citationToEEs.get( cvo ).add( new ExpressionExperimentValueObject( entry.getKey(), true, true ) );
        }

        return new ModelAndView( "bibRefAllExperiments" ).addObject( "citationToEEs", citationToEEs );
    }

    public BibliographicReferenceValueObject update( String pubMedId ) {
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            throw new EntityNotFoundException( "Could not locate reference with that id" );
        }
        return new BibliographicReferenceValueObject( this.bibliographicReferenceService.refresh( pubMedId ) );
    }

    private List<BibliographicReference> getBatch( ListBatchCommand batch ) {
        List<BibliographicReference> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();

            String orderBy;
            switch ( o ) {
                case "title":
                    orderBy = "title";
                    break;
                case "publicationDate":
                    orderBy = "publicationDate";
                    break;
                case "publication":
                    orderBy = "publication";
                    break;
                case "authorList":
                    orderBy = "authorList";
                    break;
                default:
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
