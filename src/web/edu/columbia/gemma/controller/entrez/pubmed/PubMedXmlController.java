package edu.columbia.gemma.controller.entrez.pubmed;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.InternalResourceView;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;

/**
 * Allows user to view the results of a pubMed search, with the option of submitting the results.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="pubMedXmlController"
 * @spring.property name="sessionForm" value="true"
 * @spring.property name="formView" value="pubMedForm"
 * @spring.property name="successView" value="pubMedSuccess"
 * @spring.property name = "pubMedXmlFetcher" ref="pubMedXmlFetcher"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class PubMedXmlController extends SimpleFormController {
    private boolean alreadyViewed = false;
    private BibliographicReferenceService bibliographicReferenceService;
    private String pubMedId = null;
    private PubMedXMLFetcher pubMedXmlFetcher;

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog( getClass() );

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
     * Useful for debugging, specifically with Tomcat security issues.
     * 
     * @param request
     * @param response TODO put in an mvcUtils class if you used elsewhere. I found this helpful when working with
     *        Spring's MVC.
     */
    public void logHttp( HttpServletRequest request, HttpServletResponse response ) {
        log.info( "Context Path: " + request.getContextPath() );
        log.info( "Requested Uri: " + request.getRequestURI() );
        log.info( "Authentication Type: " + request.getAuthType() );
    }

    /**
     * Obtains filename to be read from the form.
     * 
     * @param command
     * @return ModelAndView
     * @throws Exception TODO Review the way you are handling the "view within a view" ie. using InternalResourceView
     *         TODO ... I started off using the RequestDispatcher.forward(request,response), but I don't need TODO ...
     *         this as Spring provides an InternalResourceView.
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        logHttp( request, response );

        if ( pubMedId == null )
            pubMedId = RequestUtils.getStringParameter( request, "pubMedId", null );
        else {
            request.setAttribute( "pubMedId", pubMedId );
        }

        BibliographicReference br = pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
        Map myModel = new HashMap();
        myModel.put( "bibRef", br );
        request.setAttribute( "model", myModel );

        useInternalResourceView( request, response, br, myModel );

        return new ModelAndView( "bibRef" );

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
     * Check to see if the InternalResourceView has already been viewed.
     * 
     * @param request
     * @param response
     * @param myModel
     * @throws Exception TODO there must be a better (Springish) way to do this.
     */
    private void useInternalResourceView( HttpServletRequest request, HttpServletResponse response, Object object,
            Map model ) throws Exception {
        if ( !alreadyViewed ) {
            alreadyViewed = true;
            View v = new InternalResourceView( "/WEB-INF/pages/pubMedSuccess.jsp" );
            v.render( model, request, response );
        } else {
            alreadyViewed = false;
            getBibliographicReferenceService().saveBibliographicReference( ( BibliographicReference ) object );
        }
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