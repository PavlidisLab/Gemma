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
package edu.columbia.gemma.web.controller.entrez.pubmed;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.util.StringUtil;

/**
 * Controller used to search for a given pubMedId, then either save, edit, or delete it from the database. The
 * commandName specified as an XDoclet tag is optional and defaults to 'command'. The reason for specifying this is
 * because it is used by the Commons Validator validation rules.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="pubMedFormController" name="/pubMedSearch.htm /editBibRef.htm"
 * @spring.property name="sessionForm" value="true"
 * @spring.property name="commandName" value="bibliographicReference"
 * @spring.property name="formView" value="pubMed.Search.criteria.view"
 * @spring.property name="successView" value="pubMed.Search.results.view"
 * @spring.property name = "pubMedXmlFetcher" ref="pubMedXmlFetcher"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name = "externalDatabaseService" ref="externalDatabaseService"
 */
public class PubMedFormController extends SimpleFormController {
    // TODO make the successView in xdoclet tags use the redirect:xxx.html.
    private static Log log = LogFactory.getLog( PubMedFormController.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService = null;
    private ExternalDatabaseService externalDatabaseService;
    private PubMedXMLFetcher pubMedXmlFetcher = null;

    /**
     * set the command class.
     */
    public PubMedFormController() {
        super();
        /*
         * Set to true so the same object is used for editing and submitting. If sessionForm=false, the command object
         * would be request scoped, and formBackingObject() will be called for both edit and submit events, creating a
         * new command object in each case. We want to reuse the same command object across the entire form
         * edit-and-submit process. This can also be set declaratively in the spring bean definition
         */
        setSessionForm( true );
        setCommandClass( BibliographicReference.class );
    }

    /**
     * Returns the BibliographicReference object (the commandClass) from the database if it exists, else creates a new
     * object.
     * 
     * @param request
     * @return Object
     */
    @SuppressWarnings("unchecked")
    protected Object formBackingObject( HttpServletRequest request ) {
        log.debug( "entered 'formBackingObject'" );
        String pubMedId = RequestUtils.getStringParameter( request, "pubMedId", null );

        if ( pubMedId != null ) {

            // FIXME don't get all but do something better than findByExternalId as well.
            Collection<BibliographicReference> col = bibliographicReferenceService.getAllBibliographicReferences();
            for ( BibliographicReference br : col ) {
                if ( br.getPubAccession().getAccession().equals( pubMedId ) ) return br;
            }
        }

        return BibliographicReference.Factory.newInstance();
    }

    /**
     * Converts String values from the form to properties of the domain object (command class in Spring speak).
     * 
     * @param request
     * @param binder
     */
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        // do nothing since we do not need to convert the pubMedId to an object type.
    }

    /**
     * This method implements the 'save' functionality.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @throws Exception
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        if ( log.isDebugEnabled() ) log.debug( "entered 'onSubmit'" );

        int pubMedId;
        BibliographicReference br;

        pubMedId = StringUtil.relaxedParseInt( request.getParameter( "pubMedId" ) );

        br = this.pubMedXmlFetcher.retrieveByHTTP( pubMedId );

        if ( !this.bibliographicReferenceService.alreadyExists( br ) ) {
            // fill in the accession and the external database.
            if ( br.getPubAccession() == null ) {
                DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                dbEntry.setAccession( ( new Integer( pubMedId ) ).toString() );
                br.setPubAccession( dbEntry );
            }

            ExternalDatabase pubMedDb = this.externalDatabaseService.find( "PubMed" );

            if ( pubMedDb == null ) {
                log.error( "There was no external database 'PubMed'" );
                // Errors errs = new FormObjectAccessor( context ).getFormErrors();
                // errors.reject( "ExternalDatabaseError", "There was no external database 'PubMed'" );
                // return error();
                // FIXME this should use error handling, not an exception.
                throw new RuntimeException( "pubMeDB does not exist" );
            }

            br.getPubAccession().setExternalDatabase( pubMedDb );

            this.bibliographicReferenceService.saveBibliographicReference( br );
            // FIXME should be able to set the success message.
            // request.getSession().setAttribute("message",getMessageSourceAccessor().getMessage());
        }
        // FIXME put success messages in this constructor.
        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param pubMedXmlFetcher The pubMedXmlFetcher to set.
     */
    public void setPubMedXmlFetcher( PubMedXMLFetcher pubMedXmlFetcher ) {
        this.pubMedXmlFetcher = pubMedXmlFetcher;
    }

    /**
     * @param externalDatabaseService The externalDatabaseService to set.
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

}
