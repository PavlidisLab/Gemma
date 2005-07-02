package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import org.springframework.web.flow.Event;
import org.springframework.web.flow.RequestContext;
import org.springframework.web.flow.action.AbstractAction;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GetPubMedAction extends AbstractAction {

    private BibliographicReferenceService bibliographicReferenceService;

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

    protected Event doExecute( RequestContext context ) throws Exception {
        String title = ( String ) context.getFlowScope().getRequiredAttribute( "title", String.class );
        BibliographicReference br = getBibliographicReferenceService().getBibliographicReferenceByTitle( title );
        if ( br != null ) {
            context.getRequestScope().setAttribute( "bibliographicReference", br );
            return success();
        } else {
            return error();
        }
    }
}