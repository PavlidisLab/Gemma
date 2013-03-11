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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.common.BackgroundJob;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.Status;
import ubic.gemma.model.common.auditAndSecurity.StatusService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationMissingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.tasks.analysis.expression.UpdateEEDetailsCommand;
import ubic.gemma.tasks.analysis.expression.UpdatePubMedCommand;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.persistence.SessionListManager;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.taglib.expression.experiment.ExperimentQCTag;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.view.TextView;

/**
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping(value = { "/expressionExperiment", "/ee" })
public class ExpressionExperimentController {
    private static final Log log = LogFactory.getLog( ExpressionExperimentController.class.getName() );

    /**
     * Delete expression experiments.
     * 
     * @author pavlidis
     * @version $Id$
     */
    private class RemoveExpressionExperimentJob extends BackgroundJob<TaskCommand, TaskResult> {

        public RemoveExpressionExperimentJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            expressionExperimentService.delete( command.getEntityId() );

            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) ).addObject( "message",
                    "Dataset id: " + command.getEntityId() + " removed from Database" ) );

        }
    }

    private class RemovePubMed extends BackgroundJob<TaskCommand, TaskResult> {

        public RemovePubMed( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );

            ee = expressionExperimentService.thawLite( ee );

            if ( ee.getPrimaryPublication() == null ) {
                return new TaskResult( command, false );
            }

            log.info( "Removing reference" );
            ee.setPrimaryPublication( null );

            expressionExperimentService.update( ee );

            return new TaskResult( command, true );
        }

    }

    private class UpdatePubMed extends BackgroundJob<UpdatePubMedCommand, TaskResult> {

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
            result.setPrimaryCitation( CitationValueObject.convert2CitationValueObject( expressionExperiment
                    .getPrimaryPublication() ) );
            return new TaskResult( command, result );
        }

    }

    private static final Boolean AJAX = true;

    private static final int TRIM_SIZE = 800;

    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSearchService expressionExperimentSearchService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    private SearchService searchService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private TaxonService taxonService;

    @Autowired
    private SVDService svdService;
    @Autowired
    private WhatsNewService whatsNewService;
    @Autowired
    private SessionListManager sessionListManager;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    /**
     * AJAX call for remote paging store security isn't incorporated in db query, so paging needs to occur at higher
     * level. 1. a db call returns all experiments, which are filtered by the service method 2. if the user is an admin,
     * we filter out the troubled experiments 3. an appropriate page-sized chunk is then taken from this (filtered) list
     * 4. another round of db calls create and fill value objects for this chunk 5. value objects are returned
     * 
     * @param batch
     * @return
     */
    public JsonReaderResponse<ExpressionExperimentValueObject> browse( ListBatchCommand batch ) {

        if ( batch.getLimit() == 0 ) {
            batch.setStart( 0 );
        }

        int limit = batch.getLimit();
        int origStart = batch.getStart();
        // have to load entities instead of directly loading value objects because we need security filtering
        List<ExpressionExperimentValueObject> records = loadAllValueObjectsOrdered( batch );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperimentVOs( records );
        }

        assert records != null;

        /*
         * can't just do expressionExperimentService.countAll() because this will count experiments the user may not
         * have access to
         */
        int count = records.size();

        int start = origStart;
        int stop = Math.min( origStart + limit, records.size() );

        if ( batch.getLimit() == 0 ) {
            stop = records.size();
        }

        List<ExpressionExperimentValueObject> recordsSubset = records.subList( start, stop );

        // this populates securityInfo TODO populate security info in filter
        // List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
        // getExpressionExperimentValueObjects( recordsSubset ) );

        // if admin, want to show why experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.getEventInformation( recordsSubset );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                recordsSubset, count );

        return returnVal;
    }

    /**
     * AJAX
     * 
     * @param batch
     * @return
     */
    public JsonReaderResponse<ExpressionExperimentValueObject> browseByTaxon( ListBatchCommand batch, Long taxonId ) {

        int origLimit = batch.getLimit();
        int origStart = batch.getStart();
        if ( taxonId == null ) {
            return browse( batch );
        }
        Taxon taxon = taxonService.load( taxonId );
        if ( taxon == null ) {
            log.info( "Attempted to browse experiments by taxon with id = " + taxonId
                    + ", but this id is invalid. Browsing without taxon restriction." );
            return browse( batch );
        }
        List<ExpressionExperimentValueObject> records = loadAllValueObjectsOrdered( batch, taxon );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperimentVOs( records );
        }

        /*
         * can't just do countAll because this will count experiments the user may not have access to Integer count =
         * expressionExperimentService.countAll();
         */
        int count = records.size();

        int pSize = Math.min( origStart + origLimit, records.size() );

        // batch.getLimit = 0 when download all as text
        if ( batch.getLimit() == 0 ) {
            pSize = count;
        }

        List<ExpressionExperimentValueObject> recordsSubset = records.subList( origStart, pSize );

        // List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
        // getExpressionExperimentValueObjects( records.subList( origStart, pSize ) ) );

        // if admin, want to show if experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.getEventInformation( recordsSubset );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                recordsSubset, count );

        return returnVal;
    }

    /**
     * AJAX call for remote paging store
     * 
     * @param batch
     * @return
     */
    public JsonReaderResponse<ExpressionExperimentValueObject> browseSpecificIds( ListBatchCommand batch,
            Collection<Long> ids ) {

        if ( batch.getLimit() == 0 ) {
            batch.setLimit( ids.size() );
            batch.setStart( 0 );
        }

        int origLimit = batch.getLimit();
        int origStart = batch.getStart();

        Set<Long> noDupIds = new HashSet<Long>( ids );
        List<ExpressionExperimentValueObject> records = loadAllValueObjectsOrdered( batch, noDupIds );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperimentVOs( records );
        }

        int pSize = Math.min( origStart + origLimit, records.size() );

        // batch.getLimit = 0 when download all as text
        if ( batch.getLimit() == 0 ) {
            pSize = records.size();
        }

        List<ExpressionExperimentValueObject> recordsSubset = records.subList( origStart, pSize );

        // List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
        // getExpressionExperimentValueObjects( records.subList( origStart, pSize ) ) );

        // if admin, want to show if experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.getEventInformation( recordsSubset );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                recordsSubset, records.size() );
        return returnVal;
    }

    /**
     * AJAX clear entries in caches relevant to experimental design for the experiment passed in. The caches cleared are
     * the processedDataVectorCache and the caches held in ExperimentalDesignVisualizationService
     * 
     * @param eeId
     * @return msg if error occurred or empty string if successful
     */
    public void clearFromCaches( Long eeId ) {
        expressionExperimentReportService.evictFromCache( eeId );
    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the experiment and whether they can edit it
     * 
     * @param
     * @return
     */
    public boolean canCurrentUserEditExperiment( Long eeId ) {
        boolean userCanEditGroup = false;
        try {
            userCanEditGroup = securityService.isEditable( expressionExperimentService.load( eeId ) );
        } catch ( org.springframework.security.access.AccessDeniedException ade ) {
            return false;
        }
        return userCanEditGroup;
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param id
     * @return taskId
     */
    public String deleteById( Long id ) {
        if ( id == null ) return null;
        RemoveExpressionExperimentJob job = new RemoveExpressionExperimentJob( new TaskCommand( id ) );
        return taskRunningService.submitLocalJob( job );
    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the experiment and whether they can edit it
     * 
     * @param
     * @return
     */
    public boolean doesCurrentUserOwnExperiment( Long eeId ) {
        boolean userOwnsGroup = false;
        try {
            userOwnsGroup = securityService.isOwnedByCurrentUser( expressionExperimentService.load( eeId ) );
        } catch ( org.springframework.security.access.AccessDeniedException ade ) {
            return false;
        }
        return userOwnsGroup;
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

        Collection<Long> ids = expressionExperimentService.filter( searchString );

        if ( ids.isEmpty() ) {

            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "Your search yielded no results." );

        }

        if ( ids.size() == 1 ) {
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + ids.iterator().next() ) ).addObject( "message",
                    "Search Criteria: " + searchString + "; " + ids.size() + " Datasets matched." );
        }

        String list = "";
        for ( Long id : ids ) {
            list += id + ",";
        }

        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + list ) ).addObject( "message", "Search Criteria: " + searchString + "; " + ids.size()
                + " Datasets matched." );
    }

    /**
     * AJAX TODO --- include a search of subsets.
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
        return expressionExperimentService.getAnnotations( e.getId() );
    }

    /**
     * AJAX call
     * 
     * @param id
     * @return a more informative description than the regular description 1st 120 characters of ee.description +
     *         Experimental Design information returned string contains HTML tags.
     *         <p>
     *         TODO: Would be more generic if passed back a DescriptionValueObject that contains all the info necessary
     *         to reconstruct the HTML on the client side Currently only used by ExpressionExperimentGrid.js (row
     *         expander)
     */
    public String getDescription( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) return null;

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> efs = ee.getExperimentalDesign().getExperimentalFactors();

        StringBuffer descriptive = new StringBuffer();

        String eeDescription = ee.getDescription() == null ? "" : ee.getDescription().trim();

        // Need to trim?
        if ( eeDescription.length() < TRIM_SIZE + 1 )
            descriptive.append( eeDescription );
        else
            descriptive.append( eeDescription.substring( 0, TRIM_SIZE ) + "...&nbsp;&nbsp;" );

        // Is there any factor info to add?
        if ( efs.size() < 1 ) return descriptive.append( "</br><b>(No Factors)</b>" ).toString();

        String efUri = "&nbsp;<a target='_blank' href='/Gemma/experimentalDesign/showExperimentalDesign.html?eeid="
                + ee.getId() + "'>(details)</a >";
        int MAX_TAGS_TO_SHOW = 15;
        Collection<Characteristic> tags = ee.getCharacteristics();
        if ( tags.size() > 0 ) {
            descriptive.append( "</br>&nbsp;<b>Tags:</b>&nbsp;" );
            int i = 0;
            for ( Characteristic tag : tags ) {
                descriptive.append( tag.getValue() + ", " );
                if ( ++i > MAX_TAGS_TO_SHOW ) {
                    descriptive.append( " [more tags not shown]" );
                    break;
                }
            }

        }

        descriptive.append( "</br>&nbsp;<b>Factors:</b>&nbsp;" );
        for ( ExperimentalFactor ef : efs ) {
            if ( !ExperimentalDesignUtils.isBatch( ef ) ) {
                descriptive.append( ef.getName() + " (" + ef.getDescription() + "), " );
            }
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

        ee = expressionExperimentService.thawLite( ee );
        return DesignMatrixRowValueObject.Factory.getDesignMatrix( ee, true ); // ignore "batch"
    }

    /**
     * AJAX
     * <p>
     * FIXME why is this using FactorValueValueObject? The constructor doesn't seem correct; should use
     * ExperimentalFactorValueObject(factor).
     * 
     * @param eeId
     * @return a collection of factor value objects that represent the factors of a given experiment
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
     * Used to include the html for the qc table in an ext panel (without using a tag) (This method should probably be
     * in a service?)
     * 
     * @param ee
     */
    public String getQCTagHTML( ExpressionExperiment ee ) {
        ExperimentQCTag qc = new ExperimentQCTag();
        qc.setEe( ee.getId() );
        qc.setEeManagerId( ee.getId() + "-eemanager" );
        qc.setHasCorrMat( sampleCoexpressionMatrixService.hasMatrix( ee ) );
        qc.setHasNodeDegreeDist( ExpressionExperimentQCUtils.hasNodeDegreeDistFile( ee ) );
        qc.setHasPCA( svdService.hasPca( ee.getId() ) );
        qc.setNumFactors( ExpressionExperimentQCUtils.numFactors( ee ) );
        return qc.getQChtml();
    }

    /**
     * AJAX method to get data for database summary table, returned as a JSON object the slow part here is loading each
     * new or updated object in whatsNewService.retrieveReport() -> fetch()
     * 
     * @return
     */
    public JSONObject loadCountsForDataSummaryTable() {

        JSONObject summary = new JSONObject();
        net.sf.json.JSONArray taxonEntries = new net.sf.json.JSONArray();

        long bioAssayCount = bioAssayService.countAll();
        long arrayDesignCount = arrayDesignService.countAll();
        Map<Taxon, Long> unsortedEEsPerTaxon = expressionExperimentService.getPerTaxonCount();

        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<Taxon, Long>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return o1.getScientificName().compareTo( o2.getScientificName() );
            }
        } );
        LinkedHashMap<String, Long> eesPerTaxonName = new LinkedHashMap<String, Long>();

        long expressionExperimentCount = 0; // expressionExperimentService.countAll();
        for ( Iterator<Taxon> it = unsortedEEsPerTaxon.keySet().iterator(); it.hasNext(); ) {
            Taxon t = it.next();
            Long c = unsortedEEsPerTaxon.get( t );

            eesPerTaxon.put( t, c );
            eesPerTaxonName.put( t.getScientificName(), c );

            // hide 'uncommon' taxa from this table. See bug 2052
            // this bug proposed being able to make taxa private
            // it is now marked as "won't fix" so I assume they want everything to be shown
            // so I'll comment this out (for now)
            /*
             * if ( c < 10 ) { otherTaxaEECount += c; //it.remove(); }
             */
            expressionExperimentCount += c;
        }

        // this is the slow part
        WhatsNew wn = whatsNewService.retrieveReport();

        if ( wn == null ) {
            Calendar c = Calendar.getInstance();
            Date date = c.getTime();
            date = DateUtils.addWeeks( date, -1 );
            wn = whatsNewService.getReport( date );

        }
        if ( wn != null ) {
            // Get count for new assays
            int newAssayCount = wn.getNewAssayCount();

            Collection<Long> newExpressionExperimentIds = ( wn.getNewExpressionExperiments() != null ) ? EntityUtils
                    .getIds( wn.getNewExpressionExperiments() ) : new ArrayList<Long>();
            Collection<Long> updatedExpressionExperimentIds = ( wn.getUpdatedExpressionExperiments() != null ) ? EntityUtils
                    .getIds( wn.getUpdatedExpressionExperiments() ) : new ArrayList<Long>();

            int newExpressionExperimentCount = ( wn.getNewExpressionExperiments() != null ) ? wn
                    .getNewExpressionExperiments().size() : 0;
            int updatedExpressionExperimentCount = ( wn.getUpdatedExpressionExperiments() != null ) ? wn
                    .getUpdatedExpressionExperiments().size() : 0;

            /* Store counts for new and updated experiments by taxonId */
            Map<Taxon, Collection<Long>> newEEsPerTaxon = wn.getNewEEIdsPerTaxon();
            Map<Taxon, Collection<Long>> updatedEEsPerTaxon = wn.getUpdatedEEIdsPerTaxon();

            for ( Iterator<Taxon> it = unsortedEEsPerTaxon.keySet().iterator(); it.hasNext(); ) {
                Taxon t = it.next();
                JSONObject taxLine = new JSONObject();
                taxLine.put( "taxonId", t.getId() );
                taxLine.put( "taxonName", t.getScientificName() );
                taxLine.put( "totalCount", eesPerTaxon.get( t ) );
                if ( newEEsPerTaxon.containsKey( t ) ) {
                    taxLine.put( "newCount", newEEsPerTaxon.get( t ).size() );
                    taxLine.put( "newIds", newEEsPerTaxon.get( t ) );
                }
                if ( updatedEEsPerTaxon.containsKey( t ) ) {
                    taxLine.put( "updatedCount", updatedEEsPerTaxon.get( t ).size() );
                    taxLine.put( "updatedIds", updatedEEsPerTaxon.get( t ) );
                }
                taxonEntries.add( taxLine );
            }

            summary.element( "sortedCountsPerTaxon", taxonEntries );

            // Get count for new and updated array designs
            int newArrayCount = ( wn.getNewArrayDesigns() != null ) ? wn.getNewArrayDesigns().size() : 0;
            int updatedArrayCount = ( wn.getUpdatedArrayDesigns() != null ) ? wn.getUpdatedArrayDesigns().size() : 0;

            boolean drawNewColumn = ( newExpressionExperimentCount > 0 || newArrayCount > 0 || newAssayCount > 0 ) ? true
                    : false;
            boolean drawUpdatedColumn = ( updatedExpressionExperimentCount > 0 || updatedArrayCount > 0 ) ? true
                    : false;
            String date = ( wn.getDate() != null ) ? DateFormat.getDateInstance( DateFormat.LONG )
                    .format( wn.getDate() ) : "";
            date = date.replace( '-', ' ' );

            summary.element( "updateDate", date );
            summary.element( "drawNewColumn", drawNewColumn );
            summary.element( "drawUpdatedColumn", drawUpdatedColumn );
            if ( newAssayCount != 0 ) summary.element( "newBioAssayCount", new Long( newAssayCount ) );
            if ( newArrayCount != 0 ) summary.element( "newArrayDesignCount", new Long( newArrayCount ) );
            if ( updatedArrayCount != 0 ) summary.element( "updatedArrayDesignCount", new Long( updatedArrayCount ) );
            if ( newExpressionExperimentCount != 0 )
                summary.element( "newExpressionExperimentCount", newExpressionExperimentCount );
            if ( updatedExpressionExperimentCount != 0 )
                summary.element( "updatedExpressionExperimentCount", updatedExpressionExperimentCount );
            if ( newExpressionExperimentCount != 0 )
                summary.element( "newExpressionExperimentIds", newExpressionExperimentIds );
            if ( updatedExpressionExperimentCount != 0 )
                summary.element( "updatedExpressionExperimentIds", updatedExpressionExperimentIds );

        }

        summary.element( "bioAssayCount", bioAssayCount );
        summary.element( "arrayDesignCount", arrayDesignCount );

        summary.element( "expressionExperimentCount", expressionExperimentCount );

        return summary;
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

        ee = expressionExperimentService.thawLiter( ee );

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( ee.getId() );

        Collection<ExpressionExperimentValueObject> initialResults = expressionExperimentService.loadValueObjects( ids,
                false );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        getReportData( initialResults );

        /*
         * Check for multiple "preferred" qts.
         */
        int countPreferred = 0;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                countPreferred++;
            }
        }

        ExpressionExperimentValueObject initialResult = initialResults.iterator().next();
        ExpressionExperimentDetailsValueObject finalResult = new ExpressionExperimentDetailsValueObject( initialResult );
        finalResult.setHasMultiplePreferredQuantitationTypes( countPreferred > 1 );

        Collection<TechnologyType> techTypes = new HashSet<TechnologyType>();
        for ( ArrayDesign ad : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            techTypes.add( ad.getTechnologyType() );
        }

        finalResult.setHasMultipleTechnologyTypes( techTypes.size() > 1 );

        // Set the parent taxon
        Long taxonId = initialResult.getTaxonId();
        assert taxonId != null;
        Taxon taxon = taxonService.load( taxonId );
        taxonService.thaw( taxon );

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

        finalResult.setCurrentUserHasWritePermission( securityService.isEditable( ee ) );
        finalResult.setCurrentUserIsOwner( securityService.isOwnedByCurrentUser( ee ) );

        Collection<ExpressionExperimentValueObject> finalResultc = new HashSet<ExpressionExperimentValueObject>();
        finalResultc.add( finalResult );

        /*
         * populate the publication and author information
         */
        finalResult.setDescription( ee.getDescription() );

        if ( ee.getPrimaryPublication() != null && ee.getPrimaryPublication().getPubAccession() != null ) {
            finalResult
                    .setPrimaryCitation( CitationValueObject.convert2CitationValueObject( ee.getPrimaryPublication() ) );
            String accession = ee.getPrimaryPublication().getPubAccession().getAccession();

            try {
                finalResult.setPubmedId( Integer.parseInt( accession ) );
            } catch ( NumberFormatException e ) {
                log.warn( "Pubmed id not formatted correctly: " + accession );
            }
        }

        finalResult.setQChtml( getQCTagHTML( ee ) );

        boolean hasBatchInformation = false;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                hasBatchInformation = true;
                break;
            }
        }
        finalResult.setHasBatchInformation( hasBatchInformation );
        if ( hasBatchInformation ) {
            finalResult.setBatchConfound( batchConfound( ee ) );
            finalResult.setBatchEffect( batchEffect( ee ) );
        }

        Date lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( ee );
        if ( lastArrayDesignUpdate != null ) {
            finalResult.setLastArrayDesignUpdateDate( lastArrayDesignUpdate.toString() );
        }

        // experiment sets this ee belongs to
        finalResult.setExpressionExperimentSets( this.getExpressionExperimentSets( ee, false ) );

        finalResult.setCanCurrentUserEditExperiment( canCurrentUserEditExperiment( id ) );
        finalResult.setDoesCurrentUserOwnExperiment( doesCurrentUserOwnExperiment( id ) );
        finalResult.setIsPublic( securityService.isPublic( ee ) );
        finalResult.setShared( securityService.isShared( ee ) );

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
                false, null, true );
        this.expressionExperimentReportService.getReportInformation( result );
        return result;
    }

    /**
     * AJAX; get a collection of experiments that have had samples removed due to outliers (TODO: and experiment that
     * have possible batch effects detected)
     * 
     * @param id Identifier for the experiment
     */
    public JsonReaderResponse<JSONObject> loadExpressionExperimentsWithQcIssues() {

        Collection<ExpressionExperiment> outlierEEs = expressionExperimentService.getExperimentsWithOutliers();

        // List<ExpressionExperimentValueObject> batchEffectEEs =
        // expressionExperimentService.getExperimentsWithBatchEffect();
        // List<ExpressionExperimentValueObject> batchEffectEEs = new ArrayList<ExpressionExperimentValueObject>();

        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.addAll( outlierEEs );
        // ees.addAll( batchEffectEEs );

        List<JSONObject> jsonRecords = new ArrayList<JSONObject>();

        for ( ExpressionExperiment ee : ees ) {
            JSONObject record = new JSONObject();
            record.element( "id", ee.getId() );
            record.element( "shortName", ee.getShortName() );
            record.element( "name", ee.getName() );

            if ( outlierEEs.contains( ee ) ) {
                record.element( "sampleRemoved", true );
            }

            // record.element( "batchEffect", batchEffectEEs.contains( ee ) );
            jsonRecords.add( record );
        }

        JsonReaderResponse<JSONObject> returnVal = new JsonReaderResponse<JSONObject>( jsonRecords );

        return returnVal;

    }

    /**
     * AJAX - for display in tables
     * 
     * @param ids of EEs to load quantitation types for
     * @return security-filtered set of value objects.
     */
    public Collection<QuantitationTypeValueObject> loadQuantitationTypes( Long eeid ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeid );
        // need to thaw?
        ee = expressionExperimentService.thawLite( ee );
        Collection<QuantitationType> qts = ee.getQuantitationTypes();
        Collection<QuantitationTypeValueObject> qtvos = QuantitationTypeValueObject.convert2ValueObjects( qts );

        return qtvos;
    }

    /**
     * AJAX. Data summarizing the status of experiments.
     * 
     * @param taxonId can be null
     * @param limit If >0, get the most recently updated N experiments, where N <= limit; or if < 0, get the least
     *        recently updated; if 0, or null, return all.
     * @param filter if non-null, limit data sets to ones meeting criteria.
     * @param showPublic return user's public datasets too
     * @param sIds - ids
     * @return
     */
    public Collection<ExpressionExperimentValueObject> loadStatusSummaries( Long taxonId, Collection<Long> ids,
            Integer limit, Integer filter, Boolean showPublic ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<ExpressionExperimentValueObject> eeValObjectCol = null;

        boolean filterDataByUser = false;

        if ( SecurityServiceImpl.isUserAdmin() ) {
            /* proceed, just being transparent */
        } else if ( SecurityServiceImpl.isUserLoggedIn() ) {
            filterDataByUser = true;
        } else {
            /* Anonymous */
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        // limit = 10;
        // default limit to 50, should always be set on front end but it case it wasn't this
        // will keep from loading a ridiculous number of experiments
        if ( limit == null ) limit = 50;

        eeValObjectCol = getEEVOsForManager( taxonId, ids, filterDataByUser, limit, filter, showPublic );

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

        getReportData( eeValObjectCol );

        if ( timer.getTime() > 1000 ) {
            log.info( "Filling in report data for " + eeValObjectCol.size() + " EEs: " + timer.getTime() + "ms" );
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
        RemovePubMed job = new RemovePubMed( new TaskCommand( eeId ) );
        return taskRunningService.submitLocalJob( job );
    }

    /**
     * AJAX (used by experimentAndExperimentGroupCombo.js)
     * 
     * @param query
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @return Collection of SearchResultDisplayObjects
     */
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {
        boolean taxonLimited = ( taxonId != null ) ? true : false;
        List<SearchResultDisplayObject> displayResults = new ArrayList<SearchResultDisplayObject>();

        // add session bound sets
        // get any session-bound groups
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = ( taxonLimited ) ? sessionListManager
                .getModifiedExperimentSets( taxonId ) : sessionListManager.getModifiedExperimentSets();

        List<SearchResultDisplayObject> sessionSets = new ArrayList<SearchResultDisplayObject>();

        // create SearchResultDisplayObjects
        if ( sessionResult != null && sessionResult.size() > 0 ) {
            for ( SessionBoundExpressionExperimentSetValueObject eevo : sessionResult ) {
                SearchResultDisplayObject srdo = new SearchResultDisplayObject( eevo );
                srdo.setUserOwned( true );
                sessionSets.add( srdo );
            }
        }

        // keep sets in proper order (session-bound groups first)
        Collections.sort( sessionSets );
        displayResults.addAll( sessionSets );
        displayResults
                .addAll( expressionExperimentSearchService.searchExperimentsAndExperimentGroups( query, taxonId ) );

        return displayResults;
    }

    public List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId ) {

        return expressionExperimentSearchService.getAllTaxonExperimentGroup( taxonId );
    }

    /**
     * AJAX (used by ExperimentCombo.js)
     * 
     * @param query
     * @return Collection of expression experiment entity objects
     */
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        return expressionExperimentSearchService.searchExpressionExperiments( query );

    }

    /**
     * Show all experiments (optionally conditioned on either a taxon, or a list of ids)
     * 
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showAllExpressionExperiments.html", "/showAll" })
    public ModelAndView showAllExpressionExperiments( HttpServletRequest request, HttpServletResponse response ) {

        ModelAndView mav = new ModelAndView( "expressionExperiments" );
        return mav;

    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showAllExpressionExperimentLinkSummaries.html", "/manage.html" })
    public ModelAndView showAllLinkSummaries( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "expressionExperimentLinkSummary" );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showBioAssaysFromExpressionExperiment.html", "/bioAssays" })
    public ModelAndView showBioAssays( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );

        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }
        request.setAttribute( "id", id );
        ModelAndView mv = new ModelAndView( "bioAssays" ).addObject( "bioAssays",
                bioAssayService.thaw( expressionExperiment.getBioAssays() ) );

        addQCInfo( expressionExperiment, mv );
        mv.addObject( "expressionExperiment", expressionExperiment );
        return mv;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping(value = { "/showBioMaterialsFromExpressionExperiment.html", "/bioMaterials" })
    public ModelAndView showBioMaterials( HttpServletRequest request, HttpServletResponse response ) {
        String idStr = request.getParameter( "id" );

        if ( idStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }
        Long id = Long.parseLong( idStr );

        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

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

        Integer numBioMaterials = bioMaterials.size();
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterialService.thaw( bioMaterials ) );

        addQCInfo( expressionExperiment, mav );

        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping({ "/showExpressionExperiment.html", "/", "/show" })
    public ModelAndView showExpressionExperiment( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch timer = new StopWatch();
        timer.start();

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" );
        ExpressionExperiment expExp = getExpressionExperimentFromRequest( request );

        // This is only _really_ needed to get hasBatchInformation; we can get quantitation types by a service method.
        // So if this is slow, we can supply a query for the batch information.

        // expExp = expressionExperimentService.thawLite( expExp );

        mav.addObject( "expressionExperiment", expExp );
        /*
         * mav.addObject( "characteristics", expExp.getCharacteristics() );
         * 
         * Collection<QuantitationType> quantitationTypes = expExp.getQuantitationTypes(); mav.addObject(
         * "quantitationTypes", quantitationTypes ); mav.addObject( "qtCount", quantitationTypes.size() );
         * 
         * //Check for multiple "preferred" qts. int countPreferred = 0; for ( QuantitationType qt : quantitationTypes )
         * { if ( qt.getIsPreferred() ) { countPreferred++; } } mav.addObject( "hasMoreThanOnePreferredQt",
         * countPreferred > 0 );
         * 
         * AuditEvent lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( expExp, null );
         * mav.addObject( "lastArrayDesignUpdate", lastArrayDesignUpdate );
         */
        mav.addObject( "eeId", expExp.getId() );
        mav.addObject( "eeClass", ExpressionExperiment.class.getName() );
        /*
         * boolean hasBatchInformation = false; for ( ExperimentalFactor ef :
         * expExp.getExperimentalDesign().getExperimentalFactors() ) { if ( BatchInfoPopulationService.isBatchFactor( ef
         * ) ) { hasBatchInformation = true; break; } }
         * 
         * mav.addObject( "hasBatchInformation", hasBatchInformation ); if ( hasBatchInformation ) { mav.addObject(
         * "batchConfound", this.batchConfound( expExp ) ); mav.addObject( "batchArtifact", this.batchEffect( expExp )
         * ); }
         * 
         * addQCInfo( expExp, mav );
         * 
         * boolean isPrivate = securityService.isPrivate( expExp ); mav.addObject( "isPrivate", isPrivate );
         */
        if ( timer.getTime() > 200 ) {
            log.info( "Show Experiment was slow: id=" + expExp.getId() + " " + timer.getTime() + "ms" );
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
    @RequestMapping(value = { "/showExpressionExperimentSubSet.html", "/showSubset" })
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
     * Completely reset the pairing of bioassays to biomaterials so they are no longer paired. New biomaterials are
     * constructed where necessary; they retain the characteristics of the original. Experimental design might need to
     * be redone after this operation. (AJAX)
     * 
     * @param eeId
     */
    public void unmatchAllBioAssays( Long eeId ) {
        ExpressionExperiment ee = this.expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with id=" + eeId );
        }
        ee = this.expressionExperimentService.thawLite( ee );

        Collection<BioMaterial> needToProcess = new HashSet<BioMaterial>();

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                this.bioMaterialService.thaw( bm );
                Collection<BioAssay> bioAssaysUsedIn = bm.getBioAssaysUsedIn();
                if ( bioAssaysUsedIn.size() > 1 ) {
                    needToProcess.add( bm );
                }
            }
        }

        for ( BioMaterial bm : needToProcess ) {
            int i = 0;
            for ( BioAssay baU : bm.getBioAssaysUsedIn() ) {
                if ( i > 0 ) {
                    BioMaterial newMaterial = bioMaterialService.copy( bm );
                    this.bioMaterialService.thaw( newMaterial );
                    newMaterial.setName( "Modeled after " + bm.getName() );
                    newMaterial.getFactorValues().clear();
                    newMaterial.getBioAssaysUsedIn().add( baU );
                    newMaterial = ( BioMaterial ) persisterHelper.persist( newMaterial );

                    baU.getSamplesUsed().clear();
                    baU.getSamplesUsed().add( newMaterial );
                    bioAssayService.update( baU );

                }
                i++;
            }

        }

    }

    /**
     * AJAX
     * 
     * @param command
     * @return updated value object
     * @throws Exception
     */
    public ExpressionExperimentDetailsValueObject updateBasics( UpdateEEDetailsCommand command ) throws Exception {
        if ( command.getEntityId() == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        /*
         * This should be fast so I'm not using a background task.
         */

        // UpdateBasics runner = new UpdateBasics( command );
        // startTask( runner );
        Long entityId = command.getEntityId();
        ExpressionExperiment ee = expressionExperimentService.load( entityId );
        if ( ee == null )
            throw new IllegalArgumentException( "Cannot locate or access experiment with id=" + entityId );

        if ( StringUtils.isNotBlank( command.getShortName() ) && !command.getShortName().equals( ee.getShortName() ) ) {
            if ( expressionExperimentService.findByShortName( command.getShortName() ) != null ) {
                throw new IllegalArgumentException( "An experiment with short name '" + command.getShortName()
                        + "' already exists, you must use a unique name" );
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
        if ( !command.isRemovePrimaryPublication() && StringUtils.isNotBlank( command.getPubMedId() ) ) {
            updatePubMed( entityId, command.getPubMedId() );

        } else if ( command.isRemovePrimaryPublication() ) {
            removePrimaryPublication( entityId );
        }

        log.info( "Updating " + ee );
        expressionExperimentService.update( ee );

        ExpressionExperimentDetailsValueObject eeDetails = loadExpressionExperimentDetails( ee.getId() );

        // return runner.getTaskId();
        return eeDetails;
    }

    /**
     * @param id
     * @return
     */
    @RequestMapping("/refreshCorrMatrix.html")
    public ModelAndView updateCorrelationMatrix( Long id ) {
        // TODO: make this an ajax background job
        updateCorrelationMatrixFile( id );
        return new ModelAndView(
                new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
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
        UpdatePubMed job = new UpdatePubMed( command );
        return taskRunningService.submitLocalJob( job );
    }

    /**
     * Returns a collection of {@link Long} ids from strings.
     * 
     * @param idString
     * @return
     */
    protected Collection<Long> extractIds( String idString ) {
        Collection<Long> ids = new ArrayList<Long>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s.trim() ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
    }

    /*
     * Handle case of text export of a list of genes
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse) Called by
     * /Gemma/expressionExperiment/downloadExpressionExperimentList.html
     */
    @RequestMapping("/downloadExpressionExperimentList.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> eeIds = extractIds( request.getParameter( "e" ) ); // might not be any
        Collection<Long> eeSetIds = extractIds( request.getParameter( "es" ) ); // might not be there
        String eeSetName = request.getParameter( "esn" ); // might not be there

        ModelAndView mav = new ModelAndView( new TextView() );
        if ( ( eeIds == null || eeIds.isEmpty() ) && ( eeSetIds == null || eeSetIds.isEmpty() ) ) {
            mav.addObject( TextView.TEXT_PARAM, "Could not find genes to match expression experiment ids: {" + eeIds
                    + "} or expression experiment set ids {" + eeSetIds + "}" );
            return mav;
        }

        Collection<ExpressionExperimentValueObject> ees = expressionExperimentService.loadValueObjects( eeIds, false );

        for ( Long id : eeSetIds ) {
            ees.addAll( expressionExperimentSetService.getExperimentValueObjectsInSet( id ) );
        }

        mav.addObject( TextView.TEXT_PARAM, format4File( ees, eeSetName ) );
        watch.stop();
        Long time = watch.getTime();

        if ( time > 100 ) {
            log.info( "Retrieved and Formated" + ees.size() + " genes in : " + time + " ms." );
        }
        return mav;

    }

    private void addQCInfo( ExpressionExperiment expressionExperiment, ModelAndView mav ) {
        mav.addObject( "hasCorrMat", sampleCoexpressionMatrixService.hasMatrix( expressionExperiment ) );
        mav.addObject( "hasPvalueDist", ExpressionExperimentQCUtils.hasPvalueDistFiles( expressionExperiment ) );
        mav.addObject( "hasPCA", svdService.hasPca( expressionExperiment.getId() ) );
        mav.addObject( "numFactors", ExpressionExperimentQCUtils.numFactors( expressionExperiment ) ); // this is not
        // fully
        // implemented
        mav.addObject( "hasNodeDegreeDist", ExpressionExperimentQCUtils.hasNodeDegreeDistFile( expressionExperiment ) );
    }

    /**
     * Filter based on criteria of which events etc. the data sets have.
     * 
     * @param eeValObjectCol
     * @param filter
     * @return
     */
    private List<ExpressionExperimentValueObject> applyFilter( List<ExpressionExperimentValueObject> eeValObjectCol,
            Integer filter ) {
        List<ExpressionExperimentValueObject> filtered = new ArrayList<ExpressionExperimentValueObject>();
        Collection<ExpressionExperiment> eesToKeep = null;
        List<ExpressionExperimentValueObject> eeVOsToKeep = null;

        switch ( filter ) {
            case 1: // eligible for diff and don't have it.
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                eesToKeep.removeAll( expressionExperimentService.loadLackingFactors() );
                break;
            case 2: // need coexp
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 3:
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                break;
            case 4:
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 5:
                eeVOsToKeep = returnTroubled( eeValObjectCol );
                break;
            case 6:
                eesToKeep = expressionExperimentService.loadLackingFactors();
                break;
            case 7:
                eesToKeep = expressionExperimentService.loadLackingTags();
                break;
            case 8: // needs batch info
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                auditEventService.retainLackingEvent( eesToKeep, FailedBatchInformationMissingEvent.class );
                break;
            case 9:
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                break;
            case 10:
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            case 11:
                eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            default:
                throw new IllegalArgumentException( "Unknown filter: " + filter );

        }

        assert eesToKeep == null || eesToKeep.size() <= eeValObjectCol.size();

        /*
         * TODO support more filters, and use an enumeration.
         */

        // get corresponding value objects from collection param
        if ( eesToKeep != null ) {
            if ( eesToKeep.isEmpty() ) {
                return filtered;
            }
            // Map<Long, ExpressionExperiment> idMap = EntityUtils.getIdMap( eesToKeep );
            Collection<Long> ids = EntityUtils.getIds( eesToKeep );
            for ( ExpressionExperimentValueObject eevo : eeValObjectCol ) {
                if ( ids.contains( eevo.getId() ) ) {
                    filtered.add( eevo );
                }
            }
            return filtered;

        }
        if ( eeVOsToKeep != null ) {
            return eeVOsToKeep;
        }

        return eeValObjectCol;
    }

    /**
    
     */
    private String batchConfound( ExpressionExperiment ee ) {
        String result = expressionExperimentService.getBatchConfound( ee );
        if ( result == null ) {
            result = "";
        }
        return result;
    }

    /**
     * @param ee
     * @return
     */
    private String batchEffect( ExpressionExperiment ee ) {
        String result = expressionExperimentService.getBatchEffect( ee );
        if ( result == null ) {
            result = "";
        }
        return result;

    }

    /**
     * @param vectors
     * @return
     */
    private String format4File( Collection<ExpressionExperimentValueObject> ees, String eeSetName ) {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append( "# Generated by Gemma\n# " + ( new Date() ) + "\n" );
        strBuff.append( ExpressionDataFileService.DISCLAIMER + "#\n" );

        if ( eeSetName != null && eeSetName.length() != 0 ) strBuff.append( "# Experiment Set: " + eeSetName + "\n" );
        strBuff.append( "# " + ees.size() + ( ( ees.size() > 1 ) ? " experiments" : " experiment" ) + "\n#\n" );

        // add header
        strBuff.append( "Short Name\tFull Name\n" );
        for ( ExpressionExperimentValueObject ee : ees ) {
            if ( ee != null ) {
                strBuff.append( ee.getShortName() + "\t" + ee.getName() );
                strBuff.append( "\n" );
            }
        }

        return strBuff.toString();
    }

    /**
     * @param taxonId
     * @param ids - takes precedence
     * @param filterDataByUser
     * @param limit - return the N most recently (limit > 0) or least recently updated experiments (limit < 0) or all
     *        (limit == 0)
     * @param filter setting
     * @param showPublic return the user's public datasets as well
     * @return
     */
    private Collection<ExpressionExperimentValueObject> getEEVOsForManager( Long taxonId, Collection<Long> ids,
            boolean filterDataByUser, Integer limit, Integer filter, boolean showPublic ) {
        List<ExpressionExperimentValueObject> eeValObjectCol;

        Integer limitToUse = limit;
        if ( filter != null && filter > 0 ) {
            // if using a filter, need to load all to guarantee we'll have enough post filtering
            limitToUse = null;
        }

        // taxon specific?
        if ( taxonId != null && taxonId > 0 ) {
            Taxon taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
            if ( ids == null || ids.isEmpty() ) {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser,
                        limitToUse, showPublic );
            } else {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, ids, filterDataByUser,
                        limitToUse, showPublic );
            }

        } else if ( ids == null || ids.isEmpty() ) {
            // load everything (up to the limit)
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser,
                    limitToUse, showPublic );
        } else {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, ids, filterDataByUser, limitToUse,
                    showPublic );
        }

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( filter != null && filter > 0 ) eeValObjectCol = applyFilter( eeValObjectCol, filter );

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( limit != 0 && eeValObjectCol.size() > Math.abs( limit ) ) {
            log.info( "Still have to filter" );
            Collections.sort( eeValObjectCol, new Comparator<ExpressionExperimentValueObject>() {

                @Override
                public int compare( ExpressionExperimentValueObject o1, ExpressionExperimentValueObject o2 ) {
                    return -o1.getDateLastUpdated().compareTo( o2.getDateLastUpdated() );
                }
            } );

            eeValObjectCol = eeValObjectCol.subList( 0, Math.abs( limit ) );

        }

        assert eeValObjectCol.size() <= Math.abs( limit );

        return eeValObjectCol;
    }

    /**
     * @param request
     * @return
     * @throws IllegalArgumentException if a matching EE can't be loaded
     */
    private ExpressionExperiment getExpressionExperimentFromRequest( HttpServletRequest request ) {

        ExpressionExperiment expressionExperiment = null;
        Long id = null;

        if ( request.getParameter( "id" ) == null ) {

            String shortName = request.getParameter( "shortName" );

            if ( StringUtils.isNotBlank( shortName ) ) {
                expressionExperiment = expressionExperimentService.findByShortName( shortName );
            }

            if ( expressionExperiment == null ) {
                throw new IllegalArgumentException( "Unable to access experiment with shortName=" + shortName );
            }

        } else {
            try {
                id = Long.parseLong( request.getParameter( "id" ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "You must provide a valid numerical identifier" );
            }
            expressionExperiment = expressionExperimentService.load( id );

            if ( expressionExperiment == null ) {
                throw new IllegalArgumentException( "Unable to access experiment with id=" + id );
            }
        }
        return expressionExperiment;
    }

    /**
     * @param ee
     * @param includeAutoGenerated
     * @return
     */
    private Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets( ExpressionExperiment ee,
            boolean includeAutoGenerated ) {
        Collection<DatabaseBackedExpressionExperimentSetValueObject> dbEEsvos = expressionExperimentSetService
                .getLightValueObjectsFromIds( expressionExperimentSetService.findIds( ee ) );
        Collection<ExpressionExperimentSetValueObject> eesvos = new ArrayList<ExpressionExperimentSetValueObject>();

        if ( !includeAutoGenerated ) {
            for ( DatabaseBackedExpressionExperimentSetValueObject dbEEsvo : dbEEsvos ) {
                if ( !expressionExperimentSetService.isAutomaticallyGenerated( dbEEsvo.getDescription() ) ) {
                    eesvos.add( dbEEsvo );
                }
            }
        }

        return eesvos;
    }

    /**
     * Maintains order if a list is passed in
     * 
     * @param securedEEs
     * @return List<ExpressionExperimentValueObject> in the same order as the EEs passed in
     */
    private List<ExpressionExperimentValueObject> getExpressionExperimentValueObjects(
            List<ExpressionExperiment> securedEEs ) {

        if ( securedEEs.size() == 0 ) {
            return new ArrayList<ExpressionExperimentValueObject>();
        }
        StopWatch timer = new StopWatch();
        timer.start();

        List<Long> ids = new ArrayList<Long>( EntityUtils.getIds( securedEEs ) );

        // this method keeps order.
        List<ExpressionExperimentValueObject> valueObjs = ( List<ExpressionExperimentValueObject> ) expressionExperimentService
                .loadValueObjects( ids, true );

        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            Map<Long, Boolean> canEdit = new HashMap<Long, Boolean>();
            Map<Long, Boolean> owns = new HashMap<Long, Boolean>();
            for ( ExpressionExperiment ee : securedEEs ) {
                canEdit.put( ee.getId(), securityService.isEditable( ee ) );
                owns.put( ee.getId(), securityService.isOwnedByCurrentUser( ee ) );

            }
            for ( ExpressionExperimentValueObject vo : valueObjs ) {
                if ( canEdit.containsKey( vo.getId() ) ) {
                    vo.setCurrentUserHasWritePermission( canEdit.get( vo.getId() ) );
                }
                if ( owns.containsKey( vo.getId() ) ) {
                    vo.setCurrentUserIsOwner( owns.get( vo.getId() ) );
                }

            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Value objects for " + securedEEs.size() + " in " + timer.getTime() + "ms" );
        }

        assert securedEEs.size() >= valueObjs.size();
        return valueObjs;
    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     * 
     * @param taxon can be null
     * @param filterDataForUser if true, then only the data owned by the user are returned (this has no effect if you
     *        are an administrator)
     * @param showPublic TODO
     * @param eeids can be null; if taxon is non-null, this is ignored.
     * @param maximum # to retrieve, in order of most recently updated.
     * @return Collection<ExpressionExperimentValueObject>
     */
    private List<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects( Taxon taxon,
            Collection<Long> eeIds, boolean filterDataForUser, Integer limit, boolean showPublic ) {

        // TODO this can be cleaned up substantially by using new loading valueobject through secured methods

        List<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>();

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * FIXME remove troubled? Needs to be optional. For dataset management page, don't.
         */

        /* Filtering for security happens here. */
        if ( filterDataForUser ) {
            try {

                securedEEs = ( showPublic ) ? new ArrayList<ExpressionExperiment>(
                        expressionExperimentService.loadUserOwnedExpressionExperiments() )
                        : new ArrayList<ExpressionExperiment>(
                                expressionExperimentService.loadMySharedExpressionExperiments() );
                // limit won't really
                // work! Most experiments
                // are filtered out.

                if ( limit != null ) {
                    securedEEs = expressionExperimentService.findByUpdatedLimit( EntityUtils.getIds( securedEEs ),
                            limit );
                }

                if ( eeIds != null ) {
                    List<ExpressionExperiment> securedEEsfilteredByEeIds = new ArrayList<ExpressionExperiment>();

                    // only keep ExpressionExperiments that have ids contained in eeIds
                    for ( ExpressionExperiment ee : securedEEs ) {

                        if ( eeIds.contains( ee.getId() ) ) {
                            securedEEsfilteredByEeIds.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByEeIds;

                }

                if ( taxon != null ) {

                    List<ExpressionExperiment> securedEEsfilteredByTaxon = new ArrayList<ExpressionExperiment>();

                    // only keep ExpressionExperiments that have the specified Taxon
                    for ( ExpressionExperiment ee : securedEEs ) {

                        Taxon t = expressionExperimentService.getTaxon( ee );

                        if ( t != null && t.equals( taxon ) ) {
                            securedEEsfilteredByTaxon.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByTaxon;

                }
            } catch ( AccessDeniedException e ) {
                return new ArrayList<ExpressionExperimentValueObject>();
            }
        } else {
            if ( taxon != null ) {
                if ( eeIds == null ) {
                    securedEEs = expressionExperimentService.findByTaxon( taxon, limit );

                    if ( limit != null && securedEEs.size() > limit && limit >= 0 ) {
                        securedEEs = securedEEs.subList( 0, limit );
                    }

                } else {
                    securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadMultiple( eeIds ) );
                    List<ExpressionExperiment> securedEEsfilteredByTaxon = new ArrayList<ExpressionExperiment>();

                    for ( ExpressionExperiment ee : securedEEs ) {

                        Taxon t = expressionExperimentService.getTaxon( ee );

                        if ( t != null && t.getId().equals( taxon.getId() ) ) {
                            securedEEsfilteredByTaxon.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByTaxon;
                }

            } else if ( eeIds == null ) {
                if ( limit == null ) {
                    securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadAll() );
                } else {
                    securedEEs = expressionExperimentService.findByUpdatedLimit( limit );
                }
            } else {
                securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadMultiple( eeIds ) );
            }
            if ( !securedEEs.isEmpty() && !showPublic ) {
                Collection<Securable> publicEEs = securityService.choosePublic( securedEEs );
                securedEEs.removeAll( publicEEs );
            }

        }

        if ( timer.getTime() > 1000 ) {
            log.info( securedEEs.size() + " EEs in " + timer.getTime() + "ms" );
        }

        log.debug( "Loading value objects ..." );
        List<ExpressionExperimentValueObject> eevos = getExpressionExperimentValueObjects( securedEEs );
        return eevos;
    }

    /**
     * Updates the value objects with event information and summaries
     * 
     * @param expressionExperiments
     * @return most recently changed information: Map of EE ID to date of last update (or create)
     */
    private Map<Long, Date> getReportData( Collection<ExpressionExperimentValueObject> expressionExperiments ) {

        Map<Long, Date> lastUpdated = expressionExperimentReportService.getReportInformation( expressionExperiments );

        expressionExperimentReportService.getAnnotationInformation( expressionExperiments );

        Map<Long, Date> eventDates = expressionExperimentReportService.getEventInformation( expressionExperiments );

        for ( Long k : eventDates.keySet() ) {
            if ( lastUpdated.containsKey( k ) ) {
                if ( lastUpdated.get( k ).after( eventDates.get( k ) ) ) {
                    eventDates.put( k, lastUpdated.get( k ) );
                }
            }
        }
        return eventDates;
    }

    private List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch ) {
        return loadAllValueObjectsOrdered( batch, null, null );
    }

    private List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch,
            Collection<Long> ids ) {
        return loadAllValueObjectsOrdered( batch, ids, null );
    }

    private List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch,
            Collection<Long> ids, Taxon taxon ) {
        List<ExpressionExperimentValueObject> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();
            boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );

            String orderBy = "name"; // default ordering
            if ( o.equals( "shortName" ) ) {
                orderBy = "shortName";
            } else if ( o.equals( "name" ) ) {
                orderBy = "name";
            } else if ( o.equals( "bioAssayCount" ) ) {
                orderBy = "bioAssayCount";
            } else if ( o.equals( "taxon" ) ) {
                orderBy = "taxon";
            } else if ( o.equals( "troubled" ) ) {
                orderBy = "troubled";
            } else if ( o.equals( "dateLastUpdated" ) || o.equals( "modDate" ) ) {
                orderBy = "dateLastUpdated";
                descending = !descending;
            } else {
                log.error( "Tried to sort experiments by unknown sort field: " + o + ". Sorting by default: " + orderBy );
            }

            if ( ids != null ) {
                records = expressionExperimentService.loadValueObjectsOrdered( orderBy, descending, ids );
            } else if ( taxon != null ) {
                records = expressionExperimentService.loadAllValueObjectsTaxonOrdered( orderBy, descending, taxon );
            } else {
                records = expressionExperimentService.loadAllValueObjectsOrdered( orderBy, descending );
            }
        } else {
            if ( ids != null ) {
                records = new ArrayList<ExpressionExperimentValueObject>( expressionExperimentService.loadValueObjects(
                        ids, true ) );
            } else if ( taxon != null ) {
                records = expressionExperimentService.loadAllValueObjectsTaxon( taxon );
            } else {
                records = new ArrayList<ExpressionExperimentValueObject>(
                        expressionExperimentService.loadAllValueObjects() );
            }
        }

        assert records != null;

        return records;
    }

    /**
     * @param batch
     * @param taxon
     * @return
     */
    private List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch, Taxon taxon ) {
        return loadAllValueObjectsOrdered( batch, null, taxon );
    }

    /**
     * @param records
     * @return
     */
    private List<ExpressionExperimentValueObject> removeTroubledExperimentVOs(
            List<ExpressionExperimentValueObject> records ) {
        List<ExpressionExperimentValueObject> untroubled = new ArrayList<ExpressionExperimentValueObject>( records );

        Collection<ExpressionExperimentValueObject> toRemove = new ArrayList<ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject record : records ) {

            if ( record.getTroubled() ) {
                toRemove.add( record );
            }

        }
        untroubled.removeAll( toRemove );
        return untroubled;
    }

    /**
     * Read the troubled flag in each ExpressionExperimentValueObject and return only those object for which it is true
     * 
     * @param ees
     * @return
     */
    private List<ExpressionExperimentValueObject> returnTroubled( Collection<ExpressionExperimentValueObject> ees ) {
        List<ExpressionExperimentValueObject> troubled = new ArrayList<ExpressionExperimentValueObject>();

        for ( ExpressionExperimentValueObject eevo : ees ) {
            if ( eevo.getTroubled() ) {
                troubled.add( eevo );
            }
        }

        return troubled;
    }

    /**
     * Update the file used for the sample correlation heatmaps FIXME make this a background task, use the
     * ProcessedExpressionDataVectorCreateTask
     * 
     * @param id
     */
    private void updateCorrelationMatrixFile( Long id ) {
        ExpressionExperiment expressionExperiment;
        expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "Unable to access experiment with id=" + id );
        }
        sampleCoexpressionMatrixService.create( expressionExperiment, true );
    }

}