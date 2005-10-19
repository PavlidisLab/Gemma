/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.common.description;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * This controller is responsible for showing a list of all bibliographic references, as well sending the user to the
 * pubMed.Detail.view when they click on a specific link in that list.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="bibliographicReferenceController"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="methodNameResolver" ref="bibRefActions"
 */
public class BibliographicReferenceController extends MultiActionController {
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService = null;

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
    protected ModelAndView show( HttpServletRequest request, HttpServletResponse response, BindException errors ) {
        String pubMedId = request.getParameter( "pubMedId" );
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( request
                .getParameter( "pubMedId" ) );
        if ( bibRef == null ) {
            errors.reject( "bibliographicReference.notfound", new Object[] { pubMedId }, "Not found" );
            return new ModelAndView( "bibRefSearch", "bibliographicReference", bibRef );
        }
        return new ModelAndView( "pubMed.Detail.view", "bibliographicReference", bibRef );
    }

    @SuppressWarnings("unused")
    protected ModelAndView showAll( HttpServletRequest request, HttpServletResponse response, BindException errors ) {
        return new ModelAndView( "pubMed.GetAll.results.view", "bibliographicReferences", bibliographicReferenceService
                .getAllBibliographicReferences() );
    }

    @SuppressWarnings("unused")
    protected ModelAndView delete( HttpServletRequest request, HttpServletResponse response, BindException errors ) {
        String pubMedId = request.getParameter( "pubMedId" );
        BibliographicReference bibRef = bibliographicReferenceService.findByExternalId( request
                .getParameter( "pubMedId" ) );
        if ( bibRef == null ) {
            errors.reject( "bibliographicReference.notfound", new Object[] { pubMedId }, "Not found" );
            return new ModelAndView( "bibRefSearch", "bibliographicReference", bibRef );
        }
        return doDelete( request, bibRef );
    }

    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    // @SuppressWarnings("unused")
    private ModelAndView doDelete( HttpServletRequest request, BibliographicReference bibRef ) {
        bibliographicReferenceService.removeBibliographicReference( bibRef );
        log.info( "Bibliographic reference with pubMedId: " + bibRef.getPubAccession().getAccession() + " deleted" );

        // request.getSession().setAttribute(
        // "messages",
        // messageSource.getMessage( "bibliographicReference.deleted", new Object[] { bibRef.getPubAccession()
        // .getAccession() }, locale ) );
        return new ModelAndView( "bibRefSearch", "bibliographicReference", bibRef );
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    // /**
    // * @param message The message to set.
    // */
    // public void setMessageSource( ResourceBundleMessageSource messageSource ) {
    // this.messageSource = messageSource;
    // }

}
