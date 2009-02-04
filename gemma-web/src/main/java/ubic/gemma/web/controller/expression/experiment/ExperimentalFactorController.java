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
package ubic.gemma.web.controller.expression.experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="experimentalFactorController"
 * @spring.property name = "experimentalFactorService" ref="experimentalFactorService"
 * @spring.property name="methodNameResolver" ref="experimentalFactorActions"
 * @deprecated This is no longer used and can probably be deleted (corresponding entry in spring config too)
 */
public class ExperimentalFactorController extends BaseMultiActionController {

    private ExperimentalFactorService experimentalFactorService = null;

    private final String messagePrefix = "Experimenal factor with id ";
    private final String identifierNotFound = "Must provide a valid ExperimentalFactor identifier";

    /**
     * @param experimentalFactorService
     */
    public void setExperimentalFactorService( ExperimentalFactorService experimentalFactorService ) {
        this.experimentalFactorService = experimentalFactorService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExperimentalFactor experimentalFactor = experimentalFactorService.load( id );
        if ( experimentalFactor == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "id", id );
        return new ModelAndView( "experimentalFactor.detail" ).addObject( "experimentalFactor", experimentalFactor );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "experimentalFactors" ).addObject( "experimentalFactors", experimentalFactorService
                .loadAll() );
    }

}
