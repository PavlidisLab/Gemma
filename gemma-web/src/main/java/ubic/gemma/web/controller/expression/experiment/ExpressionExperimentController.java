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
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.preprocess.MeanVarianceService;
import ubic.gemma.analysis.preprocess.OutlierDetails;
import ubic.gemma.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSearchService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.tasks.AbstractTask;
import ubic.gemma.tasks.analysis.expression.UpdateEEDetailsCommand;
import ubic.gemma.tasks.analysis.expression.UpdatePubMedCommand;
import ubic.gemma.util.EntityUtils;
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
    private static final Double BATCH_EFFECT_PVALTHRESHOLD = 0.01;
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
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;
    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

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
            return browse( batch );
        }
        Taxon taxon = taxonService.load( taxonId );
        if ( taxon == null ) {
            log.info( "Attempted to browse experiments by taxon with id = " + taxonId
                    + ", but this id is invalid. Browsing without taxon restriction." );
            return browse( batch );
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
        Set<Long> noDupIds = new HashSet<>( ids );

        return this.browseSpecific( batch, noDupIds, null );

    }

    private JsonReaderResponse<ExpressionExperimentDetailsValueObject> browseSpecific( ListBatchCommand batch,
            Collection<Long> ids, Taxon taxon ) {

        int origLimit = batch.getLimit();
        int origStart = batch.getStart();

        List<ExpressionExperimentValueObject> recordsOrig = loadAllValueObjectsOrdered( batch, ids, taxon );
        List<ExpressionExperimentDetailsValueObject> records = new LinkedList<>();

        for ( ExpressionExperimentValueObject ro : recordsOrig ) {
            records.add( new ExpressionExperimentDetailsValueObject( ro ) );
        }

        // if user is not admin, remove troubled experiments
        if ( !SecurityUtil.isUserAdmin() ) {
            records = removeTroubledExperimentVOs( records );
        }

        /*
         * can't just do expressionExperimentService.countAll() because this will count experiments the user may not
         * have access to
         */
        int count = records.size();
        int pSize = Math.min( origStart + origLimit, records.size() );
        if ( batch.getLimit() == 0 ) {
            pSize = count;
        }
        List<ExpressionExperimentDetailsValueObject> recordsSubset = records.subList( origStart, pSize );

        // this populates securityInfo TODO populate security info in filter
        // List<ExpressionExperimentDetailsValueObject> valueObjects = new
        // ArrayList<ExpressionExperimentDetailsValueObject>(
        // getExpressionExperimentDetailsValueObjects( records.subList( origStart, pSize ) ) );

        // if admin, want to show why experiment is troubled
        if ( SecurityUtil.isUserAdmin() ) {
            for ( ExpressionExperimentDetailsValueObject vo : recordsSubset ) {
                ExpressionExperiment ee = this.getEESafely( vo.getId() );
                // trouble details are retrieved automatically if we set the array designs
                vo.setArrayDesigns(
                        arrayDesignService.loadValueObjects( EntityUtils.getIds( this.getADsSafely( ee ) ) ) );
            }
        }

        return new JsonReaderResponse<>( recordsSubset, count );
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
     *
     * @return msg if error occurred or empty string if successful
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
                    new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "No search criteria provided" );
        }

        Collection<Long> ids = expressionExperimentService.filter( searchString );

        if ( ids.isEmpty() ) {

            return new ModelAndView(
                    new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
                    .addObject( "message", "Your search yielded no results." );

        }

        if ( ids.size() == 1 ) {
            return new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + ids.iterator().next() ) )
                    .addObject( "message",
                            "Search Criteria: " + searchString + "; " + ids.size() + " Datasets matched." );
        }

        String list = "";
        for ( Long id : ids ) {
            list += id + ",";
        }

        return new ModelAndView(
                new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" + list ) )
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
        log.info( "Search: query='" + query + "' taxon=" + taxonId );
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
        if ( eeDescription.length() < TRIM_SIZE + 1 )
            descriptive.append( eeDescription );
        else
            descriptive.append( eeDescription.substring( 0, TRIM_SIZE ) ).append( "...&nbsp;&nbsp;" );

        // Is there any factor info to add?
        if ( efs.size() < 1 )
            return descriptive.append( "</br><b>(No Factors)</b>" ).toString();

        String efUri = "&nbsp;<a target='_blank' href='/Gemma/experimentalDesign/showExperimentalDesign.html?eeid=" + ee
                .getId() + "'>(details)</a >";
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
     * FIXME why is this using FactorValueValueObject? The constructor doesn't seem correct; should use
     * ExperimentalFactorValueObject(factor).
     *
     * @return a collection of factor value objects that represent the factors of a given experiment
     */
    public Collection<FactorValueValueObject> getExperimentalFactors( EntityDelegator e ) {

        if ( e == null || e.getId() == null )
            return null;

        ExpressionExperiment ee = this.expressionExperimentService.load( e.getId() );

        Collection<FactorValueValueObject> result = new HashSet<>();

        if ( ee.getExperimentalDesign() == null )
            return null;

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor factor : factors )
            result.add( new FactorValueValueObject( factor ) );

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
        qc.setHasCorrMat( sampleCoexpressionMatrixService.hasMatrix( ee ) );
        qc.setHasNodeDegreeDist( ExpressionExperimentQCUtils.hasNodeDegreeDistFile( ee ) );
        qc.setHasPCA( svdService.hasPca( ee.getId() ) );
        qc.setNumFactors( ExpressionExperimentQCUtils.numFactors( ee ) );
        qc.setHasMeanVariance( meanVarianceService.hasMeanVariance( ee ) );
        qc.setHasCorrDist( this.coexpressionAnalysisService.hasCoexpCorrelationDistribution( ee ) );
        qc.setNumOutliersRemoved( numOutliersRemoved( ee ) );
        try {
            qc.setNumPossibleOutliers( numPossibleOutliers( ee ) );
        } catch ( java.lang.ArrayIndexOutOfBoundsException e ) {
            log.fatal( e );
            e.printStackTrace();
        }
        return qc.getQChtml();
    }

    /**
     * How many possible sample outliers are detected?
     */
    private int numPossibleOutliers( ExpressionExperiment ee ) {
        int count;

        if ( ee == null ) {
            log.warn( " Experiment is null " );
            return 0;
        }

        // identify outliers
        if ( !sampleCoexpressionMatrixService.hasMatrix( ee ) ) {
            return 0;
        }
        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = sampleCoexpressionMatrixService.findOrCreate( ee );
        if ( sampleCorrelationMatrix.rows() < 3 ) {
            return 0;
        }
        Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliers( ee, sampleCorrelationMatrix );
        count = outliers.size();

        log.info( count + " possible outliers detected." );

        return count;
    }

    /**
     * How many possible sample outliers were removed?
     */
    private int numOutliersRemoved( ExpressionExperiment ee ) {
        int count = 0;

        if ( ee == null ) {
            log.warn( " Experiment is null " );
            return 0;
        }

        ee = expressionExperimentService.thawLite( ee );
        for ( BioAssay assay : ee.getBioAssays() ) {
            if ( assay.getIsOutlier() ) {
                count++;
            }

        }

        log.info( count + " outliers were removed." );

        return count;
    }

    /**
     * AJAX method to get data for database summary table, returned as a JSON object the slow part here is loading each
     * new or updated object in whatsNewService.retrieveReport() -> fetch()
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
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return o1.getScientificName().compareTo( o2.getScientificName() );
            }
        } );
        LinkedHashMap<String, Long> eesPerTaxonName = new LinkedHashMap<>();

        long expressionExperimentCount = 0; // expressionExperimentService.countAll();
        for ( Taxon t : unsortedEEsPerTaxon.keySet() ) {
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
            wn = whatsNewService.getReport();
        }
        if ( wn != null ) {
            // Get count for new assays
            int newAssayCount = wn.getNewAssayCount();

            Collection<Long> newExpressionExperimentIds = ( wn.getNewExpressionExperiments() != null ) ?
                    EntityUtils.getIds( wn.getNewExpressionExperiments() ) :
                    new ArrayList<Long>();
            Collection<Long> updatedExpressionExperimentIds = ( wn.getUpdatedExpressionExperiments() != null ) ?
                    EntityUtils.getIds( wn.getUpdatedExpressionExperiments() ) :
                    new ArrayList<Long>();

            int newExpressionExperimentCount = ( wn.getNewExpressionExperiments() != null ) ?
                    wn.getNewExpressionExperiments().size() :
                    0;
            int updatedExpressionExperimentCount = ( wn.getUpdatedExpressionExperiments() != null ) ?
                    wn.getUpdatedExpressionExperiments().size() :
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
            int newArrayCount = ( wn.getNewArrayDesigns() != null ) ? wn.getNewArrayDesigns().size() : 0;
            int updatedArrayCount = ( wn.getUpdatedArrayDesigns() != null ) ? wn.getUpdatedArrayDesigns().size() : 0;

            boolean drawNewColumn = ( newExpressionExperimentCount > 0 || newArrayCount > 0 || newAssayCount > 0 );
            boolean drawUpdatedColumn = ( updatedExpressionExperimentCount > 0 || updatedArrayCount > 0 );
            String date = ( wn.getDate() != null ) ?
                    DateFormat.getDateInstance( DateFormat.LONG ).format( wn.getDate() ) :
                    "";
            date = date.replace( '-', ' ' );

            summary.element( "updateDate", date );
            summary.element( "drawNewColumn", drawNewColumn );
            summary.element( "drawUpdatedColumn", drawUpdatedColumn );
            if ( newAssayCount != 0 )
                summary.element( "newBioAssayCount", new Long( newAssayCount ) );
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

        ExpressionExperiment ee = this.getEESafely( id );
        Collection<ExpressionExperimentValueObject> initialResults = expressionExperimentService
                .loadValueObjects( Collections.singleton( ee.getId() ), false );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        getReportData( initialResults );

        ExpressionExperimentValueObject initialResult = initialResults.iterator().next();
        ExpressionExperimentDetailsValueObject finalResult = new ExpressionExperimentDetailsValueObject(
                initialResult );
        // Most of DetailsVO values are set automatically through the constructor.
        // We only need to set the additional values:

        finalResult.setArrayDesigns(
                arrayDesignService.loadValueObjects( EntityUtils.getIds( this.getADsSafely( ee ) ) ) );
        finalResult.setQChtml( getQCTagHTML( ee ) );
        finalResult.setExpressionExperimentSets( this.getExpressionExperimentSets( ee, false ) );

        finalResult = this.setPrefferedAndReprocessed( finalResult, ee );
        finalResult = this.setMutipleTechTypes( finalResult, ee );
        finalResult = this.setParentTaxon( finalResult, initialResult.getTaxonId() );
        // this should be taken care of by the security interceptor. See bug 4373
        // finalResult.setUserCanWrite( securityService.isEditable( ee ) );
        // finalResult.setUserOwned( securityService.isOwnedByCurrentUser( ee ) );
        finalResult = this.setPublicationAndAuthor( finalResult, ee );
        finalResult = this.setBatchInfo( finalResult, ee );

        Date lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( ee );
        if ( lastArrayDesignUpdate != null ) {
            finalResult.setLastArrayDesignUpdateDate( lastArrayDesignUpdate.toString() );
        }

        return finalResult;
    }

    /**
     * Sets batch information and related properties
     */
    private ExpressionExperimentDetailsValueObject setBatchInfo( ExpressionExperimentDetailsValueObject finalResult,
            ExpressionExperiment ee ) {
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

        return finalResult;
    }

    /**
     * populates the publication and author information
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
                log.warn( "Pubmed id not formatted correctly: " + accession );
            }
        }

        return finalResult;
    }

    /**
     * Loads, checks not null, and thaws the array designs the given EE is associated with.
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
     */
    private ExpressionExperimentDetailsValueObject setMutipleTechTypes(
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
     */
    private ExpressionExperimentDetailsValueObject setPrefferedAndReprocessed(
            ExpressionExperimentDetailsValueObject finalResult, ExpressionExperiment ee ) {

        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( ee );

        boolean dataReprocessedFromRaw = false;
        int countPreferred = 0;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                countPreferred++;
            }
            if ( qt.getIsMaskedPreferred() != null && qt.getIsMaskedPreferred() && qt.getIsRecomputedFromRawData() ) {
                dataReprocessedFromRaw = true;
            }
        }

        finalResult.setHasMultiplePreferredQuantitationTypes( countPreferred > 1 );
        finalResult.setReprocessedFromRawData( dataReprocessedFromRaw );

        return finalResult;
    }

    /**
     * Checks and sets parent taxon and related properties
     */
    private ExpressionExperimentDetailsValueObject setParentTaxon( ExpressionExperimentDetailsValueObject finalResult,
            Long taxonId ) {
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
            return new HashSet<>();
        }

        return getFilteredExpressionExperimentValueObjects( null, ids, false, null, true );
    }

    /**
     * AJAX get experiments that used a given platform. Don't retrieve too much detail.
     *
     * @param id of platform
     */
    public Collection<ExpressionExperimentValueObject> loadExperimentsForPlatform( Long id ) {
        return getFilteredExpressionExperimentValueObjects( null,
                EntityUtils.getIds( arrayDesignService.getExpressionExperiments( arrayDesignService.load( id ) ) ),
                false, null, true );
    }

    /**
     * AJAX - for display in tables. Get more details.
     *
     * @param ids of EEs to load
     * @return security-filtered set of value objects.
     */
    public Collection<ExpressionExperimentValueObject> loadDetailedExpressionExperiments( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return new HashSet<>();
        }
        Collection<ExpressionExperimentValueObject> result = getFilteredExpressionExperimentValueObjects( null, ids,
                false, null, true );
        this.expressionExperimentReportService.getReportInformation( result );
        return result;
    }

    /**
     * AJAX; get a collection of experiments that have had samples removed due to outliers (TODO: and experiment that
     * have possible batch effects detected)
     */
    public JsonReaderResponse<JSONObject> loadExpressionExperimentsWithQcIssues() {

        Collection<ExpressionExperiment> outlierEEs = expressionExperimentService.getExperimentsWithOutliers();

        // List<ExpressionExperimentValueObject> batchEffectEEs =
        // expressionExperimentService.getExperimentsWithBatchEffect();
        // List<ExpressionExperimentValueObject> batchEffectEEs = new ArrayList<ExpressionExperimentValueObject>();

        Collection<ExpressionExperiment> ees = new HashSet<>();
        ees.addAll( outlierEEs );
        // ees.addAll( batchEffectEEs );

        List<JSONObject> jsonRecords = new ArrayList<>();

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

        return new JsonReaderResponse<>( jsonRecords );

    }

    /**
     * AJAX - for display in tables
     *
     * @return security-filtered set of value objects.
     */
    public Collection<QuantitationTypeValueObject> loadQuantitationTypes( Long eeid ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeid );
        // need to thaw?
        ee = expressionExperimentService.thawLite( ee );
        Collection<QuantitationType> qts = ee.getQuantitationTypes();

        return QuantitationTypeValueObject.convert2ValueObjects( qts );
    }

    /**
     * AJAX. Data summarizing the status of experiments.
     *
     * @param taxonId    can be null
     * @param limit      If >0, get the most recently updated N experiments, where N <= limit; or if < 0, get the least
     *                   recently updated; if 0, or null, return all.
     * @param filter     if non-null, limit data sets to ones meeting criteria.
     * @param showPublic return user's public datasets too
     */
    public Collection<ExpressionExperimentDetailsValueObject> loadStatusSummaries( Long taxonId, Collection<Long> ids,
            Integer limit, Integer filter, Boolean showPublic ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<ExpressionExperimentValueObject> vos;

        boolean filterDataByUser = false;

        if ( SecurityUtil.isUserAdmin() ) {
            /* proceed, just being transparent */
        } else if ( SecurityUtil.isUserLoggedIn() ) {
            filterDataByUser = true;
        } else {
            /* Anonymous */
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        // limit = 10;
        // default limit to 50, should always be set on front end but it case it wasn't this
        // will keep from loading a ridiculous number of experiments
        if ( limit == null )
            limit = 50;

        vos = getEEVOsForManager( taxonId, ids, filterDataByUser, limit, filter, showPublic );

        if ( vos.isEmpty() ) {
            return new HashSet<>();
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetching basic data took: " + timer.getTime() + "ms" );
        }

        /*
         * Phase I is pretty fast - even over a tunnel, about 10 seconds for 1500 data sets.
         */

        timer.reset();
        timer.start();

        getReportData( vos );

        if ( timer.getTime() > 1000 ) {
            log.info( "Filling in report data for " + vos.size() + " EEs: " + timer.getTime() + "ms" );
        }

        LinkedList<ExpressionExperimentDetailsValueObject> finalVos = new LinkedList<>(  );

        // We need to convert the VOs to detailVos and add array designs so trouble info can be correctly displayed.
        for ( ExpressionExperimentValueObject vo : vos ) {
            ExpressionExperimentDetailsValueObject detailVo= new ExpressionExperimentDetailsValueObject( vo );

            // Detail VO has many more fields but we currently only use the ADs.
            detailVo.setArrayDesigns(
                    //TODO: This is ridiculous - a method that actually loads AD VOs based on an EE id should be implemented
                    arrayDesignService.loadValueObjects(
                            EntityUtils.getIds( this.getADsSafely( this.getEESafely( vo.getId() ) ) ) ) );
            finalVos.add( detailVo );
        }

        return finalVos;
    }

    /**
     * Remove the primary publication for the given expression experiment (by id). The reference is not actually deleted
     * from the system. AJAX
     */
    public String removePrimaryPublication( Long eeId ) throws Exception {
        RemovePubMed task = new RemovePubMed( new TaskCommand( eeId ) );
        return taskRunningService.submitLocalTask( task );
    }

    /**
     * AJAX (used by experimentAndExperimentGroupCombo.js)
     *
     * @param taxonId if the search should not be limited by taxon, pass in null
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
     * @return Collection of expression experiment entity objects
     */
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        return expressionExperimentSearchService.searchExpressionExperiments( query );

    }

    /**
     * Show all experiments (optionally conditioned on either a taxon, a list of ids, or a platform)
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

        addQCInfo( expressionExperiment, mv );
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
        if ( AJAX ) {
            mav.addObject( "bioMaterialIdList", bioMaterialService.getBioMaterialIdList( bioMaterials ) );
        }

        Integer numBioMaterials = bioMaterials.size();
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterialService.thaw( bioMaterials ) );

        addQCInfo( expressionExperiment, mav );

        return mav;
    }

    @RequestMapping({ "/showExpressionExperiment.html", "/", "/show" })
    public ModelAndView showExpressionExperiment( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch timer = new StopWatch();
        timer.start();

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" );
        BioAssaySet expExp = getExpressionExperimentFromRequest( request );

        mav.addObject( "expressionExperiment", expExp );

        mav.addObject( "eeId", expExp.getId() );
        mav.addObject( "eeClass", ExpressionExperiment.class.getName() );

        if ( timer.getTime() > 200 ) {
            log.info( "Show Experiment was slow: id=" + expExp.getId() + " " + timer.getTime() + "ms" );
        }

        return mav;
    }

    /**
     * shows a list of BioAssays for an expression experiment subset
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
     */
    public void unmatchAllBioAssays( Long eeId ) {
        ExpressionExperiment ee = this.expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with id=" + eeId );
        }
        ee = this.expressionExperimentService.thawLite( ee );

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
        if ( StringUtils.isNotBlank( command.getDescription() ) && !command.getDescription()
                .equals( ee.getDescription() ) ) {
            ee.setDescription( command.getDescription() );
        }
        if ( !command.isRemovePrimaryPublication() && StringUtils.isNotBlank( command.getPubMedId() ) ) {
            updatePubMed( entityId, command.getPubMedId() );

        } else if ( command.isRemovePrimaryPublication() ) {
            removePrimaryPublication( entityId );
        }

        log.info( "Updating " + ee );
        expressionExperimentService.update( ee );

        // return runner.getTaskId();
        return loadExpressionExperimentDetails( ee.getId() );
    }

    /**
     * FIXME change name of this to reflect that it updates more than just the correlation matrix.
     */
    @RequestMapping("/refreshCorrMatrix.html")
    public ModelAndView updateCorrelationMatrix( Long id ) {
        // TODO: make this an ajax background job
        updateCorrelationMatrixFile( id );
        updateMV( id );
        return new ModelAndView(
                new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
    }

    /**
     * AJAX. Associate the given pubmedId with the given expression experiment.
     */
    public String updatePubMed( Long eeId, String pubmedId ) throws Exception {
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
        mav.addObject( "hasMeanVariance", meanVarianceService.hasMeanVariance( expressionExperiment ) );

        // FIXME don't store in a file.
        mav.addObject( "hasNodeDegreeDist", ExpressionExperimentQCUtils.hasNodeDegreeDistFile( expressionExperiment ) );

        mav.addObject( "numFactors", ExpressionExperimentQCUtils.numFactors( expressionExperiment ) );
        mav.addObject( "hasCorrDist", true ); // FIXME

        mav.addObject( "numPossibleOutliers", numPossibleOutliers( expressionExperiment ) );
        mav.addObject( "numOutliersRemoved", numOutliersRemoved( expressionExperiment ) );
    }

    /**
     * Filter based on criteria of which events etc. the data sets have.
     */
    private List<ExpressionExperimentValueObject> applyFilter( List<ExpressionExperimentValueObject> eeValObjectCol,
            Integer filter ) {
        List<ExpressionExperimentValueObject> filtered = new ArrayList<>();
        Collection<ExpressionExperiment> eesToKeep = null;
        List<ExpressionExperimentValueObject> eeVOsToKeep = null;

        /*
         * TODO This could be sped up by passing value objects to the auditEventService.
         */
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

    private String batchConfound( ExpressionExperiment ee ) {
        String result = expressionExperimentService.getBatchConfound( ee );
        if ( result == null ) {
            result = "";
        }
        return result;
    }

    private String batchEffect( ExpressionExperiment ee ) {
        BatchEffectDetails batchEffectDetails = expressionExperimentService.getBatchEffect( ee );
        String result = "";
        if ( batchEffectDetails == null ) {
            result = "";
        } else {
            if ( batchEffectDetails.getDataWasBatchCorrected() ) {
                result = "Data has been batch-corrected";
            } else if ( batchEffectDetails.getPvalue() < BATCH_EFFECT_PVALTHRESHOLD ) {
                result = "This data set may have a batch artifact (PC" + ( batchEffectDetails.getComponent() ) + "); p="
                        + String.format( "%.2g", batchEffectDetails.getPvalue() ) + "<br />";
            }
        }
        return result;

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
     * @param limit      - return the N most recently (limit > 0) or least recently updated experiments (limit < 0) or all
     *                   (limit == 0)
     * @param filter     setting
     * @param showPublic return the user's public datasets as well
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
                eeValObjectCol = this
                        .getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser, limitToUse,
                                showPublic );
            } else {
                eeValObjectCol = this
                        .getFilteredExpressionExperimentValueObjects( taxon, ids, filterDataByUser, limitToUse,
                                showPublic );
            }

        } else if ( ids == null || ids.isEmpty() ) {
            // load everything (up to the limit)
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser, limitToUse,
                    showPublic );
        } else {
            eeValObjectCol = this
                    .getFilteredExpressionExperimentValueObjects( null, ids, filterDataByUser, limitToUse, showPublic );
        }

        if ( eeValObjectCol.isEmpty() )
            return eeValObjectCol;

        if ( filter != null && filter > 0 )
            eeValObjectCol = applyFilter( eeValObjectCol, filter );

        if ( eeValObjectCol.isEmpty() )
            return eeValObjectCol;

        return eeValObjectCol;

    }

    /**
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

    private Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets( BioAssaySet ee,
            boolean includeAutoGenerated ) {

        Collection<Long> eeSetIds = expressionExperimentSetService.findIds( ee );

        if ( eeSetIds.isEmpty() ) {
            return new HashSet<>();
        }

        Collection<ExpressionExperimentSetValueObject> dbEEsvos = expressionExperimentSetService
                .loadValueObjects( eeSetIds );
        Collection<ExpressionExperimentSetValueObject> eesvos = new ArrayList<>();

        if ( !includeAutoGenerated ) {
            for ( ExpressionExperimentSetValueObject dbEEsvo : dbEEsvos ) {
                if ( !expressionExperimentSetService.isAutomaticallyGenerated( dbEEsvo.getDescription() ) ) {
                    eesvos.add( dbEEsvo );
                }
            }
        }

        return eesvos;
    }

    /**
     * Get the expression experiment value objects for the expression experiments.
     *
     * @param taxon             can be null
     * @param filterDataForUser if true, then only the data owned by the user are returned (this has no effect if you
     *                          are an administrator)
     * @return Collection<ExpressionExperimentValueObject>
     */
    private List<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects( Taxon taxon,
            Collection<Long> eeIds, boolean filterDataForUser, Integer limit, boolean showPublic ) {

        List<ExpressionExperimentValueObject> valueobjects;

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * FIXME remove troubled? Needs to be optional. For dataset management page, don't.
         */

        // the front end has the brilliant logic of sending in a negative limit to denote sorting in date ascending
        // order
        boolean descending = limit == null || limit > 0;

        if ( filterDataForUser ) {
            try {
                /*
                 * This could be sped up by making value object methods, but because these are not so many, this should
                 * be acceptable.
                 */
                List<ExpressionExperiment> ees = showPublic ?
                        new ArrayList<>( expressionExperimentService.loadUserOwnedExpressionExperiments() ) :
                        new ArrayList<>( expressionExperimentService.loadMySharedExpressionExperiments() );

                Collection<Long> ownedOrShared = EntityUtils.getIds( ees );

                valueobjects = loadInitialSetOfValueObjects( ownedOrShared, taxon, descending );
            } catch ( AccessDeniedException e ) {
                return new ArrayList<>();
            }
        } else {
            valueobjects = loadInitialSetOfValueObjects( eeIds, taxon, descending );
        }

        // Hide public data sets if desired.
        if ( !valueobjects.isEmpty() && !showPublic ) {
            Collection<ExpressionExperimentValueObject> publicEEs = securityService.choosePublic( valueobjects );
            valueobjects.removeAll( publicEEs );
        }

        // Finally, trim the list.
        if ( limit != null ) {
            valueobjects = getSubList( Math.abs( limit ), valueobjects );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( valueobjects.size() + " EEs in " + timer.getTime() + "ms" );
        }

        return valueobjects;
    }

    /**
     * Updates the value objects with event information and summaries
     *
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

    private List<ExpressionExperimentValueObject> getSubList( Integer limit,
            List<ExpressionExperimentValueObject> initialListOfValueObject ) {

        if ( limit < initialListOfValueObject.size() ) {
            initialListOfValueObject = initialListOfValueObject.subList( 0, limit );
        }
        return initialListOfValueObject;
    }

    private List<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( ListBatchCommand batch,
            Collection<Long> ids, Taxon taxon ) {
        List<ExpressionExperimentValueObject> records;

        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();
            boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );

            String orderBy = "name"; // default ordering
            switch ( o ) {
                case "shortName":
                    orderBy = "shortName";
                    break;
                case "name":
                    orderBy = "name";
                    break;
                case "bioAssayCount":
                    orderBy = "bioAssayCount";
                    break;
                case "taxon":
                    orderBy = "taxon";
                    break;
                case "troubled":
                    orderBy = "troubled";
                    break;
                case "lastUpdated":
                case "modDate":
                    orderBy = "lastUpdated";
                    descending = !descending;
                    break;
                default:
                    log.error( "Tried to sort experiments by unknown sort field: " + o + ". Sorting by default: "
                            + orderBy );
                    break;
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
                records = new ArrayList<>( expressionExperimentService.loadValueObjects( ids, true ) );
            } else if ( taxon != null ) {
                records = expressionExperimentService.loadAllValueObjectsTaxon( taxon );
            } else {
                records = new ArrayList<>( expressionExperimentService.loadAllValueObjects() );
            }
        }

        assert records != null;

        log.info( "Returning " + records.size() );

        return records;
    }

    /**
     * This is security filtered.
     *
     * @param descending - if eeIds != null, descending param is ignored and will follow the order of eeIds
     */
    private List<ExpressionExperimentValueObject> loadInitialSetOfValueObjects( Collection<Long> eeIds, Taxon taxon,
            boolean descending ) {
        List<ExpressionExperimentValueObject> initialListOfValueObject;
        if ( eeIds != null && !eeIds.isEmpty() ) {

            // only for selected IDs
            initialListOfValueObject = ( List<ExpressionExperimentValueObject> ) expressionExperimentService
                    .loadValueObjects( eeIds, true );
            if ( taxon != null ) {
                // AND filter for taxon
                for ( Iterator<ExpressionExperimentValueObject> it = initialListOfValueObject.iterator(); it
                        .hasNext(); ) {
                    ExpressionExperimentValueObject evo = it.next();
                    if ( !evo.getTaxonId().equals( taxon.getId() ) ) {
                        it.remove();
                    }
                }
            }

        } else if ( taxon != null ) {
            // everything for taxon
            initialListOfValueObject = new ArrayList<>( expressionExperimentService
                    .loadAllValueObjectsTaxonOrdered( "dateLastUpdated", descending, taxon ) );
        } else {
            // everything
            initialListOfValueObject = new ArrayList<>(
                    expressionExperimentService.loadAllValueObjectsOrdered( "lastUpdated", descending ) );
        }
        return initialListOfValueObject;
    }

    private List<ExpressionExperimentDetailsValueObject> removeTroubledExperimentVOs(
            List<ExpressionExperimentDetailsValueObject> records ) {
        List<ExpressionExperimentDetailsValueObject> untroubled = new ArrayList<>( records );

        Collection<ExpressionExperimentDetailsValueObject> toRemove = new ArrayList<>();
        for ( ExpressionExperimentDetailsValueObject record : records ) {
            if ( record.getArrayDesigns() == null ) {
                // Loading array designs which we need for the parent trouble check.
                Collection<ArrayDesign> ads = expressionExperimentService
                        .getArrayDesignsUsed( expressionExperimentService.load( record.getId() ) );
                LinkedList<Long> adIds = new LinkedList<>();
                for ( ArrayDesign ad : ads ) {
                    adIds.add( ad.getId() );
                }
                record.setArrayDesigns( arrayDesignService.loadValueObjects( adIds ) );
            }

            if ( record.getTroubled() ) {
                toRemove.add( record );
            }

        }
        untroubled.removeAll( toRemove );
        return untroubled;
    }

    /**
     * Read the troubled flag in each ExpressionExperimentValueObject and return only those object for which it is true
     */
    private List<ExpressionExperimentValueObject> returnTroubled( Collection<ExpressionExperimentValueObject> ees ) {
        List<ExpressionExperimentValueObject> troubled = new ArrayList<>();

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
     */
    private void updateCorrelationMatrixFile( Long id ) {
        ExpressionExperiment expressionExperiment;
        expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "Unable to access experiment with id=" + id );
        }
        sampleCoexpressionMatrixService.create( expressionExperiment, true );
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
            ExpressionExperiment ee = expressionExperimentService.load( taskCommand.getEntityId() );
            expressionExperimentService.delete( ee );

            return new TaskResult( taskCommand, new ModelAndView(
                    new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) )
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

            log.info( "Removing reference" );
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

                log.info( "Reference exists in system, associating..." );
                expressionExperiment.setPrimaryPublication( publication );
                expressionExperimentService.update( expressionExperiment );
            } else {
                log.info( "Searching pubmed on line .." );

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
            result.setPrimaryCitation( CitationValueObject
                    .convert2CitationValueObject( bibliographicReferenceService.thaw( publication ) ) );
            return new TaskResult( taskCommand, result );
        }

    }

}