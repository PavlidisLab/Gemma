package edu.columbia.gemma.web.controller.expression.arrayDesign;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignControllerTest extends BaseControllerTestCase {

    private MockServletContext mockCtx;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    ArrayDesignController arrayDesignController;

    ArrayDesign testArrayDesign;

    ArrayDesignService arrayDesignService;

    public void setUp() throws Exception {

        mockCtx = new MockServletContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

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
     * Tests the SignupController
     * 
     * @throws Exception
     */
    public void testOnSubmit() throws Exception {

    }
}
