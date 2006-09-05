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
package ubic.gemma.web.controller.common.description;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceImpl;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list.
 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="bibliographicReferenceController" name="/bibRefList.html"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="methodNameResolver" ref="bibRefActions"
 */
public class BibliographicReferenceController extends BaseMultiActionController {
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService = null;

    private final String messagePrefix = "Reference with PubMed Id";

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" );

        if ( pubMedId == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            throw new EntityNotFoundException( pubMedId + " not found" );
        }

        addMessage( request, "object.found", new Object[] { messagePrefix, pubMedId } );
        request.setAttribute( "accession", pubMedId );
        return new ModelAndView( "bibliogrpahicReferenceView" ).addObject( "bibliographicReference", bibRef );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "pubMed.GetAll.results.view" ).addObject( "bibliographicReferences",
                bibliographicReferenceService.getAll() );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String pubMedId = request.getParameter( "accession" );

        if ( pubMedId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide a PubMed Id" );
        }

        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( bibRef == null ) {
            throw new EntityNotFoundException( pubMedId + " not found" );
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
        return new ModelAndView( "bibRefSearch", "bibliographicReference", null );
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
        return new ModelAndView( "bibRefSearch", "bibliographicReference", bibRef );
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

}
