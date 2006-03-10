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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionExperimentLoadController" name="/loadExpressionExperiment.html"
 * @spring.property name="commandName" value="expressionExperimentLoadCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand"
 * @spring.property name="validator" ref="genericBeanValidator"
 * @spring.property name="formView" value="loadExpressionExperimentForm"
 * @spring.property name="successView" value="loadExpressionExperimentFormResult"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class ExpressionExperimentLoadController extends BaseFormController {

    private PersisterHelper persisterHelper;

    private static Log log = LogFactory.getLog( ExpressionExperimentLoadController.class.getName() );

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView onSubmit( final HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        final ExpressionExperimentLoadCommand eeLoadCommand = ( ExpressionExperimentLoadCommand ) command;
        final HttpSession session = request.getSession();

        // final double startAt = Double.parseDouble(request.getParameter("startAt"));
        // final double endAt = Double.parseDouble(request.getParameter("endAt"));
        // final long sleepTime = Long.parseLong(request.getParameter("sleepTime"));

        // TODO make this generic instead of GEO-specific.

        // validate an accession was entered
        if ( StringUtils.isBlank( eeLoadCommand.getAccession() ) ) {
            Object[] args = new Object[] { getText( "expressionExperiment.accession", request.getLocale() ) };
            errors.rejectValue( "accession", "errors.required", args, "Accession" );
            return showForm( request, response, errors );
        }

        // Thread t = new Thread( new Runnable() {
        // public void run() { // Do your real processing here
        // // once validated.. TODO put this in its own thread and update use with progress.
        //               
        log.info( "Loading " + eeLoadCommand.getAccession() );
        if ( eeLoadCommand.isLoadPlatformOnly() ) {
            log.info( "Only loading platform" );
        }
        GeoDatasetService gds = new GeoDatasetService();
        GeoConverter geoConv = new GeoConverter();
        gds.setPersister( persisterHelper );
        gds.setConverter( geoConv );
        gds.setGenerator( new GeoDomainObjectGenerator() );
        gds.setLoadPlatformOnly( eeLoadCommand.isLoadPlatformOnly() );
        if ( eeLoadCommand.isLoadPlatformOnly() ) {
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) gds.fetchAndLoad( eeLoadCommand
                    .getAccession() );
            request.setAttribute( "arrayDesigns", arrayDesigns );
        } else {
            ExpressionExperiment result = ( ExpressionExperiment ) gds.fetchAndLoad( eeLoadCommand.getAccession() );
            request.setAttribute( "expressionExperiment", result );
        }
        // place the data into the request for retrieval on next page

        session.setAttribute( "stillProcessing", Boolean.FALSE );
        // }
        // } );
        // t.start();
        // response.sendRedirect(response.encodeRedirectURL("processing.html"));

        return new ModelAndView( getSuccessView() );
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
