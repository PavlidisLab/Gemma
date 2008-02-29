/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysisResultService;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.GemmaLinkUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.web.util.ConfigurationCookie;

/**
 * @author keshav
 * @version $Id$ *
 * @spring.bean id="differentialExpressionSearchController"
 * @spring.property name = "commandName" value="diffExpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand"
 * @spring.property name = "formView" value="diffExpressionSearchForm"
 * @spring.property name = "successView" value="diffExpressionResultsByExperiment"
 * @spring.property name = "differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name = "differentialExpressionAnalysisResultService"
 *                  ref="differentialExpressionAnalysisResultService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 */
public class DifferentialExpressionSearchController extends BaseFormController {

    private static final double DEFAULT_QVALUE_THRESHOLD = 0.01;

    private Log log = LogFactory.getLog( this.getClass() );

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService = null;

    private GeneService geneService = null;

    private TaxonService taxonService = null;

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
     * Populates drop downs.
     * 
     * @param request
     * @return Map
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        log.debug( "referenceData" );

        Map<String, List<? extends Object>> dropDownMap = new HashMap<String, List<? extends Object>>();

        Collection<Taxon> taxa = taxonService.loadAll();

        List<String> validTaxaNames = new ArrayList<String>();

        for ( Taxon t : taxa ) {
            if ( SupportedTaxa.contains( t ) ) {
                validTaxaNames.add( t.getScientificName() );
            }
        }

        dropDownMap.put( "taxa", validTaxaNames );

        return dropDownMap;
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
                    if ( StringUtils.isBlank( officialSymbol ) ) {
                        throw new Exception( "Invalid official symbol in cookie - " + officialSymbol );
                    }
                    diffSearchCommand.setGeneOfficialSymbol( officialSymbol );

                    String thresholdAsString = cookie.getString( "threshold" );
                    if ( StringUtils.isBlank( thresholdAsString ) ) {
                        throw new Exception( "Invalid threshold - " + thresholdAsString );
                    }
                    double threshold = Double.parseDouble( thresholdAsString );
                    diffSearchCommand.setThreshold( threshold );

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
        diffSearchCommand.setThreshold( DEFAULT_QVALUE_THRESHOLD );

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

        Cookie cookie = new DiffExpressionSearchCookie( diffCommand );
        response.addCookie( cookie );

        String officialSymbol = diffCommand.getGeneOfficialSymbol();

        double threshold = diffCommand.getThreshold();

        String taxonScientificName = diffCommand.getTaxonName();
        Taxon t = taxonService.findByScientificName( taxonScientificName );
        Gene gene = geneService.findByOfficialSymbol( officialSymbol, t );

        String message = null;
        if ( gene == null ) {
            message = "Gene could not be found for symbol: " + officialSymbol;
            errors.addError( new ObjectError( command.toString(), null, null, message ) );
            return processErrors( request, response, command, errors, null );
        }

        Collection<DifferentialExpressionValueObject> devos = new HashSet<DifferentialExpressionValueObject>();

        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( gene );
        if ( experimentsAnalyzed == null || experimentsAnalyzed.isEmpty() ) {
            message = "No experiments analyzed with differential evidence for gene: " + officialSymbol;
            errors.addError( new ObjectError( command.toString(), null, null, message ) );
            return processErrors( request, response, command, errors, null );
        }

        for ( ExpressionExperiment e : experimentsAnalyzed ) {
            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( gene, e );

            Collection<ProbeAnalysisResult> validResults = new HashSet<ProbeAnalysisResult>();
            for ( ProbeAnalysisResult r : results ) {
                double qval = r.getCorrectedPvalue();
                log.debug( qval );
                if ( qval < threshold ) {
                    validResults.add( r );

                    DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();

                    ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
                    eevo.setId( e.getId() );
                    eevo.setName( e.getName() );
                    eevo.setShortName( e.getShortName() );
                    devo.setExpressionExperiment( eevo );

                    devo.setP( r.getCorrectedPvalue() );
                    devo.setProbe( r.getProbe().getName() );
                }
            }

        }

        if ( devos.isEmpty() ) {
            message = "No experiments found for gene " + officialSymbol + " that meet the threshold " + threshold;
            errors.addError( new ObjectError( command.toString(), null, null, message ) );
            return processErrors( request, response, command, errors, null );
        }

        ModelAndView mav = new ModelAndView( this.getSuccessView() );

        mav.addObject( "differentialExpressionValueObjects", devos );

        mav.addObject( "numDiffResults", devos.size() );

        mav.addObject( "threshold", threshold );

        mav.addObject( "geneOfficialSymbol", officialSymbol );

        return mav;
    }

    /**
     * @param geneId
     * @param threshold
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();
        Gene g = geneService.load( geneId );
        if ( g == null ) return devos;
        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
            eevo.setId( ee.getId() );
            eevo.setShortName( ee.getShortName() );
            eevo.setName( ee.getName() );
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );

            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee, threshold );

            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( results );

            for ( ProbeAnalysisResult r : results ) {
                DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();
                devo.setExpressionExperiment( eevo );
                devo.setProbe( r.getProbe().getName() );
                devo.setExperimentalFactors( new HashSet<ExperimentalFactorValueObject>() );
                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    log.warn( "No experimentalfactor(s) for ProbeAnalysisResult: " + r.getId() );
                    continue;
                }
                for ( ExperimentalFactor ef : efs ) {
                    ExperimentalFactorValueObject efvo = new ExperimentalFactorValueObject();
                    efvo.setId( ef.getId() );
                    efvo.setName( ef.getName() );
                    efvo.setDescription( ef.getDescription() );
                    Characteristic category = ef.getCategory();
                    if ( category != null ) {
                        efvo.setCategory( category.getCategory() );
                        if ( category instanceof VocabCharacteristic )
                            efvo.setCategoryUri( ( ( VocabCharacteristic ) category ).getCategoryUri() );
                    }
                    devo.getExperimentalFactors().add( efvo );
                }
                devo.setP( r.getCorrectedPvalue() );
                devos.add( devo );

            }

        }
        return devos;
    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param differentialExpressionAnalysisResultService
     */
    public void setDifferentialExpressionAnalysisResultService(
            DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService ) {
        this.differentialExpressionAnalysisResultService = differentialExpressionAnalysisResultService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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
