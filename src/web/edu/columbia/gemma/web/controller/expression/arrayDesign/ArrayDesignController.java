/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edu.columbia.gemma.web.controller.expression.arrayDesign;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="/arrayDesigns.htm /arrayDesignDetails.htm"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignController implements Controller {
    private static Log log = LogFactory.getLog( ArrayDesignController.class );
    private String uri_separator = "/";
    private ArrayDesignService arrayDesignService = null;

    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unused")
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        log.info( request.getRequestURI() );

        String uri = request.getRequestURI();

        String[] elements = StringUtils.split( uri, uri_separator );
        String path = elements[1];

        if ( path.equals( "arrayDesigns.htm" ) )
            return new ModelAndView( "arrayDesign.GetAll.results.view", "arrayDesigns", arrayDesignService
                    .getAllArrayDesigns() );

        // Don't forget to pack the attribute 'name' in the request. If you don't do this you will lose it when you get
        // to the arrayDesign.Detail.view.jsp (ie. you have all the parameters/attributes for the request right now, but
        // as
        // soon as you return ModelAndView, you will not have any parameters/attributes as this is treated as a new
        // request.
        request.setAttribute( "name", request.getParameter( "name" ) );
        return new ModelAndView( "arrayDesign.Detail.view", "arrayDesign", arrayDesignService
                .findArrayDesignByName( request.getParameter( "name" ) ) );
    }

}
