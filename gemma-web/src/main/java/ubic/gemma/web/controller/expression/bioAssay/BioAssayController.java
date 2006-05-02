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
package ubic.gemma.web.controller.expression.bioAssay;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioAssayController" name="/bioAssay/*"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name="methodNameResolver" ref="bioAssayActions"
 */
public class BioAssayController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( BioAssayController.class.getName() );

    private BioAssayService bioAssayService = null;

    private final String messagePrefix = "BioAssay with id ";

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Experimental Design name" );
        }

        BioAssay bioAssay = bioAssayService.findById( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "name", id );
        return new ModelAndView( "bioAssay.detail" ).addObject( "bioAssay", bioAssay );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssayService.loadAll() );
    }

    /**
     * TODO add delete to the model
     * 
     * @param request
     * @param response
     * @return
     */
    // @SuppressWarnings("unused")
    // public ModelAndView delete(HttpServletRequest request,
    // HttpServletResponse response) {
    // String name = request.getParameter("name");
    //
    // if (name == null) {
    // // should be a validation error.
    // throw new EntityNotFoundException("Must provide a name");
    // }
    //
    // BioAssay bioAssay = bioAssayService
    // .findByName(name);
    // if (bioAssay == null) {
    // throw new EntityNotFoundException(bioAssay
    // + " not found");
    // }
    //
    // return doDelete(request, bioAssay);
    // }
    /**
     * TODO add doDelete to the model
     * 
     * @param request
     * @param bioAssay
     * @return
     */
    // private ModelAndView doDelete(HttpServletRequest request,
    // BioAssay bioAssay) {
    // bioAssayService.delete(bioAssay);
    // log.info("Expression Experiment with name: "
    // + bioAssay.getName() + " deleted");
    // addMessage(request, "bioAssay.deleted",
    // new Object[] { bioAssay.getName() });
    // return new ModelAndView("bioAssays",
    // "bioAssay", bioAssay);
    // }
}
