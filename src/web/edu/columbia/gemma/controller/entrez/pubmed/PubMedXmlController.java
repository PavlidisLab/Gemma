package edu.columbia.gemma.controller.entrez.pubmed;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.common.description.BibliographicReference;
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
 */
public class PubMedXmlController extends SimpleFormController {
    private PubMedXMLFetcher pubMedXmlFetcher;

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog( getClass() );

    /**
     * @return Returns the pubMedXmlFetcher.
     */
    public PubMedXMLFetcher getPubMedXmlFetcher() {
        return pubMedXmlFetcher;
    }

    /**
     * Obtains filename to be read from the form.
     * 
     * @param command
     * @return ModelAndView
     * @throws IOException
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws IOException {

        String pubMedId = RequestUtils.getStringParameter( request, "pubMedId", null );

        log.info( "Context Path: " + request.getContextPath() );
        log.info( "Requested Uri: " + request.getRequestURI() );
        log.info( "Authentication Type: " + request.getAuthType() );

        BibliographicReference br = pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        Map myModel = new HashMap();
        myModel.put( "title", br.getTitle() );
        myModel.put( "publication", br.getPublication() );
        myModel.put( "authorList", br.getAuthorList() );
        myModel.put( "abstract", br.getAbstractText() );

        //return new ModelAndView( new RedirectView(getSuccessView(),true), "model", myModel );
        return new ModelAndView( getSuccessView(), "model", myModel );
    }

    /**
     * @param pubMedXmlFetcher The pubMedXmlFetcher to set.
     */
    public void setPubMedXmlFetcher( PubMedXMLFetcher pubMedXmlFetcher ) {
        this.pubMedXmlFetcher = pubMedXmlFetcher;
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