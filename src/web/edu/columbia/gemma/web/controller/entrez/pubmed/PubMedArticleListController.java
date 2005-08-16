package edu.columbia.gemma.web.controller.entrez.pubmed;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="pubMedArticleListController"
 * @spring.property name="sessionForm" value="true"
 * @spring.property name="formView" value="pubMedArticleListForm"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class PubMedArticleListController extends SimpleFormController {
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

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String view = "pubMedList";
        int maxResults = Integer.parseInt( request.getParameter( "maxResults" ) );
        Map<String, Collection> bibRefModel = new HashMap<String, Collection>();
        switch ( maxResults ) {
            case 10:
                bibRefModel.put( "bibRefs", getBibliographicReferenceService().getAllBibliographicReferences() );
                break;
            case 50:
                break;
            case 100:
                break;
        }

        return new ModelAndView( view, "model", bibRefModel );

    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
}
