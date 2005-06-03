package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import java.util.Collection;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.InternalRequestContext;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;

import edu.columbia.gemma.common.description.BibliographicReferenceService;

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
     * This is the equivalent of writing the onSubmit method in a Spring Controller, or a doGet (doPost) method in a
     * Java Servlet.
     * 
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