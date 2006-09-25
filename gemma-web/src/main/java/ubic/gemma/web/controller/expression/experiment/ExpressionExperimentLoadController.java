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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="expressionExperimentLoadController"
 * @spring.property name="commandName" value="expressionExperimentLoadCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand"
 * @spring.property name="validator" ref="genericBeanValidator"
 * @spring.property name="formView" value="loadExpressionExperimentForm"
 * @spring.property name="successView" value="expressionExperiment.detail"
 * @spring.property name="geoDatasetService" ref="geoDatasetService"
 */
public class ExpressionExperimentLoadController extends BaseFormController {

    private static Log log = LogFactory.getLog( ExpressionExperimentLoadController.class.getName() );
    private GeoDatasetService geoDatasetService;

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView onSubmit( final HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        final ExpressionExperimentLoadCommand eeLoadCommand = ( ExpressionExperimentLoadCommand ) command;

        Map<Object, Object> model = new HashMap<Object, Object>();

        // validate an accession was entered (FIXME - should be done by validation
        if ( StringUtils.isBlank( eeLoadCommand.getAccession() ) ) {
            Object[] args = new Object[] { getText( "expressionExperiment.accession", request.getLocale() ) };
            errors.rejectValue( "accession", "errors.required", args, "Accession" );
            return showForm( request, response, errors );
        }

        log.info( "Loading " + eeLoadCommand.getAccession() );
        if ( eeLoadCommand.isLoadPlatformOnly() ) {
            log.info( "Only loading platform" );
        }
        // ProgressJob job;

        if ( eeLoadCommand.isLoadPlatformOnly() ) {
            // job = ProgressManager.createProgressJob( request.getRemoteUser(), "Loading arrayDesign(s)" );
            geoDatasetService.setLoadPlatformOnly( true );
            Collection<ArrayDesign> arrayDesigns = ( Collection<ArrayDesign> ) geoDatasetService
                    .fetchAndLoad( eeLoadCommand.getAccession() );
            model.put( "arrayDesigns", arrayDesigns ); // FIXME view should be different than default.
        } else {
            // job = ProgressManager.createProgressJob( request.getRemoteUser(), "Loading expression experiment" );
            ExpressionExperiment result = ( ExpressionExperiment ) geoDatasetService.fetchAndLoad( eeLoadCommand
                    .getAccession() );
            model.put( "expressionExperiment", result );
        }
        // ProgressManager.destroyProgressJob( job );

        return new ModelAndView( getSuccessView(), model );
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
     * @param geoDatasetService the geoDatasetService to set
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
    }

}
