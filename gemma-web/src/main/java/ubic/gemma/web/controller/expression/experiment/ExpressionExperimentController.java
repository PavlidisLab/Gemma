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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentController" name="/expressionExperiment/*"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="methodNameResolver" ref="expressionExperimentActions"
 */
public class ExpressionExperimentController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( ExpressionExperimentController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;

    private final String messagePrefix = "Expression experiment with name";

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong(request.getParameter( "id" ));

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Expression Experiment id" );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.addMessage( request, "object.found", new Object[] { messagePrefix, id } );
        request.setAttribute( "name", id );
        return new ModelAndView( "expressionExperiment.detail" ).addObject( "expressionExperiment",
                expressionExperiment );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "expressionExperiments" ).addObject( "expressionExperiments",
                expressionExperimentService.loadAll() );
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

        ExpressionExperiment expressionExperiment = expressionExperimentService.findByName( name );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( expressionExperiment + " not found" );
        }

        return doDelete( request, expressionExperiment );
    }

    /**
     * @param request
     * @param expressionExperiment
     * @return
     */
    private ModelAndView doDelete( HttpServletRequest request, ExpressionExperiment expressionExperiment ) {
        expressionExperimentService.delete( expressionExperiment );
        log.info( "Expression Experiment with name: " + expressionExperiment.getName() + " deleted" );
        addMessage( request, "object.deleted", new Object[] { messagePrefix, expressionExperiment.getName() } );
        return new ModelAndView( "expressionExperiments", "expressionExperiment", expressionExperiment );
    }
}
