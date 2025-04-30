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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.util.EntityNotFoundException;

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
public class BibliographicReferenceController extends BaseController implements InitializingBean {

    private static final String messagePrefix = "Reference with PubMed Id";

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService = null;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private PubMedSearch pubMedXmlFetcher;

    @Override
    public void afterPropertiesSet() {
        pubMedXmlFetcher = new PubMedSearch( ncbiApiKey );
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

        return new ModelAndView( "bibRefAllExperiments" )
                .addObject( "citationToEEs", citationToEEs );
    }

    @RequestMapping(value = "/searchBibRefs.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView searchBibRefs() {
        return new ModelAndView( "bibRefList" );
    }

    @RequestMapping(value = "/bibRefView.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = { "id" })
    public ModelAndView showById( @RequestParam(value = "id") Long id ) {
        BibliographicReference bibRef = bibliographicReferenceService.loadOrFail( id, EntityNotFoundException::new );
        return new ModelAndView( "bibRefView" )
                .addObject( "bibliographicReference", bibRef );
    }

    @RequestMapping(value = "/bibRefView.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = { "accession" })
    public ModelAndView showByAccession( @RequestParam(value = "accession") String accession ) {
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( accession, ExternalDatabases.PUBMED );
        if ( bibRef == null ) {
            // attempt to fetch it from PubMed
            try {
                bibRef = this.pubMedXmlFetcher.fetchById( Integer.parseInt( accession ) );
                return new ModelAndView( "bibRefAdd" )
                        .addObject( "bibliographicReference", bibRef );
            } catch ( NumberFormatException e ) {
                // ignore, this will be treated as an EntityNotFoundException
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to retrieve publication with PubMed ID: " + accession, e );
            }
        }
        if ( bibRef == null ) {
            throw new EntityNotFoundException( "Could not locate reference with PubMed ID " + accession + ", either in Gemma or by querying NCBI." );
        }
        return new ModelAndView( "bibRefView" )
                .addObject( "bibliographicReference", bibRef );
    }

    private boolean isIncomplete( BibliographicReference bibRef ) {
        return bibRef.getPublicationDate() == null;
    }

    @RequestMapping(value = "/bibRefAdd.html", method = RequestMethod.POST)
    public RedirectView add( @RequestParam("accession") Integer pubMedId, @RequestParam(value = "refresh", required = false) Boolean refresh ) {
        // FIXME: allow use of the primary key as well.
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( String.valueOf( pubMedId ), ExternalDatabases.PUBMED );
        if ( bibRef == null ) {
            try {
                bibRef = this.pubMedXmlFetcher.fetchById( pubMedId );
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to retrieve publication with PubMed ID " + pubMedId + ".", e );
            }
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with PubMed ID " + pubMedId + "." );
            }
            bibRef = ( BibliographicReference ) persisterHelper.persist( bibRef );
            this.messageUtil.saveMessage( "Added " + pubMedId + " to the system." );
        } else if ( refresh != null && refresh ) {
            bibRef = this.bibliographicReferenceService.refresh( String.valueOf( pubMedId ) );
            this.messageUtil.saveMessage( "Updated record for   PubMed ID " + pubMedId + "." );
        } else {
            this.messageUtil.saveMessage( "There is already a bibliographic reference with PubMed ID " + pubMedId + "." );
        }
        return new RedirectView( "/bibRef/bibRefView.html?id=" + bibRef.getId(), true );
    }

    @RequestMapping(value = "/deleteBibRef.html", method = RequestMethod.POST)
    public ModelAndView delete( @RequestParam("acc") String pubMedId ) {
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            String message = "There is no reference with PubMed ID " + pubMedId + " in the system any more.";
            this.messageUtil.saveMessage( message );
            throw new EntityNotFoundException( message );
        }
        bibliographicReferenceService.remove( bibRef );
        log.info( "Bibliographic reference with pubMedId: " + bibRef.getPubAccession().getAccession() + " deleted" );
        Object[] parameters = new Object[] { messagePrefix, bibRef.getPubAccession().getAccession() };
        messageUtil.saveMessage( "object.deleted", parameters, "??" + "object.deleted" + "??" );
        return new ModelAndView( "bibRefView", "bibliographicReference", bibRef );
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
        }

        return new JsonReaderResponse<>( valueObjects, ( int ) count );
    }

    @SuppressWarnings("unused")
    public BibliographicReferenceValueObject load( Long id ) {
        Assert.notNull( id, "ID cannot be null." );
        BibliographicReference bss = bibliographicReferenceService.loadOrFail( id, EntityNotFoundException::new );
        bss = bibliographicReferenceService.thaw( bss );
        BibliographicReferenceValueObject bibRefs = bibliographicReferenceService.loadValueObject( bss );
        return new JsonReaderResponse<>( Collections.singletonList( bibRefs ), 1 ).getRecords().iterator().next();
    }

    @SuppressWarnings("unused")
    public BibliographicReferenceValueObject loadFromPubmedID( String pubMedID ) {
        Assert.notNull( pubMedID, "PubMed ID cannot be null." );
        return bibliographicReferenceService.findVOByExternalId( pubMedID );
    }

    public JsonReaderResponse<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings ) {
        try {
            List<BibliographicReferenceValueObject> vos = bibliographicReferenceService.search( settings );
            return new JsonReaderResponse<>( vos, vos.size() );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
    }

    public BibliographicReferenceValueObject update( String pubMedId ) {
        Assert.notNull( pubMedId, "PubMed ID cannot be null." );
        return new BibliographicReferenceValueObject( bibliographicReferenceService.refresh( pubMedId ) );
    }

    private List<BibliographicReference> getBatch( ListBatchCommand batch ) {
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
            return bibliographicReferenceService.browse( batch.getStart(), batch.getLimit(), orderBy, descending );

        } else {
            return bibliographicReferenceService.browse( batch.getStart(), batch.getLimit() );
        }
    }
}
