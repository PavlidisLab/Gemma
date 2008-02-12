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
package ubic.gemma.web.controller.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;

/**
 * Responsible for display of the Gemma home page.
 * 
 * @author joseph
 * @version $Id$
 * @spring.bean id="mainMenuController"
 * @spring.property name="formView" value="mainMenu"
 * @spring.property name="successView" value="mainMenu"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 * @spring.property name="whatsNewService" ref="whatsNewService"
 */
public class MainMenuController extends BaseFormController {
    private static Log log = LogFactory.getLog( MainMenuController.class.getName() );

    private static final String COEXPRESSION_COOKIE_NAME = "coexpressionSearchCookie";

    private ExpressionExperimentService expressionExperimentService;
    private BioAssayService bioAssayService;
    private ArrayDesignService arrayDesignService;
    private TaxonService taxonService;
    private WhatsNewService whatsNewService;

    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @return the bioAssayService
     */
    public BioAssayService getBioAssayService() {
        return bioAssayService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        ModelAndView mav = new ModelAndView( getFormView() );

        return mav;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioAssayService the bioAssayService to set
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    public void setWhatsNewService( WhatsNewService whatsNewService ) {
        this.whatsNewService = whatsNewService;
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        CoexpressionSearchCommand csc = new CoexpressionSearchCommand();
        return csc;
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        // add species
        populateTaxonReferenceData( mapping );

        return mapping;
    }

    /*
     * This is the main method to display the home page.
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        ModelAndView mav = super.showForm( request, response, errors );
        Map<String, Long> stats = new HashMap<String, Long>();

        long bioAssayCount = bioAssayService.countAll();
        stats.put( "bioAssayCount", bioAssayCount );

        long arrayDesignCount = arrayDesignService.countAll();
        stats.put( "arrayDesignCount", arrayDesignCount );

        getTaxonEECounts( mav, stats );

        WhatsNew wn = getWhatsNewReport();
        if ( wn != null && wn.getDate() != null ) {
            mav.addObject( "whatsNew", wn );
        }

        // load taxon from cookie (if it exists)
        Taxon previousTaxon = loadTaxonFromCookie( request );
        if ( previousTaxon != null ) mav.addObject( "previousTaxonName", previousTaxon.getScientificName() );

        // load stringency from cookie (if it exists)
        Long previousStringency = loadStringencyFromCookie( request );
        if ( previousStringency != null ) mav.addObject( "previousStringency", previousStringency );

        // I like to time things.F
        timer.stop();
        log.info( "Home page processing: " + timer.getTime() + "ms" );
        return mav;
    }

    /**
     * @param mav
     * @param stats
     */
    @SuppressWarnings("unchecked")
    private void getTaxonEECounts( ModelAndView mav, Map<String, Long> stats ) {
        Map<String, Long> taxonCount = expressionExperimentService.getPerTaxonCount();
        long expressionExperimentCount = 0;
        Collection<Long> values = taxonCount.values();
        for ( Long count : values ) {
            expressionExperimentCount += count;
        }
        mav.addObject( "stats", stats );
        mav.addObject( "taxonCount", taxonCount );
        mav.addObject( "expressionExperimentCount", expressionExperimentCount );
    }

    /**
     * @return
     */
    private WhatsNew getWhatsNewReport() {
        WhatsNew wn = whatsNewService.retrieveReport();
        return wn;
    }

    /**
     * @param request
     * @param csc
     */
    private Long loadStringencyFromCookie( HttpServletRequest request ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return null;

        Long stringency = new Long( 3 );

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COEXPRESSION_COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    stringency = Long.parseLong( cookie.getString( "stringency" ) );
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
        return stringency;
    }

    /**
     * @param request
     * @param csc
     */
    private Taxon loadTaxonFromCookie( HttpServletRequest request ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return null;

        Taxon previousTaxon = null;

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COEXPRESSION_COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    previousTaxon = taxonService.findByScientificName( cookie.getString( "taxonScientificName" ) );
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
        return previousTaxon;
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateTaxonReferenceData( Map mapping ) {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxa.add( taxon );
        }
        Collections.sort( taxa, new Comparator<Taxon>() {
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }

}
