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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.expression.experiment.ExpressionExperimentReportService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.taglib.displaytag.StringComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "expressionExperimentSubSetService" ref="expressionExperimentSubSetService"
 * @spring.property name = "probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @spring.property name = "expressionExperimentReportService" ref="expressionExperimentReportService"
 * @spring.property name="methodNameResolver" ref="expressionExperimentActions"
 * @spring.property name="searchService" ref="searchService"
 */
public class ExpressionExperimentController extends BackgroundProcessingMultiActionController {

    private ExpressionExperimentService expressionExperimentService = null;
    private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService = null;
    private ExpressionExperimentReportService expressionExperimentReportService = null;
    private SearchService searchService;

    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";


    /**
     * @param probe2ProbeCoexpressionService the probe2ProbeCoexpressionService to set
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param expressionExperimentSubSetService
     */
    public void setExpressionExperimentSubSetService(
            ExpressionExperimentSubSetService expressionExperimentSubSetService ) {
        this.expressionExperimentSubSetService = expressionExperimentSubSetService;
    }

    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            this.saveMessage( request, "No search critera provided" );
            return showAll( request, response );
        }

        List<ExpressionExperiment> searchResults = searchService.compassExpressionSearch( filter );

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            this.saveMessage( request, "Your search yielded no results." );
            return showAll( request, response );
        }

        String list = "";
        for ( ExpressionExperiment ee : searchResults )
            list += ee.getId() + ",";

        this.saveMessage( request, "Search Criteria: " + filter );
        this.saveMessage( request, searchResults.size() + " Datasets matched your search." );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + list ) );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        if ( request.getParameter( "id" ) == null ) {
            // should be a validator error on submit
            return redirectToList( request );
        }

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validator error on submit
            return redirectToList( request );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            return redirectToList( request );
        }
        request.setAttribute( "id", id );

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" ).addObject( "expressionExperiment",
                expressionExperiment );

        // Set s = expressionExperimentService.getQuantitationTypeCountById( id ).entrySet();
        // mav.addObject( "qtCountSet", s );
        Collection quantitationTypes = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        mav.addObject( "quantitationTypes", quantitationTypes );

        // add arrayDesigns used, by name
        Collection<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        for ( BioAssay assay : bioAssays ) {
            ArrayDesign design = assay.getArrayDesignUsed();
            if ( !arrayDesigns.contains( design ) ) {
                arrayDesigns.add( design );
            }
        }

        mav.addObject( "arrayDesigns", arrayDesigns );
        // add count of designElementDataVectors
        Long designElementDataVectorCount = new Long( expressionExperimentService
                .getDesignElementDataVectorCountById( id ) );
        mav.addObject( "designElementDataVectorCount", designElementDataVectorCount );

        Integer eeLinks = probe2ProbeCoexpressionService.countLinks( expressionExperiment );
        mav.addObject( "eeCoexpressionLinks", eeLinks );
        return mav;
    }

    private ModelAndView redirectToList( HttpServletRequest request ) {
        this.addMessage( request, "errors.objectnotfound", new Object[] { "Expression Experiment " } );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        Map m = expressionExperimentService.getQuantitationTypeCountById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperimentSubSet subset = expressionExperimentSubSetService.load( id );
        if ( subset == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        // request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", subset.getBioAssays() );
    }

    /**
     * Shows a bioassay view of a single expression experiment subset.
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showExpressionExperimentSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        String taxonId = request.getParameter( "taxonId" );
        Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();
        
        // if a taxon ID is specified, load all expression experiments for this taxon
        if (taxonId != null) {
            Taxon taxon = Taxon.Factory.newInstance();
            Long tId = Long.parseLong( taxonId );
            taxon.setId( tId );
            //taxon = taxonService.find( taxon );
            Collection<ExpressionExperimentValueObject> eeValObjectCol = this
            .getExpressionExperimentValueObjects( expressionExperimentService.getByTaxon( taxon ) );
            
            expressionExperiments.addAll( eeValObjectCol );
        }
        // if no IDs are specified, then load all expressionExperiments
        else if ( sId == null ) {
            this.saveMessage( request, "Displaying all Datasets" );
            // TODO refactor this and make more generic (that is, turning securable objects into value objects).
            // I did this because I need to go through security.
            Collection<ExpressionExperimentValueObject> eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( null );
            expressionExperiments.addAll( eeValObjectCol );
            // expressionExperiments.addAll( expressionExperimentService.loadAllValueObjects() );
        }
        // if ids are specified, then display only those expressionExperiments
        else {
            Collection eeList = new ArrayList<ExpressionExperiment>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                if ( StringUtils.isNotBlank( idList[i] ) ) {
                    //ids.add( new Long( idList[i] ) );
                    ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
                    ee.setId( new Long(idList[i]) );
                    eeList.add(ee);
                }
            }
            Collection<ExpressionExperimentValueObject> eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( eeList );
            expressionExperiments.addAll( eeValObjectCol );
            // expressionExperiments.addAll( expressionExperimentService.loadValueObjects( ids ) );
        }
        // sort expression experiments by name first
        Collections.sort( ( List<ExpressionExperimentValueObject> ) expressionExperiments, new StringComparator() {
            public int compare( Object o1, Object o2 ) {
                String s1 = ( ( ExpressionExperimentValueObject ) o1 ).getName();
                String s2 = ( ( ExpressionExperimentValueObject ) o2 ).getName();
                int comparison = s1.compareToIgnoreCase( s2 );
                return comparison;
            }
        } );
        Long numExpressionExperiments = new Long( expressionExperiments.size() );
        ModelAndView mav = new ModelAndView( "expressionExperiments" );
        mav.addObject( "expressionExperiments", expressionExperiments );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );
        return mav;

    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     * 
     * @param eeCol
     * @return Collection<ExpressionExperimentValueObject>
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects(
            Collection<ExpressionExperiment> eeCol ) {

        log.debug( SecurityService.getPrincipal() );

        Collection<ExpressionExperiment> allEEs = expressionExperimentService.loadAll();
        Collection<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>();
        
        if ( eeCol == null ) { 
            securedEEs = allEEs;
        }
        else {
            Collection ids = new LinkedHashSet();
            for ( ExpressionExperiment ee : eeCol ) {
                ids.add( ee.getId() );
            }
            securedEEs = expressionExperimentService.load( ids );
        }
        return getExpressionExperimentValueObjects( securedEEs );
    }

    /**
     * @param securedEEs
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperimentValueObject> getExpressionExperimentValueObjects( Collection<ExpressionExperiment> securedEEs ) {
        // FIXME use the ee, not the id
        Collection ids = new LinkedHashSet();
        for ( ExpressionExperiment ee : securedEEs ) {
            ids.add( ee.getId() );
        }
        return expressionExperimentService.loadValueObjects( ids );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView showAllLinkSummaries( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();

        // if no IDs are specified, then load all expressionExperiments
        if ( sId == null ) {
            this.saveMessage( request, "Displaying all Datasets" );
            Collection<ExpressionExperimentValueObject> eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( null );
            expressionExperiments.addAll( eeValObjectCol );
            // expressionExperiments.addAll( expressionExperimentService.loadAllValueObjects() );
        }

        // if ids are specified, then display only those expressionExperiments
        else {
            Collection ids = new ArrayList<Long>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                if ( StringUtils.isNotBlank( idList[i] ) ) {
                    ids.add( new Long( idList[i] ) );
                }
            }
            Collection<ExpressionExperimentValueObject> eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( null );
            expressionExperiments.addAll( eeValObjectCol );
            // expressionExperiments.addAll( expressionExperimentService.loadValueObjects( ids ) );
        }

        // load cached data
        expressionExperimentReportService.fillLinkStatsFromCache( expressionExperiments );
        // sort expression experiments by name first
        Collections.sort( ( List<ExpressionExperimentValueObject> ) expressionExperiments, new StringComparator() {
            public int compare( Object o1, Object o2 ) {
                String s1 = ( ( ExpressionExperimentValueObject ) o1 ).getName();
                String s2 = ( ( ExpressionExperimentValueObject ) o2 ).getName();
                int comparison = s1.compareToIgnoreCase( s2 );
                return comparison;
            }
        } );
        Long numExpressionExperiments = new Long( expressionExperiments.size() );
        ModelAndView mav = new ModelAndView( "expressionExperimentLinkSummary" );
        mav.addObject( "expressionExperiments", expressionExperiments );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );
        return mav;

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {

        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new EntityNotFoundException( "There was no valid identifier." );
        }

        if ( id == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ExpressionExperiment expressionExperiment = expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( expressionExperiment + " not found" );
        }

        return startJob( request, new RemoveExpressionExperimentJob( request, expressionExperiment,
                expressionExperimentService ) );

    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( sId == null ) {
            return startJob( request, new GenerateSummary( request, expressionExperimentReportService ) );
        } else {
            Collection ids = new ArrayList<Long>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                if ( StringUtils.isNotBlank( idList[i] ) ) {
                    ids.add( new Long( idList[i] ) );
                }
            }
            expressionExperimentReportService.generateSummaryObjects( ids );
            String idStr = StringUtils.join( ids.toArray(), "," );
            return new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html" ) );
        }

    }

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    class RemoveExpressionExperimentJob extends BackgroundControllerJob<ModelAndView> {

        ExpressionExperimentService expressionExperimentService;
        ExpressionExperiment ee;

        public RemoveExpressionExperimentJob( HttpServletRequest request, ExpressionExperiment ee,
                ExpressionExperimentService expressionExperimentService ) {
            super( request, getMessageUtil() );
            this.expressionExperimentService = expressionExperimentService;
            this.ee = ee;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            expressionExperimentService.thawLite( ee );
            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Deleting dataset: " + ee.getId() );

            expressionExperimentService.delete( ee );
            saveMessage( "Dataset " + ee.getShortName() + " removed from Database" );
            ee = null;

            ProgressManager.destroyProgressJob( job );
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );

        }
    }

    class GenerateSummary extends BackgroundControllerJob<ModelAndView> {

        private ExpressionExperimentReportService expressionExperimentReportService;
        private Collection ids;

        public GenerateSummary( HttpServletRequest request,
                ExpressionExperimentReportService expressionExperimentReportService ) {
            super( request, getMessageUtil() );
            this.expressionExperimentReportService = expressionExperimentReportService;
            ids = null;
        }

        public GenerateSummary( HttpServletRequest request,
                ExpressionExperimentReportService expressionExperimentReportService, Collection id ) {
            super( request, getMessageUtil() );
            this.expressionExperimentReportService = expressionExperimentReportService;
            this.ids = id;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Generating ArrayDesign Report summary" );

            if ( ids == null ) {
                saveMessage( "Generated summary for all experiments" );
                job.updateProgress( "Generated summary for all experiments" );
                expressionExperimentReportService.generateSummaryObjects();
            } else {
                saveMessage( "Generating summary for experiment" );
                job.updateProgress( "Generating summary for specified experiment" );
                expressionExperimentReportService.generateSummaryObjects( ids );
            }
            ProgressManager.destroyProgressJob( job );
            String idStr = StringUtils.join( ids.toArray(), "," );
            return new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?id=" + idStr ) );

        }
    }

    /**
     * @param expressionExperimentReportService the expressionExperimentReportService to set
     */
    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

}