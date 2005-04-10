package edu.columbia.gemma.controller.entrez.pubmed;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseControllerTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PubMedArticleListControllerTest extends BaseControllerTestCase {
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    PubMedArticleListController pubMedArticleListController;

    public void setUp() throws Exception {
        pubMedArticleListController = ( PubMedArticleListController ) ctx.getBean( "pubMedArticleListController" );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    public void tearDown() {
        pubMedArticleListController = null;
    }

    public void testOnSubmit() throws Exception {
        request.addParameter( "maxResults", "10" );

        request.setContextPath( "/Gemma" );
        request.setRequestURI( "/Gemma/pubMedArticleListForm.htm" );
        request.setAuthType( "BASIC" );
        
        ModelAndView mav = pubMedArticleListController.onSubmit( request, response, ( Object ) null, ( BindException ) null );

        assertEquals( "pubMedList", mav.getViewName() );

        //
    }

}