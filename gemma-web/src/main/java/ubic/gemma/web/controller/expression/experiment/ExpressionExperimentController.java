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

import java.io.IOException;
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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
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
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.tasks.analysis.expression.UpdateEEDetailsCommand;
import ubic.gemma.tasks.analysis.expression.UpdatePubMedCommand;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.taglib.displaytag.ExpressionExperimentValueObjectComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/expressionExperiment")
public class ExpressionExperimentController extends AbstractTaskService {

    /**
     * Delete expression experiments.
     * 
     * @author pavlidis
     * @version $Id$
     */
    private class RemoveExpressionExperimentJob extends BackgroundJob<TaskCommand> {

        public RemoveExpressionExperimentJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );
            expressionExperimentService.delete( ee );

            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) ).addObject( "message",
                    "Dataset " + ee.getShortName() + " removed from Database" ) );

        }
    }

    private class RemovePubMed extends BackgroundJob<TaskCommand> {

        Long eeId;

        public RemovePubMed( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );

            expressionExperimentService.thawLite( ee );

            if ( ee.getPrimaryPublication() == null ) {
                return new TaskResult( command, false );
            }

            log.info( "Removing reference" );
            ee.setPrimaryPublication( null );

            expressionExperimentService.update( ee );

            return new TaskResult( command, true );
        }

    }

    private class UpdateBasics extends BackgroundJob<UpdateEEDetailsCommand> {

        public UpdateBasics( UpdateEEDetailsCommand command ) {
            super( command );

        }

        @Override
        public TaskResult processJob() {

            ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );
            if ( ee == null )
                throw new IllegalArgumentException( "Cannot locate or access experiment with id="
                        + command.getEntityId() );

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

            log.info( "Updating ..." );
            expressionExperimentService.update( ee );

            ExpressionExperimentDetailsValueObject eeDetails = loadExpressionExperimentDetails( ee.getId() );
            return new TaskResult( command, eeDetails );
        }
    }

    private class UpdatePubMed extends BackgroundJob<UpdatePubMedCommand> {

        public UpdatePubMed( UpdatePubMedCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            Long eeId = command.getEntityId();
            ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeId );
            if ( expressionExperiment == null )
                throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );

            String pubmedId = command.getPubmedId();
            BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubmedId );

            if ( publication != null ) {

                log.info( "Reference exists in system, associating..." );
                expressionExperiment.setPrimaryPublication( publication );
                expressionExperimentService.update( expressionExperiment );
            } else {
                log.info( "Searching pubmed on line .." );

                // search for pubmedId
                PubMedSearch pms = new PubMedSearch();
                Collection<String> searchTerms = new ArrayList<String>();
                searchTerms.add( pubmedId );
                Collection<BibliographicReference> publications;
                try {
                    publications = pms.searchAndRetrieveIdByHTTP( searchTerms );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                // check to see if there are publications found
                // if there are none, or more than one, add an error message and do nothing
                if ( publications.size() == 0 ) {
                    log.info( "No matching publication found" );
                    throw new IllegalArgumentException( "No matching publication found" );
                } else if ( publications.size() > 1 ) {
                    log.info( "Multiple matching publications found!" );
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
                    log.info( "Found new publication, associating ..." );

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
            return new TaskResult( command, result );
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
     * Exposed for AJAX calls.
     * 
     * @param id
     * @return taskId
     */
    public String deleteById( Long id ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) return null;
        RemoveExpressionExperimentJob job = new RemoveExpressionExperimentJob( new TaskCommand( id ) );
        startTask( job );
        return job.getTaskId();
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
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "No search criteria provided" );
        }

        Map<Class<?>, List<SearchResult>> searchResultsMap = searchService.search( SearchSettings
                .ExpressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        Collection<SearchResult> searchResults = searchResultsMap.get( ExpressionExperiment.class );

        if ( searchResults == null || searchResults.size() == 0 ) {

            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "Your search yielded no results." );

        }

        if ( searchResults.size() == 1 ) {
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + searchResults.iterator().next().getId() ) ).addObject( "message", "Search Criteria: "
                    + searchString + "; " + searchResults.size() + " Datasets matched." );
        }

        String list = "";
        for ( SearchResult ee : searchResults )
            list += ee.getId() + ",";

        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + list ) ).addObject( "message", "Search Criteria: " + searchString + "; " + searchResults.size()
                + " Datasets matched." );
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
                annotationValue.setObjectClass( VocabCharacteristic.class.getSimpleName() );
            } else {
                annotationValue.setObjectClass( Characteristic.class.getSimpleName() );
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

        Collection<Characteristic> tags = ee.getCharacteristics();
        if ( tags.size() > 0 ) {
            descriptive.append( "&nbsp;<b>Tags:</b>&nbsp;" );
            int i = 0;
            for ( Characteristic tag : tags ) {
                descriptive.append( tag.getValue() + ", " );
                if ( ++i > 5 ) {
                    descriptive.append( " [more tags not shown]" );
                    break;
                }
            }
        }

        descriptive.append( "&nbsp;<b>Factors:</b>&nbsp;" );
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

        // Set the parent taxon
        Taxon taxon = taxonService.load( initialResult.getTaxonId() );
        if ( taxon.getParentTaxon() != null ) {
            finalResult.setParentTaxon( taxon.getParentTaxon().getCommonName() );
            finalResult.setParentTaxonId( taxon.getParentTaxon().getId() );
        } else {
            finalResult.setParentTaxonId( taxon.getId() );
            finalResult.setParentTaxon( taxon.getCommonName() );
        }
        
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

        if ( eeValObjectCol.isEmpty() ) {
            return new HashSet<ExpressionExperimentValueObject>();
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetching basic data took: " + timer.getTime() + "ms" );
        }

        /*
         * Phase I is pretty fast - even over a tunnel, about 10 seconds for 1500 data sets.
         */

        timer.reset();
        timer.start();

        Map<Long, Date> recentDateInfo = getReportData( eeValObjectCol );

        if ( timer.getTime() > 1000 ) {
            log.info( "Filling in report data: " + timer.getTime() + "ms" );
        }

        timer.reset();
        timer.start();

        List<ExpressionExperimentValueObject> result = getRecentlyUpdated( recentDateInfo, eeValObjectCol, limit );

        if ( timer.getTime() > 1000 ) {
            log.info( "Sorting and filtering: " + timer.getTime() + "ms; limit=" + limit );
        }

        return result;
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
        RemovePubMed runner = new RemovePubMed( new TaskCommand( eeId ) );
        startTask( runner );
        return runner.getTaskId();
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

                /*
                 * TODO: handle case of multiple taxa or 'other'.
                 */

                Taxon taxon = taxonService.load( tId );

                if ( taxon == null ) {
                    return mav.addObject( "message", "Invalid taxon id" );
                }

                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, false );
                mav.addObject( "showAll", false );
                mav.addObject( "taxon", taxon );
            } catch ( NumberFormatException e ) {
                return mav.addObject( "message", "Invalid taxon id, must be an integer" );
            }
        } else if ( sId == null ) {
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
                return mav.addObject( "message", "Invalid ids, must be a list of integers separated by commas." );
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
        mav.addObject( "hasPCAFile", ExpressionExperimentQCUtils.hasPCAFile( expressionExperiment ) );
        mav.addObject( "hasNodeDegreeDistFile", ExpressionExperimentQCUtils
                .hasNodeDegreeDistFile( expressionExperiment ) );

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
    public String updateBasics( UpdateEEDetailsCommand command ) {
        UpdateBasics runner = new UpdateBasics( command );
        startTask( runner );
        return runner.getTaskId();
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
        UpdatePubMedCommand command = new UpdatePubMedCommand( eeId );
        command.setPubmedId( pubmedId );
        UpdatePubMed runner = new UpdatePubMed( command );
        startTask( runner );
        return runner.getTaskId();
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
     * @param taxonId
     * @param ids - takes precedence
     * @param filterDataByUser
     * @return
     */
    private Collection<ExpressionExperimentValueObject> getEEVOsForManager( Long taxonId, Collection<Long> ids,
            boolean filterDataByUser ) {
        Collection<ExpressionExperimentValueObject> eeValObjectCol;
        if ( taxonId != null && ( ids == null || ids.isEmpty() ) ) {
            Taxon taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser );
        } else if ( ids == null || ids.isEmpty() ) {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser );
        } else {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, ids, filterDataByUser );
        }
        return eeValObjectCol;
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
     * @param filterDataForUser if true, then only the data owned by the user are returned (this has no effect if you
     *        are an administrator)
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
        return new ModelAndView( "mainMenu.html" ).addObject( "message", "Not found" );
    }

    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }

}