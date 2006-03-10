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
package ubic.gemma.web.controller.expression.arrayDesign;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="/arrayDesign/*"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name="methodNameResolver" ref="arrayDesignActions"
 */
public class ArrayDesignController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( ArrayDesignController.class.getName() );

    private ArrayDesignService arrayDesignService = null;

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );

        if ( name == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name" );
        }

        ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( name );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( name + " not found" );
        }

        this.addMessage( request, "arrayDesign.found", new Object[] { name } );
        request.setAttribute( "name", name );
        return new ModelAndView( "arrayDesign.detail" ).addObject( "arrayDesign", arrayDesign );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "arrayDesigns" ).addObject( "arrayDesigns", arrayDesignService.getAllArrayDesigns() );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );

        if ( name == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide a name" );
        }

        ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( name );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( arrayDesign + " not found" );
        }

        return doDelete( request, arrayDesign );
    }

    /**
     * @param request
     * @param locale
     * @param bibRef
     * @return
     */
    private ModelAndView doDelete( HttpServletRequest request, ArrayDesign arrayDesign ) {
        arrayDesignService.remove( arrayDesign );
        log.info( "Bibliographic reference with pubMedId: " + arrayDesign.getName() + " deleted" );
        addMessage( request, "arrayDesign.deleted", new Object[] { arrayDesign.getName() } );
        return new ModelAndView( "arrayDesigns", "arrayDesign", arrayDesign );
    }

}
