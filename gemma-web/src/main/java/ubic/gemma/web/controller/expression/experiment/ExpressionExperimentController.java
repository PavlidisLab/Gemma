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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.ontology.OntologyResource;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.taglib.displaytag.ExpressionExperimentValueObjectComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/expressionExperiment")
public class ExpressionExperimentController extends BackgroundProcessingMultiActionController {

    /**
     * Delete expression experiments.
     * 
     * @author pavlidis
     * @version $Id$
     */
    private class RemoveExpressionExperimentJob extends BackgroundControllerJob<ModelAndView> {

        ExpressionExperiment ee;

        public RemoveExpressionExperimentJob( ExpressionExperiment ee ) {
            super();
            this.ee = ee;
        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        @SuppressWarnings("synthetic-access")
        public ModelAndView call() throws Exception {
            ProgressJob job = init( "Deleting dataset: " + ee.getId() );
            provideAuthentication();

            expressionExperimentService.delete( ee );
            saveMessage( "Dataset " + ee.getShortName() + " removed from Database" );
            ee = null;

            ProgressManager.destroyProgressJob( job );
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );

        }
    }

    private class RemovePubMed extends BackgroundControllerJob<Boolean> {

        Long eeId;

        public RemovePubMed( HttpSession session, Long eeId ) {
            super( getMessageUtil(), session );
            this.eeId = eeId;
        }

        @SuppressWarnings("synthetic-access")
        public Boolean call() throws Exception {
            ProgressJob job = init( "Removing primary reference..." );
            provideAuthentication();

            job.updateProgress( "Loading experiment" );
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            if ( ee == null ) {
                return false;
            }
            expressionExperimentService.thawLite( ee );

            if ( ee.getPrimaryPublication() == null ) {
                return false;
            }

            job.updateProgress( "Removing reference" );
            ee.setPrimaryPublication( null );

            expressionExperimentService.update( ee );

            return true;
        }

    }

    private class UpdateBasics extends BackgroundControllerJob<ExpressionExperimentDetailsValueObject> {

        ExpressionExperimentDetailsValueObject command;
        private ExpressionExperimentService eeService;

        public UpdateBasics( ExpressionExperimentService expressionExperimentService,
                ExpressionExperimentDetailsValueObject command ) {
            super();
            this.eeService = expressionExperimentService;
            this.command = command;
        }

        public ExpressionExperimentDetailsValueObject call() throws Exception {
            ProgressJob job = init( "Updating expression experiment info..." );
            provideAuthentication();

            ExpressionExperiment ee = expressionExperimentService.load( command.getId() );
            if ( ee == null )
                throw new IllegalArgumentException( "Cannot locate or access experiment with id=" + command.getId() );

            if ( StringUtils.isNotBlank( command.getShortName() ) && !command.getShortName().equals( ee.getShortName() ) ) {
                if ( expressionExperimentService.findByShortName( command.getShortName() ) != null ) {
                    throw new IllegalArgumentException( "An experiment with short name '" + command.getShortName()
                            + "' already exists" );
                }
                ee.setShortName( command.getShortName() );
            }
            if ( StringUtils.isNotBlank( command.getName() ) && !command.getName().equals( ee.getName() ) ) {
                ee.setName( command.getName() );
            }
            if ( StringUtils.isNotBlank( command.getDescription() )
                    && !command.getDescription().equals( ee.getDescription() ) ) {
                ee.setDescription( command.getDescription() );
            }

            job.updateProgress( "Updating ..." );
            this.eeService.update( ee );

            return loadExpressionExperimentDetails( ee.getId() );
        }
    }

    private class UpdatePubMed extends BackgroundControllerJob<ExpressionExperimentDetailsValueObject> {

        Long eeId;
        String pubmedId;

        public UpdatePubMed( Long eeId, String pubmedId ) {
            super( getMessageUtil(), null );
            this.eeId = eeId;
            this.pubmedId = pubmedId;

        }

        public ExpressionExperimentDetailsValueObject call() throws Exception {
            ProgressJob job = init( "Updating primary reference..." );
            provideAuthentication();

            ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeId );
            if ( expressionExperiment == null )
                throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );

            BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubmedId );

            if ( publication != null ) {

                job.updateProgress( "Reference exists in system, associating..." );
                expressionExperiment.setPrimaryPublication( publication );
                expressionExperimentService.update( expressionExperiment );
            } else {
                job.updateProgress( "Searching pubmed on line .." );

                // search for pubmedId
                PubMedSearch pms = new PubMedSearch();
                Collection<String> searchTerms = new ArrayList<String>();
                searchTerms.add( pubmedId );
                Collection<BibliographicReference> publications = pms.searchAndRetrieveIdByHTTP( searchTerms );
                // check to see if there are publications found
                // if there are none, or more than one, add an error message and do nothing
                if ( publications.size() == 0 ) {
                    job.updateProgress( "No matching publication found" );
                    throw new IllegalArgumentException( "No matching publication found" );
                } else if ( publications.size() > 1 ) {
                    job.updateProgress( "Multiple matching publications found!" );
                    throw new IllegalArgumentException( "Multiple matching publications found!" );
                } else {
                    publication = publications.iterator().next();

                    DatabaseEntry pubAccession = DatabaseEntry.Factory.newInstance();
                    pubAccession.setAccession( pubmedId );
                    ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
                    ed.setName( "PubMed" );
                    pubAccession.setExternalDatabase( ed );

                    publication.setPubAccession( pubAccession );

                    // persist new publication
                    job.updateProgress( "Found new publication, associating ..." );

                    publication = ( BibliographicReference ) persisterHelper.persist( publication );
                    // publication = bibliographicReferenceService.findOrCreate( publication );
                    // assign to expressionExperiment
                    expressionExperiment.setPrimaryPublication( publication );

                    expressionExperimentService.update( expressionExperiment );
                }
            }
            ExpressionExperimentDetailsValueObject result = new ExpressionExperimentDetailsValueObject();
            result.setPubmedId( Integer.parseInt( pubmedId ) );
            result.setId( expressionExperiment.getId() );
            result.setPrimaryCitation( formatCitation( expressionExperiment.getPrimaryPublication() ) );
            return result;
        }

    }

    private static final Boolean AJAX = true;

    /*
     * If this is too long, tooltips break.
     */
    private static final int MAX_EVENT_DESCRIPTION_LENGTH = 200;

    private static final int TRIM_SIZE = 220;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditableUtil auditableUtil;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;

    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private PersisterHelper persisterHelper = null;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/deleteExpressionExperiment.html")
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

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( expressionExperiment + " not found" );
        }

        RemoveExpressionExperimentJob removeExpressionExperimentJob = new RemoveExpressionExperimentJob(
                expressionExperiment );

        return startJob( removeExpressionExperimentJob );

    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param id
     * @return taskId
     */
    public String deleteById( Long id ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) return null;
        RemoveExpressionExperimentJob removeExpressionExperimentJob = new RemoveExpressionExperimentJob(
                expressionExperiment );
        return run( removeExpressionExperimentJob );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/filterExpressionExperiments.html")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String searchString = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( searchString ) ) {
            this.saveMessage( request, "No search criteria provided" );
            return showAllExpressionExperiments( request, response );
        }

        Map<Class<?>, List<SearchResult>> searchResultsMap = searchService.search( SearchSettings
                .ExpressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        Collection<SearchResult> searchResults = searchResultsMap.get( ExpressionExperiment.class );

        if ( searchResults == null || searchResults.size() == 0 ) {
            this.saveMessage( request, "Your search yielded no results." );
            return showAllExpressionExperiments( request, response );
        }

        if ( searchResults.size() == 1 ) {
            this.saveMessage( request, "Search Criteria: " + searchString + "; " + searchResults.size()
                    + " Datasets matched." );
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + searchResults.iterator().next().getId() ) );
        }

        String list = "";
        for ( SearchResult ee : searchResults )
            list += ee.getId() + ",";

        this.saveMessage( request, "Search Criteria: " + searchString + "; " + searchResults.size()
                + " Datasets matched." );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + list ) );
    }

    /**
     * AJAX
     * 
     * @param query search string
     * @param taxonId (if null, all taxa are searched)
     * @return EE ids that match
     */
    public Collection<Long> find( String query, Long taxonId ) {
        log.info( "Search: " + query + " taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    /**
     * AJAX
     * 
     * @param e
     * @return
     */
    public Collection<AnnotationValueObject> getAnnotation( EntityDelegator e ) {
        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( e.getId() );
        expressionExperimentService.thawLite( expressionExperiment );
        Collection<AnnotationValueObject> annotations = new ArrayList<AnnotationValueObject>();
        for ( Characteristic c : expressionExperiment.getCharacteristics() ) {
            AnnotationValueObject annotationValue = new AnnotationValueObject();
            annotationValue.setId( c.getId() );
            annotationValue.setClassName( c.getCategory() );
            annotationValue.setTermName( c.getValue() );
            annotationValue.setEvidenceCode( c.getEvidenceCode().toString() );
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                annotationValue.setClassUri( vc.getCategoryUri() );
                String className = getLabelFromUri( vc.getCategoryUri() );
                if ( className != null ) annotationValue.setClassName( className );
                annotationValue.setTermUri( vc.getValueUri() );
                String termName = getLabelFromUri( vc.getValueUri() );
                if ( termName != null ) annotationValue.setTermName( termName );
            }
            annotations.add( annotationValue );
        }
        return annotations;
    }

    /**
     * AJAX call
     * 
     * @param id
     * @return a more informative description than the regular description 1st 120 characters of ee.description +
     *         Experimental Design information returned string contains HTML tags. TODO: Would be more generic if passed
     *         back a DescriptionValueObject that contains all the info necessary to reconstruct the HTML on the client
     *         side Currently only used by ExpressionExperimentGrid.js (row expander)
     */
    public String getDescription( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) return null;

        Collection<ExperimentalFactor> efs = ee.getExperimentalDesign().getExperimentalFactors();

        StringBuffer descriptive = new StringBuffer();

        String eeDescription = ee.getDescription() == null ? "" : ee.getDescription().trim();

        // Need to trim?
        if ( eeDescription.length() < TRIM_SIZE + 1 )
            descriptive.append( eeDescription );
        else
            descriptive.append( eeDescription.substring( 0, TRIM_SIZE ) + "...&nbsp;&nbsp;" );

        // Is there any factor info to add?
        if ( efs.size() < 1 ) return descriptive.append( "<b>(No Factors)</b>" ).toString();

        String efUri = "&nbsp;<a target='_blank' href='/Gemma/experimentalDesign/showExperimentalDesign.html?eeid="
                + ee.getId() + "'>(details)</a >";

        descriptive.append( "<b>Factors:</b>&nbsp;" );
        for ( ExperimentalFactor ef : efs ) {
            descriptive.append( ef.getName() + ", " );
        }

        // remove trailing "," and return as a string
        return descriptive.substring( 0, descriptive.length() - 2 ) + efUri;

    }

    /**
     * AJAX
     * 
     * @param e
     * @return
     */
    public Collection<DesignMatrixRowValueObject> getDesignMatrixRows( EntityDelegator e ) {

        if ( e == null || e.getId() == null ) return null;
        ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );
        if ( ee == null ) return null;

        expressionExperimentService.thawLite( ee );
        return DesignMatrixRowValueObject.Factory.getDesignMatrix( ee );
    }

    /**
     * AJAX
     * 
     * @param eeId
     * @return a collectino of factor value objects that represent the factors of a given experiment
     */
    public Collection<FactorValueValueObject> getExperimentalFactors( EntityDelegator e ) {

        if ( e == null || e.getId() == null ) return null;

        ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );

        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();

        if ( ee.getExperimentalDesign() == null ) return null;

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor factor : factors )
            result.add( new FactorValueValueObject( factor ) );

        return result;
    }

    /**
     * AJAX
     * 
     * @param id of an experimental factor
     * @return A collection of factor value objects for the specified experimental factor
     */
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator e ) {

        if ( e == null || e.getId() == null ) return null;

        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null ) return null;

        Collection<FactorValueValueObject> result = new HashSet<FactorValueValueObject>();

        Collection<FactorValue> values = ef.getFactorValues();
        for ( FactorValue value : values ) {
            result.add( new FactorValueValueObject( value ) );
        }

        return result;
    }

    /**
     * AJAX; Populate all the details.
     * 
     * @param id Identifier for the experiment
     */
    public ExpressionExperimentDetailsValueObject loadExpressionExperimentDetails( Long id ) {

        ExpressionExperiment ee = expressionExperimentService.load( id );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + id + " could be loaded" );
        }

        expressionExperimentService.thawLite( ee );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( ee.getId() );

        Collection<ExpressionExperimentValueObject> initialResults = expressionExperimentService.loadValueObjects( ids );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        getReportData( initialResults );

        ExpressionExperimentValueObject initialResult = initialResults.iterator().next();
        ExpressionExperimentDetailsValueObject finalResult = new ExpressionExperimentDetailsValueObject( initialResult );

        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        Collection<Long> adids = new HashSet<Long>();
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            adids.add( ad.getId() );
        }

        finalResult.setArrayDesigns( arrayDesignService.loadValueObjects( adids ) );

        /*
         * populate the publication and author information
         */
        finalResult.setDescription( ee.getDescription() );

        if ( ee.getPrimaryPublication() != null && ee.getPrimaryPublication().getPubAccession() != null ) {
            finalResult.setPrimaryCitation( formatCitation( ee.getPrimaryPublication() ) );
            String accession = ee.getPrimaryPublication().getPubAccession().getAccession();

            try {
                finalResult.setPubmedId( Integer.parseInt( accession ) );
            } catch ( NumberFormatException e ) {
                log.warn( "Pubmed id not formatted correctly: " + accession );
            }
        }

        return finalResult;

    }

    /**
     * AJAX - for display in tables. Don't retrieve too much detail.
     * 
     * @param ids of EEs to load
     * @return security-filtered set of value objects.
     */
    public Collection<ExpressionExperimentValueObject> loadExpressionExperiments( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return new HashSet<ExpressionExperimentValueObject>();
        }
        Collection<ExpressionExperimentValueObject> result = getFilteredExpressionExperimentValueObjects( null, ids,
                false );
        populateAnalyses( ids, result ); // FIXME make this optional.
        return result;
    }

    /**
     * AJAX. Data summarizing the status of experiments.
     * 
     * @param taxonId can be null
     * @param sIds - ids
     * @param limit If >0, get the most recently updated N experiments, where N <= limit.
     * @return
     */
    public Collection<ExpressionExperimentValueObject> loadStatusSummaries( Long taxonId, Collection<Long> ids,
            Integer limit ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<ExpressionExperimentValueObject> eeValObjectCol = null;

        boolean filterDataByUser = false;

        if ( SecurityService.isUserAdmin() ) {
            /* proceed, just being transparent */
        } else if ( SecurityService.isUserLoggedIn() ) {
            filterDataByUser = true;
        } else {
            /* Anonymous */
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        eeValObjectCol = getEEVOsForManager( taxonId, ids, filterDataByUser );

        if ( timer.getTime() > 1000 ) {
            log.info( "Phase 1 done in " + timer.getTime() + "ms" );
        }

        /*
         * Phase I is pretty fast - even over a tunnel, about 10 seconds for 1500 data sets.
         */

        timer.reset();
        timer.start();

        Map<Long, Date> recentDateInfo = getReportData( eeValObjectCol );

        if ( timer.getTime() > 1000 ) {
            log.info( "Get report data: " + timer.getTime() + "ms" );
        }

        timer.reset();
        timer.start();

        List<ExpressionExperimentValueObject> result = getRecentlyUpdated( recentDateInfo, eeValObjectCol, limit );
        if ( timer.getTime() > 1000 ) {
            log.info( "Sorting and filtering: " + timer.getTime() + "ms; limit=" + limit );
        }

        log.info( "Phase II done" );
        return result;
    }

    private Collection<ExpressionExperimentValueObject> getEEVOsForManager( Long taxonId, Collection<Long> ids,
            boolean filterDataByUser ) {
        Collection<ExpressionExperimentValueObject> eeValObjectCol;
        if ( taxonId != null ) {
            Taxon taxon = taxonService.load( taxonId );
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser );
        } else if ( ids == null || ids.isEmpty() ) {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser );
        } else {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser );
        }
        return eeValObjectCol;
    }

    /**
     * Remove the primary publication for the given expression experiment (by id). The reference is not actually deleted
     * from the system. AJAX
     * 
     * @param eeId
     * @return
     * @throws Exception
     */
    public String removePrimaryPublication( Long eeId ) throws Exception {
        RemovePubMed runner = new RemovePubMed( null, eeId );
        return run( runner );
    }

    /**
     * Show all experiments (optionally conditioned on either a taxon, or a list of ids)
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showAllExpressionExperiments.html")
    public ModelAndView showAllExpressionExperiments( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        String taxonId = request.getParameter( "taxonId" );

        Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();
        Collection<ExpressionExperimentValueObject> eeValObjectCol;
        ModelAndView mav = new ModelAndView( "expressionExperiments" );

        Collection<ExpressionExperimentValueObject> usersData;
        if ( taxonId != null ) {
            // if a taxon ID is specified, load all expression experiments for
            // this taxon
            try {
                Long tId = Long.parseLong( taxonId );
                Taxon taxon = taxonService.load( tId );
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, false );
                mav.addObject( "showAll", false );
                mav.addObject( "taxon", taxon );
            } catch ( NumberFormatException e ) {
                this.saveMessage( request, "Invalid taxon id, must be an integer" );
                return mav;
            }
        } else if ( sId == null ) {
            this.saveMessage( request, "Displaying all Datasets" );
            mav.addObject( "showAll", true );
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, false );
        } else {
            Collection<Long> eeIdList = new ArrayList<Long>();
            String[] idList = StringUtils.split( sId, ',' );
            try {
                for ( int i = 0; i < idList.length; i++ ) {
                    if ( StringUtils.isNotBlank( idList[i] ) ) {
                        eeIdList.add( Long.parseLong( idList[i] ) );
                    }
                }
            } catch ( NumberFormatException e ) {
                this.saveMessage( request, "Invalid ids, must be a list of integers separated by commas." );
                return mav;
            }
            mav.addObject( "showAll", false );
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, eeIdList, false );
        }
        expressionExperiments.addAll( eeValObjectCol );

        // sort expression experiments by name first
        Collections.sort( ( List<ExpressionExperimentValueObject> ) expressionExperiments,
                new ExpressionExperimentValueObjectComparator() );

        if ( SecurityService.isUserAdmin() ) {
            expressionExperimentReportService.fillEventInformation( expressionExperiments );
        }

        if ( !SecurityService.isUserAdmin() ) {
            auditableUtil.removeTroubledEes( expressionExperiments );
        }

        /*
         * Figure out which of the data sets belong to the current user (if anonymous, this won't do anything; is
         * administrator, they 'owned' is always true.)
         */
        usersData = this.getFilteredExpressionExperimentValueObjects( null,
                EntityUtils.getIds( expressionExperiments ), true );

        Long numExpressionExperiments = new Long( expressionExperiments.size() );

        mav.addObject( "expressionExperiments", expressionExperiments );

        mav.addObject( "eeids", EntityUtils.getIdStrings( usersData ).toArray( new String[] {} ) );

        mav.addObject( "numExpressionExperiments", numExpressionExperiments );
        return mav;

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showAllExpressionExperimentLinkSummaries.html")
    public ModelAndView showAllLinkSummaries( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "expressionExperimentLinkSummary" );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showBioAssaysFromExpressionExperiment.html")
    public ModelAndView showBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        request.setAttribute( "id", id );
        ModelAndView mv = new ModelAndView( "bioAssays" ).addObject( "bioAssays", expressionExperiment.getBioAssays() );
        mv.addObject( "expressionExperiment", expressionExperiment );
        return mv;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showBioMaterialsFromExpressionExperiment.html")
    public ModelAndView showBioMaterials( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Collection<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
        for ( BioAssay assay : bioAssays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials != null ) {
                bioMaterials.addAll( materials );
            }
        }

        ModelAndView mav = new ModelAndView( "bioMaterials" );
        if ( AJAX ) {
            StringBuilder buf = new StringBuilder();
            for ( BioMaterial bm : bioMaterials ) {
                buf.append( bm.getId() );
                buf.append( "," );
            }
            mav.addObject( "bioMaterialIdList", buf.toString().replaceAll( ",$", "" ) );
        }

        Long numBioMaterials = new Long( bioMaterials.size() );
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterials );

        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperiment.html")
    public ModelAndView showExpressionExperiment( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch timer = new StopWatch();
        timer.start();

        ExpressionExperimentImpl expressionExperiment;
        List<Long> ids = new ArrayList<Long>();
        Long id = null;

        if ( request.getParameter( "id" ) == null ) {

            String shortName = request.getParameter( "shortName" );

            if ( StringUtils.isNotBlank( shortName ) ) {
                expressionExperiment = ( ExpressionExperimentImpl ) expressionExperimentService
                        .findByShortName( shortName );

            } else {
                return redirectHome( request );
            }

        } else {

            try {
                id = Long.parseLong( request.getParameter( "id" ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "You must provide a valid numerical identifier" );
            }
            expressionExperiment = ( ExpressionExperimentImpl ) expressionExperimentService.load( id );
        }

        if ( expressionExperiment == null ) {
            return redirectHome( request );
        }

        id = expressionExperiment.getId();
        ids.add( id );

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" );

        mav.addObject( "expressionExperiment", expressionExperiment );

        getEventsOfInterest( expressionExperiment, mav );

        Collection<Characteristic> characteristics = expressionExperiment.getCharacteristics();
        mav.addObject( "characteristics", characteristics );

        Collection<QuantitationType> quantitationTypes = expressionExperimentService
                .getQuantitationTypes( expressionExperiment );
        mav.addObject( "quantitationTypes", quantitationTypes );
        mav.addObject( "qtCount", quantitationTypes.size() );

        AuditEvent lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( expressionExperiment,
                null );
        mav.addObject( "lastArrayDesignUpdate", lastArrayDesignUpdate );

        mav.addObject( "eeId", id );
        mav.addObject( "eeClass", ExpressionExperiment.class.getName() );

        mav.addObject( "hasCorrDistFile", ExpressionExperimentQCUtils.hasCorrDistFile( expressionExperiment ) );
        mav.addObject( "hasCorrMatFile", ExpressionExperimentQCUtils.hasCorrMatFile( expressionExperiment ) );
        mav.addObject( "hasPvalueDistFiles", ExpressionExperimentQCUtils.hasPvalueDistFiles( expressionExperiment ) );

        boolean isPrivate = securityService.isPrivate( expressionExperiment );
        mav.addObject( "isPrivate", isPrivate );

        if ( timer.getTime() > 200 ) {
            log.info( "Show Experiment was slow: id=" + id + " " + timer.getTime() + "ms" );
        }

        return mav;
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showExpressionExperimentSubSet.html")
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
     * AJAX
     * 
     * @param command
     * @return
     */
    public String updateBasics( ExpressionExperimentDetailsValueObject command ) {
        UpdateBasics runner = new UpdateBasics( expressionExperimentService, command );
        runner.setDoForward( false );
        return run( runner );
    }

    /**
     * AJAX. Associate the given pubmedId with the given expression experiment.
     * 
     * @param eeId
     * @param pubmedId
     * @return
     * @throws Exception
     */
    public String updatePubMed( Long eeId, String pubmedId ) throws Exception {
        UpdatePubMed runner = new UpdatePubMed( eeId, pubmedId );
        return run( runner );
    }

    /**
     * @param citation
     * @return
     */
    private String formatCitation( BibliographicReference citation ) {
        StringBuilder buf = new StringBuilder();

        if ( citation.getAuthorList() != null ) {
            String[] authors = StringUtils.split( citation.getAuthorList(), ";" );
            // if there are multiple authors, only display the first author
            if ( authors.length == 0 ) {
            } else if ( authors.length == 1 ) {
                buf.append( authors[0] + " " );
            } else {
                buf.append( authors[0] + " et al. " );
            }
        } else {
            buf.append( "[Unknown authors]" );
        }
        // display the publication year
        if ( citation.getPublicationDate() != null ) {
            Calendar pubDate = new GregorianCalendar();
            pubDate.setTime( citation.getPublicationDate() );
            buf.append( "(" + pubDate.get( Calendar.YEAR ) + ") " );
        } else {
            buf.append( "[Unknown date]" );
        }

        String volume = citation.getVolume();
        if ( StringUtils.isBlank( volume ) ) {
            volume = "[no vol.]";
        }

        String pages = citation.getPages();

        if ( StringUtils.isBlank( pages ) ) {
            pages = "[no pages]";
        }

        buf.append( citation.getTitle() + "; " + citation.getPublication() + ", " + volume + ": " + pages );

        return buf.toString();
    }

    /**
     * Trouble, validation, sample removal.
     * 
     * @param expressionExperiment
     * @param mav
     */
    private void getEventsOfInterest( ExpressionExperiment expressionExperiment, ModelAndView mav ) {
        AuditEvent troubleEvent = getLastTroubleEvent( expressionExperiment );
        if ( troubleEvent != null ) {
            mav.addObject( "troubleEvent", troubleEvent );
            auditEventService.thaw( troubleEvent );
            mav.addObject( "troubleEventDescription", StringUtils.abbreviate( StringEscapeUtils.escapeXml( troubleEvent
                    .toString() ), MAX_EVENT_DESCRIPTION_LENGTH ) );
        }
        AuditEvent validatedEvent = getLastValidationEvent( expressionExperiment );
        if ( validatedEvent != null ) {
            mav.addObject( "validatedEvent", validatedEvent );
            auditEventService.thaw( validatedEvent );
            mav.addObject( "validatedEventDescription", StringUtils.abbreviate( StringEscapeUtils
                    .escapeXml( validatedEvent.toString() ), MAX_EVENT_DESCRIPTION_LENGTH ) );
        }

        Collection<AuditEvent> sampleRemovalEvents = this.getSampleRemovalEvents( expressionExperiment );
        if ( sampleRemovalEvents.size() > 0 ) {
            AuditEvent event = sampleRemovalEvents.iterator().next();
            mav.addObject( "samplesRemoved", event ); // todo: handle multiple
            auditEventService.thaw( event );
            mav.addObject( "samplesRemovedDescription", StringUtils.abbreviate( StringEscapeUtils.escapeXml( event
                    .toString() ), MAX_EVENT_DESCRIPTION_LENGTH ) );
        }
    }

    /**
     * @param securedEEs
     * @return
     */
    private Collection<ExpressionExperimentValueObject> getExpressionExperimentValueObjects(
            Collection<ExpressionExperiment> securedEEs ) {

        if ( securedEEs.size() == 0 ) {
            return new HashSet<ExpressionExperimentValueObject>();
        }
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Long> ids = new HashSet<Long>();
        for ( ExpressionExperiment ee : securedEEs ) {
            ids.add( ee.getId() );
        }

        Collection<ExpressionExperimentValueObject> valueObjs = expressionExperimentService.loadValueObjects( ids );

        if ( timer.getTime() > 1000 ) {
            log.info( "Value objects in " + timer.getTime() + "ms" );
        }

        return valueObjs;
    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     * 
     * @param taxon can be null
     * @param eeids can be null; if taxon is non-null, this is ignored.
     * @param
     * @return Collection<ExpressionExperimentValueObject>
     */
    private Collection<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects( Taxon taxon,
            Collection<Long> eeIds, boolean filterDataForUser ) {

        Collection<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>();

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * FIXME remove troubled? Needs to be optional. For dataset managment page, don't.
         */

        /* Filtering for security happens here. */
        if ( filterDataForUser ) {
            try {
                if ( taxon != null ) {
                    securedEEs = expressionExperimentService.findByTaxon( taxon );
                } else if ( eeIds == null ) {
                    securedEEs = expressionExperimentService.loadMyExpressionExperiments();
                } else {
                    securedEEs = expressionExperimentService.loadMultiple( eeIds );
                }
            } catch ( AccessDeniedException e ) {
                log.info( "darn" );
                return new HashSet<ExpressionExperimentValueObject>();
            }
        } else {
            if ( taxon != null ) {
                securedEEs = expressionExperimentService.findByTaxon( taxon );
            } else if ( eeIds == null ) {
                securedEEs = expressionExperimentService.loadAll();
            } else {
                securedEEs = expressionExperimentService.loadMultiple( eeIds );
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "EEs in " + timer.getTime() + "ms" );
        }

        log.debug( "Loading value objects ..." );
        return getExpressionExperimentValueObjects( securedEEs );
    }

    /**
     * @param uri
     * @return
     */
    private String getLabelFromUri( String uri ) {
        OntologyResource resource = ontologyService.getResource( uri );
        if ( resource != null ) return resource.getLabel();

        return null;
    }

    /**
     * @param ee
     * @return
     */
    private AuditEvent getLastTroubleEvent( ExpressionExperiment ee ) {
        // Why doesn't this use expressionExperimentService.getLastTroubleEvent
        // ???
        AuditEvent event = auditTrailService.getLastTroubleEvent( ee );
        if ( event != null ) return event;

        // See if array design have trouble.
        for ( Object o : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            event = auditTrailService.getLastTroubleEvent( ( ArrayDesign ) o );
            if ( event != null ) return event;
        }

        return null;
    }

    private AuditEvent getLastValidationEvent( ExpressionExperiment ee ) {
        return auditTrailService.getLastValidationEvent( ee );
    }

    /**
     * @param recentDateInfo
     * @param expressionExperiments
     * @param limit Only this many will be returned (ties at the cutoff date will result in more), or all of them, but
     *        sorted.
     * @return
     */
    private List<ExpressionExperimentValueObject> getRecentlyUpdated( Map<Long, Date> recentDateInfo,
            Collection<ExpressionExperimentValueObject> expressionExperiments, int limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        List<ExpressionExperimentValueObject> results = new ArrayList<ExpressionExperimentValueObject>();

        if ( limit <= 0 || expressionExperiments.size() < limit ) {
            log.debug( "Too few studies to filter, returning all" );
            results.addAll( expressionExperiments );
            return results;
        }

        List<Date> dates = new ArrayList<Date>();
        dates.addAll( recentDateInfo.values() );
        Collections.sort( dates );

        if ( timer.getTime() > 1000 ) {
            log.info( "Date sort: " + timer.getTime() + "ms" );
        }

        timer.reset();
        timer.start();

        Date cutoff = null;
        int j = 0;
        for ( int i = dates.size() - 1; i >= 0; i-- ) {
            cutoff = dates.get( i );
            if ( ++j > limit ) {
                log.info( "Cutoff date: " + cutoff );
                break;
            }
        }

        Collection<Long> keepers = new HashSet<Long>();
        j = 0;
        for ( Long v : recentDateInfo.keySet() ) {
            Date d = recentDateInfo.get( v );
            if ( d.after( cutoff ) || d.equals( cutoff ) ) {
                keepers.add( v );
            }
        }

        for ( ExpressionExperimentValueObject vo : expressionExperiments ) {
            if ( keepers.contains( vo.getId() ) ) {
                results.add( vo );
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Filter by date: " + timer.getTime() + "ms" );
        }

        return results;

    }

    /**
     * Updates the value objects with event information and summaries
     * 
     * @param expressionExperiments
     * @return most recently changed information
     */
    private Map<Long, Date> getReportData( Collection<ExpressionExperimentValueObject> expressionExperiments ) {

        /*
         * This is only populated with experiments that have reports available on disk.
         */
        Map<Long, Date> lastUpdated = expressionExperimentReportService.fillLinkStatsFromCache( expressionExperiments );

        expressionExperimentReportService.fillAnnotationInformation( expressionExperiments );

        Map<Long, Date> eventDates = expressionExperimentReportService.fillEventInformation( expressionExperiments );

        for ( Long k : eventDates.keySet() ) {
            if ( lastUpdated.containsKey( k ) ) {
                if ( lastUpdated.get( k ).after( eventDates.get( k ) ) ) {
                    eventDates.put( k, lastUpdated.get( k ) );
                }
            }
        }

        assert eventDates.size() == expressionExperiments.size();
        return eventDates;
    }

    /**
     * @param ee
     * @return
     */
    private Collection<AuditEvent> getSampleRemovalEvents( ExpressionExperiment ee ) {
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( ee );
        Map<ExpressionExperiment, Collection<AuditEvent>> evMap = expressionExperimentService
                .getSampleRemovalEvents( ees );
        if ( evMap.containsKey( ee ) ) {
            return evMap.get( ee );
        }
        return new HashSet<AuditEvent>();
    }

    /**
     * Fill in information about analyses done on the experiments.
     * 
     * @param result
     */
    private void populateAnalyses( Collection<Long> eeids, Collection<ExpressionExperimentValueObject> result ) {

        if ( eeids.isEmpty() ) return;

        Map<Long, DifferentialExpressionAnalysis> analysisMap = differentialExpressionAnalysisService
                .findByInvestigationIds( eeids );
        for ( ExpressionExperimentValueObject eevo : result ) {
            if ( !analysisMap.containsKey( eevo.getId() ) ) {
                continue;
            }
            eevo.setDifferentialExpressionAnalysisId( analysisMap.get( eevo.getId() ).getId() );
        }
    }

    /**
     * @param request
     * @return
     */
    private ModelAndView redirectHome( HttpServletRequest request ) {
        this.addMessage( request, "errors.objectnotfound", new Object[] { "Expression Experiment" } );
        return new ModelAndView( "mainMenu.html" );
    }

}