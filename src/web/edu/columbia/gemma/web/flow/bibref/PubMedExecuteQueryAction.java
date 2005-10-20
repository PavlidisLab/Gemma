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
package edu.columbia.gemma.web.flow.bibref;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.FormObjectAccessor;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.util.StringUtil;
import edu.columbia.gemma.web.flow.AbstractFlowAction;

/**
 * A webflow action for searching the PubMed online for a reference, and then saving it into Gemma.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author pavlidis
 * @spring.bean name="pubmedExecuteQueryAction"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="pubMedXmlFetcher" ref="pubMedXmlFetcher"
 * @version $Id$
 */
public class PubMedExecuteQueryAction extends AbstractFlowAction {

    private static final String PUB_MED = "PubMed";

    private static Log log = LogFactory.getLog( PubMedExecuteQueryAction.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService;
    private ExternalDatabaseService externalDatabaseService;
    private PubMedXMLFetcher pubMedXmlFetcher;

    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param externalDatabaseService The externalDatabaseService to set.
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param pubMedXmlFetcher The pubMedXmlFetcher to set.
     */
    public void setPubMedXmlFetcher( PubMedXMLFetcher pubMedXmlFetcher ) {
        this.pubMedXmlFetcher = pubMedXmlFetcher;
    }

    /**
     * Determines the uri of the source event, and takes the appropriate action. This is the equivalent of writing the
     * onSubmit method in a Spring Controller, or a doGet (doPost) method in a Java Servlet.
     * 
     * @param context
     * @return Event
     * @exception Exception
     */
    protected Event doExecute( RequestContext context ) throws Exception {

        String event = ( String ) context.getSourceEvent().getParameter( "_eventId" );
        Integer pubMedId;
        BibliographicReference bibRef;
        Errors errors = null;

        context.getRequestScope().setAttribute( "existsInSystem", Boolean.FALSE ); // the default.

        try {
            pubMedId = new Integer( StringUtil.relaxedParseInt( ( String ) context.getSourceEvent().getParameter(
                    "pubMedId" ) ) );

            // does this mean we search twice when we do
            // search+save?
            bibRef = this.pubMedXmlFetcher.retrieveByHTTP( pubMedId.intValue() );
            context.getRequestScope().setAttribute( "pubMedId", pubMedId );
            context.getFlowScope().setAttribute( "pubMedId", pubMedId );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRef );

            if ( event.equals( "searchPubMed" ) ) { // used when searching the web.
                doSearch( context, bibRef );
            } else if ( event.equals( "saveBibRef" ) ) {
                if ( bibRef == null ) {
                    errors = new FormObjectAccessor( context ).getFormErrors();
                    errors.reject( "PubMedXmlFetcher", "No results." );
                    return error();
                }

                BibliographicReference bibRefFound = this.bibliographicReferenceService.find( bibRef );
                if ( bibRefFound != null ) {
                    context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
                    addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { pubMedId } );
                    context.getRequestScope().setAttribute( "bibliographicReference", bibRefFound );
                    context.getFlowScope().setAttribute( "bibliographicReference", bibRefFound );
                } else {
                    log.debug( "Saving bibliographic reference" );
                    // fill in the accession and the external database.
                    if ( bibRef.getPubAccession() == null ) {
                        DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                        dbEntry.setAccession( pubMedId.toString() );
                        bibRef.setPubAccession( dbEntry );
                    }

                    ExternalDatabase pubMedDb = this.externalDatabaseService.find( PUB_MED );

                    if ( pubMedDb == null ) {
                        log.error( "There was no external database '" + PUB_MED + "'" );
                        errors = new FormObjectAccessor( context ).getFormErrors();
                        errors.reject( "ExternalDatabaseError", "There was no external database '" + PUB_MED + "'" );
                        return error();
                    }

                    bibRef.getPubAccession().setExternalDatabase( pubMedDb );
                    bibRef = this.bibliographicReferenceService.saveBibliographicReference( bibRef );

                    context.getFlowScope().setAttribute( "bibliographicReference", bibRef );
                    context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
                    context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
                    addMessage( context, "bibliographicReference.saved", new Object[] { pubMedId } );

                }

            } else if ( event.equals( "delete" ) ) {
                this.bibliographicReferenceService.removeBibliographicReference( bibRef );
                addMessage( context, "bilbiographicReference.deleted", new Object[] { pubMedId } );
            }
            return success();
        }
        /* Webflow error handling. */
        catch ( IOException e ) {
            Errors errs = new FormObjectAccessor( context ).getFormErrors();
            errs.reject( "IOError", e.getLocalizedMessage() );
        } catch ( NumberFormatException e ) {
            Errors errs = new FormObjectAccessor( context ).getFormErrors();
            errs.reject( "NumberFormat", "Not a number" );
        } catch ( Exception e ) {
            Errors errs = new FormObjectAccessor( context ).getFormErrors();
            errs.reject( "OtherError", e.getLocalizedMessage() );
        }
        return error();
    }

    /**
     * @param context
     * @param bibRef
     */
    private void doSearch( RequestContext context, BibliographicReference bibRef ) {
        BibliographicReference bibRefFound = bibliographicReferenceService.find( bibRef );
        if ( bibRefFound != null ) {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
            context.getFlowScope().setAttribute( "bibliographicReference", bibRefFound );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRefFound );
            addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { new Integer( Integer
                    .parseInt( bibRef.getPubAccession().getAccession() ) ) } );
        } else {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.FALSE );
        }
    }

}