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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="bibliographicReferenceController" name="/bibRefs.htm /bibRefDetails.htm"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class BibliographicReferenceController implements Controller {
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService = null;

    private String uri_separator = "/";

    /**
     * Checks the uri and directs user to the appropriate page.
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unchecked")
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() )
            log.debug( "entered 'handleRequest'" + " HttpServletRequest: " + request + " HttpServletResponse: "
                    + response );

        String uri = request.getRequestURI();

        String[] elements = StringUtils.split( uri, uri_separator );
        String path = elements[1];

        if ( path.equals( "bibRefs.htm" ) )
            return new ModelAndView( "bibRef.GetAll.results.view", "bibliographicReferences",
                    bibliographicReferenceService.getAllBibliographicReferences() );

        /*
         * If uri does not equal 'bibRefs.htm', it must equal 'bibRefDetails.htm'. Pack the attribute 'pubMedId' in the
         * request. You must do this or it will get lost when redirecting to the view bibRef.Detail.view.jsp (ie. you
         * have access to all the parameters/attributes for the request right now, but as soon as you invoke 'new
         * ModelAndView', you will lose access to these parameters/attributes as this is treated as a new request.
         */
        request.setAttribute( "pubMedId", request.getParameter( "pubMedId" ) );

        // FIXME bibliographicReferenceService.findByExternalId( request.getParameter( "pubMedId" ) ) is returning
        // null here.
        // I have done this for now, but this must be fixed and not get all the bibliographicReferences.
        Collection<BibliographicReference> col = bibliographicReferenceService.getAllBibliographicReferences();
        BibliographicReference bibRef = null;
        for ( BibliographicReference br : col ) {
            if ( br.getPubAccession().getAccession().equals( request.getParameter( "pubMedId" ) ) ) {
                bibRef = br;
                break;
            }
        }

        log.debug( "request parameter pubMedId: " + request.getParameter( "pubMedId" )
                + " has bibliographicReference: " + bibRef );

        return new ModelAndView( "pubMed.Detail.view", "bibliographicReference", bibRef );
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

}
