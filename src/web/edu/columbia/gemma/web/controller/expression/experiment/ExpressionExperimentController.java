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
package edu.columbia.gemma.web.controller.expression.experiment;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.web.controller.common.description.BibliographicReferenceController;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author daq2101
 * @version $Id$
 * @spring.bean id="expressionExperimentController" name="/expressionExperiments.htm /experimentalDesigns.htm
 *              /expressionExperimentDetails.htm"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "messageSource" ref="messageSource"
 */
public class ExpressionExperimentController implements Controller {

    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;

    private ResourceBundleMessageSource messageSource = null;

    /**
     * @param request
     * @param response
     * @return ModelAndView - view, name of object in model, the object itself.
     * @throws Exception
     */
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() )
            log.debug( "entered 'handleRequest'" + " HttpServletRequest: " + request + " HttpServletResponse: "
                    + response );

        Locale locale = request.getLocale();

        String uri = request.getRequestURI();

        log.info( uri );

        /* handle "get all" case. */
        if ( uri.equals( "/Gemma/expressionExperiments.htm" ) )
            return new ModelAndView( "expressionExperiment.GetAll.results.view", "expressionExperiments",
                    expressionExperimentService.getAllExpressionExperiments() );

        /* handle details or delete, depending on whether _eventId=delete. */
        else if ( uri.equals( "/Gemma/expressionExperimentDetails.htm" ) ) {

            /* passed from jsp, and must be packed again to view in the next jsp. */
            request.setAttribute( "name", request.getParameter( "name" ) );
            log.debug( "request parameter: " + request.getAttribute( "name" ) );

            ExpressionExperiment ee = expressionExperimentService.findByName( request.getParameter( "name" ) );

            String event = request.getParameter( "_eventId" );
            if ( event != null && event.equals( "delete" ) ) {
                expressionExperimentService.remove( ee );
                log.info( "Expression experiment with name: " + ee.getName() + " deleted" );
                request.getSession().setAttribute(
                        "messages",
                        messageSource
                                .getMessage( "expressionExperiment.deleted", new Object[] { ee.getName() }, locale ) );
                /* delete */
                return new ModelAndView( "expressionExperiment.GetAll.results.view", "expressionExperiments",
                        expressionExperimentService.getAllExpressionExperiments() );
            }

            /* details */
            return new ModelAndView( "expressionExperiment.Detail.view", "expressionExperiment",
                    expressionExperimentService.findByName( request.getParameter( "name" ) ) );
        }

        /*
         * handle the event of clicking on the experimental designs (expressed as the number of experimental designs in
         * the collection) link for this expression experiment.
         */
        else if ( uri.equals( "/Gemma/experimentalDesigns.htm" ) ) {
            /*
             * What is the difference between getParameter vs. getAttribute? I have used getAttribute because the
             * MockHttpServletRequest does not have a setParameter method. getAttribute does not work when you fire up
             * the webapp, but getParameter does. This could be remedied if the MockHttpServletRequest provided a method
             * getParameter.
             */
            return new ModelAndView(
                    "experimentalDesign.GetAll.results.view",
                    "experimentalDesigns",
                    ( expressionExperimentService.findByName( request.getParameter( "name" ) ).getExperimentalDesigns() ) );
        } else
            throw new RuntimeException( "There is no view to match the url" );
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param messageSource The messageSource to set.
     */
    public void setMessageSource( ResourceBundleMessageSource messageSource ) {
        this.messageSource = messageSource;
    }

}
