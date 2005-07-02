package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PubMedExecuteQueryAction extends AbstractAction {

    private BibliographicReferenceService bibliographicReferenceService;
    private PubMedXMLFetcher pubMedXmlFetcher;

    /**
     * 
     */
    public PubMedExecuteQueryAction() {

    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @return Returns the pubMedXmlFetcher.
     */
    public PubMedXMLFetcher getPubMedXmlFetcher() {
        return pubMedXmlFetcher;
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
            if ( event.equals( "submitPubMed" ) ) {
                pubMedId = Integer.parseInt( ( String ) context.getSourceEvent().getParameter( "pubMedId" ) );
                br = getPubMedXmlFetcher().retrieveByHTTP( pubMedId );
                List list = new ArrayList();
                list.add( br );
                context.getRequestScope().setAttribute( "pubMedId", new Integer( pubMedId ) );
                context.getRequestScope().setAttribute( "bibliographicReferences", list );
            } else if ( event.equals( "saveBibRef" ) ) {
                pubMedId = Integer.parseInt( ( String ) context.getSourceEvent().getParameter( "_pubMedId" ) );
                br = getPubMedXmlFetcher().retrieveByHTTP( pubMedId );
                if ( !getBibliographicReferenceService().alreadyExists( br ) )
                    getBibliographicReferenceService().saveBibliographicReference( br );
            }
            return success();
        }
        // TODO When you start using value objects, do the pubMed validation in the validator.
        catch ( Exception e ) {
            return error();
        }
    }
}