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
package ubic.gemma.web.controller.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.analysis.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.util.ConfigurationCookie;

/**
 * @author keshav
 * @version $Id$ *
 * @spring.bean id="differentialExpressionSearchController"
 * @spring.property name = "commandName" value="diffExpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand"
 * @spring.property name = "formView" value="diffExpressionSearchForm"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name = "geneService" ref="geneService"
 */
public class DifferentialExpressionSearchController extends SimpleFormController {

    private Log log = LogFactory.getLog( this.getClass() );

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private GeneService geneService = null;

    private static final String COOKIE_NAME = "diffExpressionSearchCookie";

    /**
     * 
     */
    public DifferentialExpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {
        /* enter on a GET */

        DiffExpressionSearchCommand diffCommand = new DiffExpressionSearchCommand();

        DiffExpressionSearchCommand diffCommandFromCookie = loadCookie( request, diffCommand );
        if ( diffCommandFromCookie != null ) {
            diffCommand = diffCommandFromCookie;
        }

        return diffCommand;

    }

    /**
     * @param request
     * @param diffSearchCommand
     * @return
     */
    private DiffExpressionSearchCommand loadCookie( HttpServletRequest request,
            DiffExpressionSearchCommand diffSearchCommand ) {

        /*
         * If we don't have any cookies, just return. We probably won't get this situation as we'll always have at least
         * one cookie (the one with the JSESSION ID).
         */
        if ( request == null || request.getCookies() == null ) {
            return null;
        }

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    String officialSymbol = cookie.getString( "geneOfficalSymbol" );
                    if ( StringUtils.isBlank( officialSymbol ) )
                        throw new Exception( "Invalid official symbol in cookie - " + officialSymbol );

                    diffSearchCommand.setGeneOfficialSymbol( officialSymbol );

                    // Long id = Long.parseLong( cookie.getString( "geneId" ) );
                    // diffSearchCommand.setGeneId( id );
                    return diffSearchCommand;

                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    break;
                    // fine, just don't get a cookie.
                }
            }
        }

        /* If we've come this far, we have a cookie but not one that matches COOKIE_NAME. Provide friendly defaults. */
        diffSearchCommand.setGeneOfficialSymbol( "<gene sym>" );

        return diffSearchCommand;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @SuppressWarnings("unchecked")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        /* enter on a POST */

        DiffExpressionSearchCommand diffCommand = ( ( DiffExpressionSearchCommand ) command );

        String officialSymbol = diffCommand.getGeneOfficialSymbol();

        Cookie cookie = new DiffExpressionSearchCookie( diffCommand );
        response.addCookie( cookie );

        /* multiple genes can have the same symbol */
        Collection<Gene> genes = geneService.findByOfficialSymbol( officialSymbol );

        List<ExpressionExperiment> allExperiments = new ArrayList<ExpressionExperiment>();
        for ( Gene g : genes ) {
            Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService.find( g );

            allExperiments.addAll( experimentsAnalyzed );
        }

        String url = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                + "/expressionExperiment/showAllExpressionExperiments.html?id=";

        int i = 0;
        for ( ExpressionExperiment e : allExperiments ) {
            url = url + e.getId();
            if ( i < ( allExperiments.size() - 1 ) ) url = url + ",";
            i++;
        }

        return new ModelAndView( new RedirectView( url ) );

    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @author keshav
     */
    class DiffExpressionSearchCookie extends ConfigurationCookie {

        public DiffExpressionSearchCookie( DiffExpressionSearchCommand command ) {

            super( COOKIE_NAME );

            log.debug( "creating cookie" );

            // this.setProperty( "geneId", command.getGeneId() );
            String officialSymbol = command.getGeneOfficialSymbol();
            this.setProperty( "geneOfficialSymbol", officialSymbol );

            /* set cookie to expire after 2 days. */
            this.setMaxAge( 172800 );
            this.setComment( "User selections for differential expression search form." );
        }

    }
}
