/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;

/**
 * @author luke
 * @version $Id$
 */
@Controller
public class CoexpressionSearchController {

    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private static final int DEFAULT_STRINGENCY = 2;

    private static final int DEFAULT_MAX_GENES_PER_QUERY = 20;
    
    private static final int DEFAULT_MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY = 200;
    
    private static final int MAX_GENES_PER_QUERY = ConfigUtils.getInt( "gemma.coexpressionSearch.maxGenesPerQuery", DEFAULT_MAX_GENES_PER_QUERY );
    
    private static final int MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY = ConfigUtils.getInt( "gemma.coexpressionSearch.maxGenesPerCoexVisQuery", DEFAULT_MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY );

    private static final int DEFAULT_MAX_RESULTS = 200;
    
    private static final int MAX_RESULTS = ConfigUtils.getInt( "gemma.coexpressionSearch.maxResultsPerQueryGene", DEFAULT_MAX_RESULTS );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneCoexpressionService geneCoexpressionService;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private SearchService searchService = null;

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doQuickSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();

        if ( searchOptions.getGeneIds().size() != 1 ) {
            result.setErrorState( "Too many genes selected, please limit searches to one" );
            return result;
        }

        Gene gene = geneService.load( searchOptions.getGeneIds().iterator().next() );

        if ( gene == null ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;

        }

        log.info( "Coexpression search: " + searchOptions );

        gene = this.geneService.thaw( gene ); // need to thaw externalDB in taxon for marshling back to client...

        Long eeSetId = getEESet( searchOptions, gene );

        if ( eeSetId == null ) {
            result.setErrorState( "No coexpression results available" );
            log.info( "No expression experiment set results for query: " + searchOptions );
            return result;
        }

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( gene );
        result.setQueryGenes( GeneValueObject.convert2ValueObjects( genes ) );

        Collection<CoexpressionValueObjectExt> geneResults = geneCoexpressionService.coexpressionSearchQuick( eeSetId,
                genes, 2, MAX_RESULTS, false, false );
        result.setKnownGeneResults( geneResults );

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result.setErrorState( "Sorry, No genes are currently coexpressed under the selected search conditions " );
            log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearchQuick2( CoexpressionSearchCommand searchOptions ) {
        return doSearchQuick2( searchOptions, null );
    }

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearchQuick2( CoexpressionSearchCommand searchOptions,
            Collection<Long> queryGeneIds ) {
        //queryGeneIds should only be sent in on the 'my genes only' search for a cytoscape graph vis call
        //queryGeneIds is used to trim the graph appropriately when it gets too big so that not too much data is sent back to the browser

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        
        restrictSearchOptionsQueryGenes(searchOptions, queryGeneIds);
        
        Collection<Gene> genes = geneService.loadThawed( searchOptions.getGeneIds() );

        if ( genes == null || genes.isEmpty() ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;

        }
        
        boolean skipCoexpressionDetails = false;
        
        log.info( "Coexpression search: " + searchOptions );
        if (queryGeneIds!=null){
            log.info( "This is a 'my genes only' Coexpression viz search original query gene ids: " + queryGeneIds );
            //we don't need to populate all the coex details for the viz, so set skip flag to false
            skipCoexpressionDetails = true;
        }

        result.setQueryGenes( GeneValueObject.convert2ValueObjects( genes ) );
        
        //maxResults set to zero indicates no limit.  maxResults is ignored in 'my genes only' query
        Collection<CoexpressionValueObjectExt> geneResults = geneCoexpressionService.coexpressionSearchQuick(
                searchOptions.getEeIds(), genes, searchOptions.getStringency(), MAX_RESULTS,
                searchOptions.getQueryGenesOnly(), skipCoexpressionDetails );        
        
        log.info( "Returned from coexpression search: " + searchOptions );
        
        result.setKnownGeneResults( geneResults );
        
        //if this is not a cytoscape coex vis query, then get 'query-genes-only' results for the query genes (only do this if there is more than one query gene)
        if (queryGeneIds == null && searchOptions.getGeneIds().size() > 1){
            log.info( "Coexpression search step 2: getting 'query genes only' results for genes: " + genes );
            Collection<CoexpressionValueObjectExt> queryGenesOnlyResults = geneCoexpressionService.coexpressionSearchQuick(
                    searchOptions.getEeIds(), genes, 2, MAX_RESULTS,
                    true, skipCoexpressionDetails );
            
            queryGenesOnlyResults = eliminateDuplicates(queryGenesOnlyResults);
            
            result.setQueryGenesOnlyResults(queryGenesOnlyResults);
            
        }

        int stringencyTrimLimit = searchOptions.getEeIds().size();
           
        int resultsLimit = ConfigUtils.getInt( "gemma.cytoscapeweb.maxEdges", 850 );
        
        // strip down results for front end if data is too large (only happens when queryGeneIds is sent in as a parameter, i.e. cytoscape coex vis query
        if ( geneResults.size() > resultsLimit && queryGeneIds != null ) {            
            trimGraphForFrontEndDisplay(result, stringencyTrimLimit, resultsLimit, searchOptions, queryGeneIds);                        
        }
        
        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result.setErrorState( "Sorry, No genes are currently coexpressed under the selected search conditions " );
            log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearchQuick( CoexpressionSearchCommand searchOptions ) {

        return doQuickSearch( searchOptions );

    }

    /**
     * Main AJAX entry point
     * 
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        Collection<ExpressionExperiment> myEE = null;

        if ( searchOptions.getGeneIds() == null || searchOptions.getGeneIds().isEmpty() ) {
            return getEmptyResult();
        }

        if ( searchOptions.isQuick() ) {
            return doQuickSearch( searchOptions );
        }

        if ( searchOptions.getGeneIds().size() > MAX_GENES_PER_QUERY ) {
            result.setErrorState( "Too many genes selected, please limit searches to " + MAX_GENES_PER_QUERY + " genes" );
            return result;
        }

        Collection<Gene> genes = geneService.loadThawed( searchOptions.getGeneIds() );

        if ( genes.size() == 0 ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;
        }

        /*
         * Validation ...
         */
        if ( searchOptions.getTaxonId() != null ) {
            for ( Gene gene : genes ) {
                if ( !gene.getTaxon().getId().equals( searchOptions.getTaxonId() ) ) {
                    result.setErrorState( "Search for gene from wrong taxon. Please check the genes match the selected taxon" );
                    return result;
                }
            }
        }

        Collection<Long> eeIds = new HashSet<Long>();
        Long eeSetId = null;
        if ( searchOptions.getEeIds() != null ) {
            // security filter.
            eeIds = EntityUtils.getIds( expressionExperimentService.loadMultiple( searchOptions.getEeIds() ) );
        } else if ( searchOptions.getEeSetId() != null ) {
            eeSetId = searchOptions.getEeSetId();
        } else if ( StringUtils.isNotBlank( searchOptions.getEeSetName() ) ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                    .getEeSetName() );
            if ( eeSets.size() == 1 ) {
                eeSetId = eeSets.iterator().next().getId();
            } else {
                result.setErrorState( "Unknown or ambiguous set name: " + searchOptions.getEeSetName() );
                return result;
            }
        }

        /*
         * Got an ee set.
         */
        if ( eeSetId != null ) {
            eeIds = EntityUtils.getIds( expressionExperimentSetService.getExperimentsInSet( eeSetId ) );
        }

        // Add the users datasets to the selected datasets
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadMyExpressionExperiments();
            if ( myEE != null && !myEE.isEmpty() ) {
                for ( ExpressionExperiment ee : myEE ) {
                    eeIds.add( ee.getId() );
                }

                searchOptions.setForceProbeLevelSearch( true );
            } else
                log.info( "No user data to add" );
        }
        if ( eeIds.isEmpty() ) {
            result.setErrorState( "No experiments were available" );
            return result;
        }

        log.info( "Coexpression search: " + searchOptions );

        result = geneCoexpressionService.coexpressionSearch( eeIds, genes, searchOptions.getStringency(), MAX_RESULTS,
                searchOptions.getQueryGenesOnly(), searchOptions.isForceProbeLevelSearch() );

        // debug 2317, 2336
        // Collection<CoexpressionValueObjectExt> quickResults = geneCoexpressionService.coexpressionSearchQuick2(
        // eeIds,
        // genes, searchOptions.getStringency(), MAX_RESULTS, searchOptions.getQueryGenesOnly() );
        // log.info( eeIds.size() + " ees " + result.getKnownGeneResults().size() + " quick: " + quickResults.size() );
        // end debug.

        if ( searchOptions.isUseMyDatasets() ) {
            addMyDataFlag( result, myEE );
        }

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result.setErrorState( "<b> Sorry, No genes are currently coexpressed under the selected search conditions </b>" );
            log.info( "No search results for query: " + searchOptions );
        }
        return result;

    }

    /**
     * @param query
     * @param taxonId
     * @return
     * @deprecated redundant with method in ExpressionExperimentController.
     */
    @Deprecated
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        log.info( "Search: " + query + " taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    public CoexpressionMetaValueObject getEmptyResult() {
        return new CoexpressionMetaValueObject();
    }

    // TODO: Dead code?
    /*
     * Handle case of text export of the results.
     * 
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    // protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
    // throws Exception {
    //
    // if ( request.getParameter( "export" ) != null ) {
    //
    // Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
    // Collection<Gene> genes = geneService.loadMultiple( geneIds );
    // genes = geneService.thawLite( genes );
    //
    // boolean queryGenesOnly = request.getParameter( "q" ) != null;
    // int stringency = DEFAULT_STRINGENCY;
    // try {
    // stringency = Integer.parseInt( request.getParameter( "s" ) );
    // } catch ( Exception e ) {
    // log.warn( "invalid stringency; using default " + stringency );
    // }
    // Collection<Long> eeIds = new HashSet<Long>();
    // Long eeSetId = null;
    //
    // String eeSetIdString = request.getParameter( "a" );
    // if ( StringUtils.isNotBlank( eeSetIdString ) ) {
    // try {
    // eeSetId = Long.parseLong( eeSetIdString );
    // } catch ( NumberFormatException e ) {
    // log.warn( "Invalid eeSet id: " + eeSetIdString );
    // return new ModelAndView( this.getFormView() );
    // }
    //
    // ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
    // if ( eeSet == null ) {
    // throw new IllegalArgumentException( "Cannot load EE set with id=" + eeSetId );
    // }
    //
    // eeIds = EntityUtils.getIds( eeSet.getExperiments() );
    // } else if ( StringUtils.isNotBlank( request.getParameter( "an" ) ) ) {
    // Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( request
    // .getParameter( "an" ) );
    // if ( eeSets.size() == 1 ) {
    // eeSetId = eeSets.iterator().next().getId();
    //
    // ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
    // for ( BioAssaySet b : eeSet.getExperiments() ) {
    // eeIds.add( b.getId() );
    // }
    // } else {
    // log.warn( "Unknown or ambiguous set name: : " + request.getParameter( "an" ) );
    // return new ModelAndView( this.getFormView() );
    // }
    // } else {
    // eeIds = extractIds( request.getParameter( "ee" ) );
    // }
    //
    // CoexpressionMetaValueObject result = geneCoexpressionService.coexpressionSearch( eeIds, genes, stringency,
    // MAX_RESULTS, queryGenesOnly, false );
    // ModelAndView mav = new ModelAndView( new TextView() );
    // String output = result.toString();
    // mav.addObject( "text", output.length() > 0 ? output : "no results" );
    // return mav;
    //
    // }
    // return new ModelAndView( this.getFormView() );
    //
    // }

    private void addMyDataFlag( CoexpressionMetaValueObject vo, Collection<ExpressionExperiment> eesToFlag ) {

        Collection<Long> eesToFlagIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : eesToFlag ) {
            eesToFlagIds.add( ee.getId() );
        }

        if ( vo == null || vo.getKnownGeneResults() == null || vo.getKnownGeneResults().isEmpty() ) return;

        for ( CoexpressionValueObjectExt covo : vo.getKnownGeneResults() ) {
            for ( Long eeToFlag : eesToFlagIds ) {
                if ( covo.getSupportingExperiments().contains( eeToFlag ) ) {
                    covo.setContainsMyData( true );
                    break;
                }
            }
        }

    }

    /**
     * Locate an appropriate EESet to use.
     * 
     * @param searchOptions
     * @param gene
     * @return ID of the EESet, or null if none can be found.
     */
    private Long getEESet( CoexpressionSearchCommand searchOptions, Gene gene ) {
        ExpressionExperimentSet eeSet = null;
        Long eeSetId = null;

        if ( searchOptions.getEeSetId() != null ) {
            eeSet = expressionExperimentSetService.load( searchOptions.getEeSetId() );
            if ( eeSet != null ) eeSetId = eeSet.getId();

        } else {

            if ( searchOptions.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                        .getEeSetName() );

                if ( eeSets == null || eeSets.size() == 0 ) {
                    return null;
                }
                if ( eeSets.size() > 1 ) {
                    log.warn( "more than one set found using 1st." );
                }

                eeSetId = eeSets.iterator().next().getId();

            } else {
                GeneCoexpressionAnalysis analysis = null;
                String analysisName = "All " + gene.getTaxon().getCommonName();
                Collection<GeneCoexpressionAnalysis> analyses = geneCoexpressionAnalysisService
                        .findByName( analysisName );

                if ( analyses.isEmpty() ) {
                    return null;
                }

                /*
                 * Find the first enabled one.
                 */
                for ( GeneCoexpressionAnalysis a : analyses ) {
                    if ( a.getEnabled() ) {
                        analysis = a;
                    }
                }

                if ( analysis == null ) {
                    throw new IllegalStateException( "No analysis is enabled" );
                }

                eeSet = analysis.getExpressionExperimentSetAnalyzed();

                assert eeSet != null;

                eeSetId = eeSet.getId();

            }
        }
        return eeSetId;
    }
    
    private void trimGraphForFrontEndDisplay( CoexpressionMetaValueObject result, int stringencyTrimLimit, int resultsLimit, CoexpressionSearchCommand searchOptions, Collection<Long> queryGeneIds){
        
        Collection<CoexpressionValueObjectExt> geneResults = result.getKnownGeneResults();
        
        int displayTrimmedStringency = 0;
        
        int oldSize = geneResults.size();        

        log.info( "Coex Search for " + searchOptions.getGeneIds().size() + " genes: "
                + searchOptions.getGeneIds().toString() + " returned " + geneResults.size()
                + " results.  Stripping non query gene low stringency results(possible up to a stringency of "
                + stringencyTrimLimit + ")" );

        Collection<CoexpressionValueObjectExt> strippedGeneResults = new ArrayList<CoexpressionValueObjectExt>();

        for ( int i = 2; i <= stringencyTrimLimit; i++ ) {

            HashSet<Long> nodeIds = new HashSet<Long>();

            // add all coexpression links to the original queryGenes
            for ( CoexpressionValueObjectExt cvoe : geneResults ) {

                if ( ( queryGeneIds.contains( cvoe.getFoundGene().getId() ) || queryGeneIds.contains( cvoe
                        .getQueryGene().getId() ) ) ) {
                    // if one of the query or found genes is in the original search(before the my genes only) keep
                    // that coexpression value object to ensure that node sticks around

                    strippedGeneResults.add( cvoe );

                    // need to populate nodeIds appropriately based on stringency for next loop which adds 'my genes
                    // only' edges
                    if ( queryGeneIds.contains( cvoe.getFoundGene().getId() )
                            && queryGeneIds.contains( cvoe.getQueryGene().getId() ) ) {
                        nodeIds.add( cvoe.getFoundGene().getId() );
                        nodeIds.add( cvoe.getQueryGene().getId() );
                    } else if ( queryGeneIds.contains( cvoe.getFoundGene().getId() ) ) {

                        nodeIds.add( cvoe.getFoundGene().getId() );
                        if ( cvoe.getPosSupp() > i || cvoe.getNegSupp() > i ) {
                            nodeIds.add( cvoe.getQueryGene().getId() );
                        }

                    } else if ( queryGeneIds.contains( cvoe.getQueryGene().getId() ) ) {
                        nodeIds.add( cvoe.getQueryGene().getId() );
                        if ( cvoe.getPosSupp() > i || cvoe.getNegSupp() > i ) {
                            nodeIds.add( cvoe.getFoundGene().getId() );
                        }
                    }
                }

            }

            // need to loop through again to add missing coex links between non query genes(that are in the graph,
            // i.e. in graphIds) that meet stringency threshold
            // separate loop is needed because we need to know all the nodes in the graph before we can do this step

            for ( CoexpressionValueObjectExt cvoe : geneResults ) {

                if ( !queryGeneIds.contains( cvoe.getFoundGene().getId() )
                        && !queryGeneIds.contains( cvoe.getQueryGene().getId() )
                        && ( cvoe.getPosSupp() > i || cvoe.getNegSupp() > i )
                        && ( nodeIds.contains( cvoe.getFoundGene().getId() ) && nodeIds.contains( cvoe
                                .getQueryGene().getId() ) ) ) {
                    strippedGeneResults.add( cvoe );
                }

            }

            if (strippedGeneResults.size() < geneResults.size()){
                displayTrimmedStringency = i;                
            }
            
            geneResults = strippedGeneResults;

            if ( geneResults.size() < resultsLimit ) {
                log.info( "Breaking out of filter coex results loop after removing edges of stringency:" + i );                
                break;
            }

            strippedGeneResults = new HashSet<CoexpressionValueObjectExt>();
           

        }

        log.info( "Original results size: " + oldSize + " trimmed results size: " + geneResults.size()
                + "  Total results removed: " + ( oldSize - geneResults.size() ) );

        
        result.setKnownGeneResults( geneResults );        
       
        
        result.setDisplayInfo( "Results not involving query genes have been removed to a stringency of "
                + displayTrimmedStringency + " due to size of graph." );
        
        
    }
    
    private Collection<CoexpressionValueObjectExt> eliminateDuplicates(
            Collection<CoexpressionValueObjectExt> geneResults) {

        HashSet<String> coexLinkMap = new HashSet<String>();

        List<CoexpressionValueObjectExt> resultsNoDuplicates = new ArrayList<CoexpressionValueObjectExt>();

        for ( CoexpressionValueObjectExt cvoe : geneResults ) {

            Long queryGeneId = cvoe.getQueryGene().getId();
            Long foundGeneId = cvoe.getFoundGene().getId();

            if ( !coexLinkMap.contains( queryGeneId.toString() + "to" + foundGeneId.toString() )
                    && !coexLinkMap.contains( foundGeneId.toString() + "to" + queryGeneId.toString() ) ) {

                coexLinkMap.add( queryGeneId.toString() + "to" + foundGeneId.toString() );
                coexLinkMap.add( foundGeneId.toString() + "to" + queryGeneId.toString() );
                
                resultsNoDuplicates.add( cvoe );

            } 

        }

        return resultsNoDuplicates;
    }
    
    private void restrictSearchOptionsQueryGenes(CoexpressionSearchCommand searchOptions,
            Collection<Long> queryGeneIds){
        
        if (queryGeneIds==null && searchOptions.getGeneIds().size() > MAX_GENES_PER_QUERY){ 
            
            searchOptions.setGeneIds( trimGeneIds(searchOptions.getGeneIds(), MAX_GENES_PER_QUERY) );
            
        }else if(searchOptions.getGeneIds().size() > MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY){
            //this will be a 'my genes only' vis query since queryGeneIds !=null
            searchOptions.setGeneIds( trimGeneIds(searchOptions.getGeneIds(), MAX_GENES_PER_MY_GENES_ONLY_VIS_QUERY) );
            
        }
        
    }
    
    private Collection<Long> trimGeneIds(Collection<Long> geneIds, int limit){
        
        Collection<Long> trimmedGeneIds = new ArrayList<Long>();
        
        for (Long l:geneIds){                
            trimmedGeneIds.add( l );                
            if (trimmedGeneIds.size()>= limit){
                break;
            }                
        }            
        
        return trimmedGeneIds;
    }

}