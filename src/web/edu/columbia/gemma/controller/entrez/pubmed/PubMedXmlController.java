package edu.columbia.gemma.controller.entrez.pubmed;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="pubMedXmlController"
 * @spring.property name="sessionForm" value="true"
 * @spring.property name="formView" value="pubMedForm"
 * @spring.property name="successView" value="hello.jsp"
 * @spring.property name = "pubMedXmlFetcher" ref="pubMedXmlFetcher"
 */
public class PubMedXmlController extends SimpleFormController {
    private PubMedXMLFetcher pubMedXmlFetcher;

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
        Map myModel = new HashMap();
        String pubMedId = RequestUtils.getStringParameter( request, "pubMedId", null );
        BibliographicReference br = pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );
        //myModel.put("bibliographicReference",br);
        System.err.println("Context Path: " + request.getContextPath());
        System.err.println("Authentication Type: " + request.getAuthType());
        System.err.println("Requested Uri: " + request.getRequestURI());
        return new ModelAndView( new RedirectView(getSuccessView(),true), "model", myModel );
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