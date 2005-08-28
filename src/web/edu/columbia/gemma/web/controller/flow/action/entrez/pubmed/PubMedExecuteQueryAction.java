package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.Errors;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.action.FormObjectAccessor;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.util.StringUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PubMedExecuteQueryAction extends AbstractAction {
    private static Log log = LogFactory.getLog( PubMedExecuteQueryAction.class.getName() );

    private BibliographicReferenceService bibliographicReferenceService;
    private ExternalDatabaseService externalDatabaseService;
    private PubMedXMLFetcher pubMedXmlFetcher;

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
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
     * This is the equivalent of writing the onSubmit method in a Spring Controller, or a doGet (doPost) method in a
     * Java Servlet.
     * 
     * @param context
     * @return Event
     * @exception Exception
     */
    protected Event doExecute( RequestContext context ) throws Exception {

        String event = ( String ) context.getSourceEvent().getParameter( "_eventId" );
        int pubMedId;
        BibliographicReference br;
        try {
            pubMedId = StringUtil.relaxedParseInt( ( String ) context.getSourceEvent().getParameter( "pubMedId" ) );
            br = this.pubMedXmlFetcher.retrieveByHTTP( pubMedId );
            if ( event.equals( "submitPubMed" ) ) {
                List<BibliographicReference> list = new ArrayList<BibliographicReference>();
                list.add( br );
                context.getRequestScope().setAttribute( "pubMedId", new Integer( pubMedId ) );
                context.getRequestScope().setAttribute( "bibliographicReferences", list );
            } else if ( event.equals( "saveBibRef" ) ) {

                if ( br == null ) {
                    Errors errors = new FormObjectAccessor( context ).getFormErrors();
                    errors.reject( "PubMedXmlFetcher", "No results." );
                    return error();
                }

                log.debug( "Saving bibliographic reference" );
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
                        Errors errors = new FormObjectAccessor( context ).getFormErrors();
                        errors.reject( "ExternalDatabaseError", "There was no external database 'PubMed'" );

                        return error();
                    }

                    br.getPubAccession().setExternalDatabase( pubMedDb );

                    this.bibliographicReferenceService.saveBibliographicReference( br );
                }
            }
            return success();
        }
        // TODO When you start using value objects, do the pubMed validation in the validator.
        catch ( IOException e ) {
            Errors errors = new FormObjectAccessor( context ).getFormErrors();
            errors.reject( "IOError", e.getMessage() );
        } catch ( NumberFormatException e ) {
            Errors errors = new FormObjectAccessor( context ).getFormErrors();
            errors.reject( "NumberFormat", "Not a number" );
        } catch ( Exception e ) {
            Errors errors = new FormObjectAccessor( context ).getFormErrors();
            errors.reject( "GenericError", "Some other kind of error" );
        }
        return error();
    }
}