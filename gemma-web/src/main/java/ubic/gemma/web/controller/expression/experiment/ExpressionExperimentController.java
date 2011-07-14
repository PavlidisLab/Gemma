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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.analysis.preprocess.batcheffects.BatchConfoundValueObject;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.preprocess.svd.SVDService;
import ubic.gemma.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.Reference;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
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
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
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
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.tasks.analysis.expression.UpdateEEDetailsCommand;
import ubic.gemma.tasks.analysis.expression.UpdatePubMedCommand;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.common.QuantitationTypeValueObject;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.session.SessionListManager;
import ubic.gemma.web.taglib.displaytag.ExpressionExperimentValueObjectComparator;
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
            ExpressionExperiment ee = expressionExperimentService.load( command.getEntityId() );
            expressionExperimentService.delete( ee );

            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) ).addObject( "message",
                    "Dataset " + ee.getShortName() + " removed from Database" ) );

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
            result.setPrimaryCitation( formatCitation( expressionExperiment.getPrimaryPublication() ) );
            return new TaskResult( command, result );
        }

    }

    private static final Boolean AJAX = true;

    private static final int TRIM_SIZE = 800;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditableUtil auditableUtil;

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
    private ExpressionExperimentSetService expressionExperimentSetService = null;

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

    @Autowired
    ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private SessionListManager sessionListManager;

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

    private static final Double BATCH_EFFECT_PVALTHRESHOLD = 0.01;

    private static final int MAX_COMBO_PROMT_GROUP_SIZE = 100;

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
                .expressionExperimentSearch( searchString ) );

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
        for ( SearchResult ee : searchResults ) {
            list += ee.getId() + ",";
        }

        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + list ) ).addObject( "message", "Search Criteria: " + searchString + "; " + searchResults.size()
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
    public Collection<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {

        boolean taxonLimited = ( taxonId != null ) ? true : false;

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();
        List<SearchResultDisplayObject> usersResults = new LinkedList<SearchResultDisplayObject>();
        List<SearchResultDisplayObject> publicResults = new LinkedList<SearchResultDisplayObject>();
        // List<SearchResultDisplayObject> autoGenResults = new LinkedList<SearchResultDisplayObject>();

        // if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent
        // session-bound sets (not autogen sets until handling of large searches is fixed)
        if ( query.equals( "" ) ) {

            // get authenticated user's sets
            Collection<ExpressionExperimentSet> userExperimentSets = new ArrayList<ExpressionExperimentSet>();
            if ( SecurityService.isUserLoggedIn() ) {
                userExperimentSets = expressionExperimentSetService.loadMySets();
                SearchResultDisplayObject newSRDO = null;
                for ( ExpressionExperimentSet registeredUserSet : userExperimentSets ) {

                    expressionExperimentSetService.thaw( registeredUserSet );
                    taxonService.thaw( registeredUserSet.getTaxon() );
                    if ( !taxonLimited || registeredUserSet.getTaxon().getId().equals( taxonId ) ) {
                        newSRDO = new SearchResultDisplayObject( registeredUserSet );

                        // if set was automatically generated, don't label as user-created (technically was created by
                        // admin user)
                        if ( newSRDO.getName().indexOf( "All" ) != 0 ) {
                            newSRDO.setType( "usersExperimentSet" );
                            usersResults.add( newSRDO );
                        } else {
                            // autoGenResults.add( newSRDO );
                        }
                    }
                }
            }

            // FOR TESTING UNTIL SCALING ISSUES ARE WORKED OUT
            // propmt with all public groups, just limit by size for now
            Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.loadAllMultiExperimentSets(); // filtered
                                                                                                                    // by
                                                                                                                    // security.
            sets.removeAll( userExperimentSets );
            for ( ExpressionExperimentSet set : sets ) {
                if ( set.getExperiments().size() < MAX_COMBO_PROMT_GROUP_SIZE ) {
                    expressionExperimentSetService.thaw( set );
                    if ( !taxonLimited || set.getTaxon().getId().equals( taxonId ) ) {
                        if ( set.getName().indexOf( "All" ) == 0 ) {
                            /*
                             * remove auto-generated sets until scaling issues are resolved (should have all or none)
                             */
                            // autoGenResults.add( new SearchResultDisplayObject( set ) );
                        } else {
                            publicResults.add( new SearchResultDisplayObject( set ) );
                        }
                    }
                }
            }
            // end of section to be used until scaling issues are resolved

            /*
             * USE THIS CODE WHEN SCALING ISSUES ARE RESOLVED
             * 
             * // get auto generated sets // NOTE: assumption made here that total number of groups is small // If this
             * changes, may want to use searching instead of filtering // search would be for all lists where
             * 'modifiable = false' (?)
             * 
             * Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.loadAllMultiExperimentSets();
             * // filtered by security. List<SearchResultDisplayObject> autoGenSets = new
             * ArrayList<SearchResultDisplayObject>(); for ( ExpressionExperimentSet set : sets) {
             * if(set.getName().indexOf( "All" ) == 0 ){ expressionExperimentSetService.thaw( set ); if ( !taxonLimited
             * || set.getTaxon().getId().equals( taxonId ) ) { autoGenSets.add( new SearchResultDisplayObject( set ) );
             * } } }
             */
            // get any session-bound groups
            Collection<ExpressionExperimentSetValueObject> sessionResult = sessionListManager
                    .getModifiedExperimentSets();

            List<SearchResultDisplayObject> sessionSets = new ArrayList<SearchResultDisplayObject>();

            if ( sessionResult != null && sessionResult.size() > 0 ) {
                Collection<ExpressionExperimentSetValueObject> toRmv = new ArrayList<ExpressionExperimentSetValueObject>();
                // for every object passed in, create a SearchResultDisplayObject
                for ( ExpressionExperimentSetValueObject eevo : sessionResult ) {
                    if ( eevo.getTaxonId() == null ) {
                        toRmv.add( eevo );
                    } else {
                        if ( !taxonLimited || eevo.getTaxonId().equals( taxonId ) ) {
                            Reference ref = eevo.getReference();
                            if ( ref == null ) {
                                ref = new Reference( eevo.getId(), Reference.SESSION_BOUND_GROUP );
                            }
                            SearchResultDisplayObject srdo = new SearchResultDisplayObject(
                                    ExpressionExperimentSet.class, ref, eevo.getName(), eevo.getDescription(), true,
                                    eevo.getExpressionExperimentIds().size(), eevo.getTaxonId(), eevo.getTaxonName(),
                                    "userexperimentSetSession", eevo.getExpressionExperimentIds() );
                            sessionSets.add( srdo );
                        }
                    }
                }
                sessionResult.removeAll( toRmv );
            }
            

            // keep sets in proper order (user's groups first, then public ones)
            Collections.sort( sessionSets );
            displayResults.addAll( sessionSets );

            Collections.sort( usersResults );
            displayResults.addAll( usersResults );
            /*
             * autoGenResults.addAll( autoGenSets ); Collections.sort( autoGenResults );
             */// displayResults.addAll( autoGenResults );

            Collections.sort( publicResults );
            displayResults.addAll( publicResults );

            return displayResults;

        } else {// end of query = ''

            // if query is not blank...
            /*
             * GET EXPERIMENTS AND SETS
             */
            SearchSettings settings = SearchSettings.expressionExperimentSearch( query );
            settings.setGeneralSearch( true ); // add a general search
            settings.setSearchExperimentSets( true ); // add searching for experimentSets
            Taxon taxonParam = null;
            if ( taxonLimited ) {
                taxonParam = taxonService.load( taxonId );
                settings.setTaxon( taxonParam );
            }
            Map<Class<?>, List<SearchResult>> results = searchService.search( settings );

            List<SearchResult> eesSR = results.get( ExpressionExperimentSet.class );
            // prepare taxon property for being read
            for ( SearchResult sr : eesSR ) {
                ExpressionExperimentSet ees = ( ExpressionExperimentSet ) sr.getResultObject();
                expressionExperimentSetService.thaw( ees );
            }
            Collection<SearchResultDisplayObject> experiments = SearchResultDisplayObject
                    .convertSearchResults2SearchResultDisplayObjects( results.get( ExpressionExperiment.class ) );
            Collection<SearchResultDisplayObject> experimentSets = SearchResultDisplayObject
                    .convertSearchResults2SearchResultDisplayObjects( eesSR );

            // when searching for an experiment by short name, one or more experiment set(s) is(are) also returned
            // ex: searching 'GSE2178' gets the experiment and a group called GSE2178 with 1 member
            // to fix this, if 1 ee is returned and the group only has 1 member and it's member has the
            // same Id as the 1 ee returned, then don't return the ee set
            if ( experiments.size() == 1 && experimentSets.size() > 0 ) {
                Long eid = experiments.iterator().next().getReference().getId();
                Collection<SearchResultDisplayObject> toRmv = new ArrayList<SearchResultDisplayObject>();
                for ( SearchResultDisplayObject srdo : experimentSets ) {
                    if ( srdo.getMemberIds().size() == 1 && ( srdo.getMemberIds().toArray() )[0].equals( eid ) ) {
                        toRmv.add( srdo );
                    }
                }
                experimentSets.removeAll( toRmv );
            }

            Taxon taxon = null;
            // for each experiment search result display object, set the taxon -- pretty hacky
            Collection<SearchResultDisplayObject> toRmv = new ArrayList<SearchResultDisplayObject>();
            for ( SearchResultDisplayObject srdo : experiments ) {
                if ( taxonLimited ) {
                    taxon = taxonParam;
                } else {
                    taxon = expressionExperimentService.getTaxon( srdo.getReference().getId() );
                }
                if ( taxon == null ) {
                    log.warn( "Experiment had null taxon, was excluded from results: experiment id="
                            + srdo.getReference().getId() + " shortname=" + srdo.getName() );
                    toRmv.add( srdo );
                } else {
                    srdo.setTaxonId( taxon.getId() );
                    srdo.setTaxonName( taxon.getCommonName() );
                }
            }
            experiments.removeAll( toRmv );

            // if an eeSet is owned by the user, mark it as such (used for giving it a special background colour in
            // search results)
            // TODO make a db call so you can just test each experiment by ID to see if the owner is the current user
            // (avoids loading all user's experiments)
            // probably not high priority fix b/c users won't tend to have many sets
            ArrayList<Long> userSetsIds = new ArrayList<Long>();
            // get ids of user's sets
            if ( SecurityService.isUserLoggedIn() && experimentSets.size() > 0 ) {
                Collection<ExpressionExperimentSet> myEEsets = expressionExperimentSetService.loadMySets();
                for ( ExpressionExperimentSet myEESet : myEEsets ) {
                    userSetsIds.add( myEESet.getId() );
                }
                // tag search result display objects appropriately
                for ( SearchResultDisplayObject srdo : experimentSets ) {
                    // if set was automatically generated, don't label as user-created (technically was created by admin
                    // user)
                    if ( userSetsIds.contains( srdo.getReference().getId() )
                            && srdo.getDescription().indexOf( "Automatically generated" ) < 0 ) {
                        srdo.setType( "usersExperimentSet" );
                    }
                }
            }

            /*
             * ALL RESULTS BY TAXON GROUPS
             */

            // if >1 result, add a group whose members are all experiments returned from search
            if ( ( experiments.size() + experimentSets.size() ) > 1 ) {

                // if an experiment was returned by both experiment and experiment set search, don't count it twice
                // (managed by set)
                HashSet<Long> eeIds = new HashSet<Long>();
                HashMap<Long, HashSet<Long>> eeIdsByTaxonId = new HashMap<Long, HashSet<Long>>();

                // add every individual experiment to the set
                for ( SearchResultDisplayObject srdo : experiments ) {
                    if ( !eeIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                        eeIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
                    }
                    eeIdsByTaxonId.get( srdo.getTaxonId() ).add( srdo.getReference().getId() );

                    eeIds.add( srdo.getReference().getId() );
                }

                // if there's a group, get the number of members
                if ( experimentSets.size() > 0 ) {
                    // for each group
                    for ( SearchResult eesSRO : eesSR ) {
                        // get the ids of the experiment members
                        Iterator<BioAssaySet> iter = ( ( ExpressionExperimentSet ) eesSRO.getResultObject() )
                                .getExperiments().iterator();
                        Long id = null;
                        while ( iter.hasNext() ) {
                            id = iter.next().getId();
                            eeIds.add( id );
                            // add experiment set members to the hashmap
                            taxon = expressionExperimentService.getTaxon( id );
                            if ( !eeIdsByTaxonId.containsKey( taxon.getId() ) ) {
                                eeIdsByTaxonId.put( taxon.getId(), new HashSet<Long>() );
                            }
                            eeIdsByTaxonId.get( taxon.getId() ).add( id );
                        }
                    }
                }

                // make an entry for each taxon

                Long taxonId2 = null;
                for ( Map.Entry<Long, HashSet<Long>> entry : eeIdsByTaxonId.entrySet() ) {
                    taxonId2 = entry.getKey();
                    taxon = taxonService.load( taxonId2 );
                    Reference ref = new Reference( null, Reference.UNMODIFIED_SESSION_BOUND_GROUP );
                    if ( taxon != null && entry.getValue().size() > 0 ) {
                        displayResults.add( new SearchResultDisplayObject( ExpressionExperimentSet.class, ref, "All "
                                + taxon.getCommonName() + " results for '" + query + "'", "All "
                                + taxon.getCommonName() + " experiments found for your query", true, entry.getValue()
                                .size(), taxon.getId(), taxon.getCommonName(), "freeText", entry.getValue() ) );
                    }
                }
            }

            displayResults.addAll( experimentSets );
            displayResults.addAll( experiments );

            if ( displayResults.isEmpty() ) {
                log.info( "No results for search: " + query );
            } else {
                log.info( "Results for search: " + query + " size=" + displayResults.size() + " entry0: "
                        + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getName() + " id:"
                        + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getReference().getId() );
            }
            return displayResults;
        }
    }

    /**
     * AJAX (used by ExperimentCombo.js)
     * 
     * @param query
     * @return Collection of expression experiment entity objects
     */
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        SearchSettings settings = SearchSettings.expressionExperimentSearch( query );
        List<SearchResult> experimentSearchResults = searchService.search( settings ).get( ExpressionExperiment.class );

        if ( experimentSearchResults == null || experimentSearchResults.isEmpty() ) {
            log.info( "No experiments for search: " + query );
            return new HashSet<ExpressionExperimentValueObject>();
        }

        log.info( "Experiment search: " + query + ", " + experimentSearchResults.size() + " found" );
        Collection<ExpressionExperimentValueObject> experimentValueObjects = ExpressionExperimentValueObject
                .convert2ValueObjects( expressionExperimentService.loadMultiple( EntityUtils
                        .getIds( experimentSearchResults ) ) );
        log.info( "Experiment search: " + experimentValueObjects.size() + " value objects returned." );
        return experimentValueObjects;
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
        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );
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
        int MAX_TAGS_TO_SHOW = 10;
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

        ee = expressionExperimentService.thawLite( ee );

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
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
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
        Taxon taxon = taxonService.load( initialResult.getTaxonId() );
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

        Collection<ExpressionExperimentValueObject> finalResultc = new HashSet<ExpressionExperimentValueObject>();
        finalResultc.add( finalResult );

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
        
        finalResult.setQChtml(getQCTagHTML(ee));

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
        

        AuditEvent lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( ee, null );
        if(lastArrayDesignUpdate != null){
            finalResult.setLastArrayDesignUpdateDate( lastArrayDesignUpdate.getDate().toString() );
        }
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
        qc.setHasCorrMat( ExpressionExperimentQCUtils.hasCorrMatFile( ee ) );
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
                false );
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

        if ( SecurityService.isUserAdmin() ) {
            /* proceed, just being transparent */
        } else if ( SecurityService.isUserLoggedIn() ) {
            filterDataByUser = true;
        } else {
            /* Anonymous */
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        // limit = 10;

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

        Long numExpressionExperiments = Long.valueOf( expressionExperiments.size() );

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

        ExpressionExperiment expExp = getExpressionExperimentFromRequest( request );

        /*
         * This is only _really_ needed to get hasBatchInformation; we can get quantitation types by a service method.
         * So if this is slow, we can supply a query for the batch information.
         */
        expExp = expressionExperimentService.thawLite( expExp );

        ModelAndView mav = new ModelAndView( "expressionExperiment.detail" );

        mav.addObject( "expressionExperiment", expExp );

        mav.addObject( "characteristics", expExp.getCharacteristics() );

        Collection<QuantitationType> quantitationTypes = expExp.getQuantitationTypes();
        mav.addObject( "quantitationTypes", quantitationTypes );
        mav.addObject( "qtCount", quantitationTypes.size() );

        /*
         * Check for multiple "preferred" qts.
         */
        int countPreferred = 0;
        for ( QuantitationType qt : quantitationTypes ) {
            if ( qt.getIsPreferred() ) {
                countPreferred++;
            }
        }
        mav.addObject( "hasMoreThanOnePreferredQt", countPreferred > 0 );

        AuditEvent lastArrayDesignUpdate = expressionExperimentService.getLastArrayDesignUpdate( expExp, null );
        mav.addObject( "lastArrayDesignUpdate", lastArrayDesignUpdate );

        mav.addObject( "eeId", expExp.getId() );
        mav.addObject( "eeClass", ExpressionExperiment.class.getName() );

        boolean hasBatchInformation = false;
        for ( ExperimentalFactor ef : expExp.getExperimentalDesign().getExperimentalFactors() ) {
            if ( BatchInfoPopulationService.isBatchFactor( ef ) ) {
                hasBatchInformation = true;
                break;
            }
        }

        mav.addObject( "hasBatchInformation", hasBatchInformation );
        if ( hasBatchInformation ) {
            mav.addObject( "batchConfound", this.batchConfound( expExp ) );
            mav.addObject( "batchArtifact", this.batchEffect( expExp ) );
        }

        addQCInfo( expExp, mav );

        boolean isPrivate = securityService.isPrivate( expExp );
        mav.addObject( "isPrivate", isPrivate );

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
     */
    public ExpressionExperimentDetailsValueObject updateBasics( UpdateEEDetailsCommand command ) {
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
        mav.addObject( "hasCorrMat", ExpressionExperimentQCUtils.hasCorrMatFile( expressionExperiment ) );
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
    private Collection<ExpressionExperimentValueObject> applyFilter(
            Collection<ExpressionExperimentValueObject> eeValObjectCol, Integer filter ) {
        Collection<ExpressionExperimentValueObject> filtered = new HashSet<ExpressionExperimentValueObject>();
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

        Collection<BatchConfoundValueObject> confounds = BatchConfound.test( ee );

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
     * @param citation
     * @return
     */
    private String formatCitation( BibliographicReference citation ) {
        StringBuilder buf = new StringBuilder();

        if ( citation.getAuthorList() != null ) {
            String[] authors = StringUtils.split( citation.getAuthorList(), ";" );
            // if there are multiple authors, only display the first author
            if ( authors.length == 1 ) {
                buf.append( authors[0] + " " );
            } else if ( authors.length > 0 ) {
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
     * @param limit - return the N most recently (limit > 0) or least recently updated experiments (limit < 0) or all
     *        (limit == 0)
     * @param filter setting
     * @return
     */
    private Collection<ExpressionExperimentValueObject> getEEVOsForManager( Long taxonId, Collection<Long> ids,
            boolean filterDataByUser, Integer limit, Integer filter ) {
        Collection<ExpressionExperimentValueObject> eeValObjectCol;
        if ( taxonId != null ) {
            Taxon taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
            if ( ids == null || ids.isEmpty() ) {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, null, filterDataByUser );
            } else {
                eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( taxon, ids, filterDataByUser );
            }

        } else if ( ids == null || ids.isEmpty() ) {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, null, filterDataByUser );
        } else {
            eeValObjectCol = this.getFilteredExpressionExperimentValueObjects( null, ids, filterDataByUser );
        }

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( filter != null && filter > 0 ) eeValObjectCol = applyFilter( eeValObjectCol, filter );

        if ( eeValObjectCol.isEmpty() ) return eeValObjectCol;

        if ( limit != null && limit != 0 ) {
            Collection<Long> idsOfFetched = EntityUtils.getIds( eeValObjectCol );
            Map<ExpressionExperiment, Date> filteredByLimit = this.expressionExperimentService.findByUpdatedLimit(
                    idsOfFetched, limit );

            Map<Long, Date> filterdByLimitIdMap = new HashMap<Long, Date>();
            for ( ExpressionExperiment e : filteredByLimit.keySet() ) {
                filterdByLimitIdMap.put( e.getId(), filteredByLimit.get( e ) );
            }

            for ( Iterator<ExpressionExperimentValueObject> it = eeValObjectCol.iterator(); it.hasNext(); ) {
                ExpressionExperimentValueObject obj = it.next();
                Long id = obj.getId();
                if ( !filterdByLimitIdMap.containsKey( id ) ) {
                    it.remove();
                } else {
                    /*
                     * This is where the 'last update date' gets filled in.
                     */
                    obj.setDateLastUpdated( filterdByLimitIdMap.get( id ) );
                }

            }
        }

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
        Collection<Long> ids = EntityUtils.getIds( securedEEs );

        Collection<ExpressionExperimentValueObject> valueObjs = expressionExperimentService.loadValueObjects( ids );

        if ( SecurityService.isUserAdmin() ) {
            for ( ExpressionExperimentValueObject vo : valueObjs ) {
                vo.setCurrentUserHasWritePermission( true );
            }
        } else if ( SecurityService.isUserLoggedIn() ) {
            Map<Long, Boolean> canEdit = new HashMap<Long, Boolean>();
            for ( ExpressionExperiment ee : securedEEs ) {
                canEdit.put( ee.getId(), securityService.isEditable( ee ) );
            }
            for ( ExpressionExperimentValueObject vo : valueObjs ) {
                if ( !canEdit.containsKey( vo.getId() ) ) continue;
                vo.setCurrentUserHasWritePermission( canEdit.get( vo.getId() ) );
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Value objects for " + securedEEs.size() + " in " + timer.getTime() + "ms" );
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

                securedEEs = expressionExperimentService.loadMySharedExpressionExperiments();

                if ( eeIds != null ) {
                    Collection<ExpressionExperiment> securedEEsfilteredByEeIds = new ArrayList<ExpressionExperiment>();

                    // only keep ExpressionExperiments that have ids contained in eeIds
                    for ( ExpressionExperiment ee : securedEEs ) {

                        if ( eeIds != null && eeIds.contains( ee.getId() ) ) {
                            securedEEsfilteredByEeIds.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByEeIds;

                }

                if ( taxon != null ) {

                    Collection<ExpressionExperiment> securedEEsfilteredByTaxon = new ArrayList<ExpressionExperiment>();

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
                return new HashSet<ExpressionExperimentValueObject>();
            }
        } else {
            if ( taxon != null ) {
                if ( eeIds == null ) {
                    securedEEs = expressionExperimentService.findByTaxon( taxon );
                } else {
                    securedEEs = expressionExperimentService.loadMultiple( eeIds );
                    Collection<ExpressionExperiment> securedEEsfilteredByTaxon = new ArrayList<ExpressionExperiment>();

                    for ( ExpressionExperiment ee : securedEEs ) {

                        Taxon t = expressionExperimentService.getTaxon( ee.getId() );

                        if ( t != null && t.getId().equals( taxon.getId() ) ) {
                            securedEEsfilteredByTaxon.add( ee );
                        }
                    }

                    securedEEs = securedEEsfilteredByTaxon;
                }

            } else if ( eeIds == null ) {
                securedEEs = expressionExperimentService.loadAll();
            } else {
                securedEEs = expressionExperimentService.loadMultiple( eeIds );
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( securedEEs.size() + " EEs in " + timer.getTime() + "ms" );
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

        // FIXME Duplicates code in ProcessedExpressionDataVectorCreateTask
        Collection<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorService
                .getProcessedDataVectors( expressionExperiment );
        if ( vectors.isEmpty() ) {
            vectors = processedExpressionDataVectorCreateService.computeProcessedExpressionData( expressionExperiment );
        }
        FilterConfig fconfig = new FilterConfig();
        fconfig.setIgnoreMinimumRowsThreshold( true );
        fconfig.setIgnoreMinimumSampleThreshold( true );
        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( expressionExperiment,
                fconfig, vectors );
        ExpressionDataSampleCorrelation.process( datamatrix, expressionExperiment );
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
            mav.addObject( "text", "Could not find genes to match expression experiment ids: {" + eeIds
                    + "} or expression experiment set ids {" + eeSetIds + "}" );
            return mav;
        }
        Collection<ExpressionExperiment> ees = new ArrayList<ExpressionExperiment>();
        for ( Long id : eeIds ) {
            ees.add( expressionExperimentService.load( id ) );
        }
        for ( Long id : eeSetIds ) {
            for ( BioAssaySet ee : expressionExperimentSetService.load( id ).getExperiments() ) {
                ees.addAll( ee.getBioAssays() );
            }
        }

        mav.addObject( "text", format4File( ees, eeSetName ) );
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
    private String format4File( Collection<ExpressionExperiment> bas, String eeSetName ) {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append( "# Generated by Gemma\n# " + ( new Date() ) + "\n" );
        strBuff.append( ExpressionDataFileService.DISCLAIMER + "#\n" );

        if ( eeSetName != null && eeSetName.length() != 0 ) strBuff.append( "# Experiment Set: " + eeSetName + "\n" );
        strBuff.append( "# " + bas.size() + ( ( bas.size() > 1 ) ? " experiments" : " experiment" ) + "\n#\n" );

        // add header
        strBuff.append( "Short Name\tFull Name\n" );
        for ( ExpressionExperiment ee : bas ) {
            if ( ee != null ) {
                strBuff.append( ee.getShortName() + "\t" + ee.getName() );
                strBuff.append( "\n" );
            }
        }

        return strBuff.toString();
    }

}