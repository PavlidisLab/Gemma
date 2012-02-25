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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfoundValueObject;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.QuantitationTypeValueObject;
import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationMissingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
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
            expressionExperimentService.delete( command.getEntityId() );

            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) ).addObject( "message",
                    "Dataset id: " + command.getEntityId() + " removed from Database" ) );

        }
    }

    private class RemovePubMed extends BackgroundJob<TaskCommand> {

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
            result.setPrimaryCitation( CitationValueObject.convert2CitationValueObject( expressionExperiment
                    .getPrimaryPublication() ) );
            return new TaskResult( command, result );
        }

    }

    private static final Boolean AJAX = true;

    private static final int TRIM_SIZE = 800;

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
    private ExpressionExperimentReportService expressionExperimentReportService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ExpressionExperimentSearchService expressionExperimentSearchService = null;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService = null;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;

    private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

    @Autowired
    private Persister persisterHelper = null;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private WhatsNewService whatsNewService;

    @Autowired
    private SessionListManager sessionListManager;

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

    private static final Double BATCH_EFFECT_PVALTHRESHOLD = 0.01;

    /**
     * Exposed for AJAX calls.
     * 
     * @param id
     * @return taskId
     */
    public String deleteById( Long id ) {        
        if ( id == null ) return null;       
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

        Collection<Long> ids = expressionExperimentService.filter( searchString );

        if ( ids.isEmpty() ) {

            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "Your search yielded no results." );

        }

        if ( ids.size() == 1 ) {
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + ids.iterator().next() ) ).addObject( "message", "Search Criteria: " + searchString + "; "
                    + ids.size() + " Datasets matched." );
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
     * AJAX (used by experimentAndExperimentGroupCombo.js)
     * 
     * @param query
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @return Collection of SearchResultDisplayObjects
     */
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {
        boolean taxonLimited = ( taxonId != null ) ? true : false;
        List<SearchResultDisplayObject> displayResults = expressionExperimentSearchService
                .searchExperimentsAndExperimentGroups( query, taxonId );

        // add session bound sets
        // get any session-bound groups
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = ( taxonLimited ) ? sessionListManager
                .getModifiedExperimentSets( taxonId )
                : sessionListManager.getModifiedExperimentSets();

        List<SearchResultDisplayObject> sessionSets = new ArrayList<SearchResultDisplayObject>();

        // create SearchResultDisplayObjects
        if ( sessionResult != null && sessionResult.size() > 0 ) {
            for ( SessionBoundExpressionExperimentSetValueObject eevo : sessionResult ) {
                SearchResultDisplayObject srdo = new SearchResultDisplayObject( eevo );
                srdo.setUserOwned( true );
                sessionSets.add( srdo );
            }
        }

        // keep sets in proper order (user's groups first, then public ones)
        Collections.sort( sessionSets );
        displayResults.addAll( sessionSets );

        return displayResults;
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

        ee = expressionExperimentService.thawLiter( ee );
        
        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );        

        Collection<Long> ids = new HashSet<Long>();
        ids.add( ee.getId() );

        Collection<ExpressionExperimentValueObject> initialResults = expressionExperimentService.loadValueObjects( ids );

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
            finalResult.setPrimaryCitation( CitationValueObject
                    .convert2CitationValueObject( ee.getPrimaryPublication() ) );
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
            if ( BatchInfoPopulationService.isBatchFactor( ef ) ) {
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
        Collection<DatabaseBackedExpressionExperimentSetValueObject> dbEEsvos = expressionExperimentSetService
                .getLightValueObjectsFromIds( expressionExperimentSetService.findIds( ee ) );
        Collection<ExpressionExperimentSetValueObject> eesvos = new ArrayList<ExpressionExperimentSetValueObject>();
        eesvos.addAll( dbEEsvos );
        finalResult.setExpressionExperimentSets( eesvos );

        finalResult.setCanCurrentUserEditExperiment( canCurrentUserEditExperiment( id ) );
        finalResult.setDoesCurrentUserOwnExperiment( doesCurrentUserOwnExperiment( id ) );

        return finalResult;

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
     * Used to include the html for the qc table in an ext panel (without using a tag) (This method should probably be
     * in a service?)
     * 
     * @param ee
     */
    public String getQCTagHTML( ExpressionExperiment ee ) {
        ExperimentQCTag qc = new ExperimentQCTag();
        qc.setEe( ee.getId() );
        qc.setHasCorrDist( ExpressionExperimentQCUtils.hasCorrDistFile( ee ) );
        qc.setHasCorrMat( sampleCoexpressionMatrixService.hasMatrix( ee ) );
        qc.setHasNodeDegreeDist( ExpressionExperimentQCUtils.hasNodeDegreeDistFile( ee ) );
        qc.setHasPCA( svdService.hasPca( ee ) );
        qc.setHasPvalueDist( ExpressionExperimentQCUtils.hasPvalueDistFiles( ee ) );
        qc.setNumFactors( ExpressionExperimentQCUtils.numFactors( ee ) );
        return qc.getQChtml();
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
                false, -1 );
        // populateAnalyses( result ); // FIXME make this optional.
        return result;
    }

    /**
     * AJAX. Data summarizing the status of experiments.
     * 
     * @param taxonId can be null
     * @param sIds - ids
     * @param limit If >0, get the most recently updated N experiments, where N <= limit; or if < 0, get the least
     *        recently updated; if 0, or null, return all.
     * @param filter if non-null, limit data sets to ones meeting criteria.
     * @return
     */
    public Collection<ExpressionExperimentValueObject> loadStatusSummaries( Long taxonId, Collection<Long> ids,
            Integer limit, Integer filter ) {
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
        if(limit == null || limit <= 0) limit = 50;

        eeValObjectCol = getEEVOsForManager( taxonId, ids, filterDataByUser, limit, filter );

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
        RemovePubMed runner = new RemovePubMed( new TaskCommand( eeId ) );
        startTask( runner );
        return runner.getTaskId();
    }

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

        int limit = batch.getLimit();
        int origStart = batch.getStart();
        List<ExpressionExperiment> records = loadAllOrdered( batch );

        int start = origStart;
        int stop = Math.min( origStart + limit, records.size() );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperiments( records );
        }

        /*
         * can't just do expressionExperimentService.countAll() because this will count experiments the user may not
         * have access to
         */
        int count = records.size();

        List<ExpressionExperiment> recordsSubset = records.subList( start, stop );

        List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
                getExpressionExperimentValueObjects( recordsSubset ) );

        // if admin, want to show why experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.fillEventInformation( valueObjects );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                valueObjects, count );

        return returnVal;
    }

    /**
     * Return a version of the passed in list where all troubled experiments have been removed
     * 
     * @param records
     * @return filtered list of records
     */
    private List<ExpressionExperiment> removeTroubledExperiments( List<ExpressionExperiment> records ) {
        List<ExpressionExperiment> untroubled = new ArrayList<ExpressionExperiment>( records );
        // Map<Long, AuditEvent> troubleEvents = expressionExperimentReportService.getTroubledEvents( records );
        // Long id = null;
        Collection<ExpressionExperiment> toRemove = new ArrayList<ExpressionExperiment>();
        for ( ExpressionExperiment record : records ) {
            if ( record.getStatus() != null && record.getStatus().getTroubled() ) {
                toRemove.add( record );
            }
            // id = record.getId();
            // if ( troubleEvents.containsKey( id ) ) {
            // toRemove.add( record );
            // }
        }
        untroubled.removeAll( toRemove );
        return untroubled;
    }

    /**
     * AJAX call for remote paging store
     * 
     * @param batch
     * @return
     */
    public JsonReaderResponse<ExpressionExperimentValueObject> browseSpecificIds( ListBatchCommand batch,
            Collection<Long> ids ) {
        int origLimit = batch.getLimit();
        int origStart = batch.getStart();

        Set<Long> noDupIds = new HashSet<Long>( ids );
        List<ExpressionExperiment> records = loadAllOrdered( batch, noDupIds );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperiments( records );
        }

        List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
                getExpressionExperimentValueObjects( records.subList( origStart, Math.min( origStart + origLimit,
                        records.size() ) ) ) );

        // if admin, want to show if experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.fillEventInformation( valueObjects );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                valueObjects, records.size() );
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
        List<ExpressionExperiment> records = loadAllOrdered( batch, taxon );

        // if user is not admin, remove troubled experiments
        if ( !SecurityServiceImpl.isUserAdmin() ) {
            records = removeTroubledExperiments( records );
        }

        /*
         * can't just do countAll because this will count experiments the user may not have access to Integer count =
         * expressionExperimentService.countAll();
         */
        int count = records.size();

        List<ExpressionExperimentValueObject> valueObjects = new ArrayList<ExpressionExperimentValueObject>(
                getExpressionExperimentValueObjects( records.subList( origStart, Math.min( origStart + origLimit,
                        records.size() ) ) ) );

        // if admin, want to show if experiment is troubled
        if ( SecurityServiceImpl.isUserAdmin() ) {
            expressionExperimentReportService.fillEventInformation( valueObjects );
        }

        JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new JsonReaderResponse<ExpressionExperimentValueObject>(
                valueObjects, count );

        return returnVal;
    }

    // /**
    // * AJAX call for remote paging store
    // *
    // * @param batch
    // * @return public JsonReaderResponse<ExpressionExperimentValueObject> browseTaxon( ListBatchCommand batch, Long
    // * taxonId ) { List<ExpressionExperiment> records = loadAllOrdered( batch );
    // * List<ExpressionExperimentValueObject> valueObjects = new
    // * ArrayList<ExpressionExperimentValueObject>(getExpressionExperimentValueObjects( records ));
    // * JsonReaderResponse<ExpressionExperimentValueObject> returnVal = new
    // * JsonReaderResponse<ExpressionExperimentValueObject>( valueObjects, ids.size() ); return returnVal; }
    // */
    // private List<ExpressionExperiment> getBatch( ListBatchCommand batch ) {
    // return getBatch( batch, null );
    // }

    // private List<ExpressionExperiment> getBatch( ListBatchCommand batch, Collection<Long> ids ) {
    // List<ExpressionExperiment> records;
    // if ( StringUtils.isNotBlank( batch.getSort() ) ) {
    //
    // String o = batch.getSort();
    //
    // String orderBy = "name"; // default ordering
    // if ( o.equals( "shortName" ) ) {
    // orderBy = "shortName";
    // } else if ( o.equals( "name" ) ) {
    // orderBy = "name";
    // } else if ( o.equals( "bioAssayCount" ) ) {
    // orderBy = "bioAssayCount";
    // } else if ( o.equals( "taxon" ) ) {
    // orderBy = "taxon";
    // } else {
    // throw new IllegalArgumentException( "Unknown sort field: " + o );
    // }
    //
    // boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );
    // if ( ids != null ) {
    // records = expressionExperimentService.browseSpecificIds( batch.getStart(), batch.getLimit(), orderBy,
    // descending, ids );
    // } else {
    // records = expressionExperimentService.browse( batch.getStart(), batch.getLimit(), orderBy, descending );
    // }
    // } else {
    // if ( ids != null ) {
    // records = expressionExperimentService.browseSpecificIds( batch.getStart(), batch.getLimit(), ids );
    // } else {
    // records = expressionExperimentService.browse( batch.getStart(), batch.getLimit() );
    // }
    // }
    // return records;
    // }

    private List<ExpressionExperiment> loadAllOrdered( ListBatchCommand batch ) {
        return loadAllOrdered( batch, null, null );
    }

    private List<ExpressionExperiment> loadAllOrdered( ListBatchCommand batch, Collection<Long> ids ) {
        return loadAllOrdered( batch, ids, null );
    }

    private List<ExpressionExperiment> loadAllOrdered( ListBatchCommand batch, Taxon taxon ) {
        return loadAllOrdered( batch, null, taxon );
    }

    private List<ExpressionExperiment> loadAllOrdered( ListBatchCommand batch, Collection<Long> ids, Taxon taxon ) {
        List<ExpressionExperiment> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();

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
            } else {
                throw new IllegalArgumentException( "Unknown sort field: " + o );
            }

            boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );
            if ( ids != null ) {
                records = expressionExperimentService.loadMultipleOrdered( orderBy, descending, ids );
            } else if ( taxon != null ) {
                records = expressionExperimentService.loadAllTaxonOrdered( orderBy, descending, taxon );
            } else {
                records = expressionExperimentService.loadAllOrdered( orderBy, descending );
            }
        } else {
            if ( ids != null ) {
                records = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadMultiple( ids ) );
            } else if ( taxon != null ) {
                records = expressionExperimentService.loadAllTaxon( taxon );
            } else {
                records = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadAll() );
            }
        }
        return records;
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

        ModelAndView mav = new ModelAndView( "expressionExperiments" );
        return mav;

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
                    .getIds( wn.getUpdatedExpressionExperiments() )
                    : new ArrayList<Long>();

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

        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }
        request.setAttribute( "id", id );
        ModelAndView mv = new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssayService
                .thaw( expressionExperiment.getBioAssays() ) );

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
    @RequestMapping("/showBioMaterialsFromExpressionExperiment.html")
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
    @RequestMapping( { "/showExpressionExperiment.html", "/" })
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
        UpdatePubMed runner = new UpdatePubMed( command );
        startTask( runner );
        return runner.getTaskId();
    }

    private void addQCInfo( ExpressionExperiment expressionExperiment, ModelAndView mav ) {
        mav.addObject( "hasCorrDist", ExpressionExperimentQCUtils.hasCorrDistFile( expressionExperiment ) );
        mav.addObject( "hasCorrMat", sampleCoexpressionMatrixService.hasMatrix( expressionExperiment ) );
        mav.addObject( "hasPvalueDist", ExpressionExperimentQCUtils.hasPvalueDistFiles( expressionExperiment ) );
        mav.addObject( "hasPCA", svdService.hasPca( expressionExperiment ) );
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

        eesToKeep = expressionExperimentService.loadMultiple( EntityUtils.getIds( eeValObjectCol ) );

        assert eesToKeep.size() <= eeValObjectCol.size();

        switch ( filter ) {
            case 1: // eligible for diff and don't have it.
                auditEventService.retainLackingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                eesToKeep.removeAll( expressionExperimentService.loadLackingFactors() );
                break;
            case 2: // need coexp
                auditEventService.retainLackingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 3:
                auditEventService.retainHavingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                break;
            case 4:
                auditEventService.retainHavingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 5:
                eesToKeep = expressionExperimentService.loadTroubled();
                break;
            case 6:
                eesToKeep = expressionExperimentService.loadLackingFactors();
                break;
            case 7:
                eesToKeep = expressionExperimentService.loadLackingTags();
                break;
            case 8: // needs batch info
                auditEventService.retainLackingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                auditEventService.retainLackingEvent( eesToKeep, FailedBatchInformationMissingEvent.class );
                break;
            case 9:
                auditEventService.retainHavingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                break;
            case 10:
                auditEventService.retainLackingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            case 11:
                auditEventService.retainHavingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            default:
                throw new IllegalArgumentException( "Unknown filter: " + filter );

        }

        /*
         * TODO support more filters, and use an enumeration.
         */

        if ( eesToKeep != null ) {
            if ( eesToKeep.isEmpty() ) {
                return filtered;
            }
            Map<Long, ExpressionExperiment> idMap = EntityUtils.getIdMap( eesToKeep );
            for ( ExpressionExperimentValueObject eevo : eeValObjectCol ) {
                if ( idMap.containsKey( eevo.getId() ) ) {
                    filtered.add( eevo );
                }
            }
            return filtered;

        }

        // temporary - no filtering.
        return eeValObjectCol;
    }

    /**
    
     */
    private String batchConfound( ExpressionExperiment ee ) {
        Collection<BatchConfoundValueObject> confounds;
        try {
            confounds = BatchConfound.test( ee );
        } catch ( Exception e ) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append( "" );

        for ( BatchConfoundValueObject c : confounds ) {
            if ( c.getP() < BATCH_CONFOUND_THRESHOLD ) {
                String factorName = c.getEf().getName();
                buf.append( "Factor: " + factorName + " may be confounded with batches; p="
                        + String.format( "%.2g", c.getP() ) + "<br />" );
            }
        }
        return buf.toString();
    }

    /**
     * @param ee
     * @return
     */
    private String batchEffect( ExpressionExperiment ee ) {
        String result = "";

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationService.isBatchFactor( ef ) ) {
                SVDValueObject svd = svdService.svdFactorAnalysis( ee );
                if ( svd == null ) break;
                for ( Integer component : svd.getFactorPvals().keySet() ) {
                    Map<Long, Double> cmpEffects = svd.getFactorPvals().get( component );

                    Double pval = cmpEffects.get( ef.getId() );
                    if ( pval != null && pval < BATCH_EFFECT_PVALTHRESHOLD ) {
                        result = "This data set may have a batch artifact (PC" + ( component + 1 ) + "); p="
                                + String.format( "%.2g", pval ) + "<br />";
                    }

                }
                break;
            }
        }
        return result;

    }

    /**
     * @param taxonId
     * @param ids - takes precedence
     * @param filterDataByUser
     * @param limit - return the N most recently (limit > 0) or least recently updated experiments (limit < 0) or all
     *        (limit == 0)
     * @param filter setting
     * @return
     */
    private Collection<ExpressionExperimentValueObject> getEEVOsForManager( Long taxonId, Collection<Long> ids,
            boolean filterDataByUser, Integer limit, Integer filter ) {
        List<ExpressionExperimentValueObject> eeValObjectCol;

        Integer limitToUse = limit;
        if ( filter != null && filter > 0 ) {
            // HACK TO FIX!! THIS IS JUST SO IT DOESN'T LOAD ALLLLL EEs 
            // NEED A BETTER WAY TO FIX THIS
            // (can't just load the limit because we will be filtering later for experiments with differential expression, PCA, factors etc.)
            limitToUse = limit + 100;
        }

        // taxon specific?
        if ( taxonId != null && taxonId > 0) {
            Taxon taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
            if ( ids == null || ids.isEmpty() ) {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser,
                        limitToUse );
            } else {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, ids, filterDataByUser,
                        limitToUse );
            }

        } else if ( ids == null || ids.isEmpty() ) {
            // load everything (up to the limit)
            eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser, limitToUse );
        } else {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, ids, filterDataByUser, limitToUse );
        }

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( filter != null && filter > 0 ) eeValObjectCol = applyFilter( eeValObjectCol, filter );

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( limit != 0 && eeValObjectCol.size() > limit ) {
            log.info( "Still have to filter" );
            Collections.sort( eeValObjectCol, new Comparator<ExpressionExperimentValueObject>() {

                @Override
                public int compare( ExpressionExperimentValueObject o1, ExpressionExperimentValueObject o2 ) {
                    return -o1.getDateLastUpdated().compareTo( o2.getDateLastUpdated() );
                }
            } );

            eeValObjectCol = eeValObjectCol.subList( 0, limit );

        }

        assert limit <= 0 || eeValObjectCol.size() <= limit;

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
     * Maintains order if a list is passed in
     * 
     * @param securedEEs
     * @return List<ExpressionExperimentValueObject> in the same order as the EEs passed in
     */
    private List<ExpressionExperimentValueObject> getExpressionExperimentValueObjects(
            Collection<ExpressionExperiment> securedEEsCol ) {

        List<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>( securedEEsCol );

        if ( securedEEsCol.size() == 0 ) {
            return new ArrayList<ExpressionExperimentValueObject>();
        }
        StopWatch timer = new StopWatch();
        timer.start();

        // this method keeps order.
        List<ExpressionExperimentValueObject> valueObjs = ( List<ExpressionExperimentValueObject> ) expressionExperimentService
                .loadValueObjects( EntityUtils.getIds( securedEEs ) );

        if ( SecurityServiceImpl.isUserAdmin() ) {
            for ( ExpressionExperimentValueObject vo : valueObjs ) {
                vo.setCurrentUserHasWritePermission( true );
            }
        } else if ( SecurityServiceImpl.isUserLoggedIn() ) {
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

        assert securedEEsCol.size() >= valueObjs.size();
        return valueObjs;
    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     * 
     * @param taxon can be null
     * @param eeids can be null; if taxon is non-null, this is ignored.
     * @param filterDataForUser if true, then only the data owned by the user are returned (this has no effect if you
     *        are an administrator)
     * @param maximum # to retrieve, in order of most recently updated. Enter -1 to have no limit. (not the guaranteed
     *        maximum)
     * @return Collection<ExpressionExperimentValueObject>
     */
    private List<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects( Taxon taxon,
            Collection<Long> eeIds, boolean filterDataForUser, int limit ) {

        List<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>();

        StopWatch timer = new StopWatch();
        timer.start();

        int OVERSHOOT = 10; // how many extra to get in case our limit is not reached due to a filter.

        /*
         * FIXME remove troubled? Needs to be optional. For dataset managment page, don't.
         */

        /* Filtering for security happens here. */
        if ( filterDataForUser ) {
            try {

                securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService
                        .loadMySharedExpressionExperiments() ); // limit won't really
                // work! Most experiments
                // are filtered out.

                if ( limit > 0 ) {
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

                        Taxon t = expressionExperimentService.getTaxon( ee.getId() );

                        if ( t != null && t.getId().equals( taxon.getId() ) ) {
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
                    securedEEs = expressionExperimentService.findByTaxon( taxon, limit + OVERSHOOT );

                    if ( securedEEs.size() > limit && limit >= 0) {
                        securedEEs = securedEEs.subList( 0, limit );
                    }

                } else {
                    securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadMultiple( eeIds ) );
                    List<ExpressionExperiment> securedEEsfilteredByTaxon = new ArrayList<ExpressionExperiment>();

                    for ( ExpressionExperiment ee : securedEEs ) {

                        Taxon t = expressionExperimentService.getTaxon( ee.getId() );

                        if ( t != null && t.getId().equals( taxon.getId() ) ) {
                            securedEEsfilteredByTaxon.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByTaxon;
                }

            } else if ( eeIds == null ) {
                securedEEs = expressionExperimentService.findByUpdatedLimit( limit );
            } else {
                securedEEs = new ArrayList<ExpressionExperiment>( expressionExperimentService.loadMultiple( eeIds ) );
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

        /*
         * This is only populated with experiments that have reports available on disk.
         */
        Map<Long, Date> lastUpdated = expressionExperimentReportService.fillReportInformation( expressionExperiments );

        expressionExperimentReportService.fillAnnotationInformation( expressionExperiments );

        Map<Long, Date> eventDates = expressionExperimentReportService.fillEventInformation( expressionExperiments );

        for ( Long k : eventDates.keySet() ) {
            if ( lastUpdated.containsKey( k ) ) {
                if ( lastUpdated.get( k ).after( eventDates.get( k ) ) ) {
                    eventDates.put( k, lastUpdated.get( k ) );
                }
            }
        }
        return eventDates;
    }

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

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

    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
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
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) throws Exception {

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

        Collection<ExpressionExperimentValueObject> ees = expressionExperimentService.loadValueObjects( eeIds );

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

}