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

import gemma.gsec.SecurityService;
import gemma.gsec.util.SecurityUtil;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.preprocess.MeanVarianceService;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.report.WhatsNew;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.search.SearchResultDisplayObject;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.core.tasks.analysis.expression.UpdateEEDetailsCommand;
import ubic.gemma.core.tasks.analysis.expression.UpdatePubMedCommand;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.ControllerUtils;
import ubic.gemma.web.persistence.SessionListManager;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.taglib.expression.experiment.ExperimentQCTag;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.view.TextView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

/**
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
@Controller
@RequestMapping(value = { "/expressionExperiment", "/ee" })
public class ExpressionExperimentController {

    private static final Log log = LogFactory.getLog( ExpressionExperimentController.class.getName() );
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
    private AuditTrailService auditTrailService;
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
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private GeeqService geeqService;

    /**
     * AJAX call for remote paging store security isn't incorporated in db query, so paging needs to occur at higher
     * level.
     * <ol>
     * <li>a db call returns all experiments, which are filtered by the service method
     * <li>if the user is an admin, we filter out the troubled experiments
     * <li>an appropriate page-sized chunk is then taken from this (filtered) list
     * <li>another round of db calls create and fill value objects for this chunk
     * <li>value objects are returned
     * </ol>
     */
    public JsonReaderResponse<ExpressionExperimentDetailsValueObject> browse( ListBatchCommand batch ) {

        if ( batch.getLimit() == 0 ) {
            batch.setStart( 0 );
        }

        return this.browseSpecific( batch, null, null );

    }

    public JsonReaderResponse<ExpressionExperimentDetailsValueObject> browseByTaxon( ListBatchCommand batch,
            Long taxonId ) {

        if ( taxonId == null ) {
            return this.browse( batch );
        }
        Taxon taxon = taxonService.load( taxonId );
        if ( taxon == null ) {
            ExpressionExperimentController.log.info( "Attempted to browse experiments by taxon with id = " + taxonId
                    + ", but this id is invalid. Browsing without taxon restriction." );
            return this.browse( batch );
        }

        return this.browseSpecific( batch, null, taxon );

    }

    /**
     * AJAX call for remote paging store
     */
    public JsonReaderResponse<ExpressionExperimentDetailsValueObject> browseSpecificIds( ListBatchCommand batch,
            Collection<Long> ids ) {

        if ( batch.getLimit() == 0 ) {
            batch.setLimit( ids.size() );
            batch.setStart( 0 );
        }
        List<Long> noDupIds = new ArrayList<>( ids );

        return this.browseSpecific( batch, noDupIds, null );

    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the experiment and whether they can edit it
     */
    public boolean canCurrentUserEditExperiment( Long eeId ) {
        boolean userCanEditGroup;
        try {
            userCanEditGroup = securityService.isEditable( expressionExperimentService.load( eeId ) );
        } catch ( org.springframework.security.access.AccessDeniedException ade ) {
            return false;
        }
        return userCanEditGroup;
    }

    /**
     * AJAX clear entries in caches relevant to experimental design for the experiment passed in. The caches cleared are
     * the processedDataVectorCache and the caches held in ExperimentalDesignVisualizationService
     */
    public void clearFromCaches( Long eeId ) {
        expressionExperimentReportService.evictFromCache( eeId );
    }

    /**
     * Exposed for AJAX calls.
     */
    public String deleteById( Long id ) {
        if ( id == null )
            return null;
        RemoveExpressionExperimentTask task = new RemoveExpressionExperimentTask( new TaskCommand( id ) );
        return taskRunningService.submitLocalTask( task );
    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the experiment and whether they can edit it
     */
    public boolean doesCurrentUserOwnExperiment( Long eeId ) {
        boolean userOwnsGroup;
        try {
            userOwnsGroup = securityService.isOwnedByCurrentUser( expressionExperimentService.load( eeId ) );
        } catch ( org.springframework.security.access.AccessDeniedException ade ) {
            return false;
        }
        return userOwnsGroup;
    }

    @RequestMapping("/filterExpressionExperiments.html")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String searchString = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( searchString ) ) {
            return new ModelAndView(
                    new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html", true ) )
                    .addObject( "message", "No search criteria provided" );
        }

        Collection<Long> ids = expressionExperimentService.filter( searchString );

        if ( ids.isEmpty() ) {

            return new ModelAndView(
                    new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html", true ) )
                    .addObject( "message", "Your search yielded no results." );

        }

        if ( ids.size() == 1 ) {
            return new ModelAndView(
                    new RedirectView( "/expressionExperiment/showExpressionExperiment.html?id=" + ids.iterator().next(),
                            true ) ).addObject( "message",
                    "Search Criteria: " + searchString + "; " + ids.size() + " Datasets matched." );
        }

        StringBuilder list = new StringBuilder();
        for ( Long id : ids ) {
            list.append( id ).append( "," );
        }

        return new ModelAndView(
                new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html?id=" + list, true ) )
                .addObject( "message", "Search Criteria: " + searchString + "; " + ids.size() + " Datasets matched." );
    }

    /**
     * AJAX TODO --- include a search of subsets.
     *
     * @param query   search string
     * @param taxonId (if null, all taxa are searched)
     * @return EE ids that match
     */
    public Collection<Long> find( String query, Long taxonId ) {
        ExpressionExperimentController.log.info( "Search: query='" + query + "' taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    public List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId ) {

        return expressionExperimentSearchService.getAllTaxonExperimentGroup( taxonId );
    }

    /**
     * AJAX
     */
    public Collection<AnnotationValueObject> getAnnotation( EntityDelegator e ) {
        if ( e == null || e.getId() == null )
            return null;
        return expressionExperimentService.getAnnotations( e.getId() );
    }

    /**
     * AJAX call
     *
     * @return a more informative description than the regular description 1st 120 characters of ee.description +
     * Experimental Design information returned string contains HTML tags.
     * TODO: Would be more generic if passed back a DescriptionValueObject that contains all the info necessary
     * to reconstruct the HTML on the client side Currently only used by ExpressionExperimentGrid.js (row
     * expander)
     */
    public String getDescription( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null )
            return null;

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> efs = ee.getExperimentalDesign().getExperimentalFactors();

        StringBuilder descriptive = new StringBuilder();

        String eeDescription = ee.getDescription() == null ? "" : ee.getDescription().trim();

        // Need to trim?
        if ( eeDescription.length() < ExpressionExperimentController.TRIM_SIZE + 1 )
            descriptive.append( eeDescription );
        else
            descriptive.append( eeDescription.substring( 0, ExpressionExperimentController.TRIM_SIZE ) )
                    .append( "...&nbsp;&nbsp;" );

        // Is there any factor info to add?
        if ( efs.size() < 1 )
            return descriptive.append( "</br><b>(No Factors)</b>" ).toString();

        String efUri = "&nbsp;<a target='_blank' href='" + Settings.getRootContext()
                + "/experimentalDesign/showExperimentalDesign.html?eeid=" + ee.getId() + "'>(details)</a >";
        int MAX_TAGS_TO_SHOW = 15;
        Collection<Characteristic> tags = ee.getCharacteristics();
        if ( tags.size() > 0 ) {
            descriptive.append( "</br>&nbsp;<b>Tags:</b>&nbsp;" );
            int i = 0;
            for ( Characteristic tag : tags ) {
                descriptive.append( tag.getValue() ).append( ", " );
                if ( ++i > MAX_TAGS_TO_SHOW ) {
                    descriptive.append( " [more tags not shown]" );
                    break;
                }
            }

        }

        descriptive.append( "</br>&nbsp;<b>Factors:</b>&nbsp;" );
        for ( ExperimentalFactor ef : efs ) {
            if ( !ExperimentalDesignUtils.isBatch( ef ) ) {
                descriptive.append( ef.getName() ).append( " (" ).append( ef.getDescription() ).append( "), " );
            }
        }

        // remove trailing "," and return as a string
        return descriptive.substring( 0, descriptive.length() - 2 ) + efUri;

    }

    /**
     * AJAX
     */
    public Collection<DesignMatrixRowValueObject> getDesignMatrixRows( EntityDelegator e ) {

        if ( e == null || e.getId() == null )
            return null;
        ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );
        if ( ee == null )
            return null;

        ee = expressionExperimentService.thawLite( ee );
        return DesignMatrixRowValueObject.Factory.getDesignMatrix( ee, true ); // ignore "batch"
    }

    /**
     * AJAX
     *
     * @return a collection of factor value objects that represent the factors of a given experiment
     */
    public Collection<ExperimentalFactorValueObject> getExperimentalFactors( EntityDelegator e ) {

        if ( e == null || e.getId() == null )
            return null;

        ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );

        Collection<ExperimentalFactorValueObject> result = new HashSet<>();

        if ( ee.getExperimentalDesign() == null )
            return null;

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor factor : factors )
            result.add( new ExperimentalFactorValueObject( factor ) );

        return result;
    }

    /**
     * AJAX
     *
     * @return A collection of factor value objects for the specified experimental factor
     */
    public Collection<FactorValueValueObject> getFactorValues( EntityDelegator e ) {

        if ( e == null || e.getId() == null )
            return null;

        ExperimentalFactor ef = this.experimentalFactorService.load( e.getId() );
        if ( ef == null )
            return null;

        Collection<FactorValueValueObject> result = new HashSet<>();

        Collection<FactorValue> values = ef.getFactorValues();
        for ( FactorValue value : values ) {
            result.add( new FactorValueValueObject( value ) );
        }

        return result;
    }

    /**
     * Used to include the html for the qc table in an ext panel (without using a tag) (This method should probably be
     * in a service?)
     */
    public String getQCTagHTML( ExpressionExperiment ee ) {
        ExperimentQCTag qc = new ExperimentQCTag();
        qc.setEe( ee.getId() );
        qc.setEeManagerId( ee.getId() + "-eemanager" );
        qc.setHasCorrMat( sampleCoexpressionAnalysisService.hasAnalysis( ee ) );
        qc.setHasNodeDegreeDist( ExpressionExperimentQCUtils.hasNodeDegreeDistFile( ee ) );
        qc.setHasPCA( svdService.hasPca( ee.getId() ) );
        qc.setNumFactors( ExpressionExperimentQCUtils.numFactors( ee ) );
        qc.setHasMeanVariance( meanVarianceService.hasMeanVariance( ee ) );
        qc.setHasCorrDist( this.coexpressionAnalysisService.hasCoexpCorrelationDistribution( ee ) );
        qc.setNumOutliersRemoved( this.numOutliersRemoved( ee ) );
        try {
            qc.setNumPossibleOutliers( this.numPossibleOutliers( ee ) );
        } catch ( java.lang.ArrayIndexOutOfBoundsException e ) {
            ExpressionExperimentController.log.fatal( e );
            e.printStackTrace();
        }
        return qc.getQChtml();
    }

    /**
     * AJAX method to get data for database summary table, returned as a JSON object the slow part here is loading each
     * new or updated object in whatsNewService.retrieveReport() -&gt; fetch()
     *
     * @return json
     */
    public JSONObject loadCountsForDataSummaryTable() {

        JSONObject summary = new JSONObject();
        net.sf.json.JSONArray taxonEntries = new net.sf.json.JSONArray();

        long bioMaterialCount = bioMaterialService.countAll();
        long arrayDesignCount = arrayDesignService.countAll();
        Map<Taxon, Long> unsortedEEsPerTaxon = expressionExperimentService.getPerTaxonCount();

        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return o1.getScientificName().compareTo( o2.getScientificName() );
            }
        } );

        long expressionExperimentCount = 0; // expressionExperimentService.countAll();
        for ( Taxon t : unsortedEEsPerTaxon.keySet() ) {
            Long c = unsortedEEsPerTaxon.get( t );

            eesPerTaxon.put( t, c );
            expressionExperimentCount += c;
        }

        // this is the slow part
        WhatsNew wn = whatsNewService.retrieveReport();

        if ( wn == null ) {
            wn = whatsNewService.getReport();
        }
        if ( wn != null ) {
            // Get count for new assays
            int newBioMaterialCount = wn.getNewBioMaterialCount();

            Collection<ExpressionExperiment> newExpressionExperiments = wn.getNewExpressionExperiments();
            Collection<Long> newExpressionExperimentIds = ( newExpressionExperiments != null ) ?
                    EntityUtils.getIds( newExpressionExperiments ) :
                    new ArrayList<Long>();
            Collection<ExpressionExperiment> updatedExpressionExperiments = wn.getUpdatedExpressionExperiments();
            Collection<Long> updatedExpressionExperimentIds = ( updatedExpressionExperiments != null ) ?
                    EntityUtils.getIds( updatedExpressionExperiments ) :
                    new ArrayList<Long>();

            int newExpressionExperimentCount = ( newExpressionExperiments != null ) ?
                    newExpressionExperiments.size() :
                    0;
            int updatedExpressionExperimentCount = ( updatedExpressionExperiments != null ) ?
                    updatedExpressionExperiments.size() :
                    0;

            /* Store counts for new and updated experiments by taxonId */
            Map<Taxon, Collection<Long>> newEEsPerTaxon = wn.getNewEEIdsPerTaxon();
            Map<Taxon, Collection<Long>> updatedEEsPerTaxon = wn.getUpdatedEEIdsPerTaxon();

            for ( Taxon t : unsortedEEsPerTaxon.keySet() ) {
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
            Collection<ArrayDesign> newArrayDesigns = wn.getNewArrayDesigns();
            int newArrayCount = ( newArrayDesigns != null ) ? newArrayDesigns.size() : 0;
            Collection<ArrayDesign> updatedArrayDesigns = wn.getUpdatedArrayDesigns();
            int updatedArrayCount = ( updatedArrayDesigns != null ) ? updatedArrayDesigns.size() : 0;

            boolean drawNewColumn = ( newExpressionExperimentCount > 0 || newArrayCount > 0
                    || newBioMaterialCount > 0 );
            boolean drawUpdatedColumn = ( updatedExpressionExperimentCount > 0 || updatedArrayCount > 0 );
            String date = ( wn.getDate() != null ) ?
                    DateFormat.getDateInstance( DateFormat.LONG ).format( wn.getDate() ) :
                    "";
            date = date.replace( '-', ' ' );

            summary.element( "updateDate", date );
            summary.element( "drawNewColumn", drawNewColumn );
            summary.element( "drawUpdatedColumn", drawUpdatedColumn );
            if ( newBioMaterialCount != 0 )
                summary.element( "newBioMaterialCount", new Long( newBioMaterialCount ) );
            if ( newArrayCount != 0 )
                summary.element( "newArrayDesignCount", new Long( newArrayCount ) );
            if ( updatedArrayCount != 0 )
                summary.element( "updatedArrayDesignCount", new Long( updatedArrayCount ) );
            if ( newExpressionExperimentCount != 0 )
                summary.element( "newExpressionExperimentCount", newExpressionExperimentCount );
            if ( updatedExpressionExperimentCount != 0 )
                summary.element( "updatedExpressionExperimentCount", updatedExpressionExperimentCount );
            if ( newExpressionExperimentCount != 0 )
                summary.element( "newExpressionExperimentIds", newExpressionExperimentIds );
            if ( updatedExpressionExperimentCount != 0 )
                summary.element( "updatedExpressionExperimentIds", updatedExpressionExperimentIds );

        }

        summary.element( "bioMaterialCount", bioMaterialCount );
        summary.element( "arrayDesignCount", arrayDesignCount );

        summary.element( "expressionExperimentCount", expressionExperimentCount );

        return summary;
    }

    /**
     * AJAX; Populate all the details.
     *
     * @param id Identifier for the experiment
     * @return ee details vo
     */
    public ExpressionExperimentDetailsValueObject loadExpressionExperimentDetails( Long id ) {

        ExpressionExperiment ee = this.getEESafely( id );
        Collection<ExpressionExperimentDetailsValueObject> initialResults = expressionExperimentService
                .loadDetailsValueObjects( null, false, Collections.singleton( id ), null, 0, 0 );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        expressionExperimentReportService.populateReportInformation( initialResults );
        expressionExperimentReportService.getAnnotationInformation( initialResults );
        expressionExperimentReportService.populateEventInformation( initialResults );

        ExpressionExperimentDetailsValueObject finalResult = initialResults.iterator().next();
        // Most of DetailsVO values are set automatically through the constructor.
        // We only need to set the additional values:

        finalResult.setQChtml( this.getQCTagHTML( ee ) );
        finalResult.setExpressionExperimentSets( this.getExpressionExperimentSets( ee ) );

        finalResult = this.setPreferredAndReprocessed( finalResult, ee );
        finalResult = this.setMultipleTechTypes( finalResult, ee );

        finalResult = this.setPublicationAndAuthor( finalResult, ee );
        finalResult = this.setBatchInfo( finalResult, ee );

        Date lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( ee );
        if ( lastArrayDesignUpdate != null ) {
            finalResult.setLastArrayDesignUpdateDate( lastArrayDesignUpdate.toString() );
        }

        return finalResult;
    }

    public void recalculateBatchConfound( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        ee.setBatchConfound( expressionExperimentService.getBatchConfound( ee ) );
        expressionExperimentService.update( ee );
    }

    public void recalculateBatchEffect( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        ee.setBatchEffect( expressionExperimentService.getBatchEffectDescription( ee ) );
        expressionExperimentService.update( ee );
    }

    public void runGeeq( Long id, String mode ) {
        geeqService.calculateScore( id, mode );
    }

    public void setGeeqManualSettings( long id, GeeqAdminValueObject vo ) {
        geeqService.setManualOverrides( id, vo );
    }

    /**
     * AJAX - for display in tables. Don't retrieve too much detail.
     *
     * @param ids of EEs to load
     * @return security-filtered set of value objects.
     */
    public Collection<ExpressionExperimentDetailsValueObject> loadExpressionExperiments( List<Long> ids ) {
        if ( ids.isEmpty() ) {
            return new HashSet<>();
        }
        return this.getFilteredExpressionExperimentValueObjects( null, ids, 0, true );
    }

    /**
     * AJAX get experiments that used a given platform. Don't retrieve too much detail.
     *
     * @param id of platform
     */
    public Collection<ExpressionExperimentDetailsValueObject> loadExperimentsForPlatform( Long id ) {
        return this.getFilteredExpressionExperimentValueObjects( null, ( List<Long> ) EntityUtils
                .getIds( arrayDesignService.getExpressionExperiments( arrayDesignService.load( id ) ) ), 0, true );
    }

    /**
     * AJAX - for display in tables. Get more details.
     *
     * @param ids of EEs to load
     * @return security-filtered set of value objects.
     */
    public Collection<ExpressionExperimentDetailsValueObject> loadDetailedExpressionExperiments(
            Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return new HashSet<>();
        }
        Collection<ExpressionExperimentDetailsValueObject> result = this
                .getFilteredExpressionExperimentValueObjects( null, null, 0, true );
        this.expressionExperimentReportService.populateReportInformation( result );
        return result;
    }

    /**
     * AJAX; get a collection of experiments that have had samples removed due to outliers
     * TODO: and experiment that have possible batch effects detected
     *
     * @return json reader response
     */
    public JsonReaderResponse<JSONObject> loadExpressionExperimentsWithQcIssues() {

        Collection<ExpressionExperiment> outlierEEs = expressionExperimentService.getExperimentsWithOutliers();

        Collection<ExpressionExperiment> ees = new HashSet<>();
        ees.addAll( outlierEEs );
        // ees.addAll( batchEffectEEs );

        List<JSONObject> jsonRecords = new ArrayList<>();

        for ( ExpressionExperiment ee : ees ) {
            //noinspection MismatchedQueryAndUpdateOfCollection
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

        return new JsonReaderResponse<>( jsonRecords );

    }

    /**
     * AJAX - for display in tables
     *
     * @param eeId ee id
     * @return security-filtered set of value objects.
     */
    public Collection<QuantitationTypeValueObject> loadQuantitationTypes( Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        // need to thawRawAndProcessed?
        ee = expressionExperimentService.thawLite( ee );
        Collection<QuantitationType> qts = ee.getQuantitationTypes();

        return quantitationTypeService.loadValueObjects( qts );
    }

    /**
     * AJAX. Data summarizing the status of experiments.
     *
     * @param taxonId    can be null
     * @param limit      If &gt;0, get the most recently updated N experiments, where N &lt;= limit; or if &lt; 0, get
     *                   the
     *                   least
     *                   recently updated; if 0, or null, return all.
     * @param filter     if non-null, limit data sets to ones meeting criteria.
     * @param showPublic return user's public datasets too
     * @return ee details vos
     */
    public Collection<ExpressionExperimentDetailsValueObject> loadStatusSummaries( Long taxonId, List<Long> ids,
            Integer limit, Integer filter, Boolean showPublic ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<ExpressionExperimentDetailsValueObject> vos;

        if ( !SecurityUtil.isUserLoggedIn() ) {
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        if ( limit == null ) {
            limit = 50;
        }
        vos = this.getEEVOsForManager( taxonId, ids, limit, filter, showPublic );

        if ( vos.isEmpty() ) {
            return new HashSet<>();
        }

        if ( timer.getTime() > 1000 ) {
            ExpressionExperimentController.log.info( "Fetching basic data took: " + timer.getTime() + "ms" );
        }

        timer.reset();
        timer.start();

        expressionExperimentReportService.getAnnotationInformation( vos );
        expressionExperimentReportService.populateEventInformation( vos );

        if ( timer.getTime() > 1000 ) {
            ExpressionExperimentController.log
                    .info( "Filling in report data for " + vos.size() + " EEs: " + timer.getTime() + "ms" );
        }

        // We need to convert the VOs to detailVos and add array designs so trouble info can be correctly displayed.
        for ( ExpressionExperimentDetailsValueObject vo : vos ) {
            vo.setArrayDesigns( arrayDesignService.loadValueObjectsForEE( vo.getId() ) );
        }

        return vos;
    }

    /**
     * Remove the primary publication for the given expression experiment (by id). The reference is not actually deleted
     * from the system. AJAX
     *
     * @param eeId ee id
     * @return string
     */
    @SuppressWarnings("UnusedReturnValue") // AJAX method - Possibly used in JS
    public String removePrimaryPublication( Long eeId ) {
        RemovePubMed task = new RemovePubMed( new TaskCommand( eeId ) );
        return taskRunningService.submitLocalTask( task );
    }

    /**
     * AJAX (used by experimentAndExperimentGroupCombo.js)
     *
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @param query   query
     * @return Collection of SearchResultDisplayObjects
     */
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {
        boolean taxonLimited = ( taxonId != null );
        List<SearchResultDisplayObject> displayResults = new ArrayList<>();

        // add session bound sets
        // get any session-bound groups
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = ( taxonLimited ) ?
                sessionListManager.getModifiedExperimentSets( taxonId ) :
                sessionListManager.getModifiedExperimentSets();

        List<SearchResultDisplayObject> sessionSets = new ArrayList<>();

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

        for ( SearchResultDisplayObject r : displayResults ) {
            r.setOriginalQuery( query );
        }

        return displayResults;
    }

    /**
     * AJAX (used by ExperimentCombo.js)
     *
     * @param query query
     * @return Collection of expression experiment entity objects
     */
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        return expressionExperimentSearchService.searchExpressionExperiments( query );

    }

    /**
     * Show all experiments (optionally conditioned on either a taxon, a list of ids, or a platform)
     *
     * @param request  request
     * @param response response
     * @return model and view
     */
    @RequestMapping(value = { "/showAllExpressionExperiments.html", "/showAll" })
    public ModelAndView showAllExpressionExperiments( HttpServletRequest request, HttpServletResponse response ) {

        return new ModelAndView( "expressionExperiments" );

    }

    @RequestMapping(value = { "/showAllExpressionExperimentLinkSummaries.html", "/manage.html" })
    public ModelAndView showAllLinkSummaries( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "expressionExperimentLinkSummary" );
    }

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
        ModelAndView mv = new ModelAndView( "bioAssays" )
                .addObject( "bioAssays", bioAssayService.thaw( expressionExperiment.getBioAssays() ) );

        this.addQCInfo( expressionExperiment, mv );
        mv.addObject( "expressionExperiment", expressionExperiment );
        return mv;
    }

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
        Collection<BioMaterial> bioMaterials = new ArrayList<>();
        for ( BioAssay assay : bioAssays ) {
            BioMaterial material = assay.getSampleUsed();
            if ( material != null ) {
                bioMaterials.add( material );
            }
        }

        ModelAndView mav = new ModelAndView( "bioMaterials" );
        if ( ExpressionExperimentController.AJAX ) {
            mav.addObject( "bioMaterialIdList", bioMaterialService.getBioMaterialIdList( bioMaterials ) );
        }

        Integer numBioMaterials = bioMaterials.size();
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterialService.thaw( bioMaterials ) );

        this.addQCInfo( expressionExperiment, mav );

        return mav;
    }

    @RequestMapping({ "/showExpressionExperiment.html", "/", "/show" })
    public ModelAndView showExpressionExperiment( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch timer = new StopWatch();
        timer.start();

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" );
        BioAssaySet expExp = this.getExpressionExperimentFromRequest( request );

        mav.addObject( "expressionExperiment", expExp );

        mav.addObject( "eeId", expExp.getId() );
        mav.addObject( "eeClass", ExpressionExperiment.class.getName() );

        if ( timer.getTime() > 200 ) {
            ExpressionExperimentController.log
                    .info( "Show Experiment was slow: id=" + expExp.getId() + " " + timer.getTime() + "ms" );
        }

        return mav;
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     *
     * @param request  request
     * @param response response
     * @return model and view
     */
    @RequestMapping(value = { "/showExpressionExperimentSubSet.html", "/showSubset" })
    public ModelAndView showSubSet( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );

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
     * @param eeId ee id
     */
    public void unmatchAllBioAssays( Long eeId ) {
        ExpressionExperiment ee = this.expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with id=" + eeId );
        }
        ee = expressionExperimentService.thawLite( ee );

        Collection<BioMaterial> needToProcess = new HashSet<>();

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            this.bioMaterialService.thaw( bm );
            Collection<BioAssay> bioAssaysUsedIn = bm.getBioAssaysUsedIn();
            if ( bioAssaysUsedIn.size() > 1 ) {
                needToProcess.add( bm );
            }
        }

        // FIXME this should be in a transaction!
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

                    baU.setSampleUsed( newMaterial );
                    bioAssayService.update( baU );

                }
                i++;
            }

        }

    }

    public ExpressionExperimentDetailsValueObject updateBasics( UpdateEEDetailsCommand command ) throws Exception {
        if ( command.getEntityId() == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        /*
         * This should be fast so I'm not using a background task.
         */
        String details = "Changed: ";
        boolean changed = false;
        Long entityId = command.getEntityId();
        ExpressionExperiment ee = expressionExperimentService.load( entityId );
        if ( ee == null )
            throw new IllegalArgumentException( "Cannot locate or access experiment with id=" + entityId );

        if ( StringUtils.isNotBlank( command.getShortName() ) && !command.getShortName().equals( ee.getShortName() ) ) {
            if ( expressionExperimentService.findByShortName( command.getShortName() ) != null ) {
                throw new IllegalArgumentException( "An experiment with short name '" + command.getShortName()
                        + "' already exists, you must use a unique name" );
            }
            details += "short name (" + ee.getShortName() + " -> " + command.getShortName() + ")";
            changed = true;
            ee.setShortName( command.getShortName() );
        }
        if ( StringUtils.isNotBlank( command.getName() ) && !command.getName().equals( ee.getName() ) ) {
            details += ( changed ? ", " : "" ) + "name (" + ee.getName() + " -> " + command.getName() + ")";
            changed = true;
            ee.setName( command.getName() );
        }
        if ( StringUtils.isNotBlank( command.getDescription() ) && !command.getDescription()
                .equals( ee.getDescription() ) ) {
            details +=
                    ( changed ? ", " : "" ) + "description (" + ee.getDescription() + " -> " + command.getDescription()
                            + ")";
            changed = true;
            ee.setDescription( command.getDescription() );
        }
        if ( !command.isRemovePrimaryPublication() && StringUtils.isNotBlank( command.getPubMedId() ) ) {
            if ( ee.getPrimaryPublication() != null ) {
                details += ( changed ? ", " : "" ) + "primary publication (id " + ee.getPrimaryPublication().getId()
                        + " -> " + command.getPubMedId() + ")";
            } else {
                details += ( changed ? ", " : "" ) + "primary publication ( none -> " + command.getPubMedId() + ")";
            }
            changed = true;
            this.updatePubMed( entityId, command.getPubMedId() );
        } else if ( command.isRemovePrimaryPublication() ) {
            details += ( changed ? ", " : "" ) + "removed primary publication";
            changed = true;
            this.removePrimaryPublication( entityId );
        }

        if ( changed ) {
            ExpressionExperimentController.log.info( "Updating " + ee );
            auditTrailService
                    .addUpdateEvent( ee, CommentedEvent.Factory.newInstance(), "Updated experiment details", details );
            expressionExperimentService.update( ee );
        }

        return this.loadExpressionExperimentDetails( ee.getId() );
    }

    /*
     * FIXME change name of this to reflect that it updates more than just the correlation matrix.
     */
    @RequestMapping("/refreshCorrMatrix.html")
    public ModelAndView updateCorrelationMatrix( Long id ) {
        // TODO: make this an ajax background job
        this.updateCorrelationMatrixFile( id );
        this.updateMV( id );
        return new ModelAndView(
                new RedirectView( "/expressionExperiment/showExpressionExperiment.html?id=" + id, true ) );
    }

    /**
     * AJAX. Associate the given pubmedId with the given expression experiment.
     *
     * @param eeId     ee id
     * @param pubmedId pubmed id
     * @return string
     */
    @SuppressWarnings("UnusedReturnValue") // AJAX method - possibly used in JS
    public String updatePubMed( Long eeId, String pubmedId ) {
        UpdatePubMedCommand command = new UpdatePubMedCommand( eeId );
        command.setPubmedId( pubmedId );
        UpdatePubMed task = new UpdatePubMed( command );
        return taskRunningService.submitLocalTask( task );
    }

    @RequestMapping("/downloadExpressionExperimentList.html")
    protected ModelAndView handleRequestInternal( HttpServletRequest request ) {

        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Long> eeIds = ControllerUtils.extractIds( request.getParameter( "e" ) ); // might not be any
        Collection<Long> eeSetIds = ControllerUtils.extractIds( request.getParameter( "es" ) ); // might not be there
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

        mav.addObject( TextView.TEXT_PARAM, this.format4File( ees, eeSetName ) );
        watch.stop();
        Long time = watch.getTime();

        if ( time > 100 ) {
            ExpressionExperimentController.log
                    .info( "Retrieved and Formated" + ees.size() + " genes in : " + time + " ms." );
        }
        return mav;
    }

    private JsonReaderResponse<ExpressionExperimentDetailsValueObject> browseSpecific( ListBatchCommand batch,
            List<Long> ids, Taxon taxon ) {

        Collection<ExpressionExperimentDetailsValueObject> records = this
                .loadAllValueObjectsOrdered( batch, ids, taxon );

        int count = SecurityUtil.isUserAdmin() ?
                expressionExperimentService.countAll() :
                expressionExperimentService.countNotTroubled();

        return new JsonReaderResponse<>( records, count );
    }

    /**
     * How many possible sample outliers are detected?
     */
    private int numPossibleOutliers( ExpressionExperiment ee ) {
        int count;

        if ( ee == null ) {
            ExpressionExperimentController.log.warn( " Experiment is null " );
            return 0;
        }

        // identify outliers
        if ( !sampleCoexpressionAnalysisService.hasAnalysis( ee ) ) {
            return 0;
        }

        Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( ee );
        count = outliers.size();

        if ( count > 0 )
            ExpressionExperimentController.log.info( count + " possible outliers detected." );

        return count;
    }

    /**
     * How many possible sample outliers were removed?
     */
    private int numOutliersRemoved( ExpressionExperiment ee ) {
        int count = 0;

        if ( ee == null ) {
            ExpressionExperimentController.log.warn( " Experiment is null " );
            return 0;
        }

        ee = expressionExperimentService.thawLite( ee );
        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() != null && assay.getIsOutlier() ) {
                count++;
            }

        }

        ExpressionExperimentController.log.info( count + " outliers were removed." );

        return count;
    }

    /**
     * Sets batch information and related properties
     *
     * @param ee          ee
     * @param finalResult result
     * @return ee details vo
     */
    private ExpressionExperimentDetailsValueObject setBatchInfo( ExpressionExperimentDetailsValueObject finalResult,
            ExpressionExperiment ee ) {
        boolean hasBatchInformation = expressionExperimentService.checkHasBatchInfo( ee );

        finalResult.setHasBatchInformation( hasBatchInformation );
        if ( hasBatchInformation ) {
            finalResult.setBatchConfound( expressionExperimentService.getBatchConfound( ee ) );
            finalResult.setBatchEffect( expressionExperimentService.getBatchEffectDescription( ee ) );
        }

        return finalResult;
    }

    /**
     * populates the publication and author information
     *
     * @param ee          ee
     * @param finalResult result
     * @return ee details vo
     */
    private ExpressionExperimentDetailsValueObject setPublicationAndAuthor(
            ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {

        finalResult.setDescription( ee.getDescription() );

        if ( ee.getPrimaryPublication() != null && ee.getPrimaryPublication().getPubAccession() != null ) {
            finalResult.setPrimaryCitation(
                    CitationValueObject.convert2CitationValueObject( ee.getPrimaryPublication() ) );
            String accession = ee.getPrimaryPublication().getPubAccession().getAccession();

            try {
                finalResult.setPubmedId( Integer.parseInt( accession ) );
            } catch ( NumberFormatException e ) {
                ExpressionExperimentController.log.warn( "Pubmed id not formatted correctly: " + accession );
            }
        }

        return finalResult;
    }

    /**
     * Loads, checks not null, and thaws the array designs the given EE is associated with.
     *
     * @param ee ee
     * @return ads
     */
    private Collection<ArrayDesign> getADsSafely( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( ads == null ) {
            throw new IllegalArgumentException( "No array designs for experiment " + ee.getId() + " could be loaded." );
        }
        ads = arrayDesignService.thawLite( ads );

        return ads;
    }

    /**
     * Loads, checks not null, and thaws the ee with given ID;
     *
     * @param id id
     * @return ee
     */
    private ExpressionExperiment getEESafely( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + id + " could be loaded" );
        }
        ee = expressionExperimentService.thawLiter( ee );

        return ee;
    }

    /**
     * Checks and sets multiple technology types
     *
     * @param ee          ee
     * @param finalResult result
     * @return ee details vo
     */
    private ExpressionExperimentDetailsValueObject setMultipleTechTypes(
            ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {
        Collection<TechnologyType> techTypes = new HashSet<>();
        for ( ArrayDesign ad : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            techTypes.add( ad.getTechnologyType() );
        }

        finalResult.setHasMultipleTechnologyTypes( techTypes.size() > 1 );

        return finalResult;
    }

    /**
     * Check for multiple "preferred" qts and reprocessing.
     *
     * @param ee          ee
     * @param finalResult result
     * @return ee details vo
     */
    private ExpressionExperimentDetailsValueObject setPreferredAndReprocessed(
            ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        int countPreferred = 0;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                countPreferred++;
            }
            if ( qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
            }
        }

        finalResult.setHasMultiplePreferredQuantitationTypes( countPreferred > 1 );
        finalResult.setReprocessedFromRawData( dataReprocessedFromRaw );

        return finalResult;
    }

    private void addQCInfo( ExpressionExperiment expressionExperiment, ModelAndView mav ) {
        mav.addObject( "hasCorrMat", sampleCoexpressionAnalysisService.hasAnalysis( expressionExperiment ) );
        mav.addObject( "hasPvalueDist", ExpressionExperimentQCUtils.hasPvalueDistFiles( expressionExperiment ) );
        mav.addObject( "hasPCA", svdService.hasPca( expressionExperiment.getId() ) );
        mav.addObject( "hasMeanVariance", meanVarianceService.hasMeanVariance( expressionExperiment ) );

        // FIXME don't store in a file.
        mav.addObject( "hasNodeDegreeDist", ExpressionExperimentQCUtils.hasNodeDegreeDistFile( expressionExperiment ) );

        mav.addObject( "numFactors", ExpressionExperimentQCUtils.numFactors( expressionExperiment ) );
        mav.addObject( "hasCorrDist", true ); // FIXME

        mav.addObject( "numPossibleOutliers", this.numPossibleOutliers( expressionExperiment ) );
        mav.addObject( "numOutliersRemoved", this.numOutliersRemoved( expressionExperiment ) );
    }

    /**
     * Filter based on criteria of which events etc. the data sets have.
     *
     * @param eeValObjectCol ee vos
     * @param filter         filter
     * @return filtered vos
     */
    private Collection<ExpressionExperimentDetailsValueObject> applyFilter(
            Collection<ExpressionExperimentDetailsValueObject> eeValObjectCol, Integer filter ) {
        List<ExpressionExperimentDetailsValueObject> filtered = new ArrayList<>();
        Collection<ExpressionExperiment> eesToKeep = null;
        List<ExpressionExperimentDetailsValueObject> eeVOsToKeep = null;

        switch ( filter ) {
            case 1: // eligible for diff and don't have it.
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                eesToKeep.removeAll( expressionExperimentService.loadLackingFactors() );
                break;
            case 2: // need coexp
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 3:
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, DifferentialExpressionAnalysisEvent.class );
                break;
            case 4:
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, LinkAnalysisEvent.class );
                break;
            case 5:
                // FIXME this can now be delegated to the DAO layer
                eeVOsToKeep = this.returnTroubled( eeValObjectCol, true );
                break;
            case 6:
                eesToKeep = expressionExperimentService.loadLackingFactors();
                break;
            case 7:
                eesToKeep = expressionExperimentService.loadLackingTags();
                break;
            case 8: // needs batch info
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                auditEventService.retainLackingEvent( eesToKeep, FailedBatchInformationMissingEvent.class );
                break;
            case 9:
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, BatchInformationFetchingEvent.class );
                break;
            case 10:
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainLackingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            case 11:
                eesToKeep = expressionExperimentService.load( EntityUtils.getIds( eeValObjectCol ) );
                auditEventService.retainHavingEvent( eesToKeep, PCAAnalysisEvent.class );
                break;
            case 12:
                // FIXME this can now be delegated to the DAO layer
                eeVOsToKeep = this.returnNeedsAttention( eeValObjectCol );
                break;
            case 13:
                // FIXME this can now be delegated to the DAO layer
                eeVOsToKeep = this.returnTroubled( eeValObjectCol, false );
                break;
            default:
                throw new IllegalArgumentException( "Unknown filter: " + filter );

        }

        assert eesToKeep == null || eesToKeep.size() <= eeValObjectCol.size();

        // get corresponding value objects from collection param
        if ( eesToKeep != null ) {
            if ( eesToKeep.isEmpty() ) {
                return filtered;
            }
            // Map<Long, ExpressionExperiment> idMap = EntityUtils.getIdMap( eesToKeep );
            Collection<Long> ids = EntityUtils.getIds( eesToKeep );
            for ( ExpressionExperimentDetailsValueObject eevo : eeValObjectCol ) {
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

    private String format4File( Collection<ExpressionExperimentValueObject> ees, String eeSetName ) {
        StringBuilder strBuff = new StringBuilder();
        strBuff.append( "# Generated by Gemma\n# " ).append( new Date() ).append( "\n" );
        strBuff.append( ExpressionDataFileService.DISCLAIMER + "#\n" );

        if ( eeSetName != null && eeSetName.length() != 0 )
            strBuff.append( "# Experiment Set: " ).append( eeSetName ).append( "\n" );
        strBuff.append( "# " ).append( ees.size() ).append( ( ees.size() > 1 ) ? " experiments" : " experiment" )
                .append( "\n#\n" );

        // add header
        strBuff.append( "Short Name\tFull Name\n" );
        for ( ExpressionExperimentValueObject ee : ees ) {
            if ( ee != null ) {
                strBuff.append( ee.getShortName() ).append( "\t" ).append( ee.getName() );
                strBuff.append( "\n" );
            }
        }

        return strBuff.toString();
    }

    /**
     * @param ids        - takes precedence
     * @param limit      - return the N most recently (limit > 0) or least recently updated experiments (limit < 0) or
     *                   all
     *                   (limit == 0)
     * @param filter     setting
     * @param showPublic return the user's public datasets as well
     * @return ee details vos
     */
    private Collection<ExpressionExperimentDetailsValueObject> getEEVOsForManager( Long taxonId, List<Long> ids,
            Integer limit, Integer filter, boolean showPublic ) {
        Collection<ExpressionExperimentDetailsValueObject> eeVos;

        // Limit default desc - lastUpdated is a date and the most recent date is the largest one.
        eeVos = this
                .getFilteredExpressionExperimentValueObjects( taxonService.load( taxonId ), ids, limit, showPublic );

        if ( filter != null && filter > 0 ) {
            eeVos = this.applyFilter( eeVos, filter );
        }
        if ( eeVos.isEmpty() ) {
            return eeVos;
        }

        return eeVos;

    }

    /**
     * @param request request
     * @return bio assay set
     * @throws IllegalArgumentException if a matching EE can't be loaded
     */
    private BioAssaySet getExpressionExperimentFromRequest( HttpServletRequest request ) {

        BioAssaySet expressionExperiment = null;
        Long id;

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

    private Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets( BioAssaySet ee ) {

        Collection<Long> eeSetIds = expressionExperimentSetService.findIds( ee );

        if ( eeSetIds.isEmpty() ) {
            return new HashSet<>();
        }

        Collection<ExpressionExperimentSetValueObject> vos = expressionExperimentSetService
                .loadValueObjectsByIds( eeSetIds );
        Collection<ExpressionExperimentSetValueObject> sVos = new ArrayList<>();

        for ( ExpressionExperimentSetValueObject vo : vos ) {
            if ( !expressionExperimentSetService.isAutomaticallyGenerated( vo.getDescription() ) ) {
                sVos.add( vo );
            }
        }

        return sVos;
    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     *
     * @param taxon      can be null
     * @param limit      limit
     * @param eeIds      ee ids
     * @param showPublic show public
     * @return Collection<ExpressionExperimentValueObject>
     */
    private Collection<ExpressionExperimentDetailsValueObject> getFilteredExpressionExperimentValueObjects( Taxon taxon,
            List<Long> eeIds, Integer limit, boolean showPublic ) {

        Collection<ExpressionExperimentDetailsValueObject> vos = expressionExperimentService
                .loadDetailsValueObjects( "curationDetails.lastUpdated", limit > 0, eeIds, taxon, Math.abs( limit ),
                        0 );
        // Hide public data sets if desired.
        if ( !vos.isEmpty() && !showPublic ) {
            Collection<ExpressionExperimentDetailsValueObject> publicEEs = securityService.choosePublic( vos );
            vos.removeAll( publicEEs );
        }

        return vos;
    }

    private List<ExpressionExperimentValueObject> getSubList( Integer limit,
            List<ExpressionExperimentValueObject> initialListOfValueObject ) {

        if ( limit < initialListOfValueObject.size() ) {
            initialListOfValueObject = initialListOfValueObject.subList( 0, limit );
        }
        return initialListOfValueObject;
    }

    private Collection<ExpressionExperimentDetailsValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch,
            List<Long> ids, Taxon taxon ) {
        String o = batch.getSort();
        boolean desc = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );
        int limit = batch.getLimit();
        int start = batch.getStart();
        return expressionExperimentService.loadDetailsValueObjects( o, desc, ids, taxon, limit, start );
    }

    /**
     * Read the troubled flag in each ExpressionExperimentValueObject and return only those object for which it is equal
     * to the shouldBeTroubled parameter.
     *
     * @param shouldBeTroubled set to true if the filter should keep the EEVOs that are troubled, or false to keep only
     *                         the not-troubled ones.
     * @param eevos            ee vos
     * @return ee vos
     */
    private <T extends ExpressionExperimentValueObject> List<T> returnTroubled( Collection<T> eevos,
            boolean shouldBeTroubled ) {
        List<T> filtered = new ArrayList<>();

        for ( T eevo : eevos ) {
            if ( eevo.getTroubled() == shouldBeTroubled ) {
                filtered.add( eevo );
            }
        }

        return filtered;
    }

    /**
     * Read the needs attention flag in each ExpressionExperimentValueObject and return only those object for which it
     * is true
     *
     * @param ees ees
     * @return ee detail vos
     */
    private List<ExpressionExperimentDetailsValueObject> returnNeedsAttention(
            Collection<ExpressionExperimentDetailsValueObject> ees ) {
        List<ExpressionExperimentDetailsValueObject> troubled = new ArrayList<>();

        for ( ExpressionExperimentDetailsValueObject eevo : ees ) {
            if ( eevo.getNeedsAttention() ) {
                troubled.add( eevo );
            }
        }

        return troubled;
    }

    /**
     * Update the file used for the sample correlation heatmaps
     * FIXME make this a background task, use the ProcessedExpressionDataVectorCreateTask
     *
     * @param id id
     */
    private void updateCorrelationMatrixFile( Long id ) {
        ExpressionExperiment ee;
        ee = expressionExperimentService.load( id );
        ee = expressionExperimentService.thawLiter( ee );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Unable to access experiment with id=" + id );
        }
        sampleCoexpressionAnalysisService.compute( ee );
    }

    private void updateMV( Long id ) {
        ExpressionExperiment expressionExperiment;
        expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "Unable to access experiment with id=" + id );
        }
        meanVarianceService.create( expressionExperiment, true );
    }

    /**
     * Delete expression experiments.
     *
     * @author pavlidis
     */
    private class RemoveExpressionExperimentTask extends AbstractTask<TaskResult, TaskCommand> {

        public RemoveExpressionExperimentTask( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {
            expressionExperimentService.remove( taskCommand.getEntityId() );

            return new TaskResult( taskCommand, new ModelAndView(
                    new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html", true ) )
                    .addObject( "message", "Dataset id: " + taskCommand.getEntityId() + " removed from Database" ) );

        }
    }

    private class RemovePubMed extends AbstractTask<TaskResult, TaskCommand> {

        public RemovePubMed( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {
            ExpressionExperiment ee = expressionExperimentService.load( taskCommand.getEntityId() );

            ee = expressionExperimentService.thawLite( ee );

            if ( ee.getPrimaryPublication() == null ) {
                return new TaskResult( taskCommand, false );
            }

            ExpressionExperimentController.log.info( "Removing reference" );
            ee.setPrimaryPublication( null );

            expressionExperimentService.update( ee );

            return new TaskResult( taskCommand, true );
        }

    }

    private class UpdatePubMed extends AbstractTask<TaskResult, UpdatePubMedCommand> {

        public UpdatePubMed( UpdatePubMedCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {
            Long eeId = taskCommand.getEntityId();
            ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeId );
            if ( expressionExperiment == null )
                throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );

            String pubmedId = taskCommand.getPubmedId();
            BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubmedId );

            if ( publication != null ) {

                ExpressionExperimentController.log.info( "Reference exists in system, associating..." );
                expressionExperiment.setPrimaryPublication( publication );
                expressionExperimentService.update( expressionExperiment );
            } else {
                ExpressionExperimentController.log.info( "Searching pubmed on line .." );

                // search for pubmedId
                PubMedSearch pms = new PubMedSearch();
                Collection<String> searchTerms = new ArrayList<>();
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
                    ExpressionExperimentController.log.info( "No matching publication found" );
                    throw new IllegalArgumentException( "No matching publication found" );
                } else if ( publications.size() > 1 ) {
                    ExpressionExperimentController.log.info( "Multiple matching publications found!" );
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
                    ExpressionExperimentController.log.info( "Found new publication, associating ..." );

                    publication = ( BibliographicReference ) persisterHelper.persist( publication );
                    // publication = bibliographicReferenceService.findOrCreate( publication );
                    // assign to expressionExperiment
                    expressionExperiment.setPrimaryPublication( publication );

                    expressionExperimentService.update( expressionExperiment );
                }
            }
            ExpressionExperimentDetailsValueObject result = new ExpressionExperimentDetailsValueObject(
                    expressionExperiment );
            result.setPubmedId( Integer.parseInt( pubmedId ) );
            publication = bibliographicReferenceService.thaw( publication );
            result.setPrimaryCitation( CitationValueObject.convert2CitationValueObject( publication ) );
            return new TaskResult( taskCommand, result );
        }

    }

}
