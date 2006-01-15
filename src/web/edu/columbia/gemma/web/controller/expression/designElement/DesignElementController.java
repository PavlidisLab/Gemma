/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package edu.columbia.gemma.web.controller.expression.designElement;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.web.controller.BaseMultiActionController;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="designElementController" name="/designElement/*"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name="methodNameResolver" ref="designElementActions"
 */
public class DesignElementController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( DesignElementController.class.getName() );

    private ArrayDesignService arrayDesignService = null;

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
    // String name = request.getParameter( "name" );
    //
    // if ( name == null ) {
    // // should be a validation error, on 'submit'.
    // throw new EntityNotFoundException( "Must provide an Array Design name" );
    // }
    //
    // DesignElement designElement = designElementService.findArrayDesignByName( name );
    // if ( designElement == null ) {
    // throw new EntityNotFoundException( name + " not found" );
    // }
    //
    // this.addMessage( request, "designElement.found", new Object[] { name } );
    // request.setAttribute( "name", name );
    // return new ModelAndView( "designElement.detail" ).addObject( "designElement", designElement );
    // }
    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings({"unused","unchecked"})
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( "entered showAll from " + request.getRequestURI() );

        String name = request.getParameter( "name" );

        ArrayDesign ad = arrayDesignService.findArrayDesignByName( name );
        Collection<ArrayDesign> ads = ad.getDesignElements();

        return new ModelAndView( "designElements" ).addObject( "designElements", ads );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
    // String name = request.getParameter( "name" );
    //
    // if ( name == null ) {
    // // should be a validation error.
    // throw new EntityNotFoundException( "Must provide a name" );
    // }
    //
    // DesignElement designElement = designElementService.findDesignElementByName( name );
    // if ( designElement == null ) {
    // throw new EntityNotFoundException( designElement + " not found" );
    // }
    //
    // return doDelete( request, designElement );
    // }
    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    // private ModelAndView doDelete( HttpServletRequest request, DesignElement designElement ) {
    // designElementService.remove( designElement );
    // log.info( "Bibliographic reference with pubMedId: " + designElement.getName() + " deleted" );
    // addMessage( request, "designElement.deleted", new Object[] { designElement.getName() } );
    // return new ModelAndView( "designElements", "designElement", designElement );
    // }
    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}
