package edu.columbia.gemma.web.controller.expression.arrayDesign;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignControllerTest extends BaseControllerTestCase {

    // private MockServletContext mockCtx;
    private MockHttpServletRequest request;
    // private MockHttpServletResponse response;

    ArrayDesignController arrayDesignController;

    ArrayDesign testArrayDesign;

    ArrayDesignService arrayDesignService;

    public void setUp() throws Exception {

        // mockCtx = new MockServletContext();

        request = new MockHttpServletRequest();
        // response = new MockHttpServletResponse();

        arrayDesignController = ( ArrayDesignController ) ctx.getBean( "arrayDesignController" );

        testArrayDesign = ArrayDesign.Factory.newInstance();

        arrayDesignService = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );

    }

    /**
     * Tear down objects.
     */
    public void tearDown() {
        arrayDesignController = null;
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testShowAllArrayDesigns() throws Exception {
        ArrayDesignController a = ( ArrayDesignController ) ctx.getBean( "arrayDesignController" );
        request.setRequestURI( "Gemma/arrayDesign/showAllArrayDesigns.html" );
        ModelAndView mav = a.showAll( request, ( HttpServletResponse ) null );
        Collection<ArrayDesign> c = ( mav.getModel() ).values();
        assertNotNull( c );
        assertEquals( mav.getViewName(), "arrayDesigns" );
    }
}
