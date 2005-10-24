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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.FormObjectRetrievalFailureException;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.web.flow.AbstractFlowFormAction;

/**
 * A webflow action for searching the PubMed online for a reference, and then saving it into Gemma.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author pavlidis
 * @spring.bean name="pubmedExecuteQueryAction"
 * @spring.property name="formObjectClass" value="edu.columbia.gemma.common.description.BibliographicReference"
 * @spring.property name="formObjectName" value="bibliographicReference"
 * @spring.property name="formObjectScopeAsString" value="flow"
 * @spring.property name="validator" ref="pubMedAccessionValidator"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="pubMedXmlFetcher" ref="pubMedXmlFetcher"
 * @version $Id$
 */
public class PubMedExecuteQueryAction extends AbstractFlowFormAction {

    private static final String PUB_MED = "PubMed";

    private static Log log = LogFactory.getLog( PubMedExecuteQueryAction.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService;
    private ExternalDatabaseService externalDatabaseService;
    private PubMedXMLFetcher pubMedXmlFetcher;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.webflow.action.FormAction#loadFormObject(org.springframework.webflow.RequestContext)
     */
    @Override
    @SuppressWarnings("unused")
    protected Object createFormObject( RequestContext context ) throws FormObjectRetrievalFailureException, Exception {
        BibliographicReference bibRef = BibliographicReference.Factory.newInstance();
        bibRef.setPubAccession( DatabaseEntry.Factory.newInstance() );
        return bibRef;
    }

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
     * @param context
     * @return
     * @throws Exception
     */
    public Event delete( RequestContext context ) throws Exception {

        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getRequiredAttribute(
                "bibliographicReference" );
        String accession = bibRef.getPubAccession().getAccession();
        // String accession = ( String ) context.getSourceEvent().getParameter( "pubAccession.accession" );
        if ( accession == null ) {
            this.getFormErrors( context ).reject( "error.noCriteria", "You must enter an accession number." );
            return error();
        }

        BibliographicReference bibRefFound = bibliographicReferenceService.findByExternalId( accession );
        if ( bibRefFound == null ) {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.FALSE );
            return error();
        }
        this.bibliographicReferenceService.removeBibliographicReference( bibRefFound );

        addMessage( context, "bilbiographicReference.deleted", new Object[] { accession } );
        return success();
    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event searchNCBI( RequestContext context ) throws Exception {

        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getRequiredAttribute(
                "bibliographicReference" );

        String accession = bibRef.getPubAccession().getAccession();
        // String accession = ( String ) context.getSourceEvent().getParameter( "pubAccession.accession" );

        if ( accession == null ) {
            this.getFormErrors( context ).reject( "error.noCriteria", "You must enter an accession number." );
            return error();
        }

        // first see if we already have it in the system.
        BibliographicReference bibRefFound = bibliographicReferenceService.findByExternalId( accession );
        if ( bibRefFound != null ) {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
            context.getFlowScope().setAttribute( "bibliographicReference", bibRefFound );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRefFound );
            addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { accession } );
        } else {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.FALSE );
            try {
                bibRef = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( accession ) );
                context.getRequestScope().setAttribute( "accession", accession );
                context.getFlowScope().setAttribute( "accession", accession );
                context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
                context.getFlowScope().setAttribute( "bibliographicReference", bibRef );
            } catch ( NumberFormatException e ) {
                return error( e );
            }
        }
        return success();
    }

    /**
     * Search Gemma for the reference.
     * 
     * @param context
     * @return
     * @throws Exception
     */
    public Event search( RequestContext context ) throws Exception {

        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getRequiredAttribute(
                "bibliographicReference" );

        String accession = bibRef.getPubAccession().getAccession();
        // String accession = ( String ) context.getSourceEvent().getParameter( "pubAccession.accession" );
        if ( accession == null ) {
            this.getFormErrors( context ).reject( "error.noCriteria", "You must enter an accession number." );
            return error();
        }

        BibliographicReference bibRefFound = bibliographicReferenceService.findByExternalId( accession );
        if ( bibRefFound != null ) {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
            context.getFlowScope().setAttribute( "bibliographicReference", bibRefFound );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRefFound );
            addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { bibRefFound.getPubAccession()
                    .getAccession() } );
        } else {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.FALSE );
        }
        return success();
    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {
        BibliographicReference bibRef = ( BibliographicReference ) context.getFlowScope().getRequiredAttribute(
                "bibliographicReference" );

        if ( bibRef == null || bibRef.getPubAccession() == null ) return error();

        BibliographicReference bibRefFound = this.bibliographicReferenceService.find( bibRef );

        String accession = bibRef.getPubAccession().getAccession();

        if ( bibRefFound != null ) {
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
            addMessage( context, "bibliographicReference.alreadyInSystem", new Object[] { accession } );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRefFound );
            context.getFlowScope().setAttribute( "bibliographicReference", bibRefFound );
        } else { // it's new.
            log.debug( "Saving bibliographic reference" );

            // fill in the accession and the external database.
            if ( bibRef.getPubAccession() == null ) {
                DatabaseEntry dbEntry = DatabaseEntry.Factory.newInstance();
                dbEntry.setAccession( accession );
                bibRef.setPubAccession( dbEntry );
            }

            ExternalDatabase pubMedDb = this.externalDatabaseService.find( PUB_MED );

            if ( pubMedDb == null ) {
                log.error( "There was no external database '" + PUB_MED + "'" );
                // errors = new FormObjectAccessor( context ).getFormErrors();
                // errors.reject( "ExternalDatabaseError", "There was no external database '" + PUB_MED + "'" );
                return error();
            }

            bibRef.getPubAccession().setExternalDatabase( pubMedDb );
            bibRef = this.bibliographicReferenceService.saveBibliographicReference( bibRef );

            context.getFlowScope().setAttribute( "bibliographicReference", bibRef );
            context.getRequestScope().setAttribute( "bibliographicReference", bibRef );
            context.getRequestScope().setAttribute( "existsInSystem", Boolean.TRUE );
            addMessage( context, "bibliographicReference.saved", new Object[] { accession } );

        }
        return success();
    }

}