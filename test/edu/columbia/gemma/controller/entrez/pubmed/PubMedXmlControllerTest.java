package edu.columbia.gemma.controller.entrez.pubmed;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.controller.LoaderController;
import junit.framework.TestCase;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class PubMedXmlControllerTest extends BaseControllerTestCase {
    PubMedXmlController pubMedXmlController;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private View returnedView;
    
    public void setUp() throws Exception {
        pubMedXmlController = ( PubMedXmlController ) ctx.getBean( "pubMedXmlController" );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        //returnedView = new RedirectView( "home.jsp" );
    }
    
    public void tearDown() {
        pubMedXmlController = null;
    }
    
    public void testOnSubmit() throws Exception {
        request.addParameter( "pubMedId", "15173114" );
        
        request.setContextPath("/Gemma");
        request.setRequestURI("/Gemma/pubMedForm.htm");
        request.setAuthType("BASIC");
        ModelAndView mav = pubMedXmlController.onSubmit( request, response, ( Object ) null, ( BindException ) null );
        
        assertEquals( "bibRef", mav.getViewName() );

        //
    }

}
