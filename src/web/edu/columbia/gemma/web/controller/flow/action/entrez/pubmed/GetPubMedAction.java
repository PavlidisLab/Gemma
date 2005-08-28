package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.AbstractAction;

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
    private static Log log = LogFactory.getLog( GetPubMedAction.class.getName() );
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
        String pubMedId = ( String ) context.getFlowScope().getRequiredAttribute( "pubMedId", String.class );
        BibliographicReference br = getBibliographicReferenceService().findByExternalId( pubMedId );
        if ( br != null ) {
            context.getRequestScope().setAttribute( "bibliographicReference", br );
            return success();
        }
        log.error("Didn't find pubMedId " + pubMedId );
        return error();
    }
}