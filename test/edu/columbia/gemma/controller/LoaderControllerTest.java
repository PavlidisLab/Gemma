package edu.columbia.gemma.controller;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.web.controller.LoaderController;

/**
 * This test makes use of Spring's MockHttpServletRequest and MockHttpServletResponse. I have used TaxonLoaderService as
 * the loader of choice as it is fast (small file to read from).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class LoaderControllerTest extends BaseControllerTestCase {

    private LoaderController c;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private View returnedView;

    public void setUp() throws Exception {
        c = ( LoaderController ) ctx.getBean( "loaderController" );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        returnedView = new RedirectView( "home.jsp" );
    }

    public void tearDown() {
        c = null;
    }

//    /**
//     * hasHeader = true The View returned by the onSubmit method ( a RedirectView(String view) ) will obviously not
//     * point to the same place on the heap as returnedView defined in setUp(). Calling toString on both will, however
//     * yield the same String value: org.springframework.web.servlet.view.RedirectView: unnamed; URL [home.jsp]
//     * 
//     * @throws Exception
//     */
//    public void testOnSubmitHeaderIsTrue() throws Exception {
//        request.addParameter( "hasHeader", "true" );
//        request.addParameter( "typeOfLoader", "taxonLoaderService" );
//        ModelAndView mav = c.onSubmit( request, response, ( Object ) null, ( BindException ) null );
//        assertEquals( returnedView.toString(), mav.getView().toString() );
//
//        //
//    } // commented out by pp

    /**
     * hasHeader = false Calling onSubmit with hasHeader = false will cause a NumberFormatException, as expected. This
     * results in a call to the view numberFormatError.jsp (to display the error), with the view name being
     * numberFormatError. TODO is it possible to test this if LoaderController throws an exception?
     * 
     * @throws Exception
     */
    //    public void testOnSubmitHeaderIsFalse() throws Exception {
    //        request.addParameter( "hasHeader", "false" );
    //        request.addParameter( "typeOfLoader", "taxonLoaderService" );
    //        ModelAndView mav = c.onSubmit( request, response, ( Object ) null, ( BindException ) null );
    //        System.err.println(mav.getView().toString());
    //        assertEquals( null, mav.getView().toString() );
    //    }
}