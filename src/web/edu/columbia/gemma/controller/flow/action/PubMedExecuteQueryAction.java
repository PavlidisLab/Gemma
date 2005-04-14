package edu.columbia.gemma.controller.flow.action;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;
import org.springframework.web.flow.execution.servlet.HttpServletRequestEvent;

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
     * This is the equivalent of writing the onSubmit method in a Spring Controller,
     * or a doGet (doPost) method in a Java Servlet.
     * @param context
     * @return Event
     * @exception Exception 
     */
    protected Event doExecuteAction( RequestContext context ) throws Exception {
        
//        int pubMedId = Integer.parseInt( ( ( HttpServletRequestEvent ) context.getOriginatingEvent() ).getRequest()
//                .getParameter( "pubMedId" ) );
        int pubMedId = Integer.parseInt((String)context.getOriginatingEvent().getParameter("pubMedId"));
        
        BibliographicReference br = getPubMedXmlFetcher().retrieveByHTTP( pubMedId );
        List list = new ArrayList();
        list.add( br );
        //TODO Ask user for their choice.(like I did before).
        //TODO When you persist the bibRef, persist the DatabaseEntry.
        if ( ! getBibliographicReferenceService().alreadyExists( br ))
            getBibliographicReferenceService().saveBibliographicReference( br );
      
        context.getRequestScope().setAttribute( "bibliographicReferences", list );
        return success();
    }
}