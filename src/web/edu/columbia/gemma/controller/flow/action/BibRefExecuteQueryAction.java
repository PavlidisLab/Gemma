package edu.columbia.gemma.controller.flow.action;

import java.util.ArrayList;
import java.util.Collection;
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
public class BibRefExecuteQueryAction extends AbstractAction {

    private BibliographicReferenceService bibliographicReferenceService;

    /**
     * 
     */
    public BibRefExecuteQueryAction() {

    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }
    
    /**
     * This is the equivalent of writing the onSubmit method in a Spring Controller,
     * or a doGet (doPost) method in a Java Servlet.
     * @param context
     * @return Event
     * @exception Exception 
     */
    protected Event doExecuteAction( RequestContext context ) throws Exception {
     
        Collection col = getBibliographicReferenceService().getAllBibliographicReferences();
        context.getRequestScope().setAttribute( "bibliographicReferences", col );
        
        return success();
    }
}