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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="bibliographicReferenceController" name="/bibRefList.html"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="methodNameResolver" ref="bibRefActions"
 * @spring.property name="pubMedXmlFetcher" ref="pubMedXmlFetcher"
 */
public class BibliographicReferenceController extends BaseMultiActionController {
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService = null;
    private PersisterHelper persisterHelper;
    private final String messagePrefix = "Reference with PubMed Id";
    private PubMedXMLFetcher pubMedXmlFetcher;

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" );

        // FIXME: allow use of the primary key as well.

        if ( pubMedId == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId
                        + ", either in Gemma or at NCBI" );
            }
        }

        addMessage( request, "object.found", new Object[] { messagePrefix, pubMedId } );
        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReference", bibRef ).addObject(
                "existsInSystem", Boolean.TRUE );
    }

    /**
     * For AJAX calls.
     * 
     * @param id
     */
    public void update( Long id ) {
        BibliographicReference bibRef = bibliographicReferenceService.load( id );
        if ( id == null ) {
            throw new EntityNotFoundException( "Could not locate reference with that id" );
        }

        String pubMedId = bibRef.getPubAccession().getAccession();
        BibliographicReference fresh = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        bibRef.setMeshTerms( fresh.getMeshTerms() );
        bibRef.setChemicals( fresh.getChemicals() );
        bibRef.setKeywords( fresh.getKeywords() );

        bibliographicReferenceService.update( bibRef );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public ModelAndView add( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "acc" ); // FIXME: allow use of the primary key as well.

        if ( pubMedId == null ) {
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
            if ( bibRef == null ) {
                throw new EntityNotFoundException( "Could not locate reference with pubmed id=" + pubMedId );
            }
            bibRef = ( BibliographicReference ) persisterHelper.persist( bibRef );
        }

        saveMessage( request, "Added " + pubMedId + " to the system." );
        return new ModelAndView( "bibRefView" ).addObject( "bibliographicReference", bibRef ).addObject(
                "existsInSystem", Boolean.TRUE );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response ) {
        Collection<BibliographicReference> allExperimentLinkedReferences = bibliographicReferenceService
                .getAllExperimentLinkedReferences();

        // FIXME this loop is slow; instead just put a link to the service method to get the experiments.
        Collection<BibliographicReferenceValueObject> vos = new HashSet<BibliographicReferenceValueObject>();
        for ( BibliographicReference b : allExperimentLinkedReferences ) {
            Collection<ExpressionExperiment> ees = bibliographicReferenceService.getRelatedExperiments( b );
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( b );
            vo.setExperiments( ees );
            vos.add( vo );
        }

        return new ModelAndView( "bibRefList" ).addObject( "bibliographicReferences", vos );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings( { "unused", "unchecked" })
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

    /**
     * Error handler for 'not found' condition.
     * 
     * @param request
     * @param response
     * @param error
     * @return
     */
    @SuppressWarnings( { "unchecked", "unused" })
    public ModelAndView notFoundError( HttpServletRequest request, HttpServletResponse response,
            EntityNotFoundException error ) {
        List<String> errors = ( List<String> ) request.getAttribute( "errors" );
        if ( errors == null ) {
            errors = new ArrayList<String>();
        }
        errors.add( error.getMessage() );
        request.setAttribute( "errors", errors );
        saveMessage( request, error.getMessage() );
        return new ModelAndView( "bibRefView", "bibliographicReference", null );
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
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.multiaction.MultiActionController#newCommandObject(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object newCommandObject( Class clazz ) throws Exception {
        if ( clazz.isAssignableFrom( BibliographicReferenceImpl.class ) ) {
            BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
            bibRef.setPubAccession( DatabaseEntry.Factory.newInstance() );
            return bibRef;
        }
        return super.newCommandObject( clazz );
    }

    /**
     * @param pubMedXmlFetcher The pubMedXmlFetcher to set.
     */
    public void setPubMedXmlFetcher( PubMedXMLFetcher pubMedXmlFetcher ) {
        this.pubMedXmlFetcher = pubMedXmlFetcher;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

}
