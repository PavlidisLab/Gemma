/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.analysis.expression.diff.DiffExpressionSelectedFactorCommand;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionMetaAnalysisValueObject;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchService;
import ubic.gemma.web.visualization.DifferentialExpressionGenesConditionsValueObject;

/**
 * A controller used to get differential expression analysis and meta analysis results.
 * 
 * @author keshav
 * @version $Id$ *
 */
@Controller
public class DifferentialExpressionSearchController {

    private static Log log = LogFactory.getLog( DifferentialExpressionSearchController.class.getName() );
    
    private static final double DEFAULT_THRESHOLD = 0.01;

    private static final int MAX_GENES_PER_QUERY = 20;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    
    @Autowired
    private GeneDifferentialExpressionService geneDifferentialExpressionService;
    
    @Autowired
    private GeneService geneService;
    
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private DifferentialExpressionGeneConditionSearchService geneConditionSearchService;
    
    public DifferentialExpressionGenesConditionsValueObject getDiffExpSearchResult (String taskId) {
        return this.geneConditionSearchService.getDiffExpSearchResult( taskId );
    }

    public ubic.gemma.web.visualization.DifferentialExpressionGeneConditionSearchServiceImpl.TaskProgress getDiffExpSearchTaskProgress(String taskId) {
        return this.geneConditionSearchService.getDiffExpSearchTaskProgress( taskId );
    }
    
    public String scheduleDiffExpSearchTask (
            Long taxonId,
            Collection<ExpressionExperimentSetValueObject> datasetValueObjects,
            Collection<GeneSetValueObject> geneValueObjects,
            List<String> geneSessionGroupQueries,
            List<String> experimentSessionGroupQueries ) {

        log.info("Starting gene x condition search...");
        // Load experiments
        List<Collection<ExpressionExperiment>> experiments = new ArrayList<Collection<ExpressionExperiment>>();
        List<String> datasetGroupNames = new ArrayList<String>();
        for ( ExpressionExperimentSetValueObject eevo : datasetValueObjects ) {
            if ( eevo != null ) {
                experiments.add( loadExperimentsByIds( eevo.getExpressionExperimentIds() ) );
                datasetGroupNames.add( eevo.getName() );
            }
        }
        
        // Load genes
        List<List<Gene>> genes = new ArrayList<List<Gene>>();
        List<String> geneGroupNames = new ArrayList<String>();

        for ( GeneSetValueObject gsvo : geneValueObjects ) {
            if ( gsvo != null ) {
                geneGroupNames.add( gsvo.getName() );
                genes.add( new ArrayList<Gene>( geneService.loadMultiple( gsvo.getGeneIds() ) ) );
            }
        }

        String taskId = geneConditionSearchService.scheduleDiffExpSearchTask ( genes, experiments, geneGroupNames, datasetGroupNames );
        return taskId;
    }

        
//    public DifferentialExpressionGenesConditionsValueObject geneConditionSearch (
//            Long taxonId,
//            Collection<ExpressionExperimentSetValueObject> datasetValueObjects,
//            Collection<GeneSetValueObject> geneValueObjects,
//            List<String> geneSessionGroupQueries,
//            List<String> experimentSessionGroupQueries ) {
//
//        log.info("Starting gene x condition search...");
//        org.springframework.util.StopWatch watch = new org.springframework.util.StopWatch("geneConditionSearch");
//        watch.start("Loading "+ datasetValueObjects.size() +" experiment sets.");
//        // Load experiments
//        List<Collection<ExpressionExperiment>> experiments = new ArrayList<Collection<ExpressionExperiment>>();
//        List<String> datasetGroupNames = new ArrayList<String>();
//        for ( ExpressionExperimentSetValueObject eevo : datasetValueObjects ) {
//            if ( eevo != null ) {
//                experiments.add( loadExperimentsByIds( eevo.getExpressionExperimentIds() ) );
//                datasetGroupNames.add( eevo.getName() );
//            }
//        }
//        
//        watch.stop();
//        watch.start("Loading "+ geneValueObjects.size() +" gene sets.");
//        // updates param
//        //recreateSessionBoundEEGroupsFromBookmark( experimentSessionGroupQueries, experiments, datasetGroupNames );
//
//        // Load genes
//        List<List<Gene>> genes = new ArrayList<List<Gene>>();
//        List<String> geneGroupNames = new ArrayList<String>();
//
//        for ( GeneSetValueObject gsvo : geneValueObjects ) {
//            if ( gsvo != null ) {
//                geneGroupNames.add( gsvo.getName() );
//                genes.add( new ArrayList<Gene>( geneService.loadMultiple( gsvo.getGeneIds() ) ) );
//            }
//        }
//
//        // note: this method makes changes to params
////        recreateSessionBoundGeneGroupsFromBookmark( geneSessionGroupQueries, genes, geneNames, geneFullNames, geneIds,
////                geneGroupNames );
//
//        watch.stop();
//        watch.start("Pupulate heatmap object.");        
//        DifferentialExpressionGenesConditionsValueObject result = geneConditionSearchService.createGenesConditionsValueObject( genes, experiments, geneGroupNames, datasetGroupNames );
//        watch.stop();
//
//        log.info( watch.prettyPrint() );
//        
//        return result;
//    }
        
    /**
     * Session-bound gene groups are encoded in bookmarkable URLs as the query that made them (minus modifications) here
     * we're "recreating" these groups (session-bound groups aren't actually created) Makes changes to params
     * 
     * @param geneSessionGroupQueries
     * @param genes
     * @param geneNames
     * @param geneFullNames
     * @param geneIds
     * @param geneGroupNames
     */
    private void recreateSessionBoundGeneGroupsFromBookmark( List<String> geneSessionGroupQueries,
            List<List<Gene>> genes, List<List<String>> geneNames, List<List<String>> geneFullNames,
            List<List<Long>> geneIds, List<String> geneGroupNames ) {
        List<Gene> genesInsideSet;
        List<String> geneNamesInsideSet;
        List<String> geneFullNamesInsideSet;
        List<Long> geneIdsInsideSet;

        for ( String query : geneSessionGroupQueries ) {
            if ( query.contains( ";" ) && query.contains( ":" ) ) {
                String[] param = query.split( ";" );
                // get the taxon value
                String tax = param[0].split( ":" )[1];
                Taxon taxon = null;
                try {
                    taxon = taxonService.load( new Long( tax ) );
                } catch ( NumberFormatException nfe ) {
                    throw new NumberFormatException(
                            "Taxon id in URL is invlaid. Cannot perform search. Taxon id was: " + tax );
                }
                // get the query
                String term = param[1].split( ":" )[1];
                genesInsideSet = new ArrayList<Gene>();
                geneNamesInsideSet = new ArrayList<String>();
                geneFullNamesInsideSet = new ArrayList<String>();
                geneIdsInsideSet = new ArrayList<Long>();

                if ( term.matches( "^GO_\\d+" ) ) {
                    GeneSet goSet = this.geneSetSearch.findByGoId( term, taxon );
                    if ( goSet != null ) {
                        Gene gene = null;
                        for ( GeneSetMember gsm : goSet.getMembers() ) {
                            gene = gsm.getGene();
                            genesInsideSet.add( gene );
                            geneNamesInsideSet.add( gene.getOfficialSymbol() );
                            geneFullNamesInsideSet.add( gene.getOfficialName() );
                            geneIdsInsideSet.add( gene.getId() );
                        }
                        geneGroupNames.add( term );
                    } else {
                        log.warn( "Could not find GO group to match " + term );
                    }

                } else {
                    SearchSettings settings = new SearchSettings( term );
                    settings.noSearches();
                    settings.setGeneralSearch( true ); // add a general search, needed for finding GO groups
                    settings.setSearchGenes( true ); // add searching for genes
                    settings.setSearchGeneSets( true ); // add searching for geneSets
                    settings.setTaxon( taxon ); // this doesn't work yet

                    Map<Class<?>, List<SearchResult>> results = searchService.search( settings );
                    List<SearchResult> geneSetSearchResults = results.get( GeneSet.class );
                    List<SearchResult> geneSearchResults = results.get( Gene.class );

                    Gene gene = null;
                    for ( SearchResult sr : geneSearchResults ) {
                        gene = ( Gene ) sr.getResultObject();
                        genesInsideSet.add( gene );
                        geneNamesInsideSet.add( gene.getOfficialSymbol() );
                        geneFullNamesInsideSet.add( gene.getOfficialName() );
                        geneIdsInsideSet.add( gene.getId() );
                    }
                    GeneSet geneSet = null;
                    for ( SearchResult sr : geneSetSearchResults ) {
                        geneSet = ( GeneSet ) sr.getResultObject();
                        for ( GeneSetMember gsm : geneSet.getMembers() ) {
                            if ( !genesInsideSet.contains( gsm.getGene() ) ) {
                                genesInsideSet.add( gsm.getGene() );
                                geneNamesInsideSet.add( gsm.getGene().getOfficialSymbol() );
                                geneFullNamesInsideSet.add( gsm.getGene().getOfficialName() );
                                geneIdsInsideSet.add( gsm.getGene().getId() );
                            }
                        }
                    }
                    geneGroupNames.add( "All " + taxon.getCommonName() + " results for '" + term + "'" );
                }
                genes.add( genesInsideSet );
                geneNames.add( geneNamesInsideSet );
                geneFullNames.add( geneFullNamesInsideSet );
                geneIds.add( geneIdsInsideSet );
            }

        }
    }

    /**
     * session-bound experiment groups are encoded in bookmarkable URLs as the query that made them (minus
     * modifications) here we're "recreating" these groups (session-bound groups aren't actually created) Makes changes
     * to params
     * 
     * @param experimentSessionGroupQueries
     * @param experiments
     * @param datasetGroupNames
     */
    private void recreateSessionBoundEEGroupsFromBookmark( List<String> experimentSessionGroupQueries,
            List<Collection<BioAssaySet>> experiments, List<String> datasetGroupNames ) {

        for ( String query : experimentSessionGroupQueries ) {
            if ( query.contains( ";" ) && query.contains( ":" ) ) {
                String[] param = query.split( ";" );
                // get the taxon value
                String tax = param[0].split( ":" )[1];
                Taxon taxon = null;
                try {
                    taxon = taxonService.load( new Long( tax ) );
                } catch ( NumberFormatException nfe ) {
                    throw new NumberFormatException(
                            "Taxon id in URL is invlaid. Cannot perform search. Taxon id was: " + tax );
                }
                // get the query
                String term = param[1].split( ":" )[1];
                List<BioAssaySet> experimentsInGroup = new ArrayList<BioAssaySet>();

                SearchSettings settings = SearchSettings.expressionExperimentSearch( term );
                settings.setGeneralSearch( true ); // add a general search
                settings.setSearchExperimentSets( true ); // add searching for experimentSets
                settings.setTaxon( taxon );

                Map<Class<?>, List<SearchResult>> srs = searchService.search( settings );
                List<SearchResult> eeSearchResults = srs.get( ExpressionExperiment.class );
                List<SearchResult> eeSetSearchResults = srs.get( ExpressionExperimentSet.class );

                ExpressionExperiment ee = null;
                ExpressionExperimentSet eeSet = null;
                for ( SearchResult sr : eeSearchResults ) {
                    ee = ( ExpressionExperiment ) sr.getResultObject();
                    experimentsInGroup.add( ee );
                }
                for ( SearchResult sr : eeSetSearchResults ) {
                    eeSet = ( ExpressionExperimentSet ) sr.getResultObject();
                    // don't want duplicates in list
                    experimentsInGroup.removeAll( eeSet.getExperiments() );
                    experimentsInGroup.addAll( eeSet.getExperiments() );
                }
                if ( !experimentsInGroup.isEmpty() ) {
                    experiments.add( experimentsInGroup );
                    datasetGroupNames.add( "All " + taxon.getCommonName() + " results for '" + term + "'" );
                }

            }

        }
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {

        return this.getDifferentialExpression( geneId, threshold, null );
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned. This method is just like getDifferentialExpression but any analyses
     * with the 'batch' factor are filtered out because they are not biologically relevant
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionWithoutBatch( Long geneId,
            double threshold, Integer limit ) {

        Collection<DifferentialExpressionValueObject> analyses = getDifferentialExpression( geneId, threshold, limit );

        // for each DifferentialExpressionValueObject, check if its factors includes a batch factor and if so, remove
        // the batch factor
        Collection<DifferentialExpressionValueObject> toRemove = new ArrayList<DifferentialExpressionValueObject>();
        for ( DifferentialExpressionValueObject analysis : analyses ) {
            for ( ExperimentalFactorValueObject factor : analysis.getExperimentalFactors() ) {
                if ( ExperimentalDesignUtils.isBatch( factor ) ) {
                    toRemove.add( analysis );
                }
            }
        }
        analyses.removeAll( toRemove );

        return analyses;
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold,
            Integer limit ) {

        Gene g = geneService.thaw( geneService.load( geneId ) );

        if ( g == null ) {
            return new ArrayList<DifferentialExpressionValueObject>();
        }

        return geneDifferentialExpressionService.getDifferentialExpression( g, threshold, limit );
    }

    /**
     * AJAX entry which returns differential expression results for the gene with the given id, in the selected factors,
     * at the given significance threshold.
     * 
     * @param geneId
     * @param threshold corrected pvalue threshold (normally this means FDR)
     * @param factorMap
     * @deprecated as far as I can tell this is not used.
     * @return
     */
    @Deprecated
    public Collection<DifferentialExpressionValueObject> getDifferentialExpressionForFactors( Long geneId,
            double threshold, Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        if ( factorMap.isEmpty() || geneId == null ) {
            return null;
        }

        Gene g = geneService.load( geneId );
        Collection<DifferentialExpressionValueObject> result = geneDifferentialExpressionService
                .getDifferentialExpression( g, threshold, factorMap );

        return result;
    }

    /**
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffExpressionForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> eeScopeIds = command.getEeIds();
        int eeScopeSize = 0;

        if ( eeScopeIds != null && !eeScopeIds.isEmpty() ) {

            // do we need to validate these ids further? It should get checked late (in the analysis stage)

            eeScopeSize = eeScopeIds.size();
        } else {
            if ( command.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSet = this.expressionExperimentSetService.findByName( command
                        .getEeSetName() );

                if ( eeSet == null || eeSet.isEmpty() ) {
                    throw new IllegalArgumentException( "Unknown or ambiguous set name: " + command.getEeSetName() );
                }

                eeScopeSize = eeSet.iterator().next().getExperiments().size();
            } else {
                Long eeSetId = command.getEeSetId();
                if ( eeSetId >= 0 ) {
                    ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
                    // validation/security check.
                    if ( eeSet == null ) {
                        throw new IllegalArgumentException( "No such set with id=" + eeSetId );
                    }
                    eeScopeSize = eeSet.getExperiments().size();
                }
            }

        }

        Collection<Long> geneIds = command.getGeneIds();

        if ( geneIds.size() > MAX_GENES_PER_QUERY ) {
            throw new IllegalArgumentException( "Too many genes selected, please limit searches to "
                    + MAX_GENES_PER_QUERY );
        }

        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = command.getSelectedFactors();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionMetaAnalysisValueObject> mavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpressionMetaAnalysis( geneId,
                    selectedFactors, threshold );

            if ( mavo == null ) {
                continue; // no results.
            }

            mavo.setSortKey();
            if ( selectedFactors != null && !selectedFactors.isEmpty() ) {
                mavo.setNumSearchedExperiments( selectedFactors.size() );
            }

            mavo.setNumExperimentsInScope( eeScopeSize );

            mavos.add( mavo );

        }

        return mavos;
    }

    public ExpressionExperimentSetService getExpressionExperimentSetService() {
        return expressionExperimentSetService;
    }

    /**
     * AJAX entry.
     * <p>
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
     * 
     * @param eeIds
     */
    public Collection<ExpressionExperimentExperimentalFactorValueObject> getFactors( final Collection<Long> eeIds ) {

        Collection<ExpressionExperimentExperimentalFactorValueObject> result = new HashSet<ExpressionExperimentExperimentalFactorValueObject>();

        final Collection<Long> securityFilteredIds = securityFilterExpressionExperimentIds( eeIds );

        if ( securityFilteredIds.size() == 0 ) {
            return result;
        }

        log.debug( "Getting factors for experiments with ids: "
                + StringUtils.abbreviate( securityFilteredIds.toString(), 100 ) );

        Collection<Long> filteredEeIds = new HashSet<Long>();

        Map<Long, DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigationIds( securityFilteredIds );

        if ( diffAnalyses.isEmpty() ) {
            log.debug( "No differential expression analyses for given ids: " + StringUtils.join( filteredEeIds, ',' ) );
            return result;
        }

        Collection<ExpressionExperimentValueObject> eevos = this.expressionExperimentService
                .loadValueObjects( diffAnalyses.keySet(), false );

        Map<Long, ExpressionExperimentValueObject> eevoMap = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevoMap.put( eevo.getId(), eevo );
        }

        for ( Long id : diffAnalyses.keySet() ) {

            DifferentialExpressionAnalysis analysis = diffAnalyses.get( id );
            differentialExpressionAnalysisService.thaw( analysis );

            Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
            for ( FactorAssociatedAnalysisResultSet fars : analysis.getResultSets() ) {
                // FIXME includes factors making up interaction terms, but shouldn't
                // matter, because they will be included as main effects too. If not, this will be wrong!
                factors.addAll( fars.getExperimentalFactors() );
            }

            filteredEeIds.add( id );
            ExpressionExperimentValueObject eevo = eevoMap.get( id );
            ExpressionExperimentExperimentalFactorValueObject eeefvo = new ExpressionExperimentExperimentalFactorValueObject();
            eeefvo.setExpressionExperiment( eevo );
            eeefvo.setNumFactors( factors.size() );
            for ( ExperimentalFactor ef : factors ) {
                ExperimentalFactorValueObject efvo = geneDifferentialExpressionService
                        .configExperimentalFactorValueObject( ef );
                eeefvo.getExperimentalFactors().add( efvo );
            }

            result.add( eeefvo );
        }
        log.info( "Filtered experiments.  Returning factors for experiments with ids: "
                + StringUtils.abbreviate( filteredEeIds.toString(), 100 ) );
        return result;
    }

    /**
     * @param differentialExpressionAnalyzerService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    /**
     * @param geneDifferentialExpressionService
     */
    public void setGeneDifferentialExpressionService(
            GeneDifferentialExpressionService geneDifferentialExpressionService ) {
        this.geneDifferentialExpressionService = geneDifferentialExpressionService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param fs
     * @return
     */
    private Collection<DiffExpressionSelectedFactorCommand> extractFactorInfo( String fs ) {
        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = new HashSet<DiffExpressionSelectedFactorCommand>();
        try {
            if ( fs != null ) {
                String[] fss = fs.split( "," );
                for ( String fm : fss ) {
                    String[] m = fm.split( "\\." );
                    if ( m.length != 2 ) {
                        continue;
                    }
                    String eeIdStr = m[0];
                    String efIdStr = m[1];

                    Long eeId = Long.parseLong( eeIdStr );
                    Long efId = Long.parseLong( efIdStr );
                    DiffExpressionSelectedFactorCommand dsfc = new DiffExpressionSelectedFactorCommand( eeId, efId );
                    selectedFactors.add( dsfc );
                }
            }
        } catch ( NumberFormatException e ) {
            log.warn( "Error parsing factor info" );
        }
        return selectedFactors;
    }

    /**
     * Returns the results of the meta-analysis.
     * 
     * @param geneId
     * @param eeIds
     * @param selectedFactors
     * @param threshold
     * @return
     */
    private DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( Long geneId,
            Collection<DiffExpressionSelectedFactorCommand> selectedFactors, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) {
            log.warn( "No Gene with id=" + geneId );
            return null;
        }

        /* find experiments that have had the diff cli run on it and have the gene g (analyzed) - security filtered. */
        Collection<BioAssaySet> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        if ( experimentsAnalyzed.size() == 0 ) {
            throw new EntityNotFoundException( "No results were found: no experiment analyzed those genes" );
        }

        /* the 'chosen' factors (and their associated experiments) */
        Map<Long, Long> eeFactorsMap = new HashMap<Long, Long>();
        for ( DiffExpressionSelectedFactorCommand selectedFactor : selectedFactors ) {
            Long eeId = selectedFactor.getEeId();
            eeFactorsMap.put( eeId, selectedFactor.getEfId() );
            if ( log.isDebugEnabled() ) log.debug( eeId + " --> " + selectedFactor.getEfId() );
        }

        /*
         * filter experiments that had the diff cli run on it and are in the scope of eeFactorsMap eeIds
         * (active/available to the user).
         */
        Collection<BioAssaySet> activeExperiments = null;
        if ( eeFactorsMap.keySet() == null || eeFactorsMap.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<BioAssaySet>();
            for ( BioAssaySet ee : experimentsAnalyzed ) {
                if ( eeFactorsMap.keySet().contains( ee.getId() ) ) {
                    activeExperiments.add( ee );
                }
            }
        }

        if ( activeExperiments.isEmpty() ) {
            throw new EntityNotFoundException(
                    "No results were found: none of the experiments selected analyzed those genes" );
        }

        DifferentialExpressionMetaAnalysisValueObject mavo = geneDifferentialExpressionService
                .getDifferentialExpressionMetaAnalysis( threshold, g, eeFactorsMap, activeExperiments );

        return mavo;
    }

    /*
     * Helper method to get factor values. TODO: Fix FactoValue class to return correct factor value in the first place.
     */
    private String getFactorValueString( FactorValue fv ) {
        if ( fv == null ) return "null";

        if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
            String fvString = "";
            for ( Characteristic c : fv.getCharacteristics() ) {
                fvString += c.getValue() + " ";
            }
            return fvString;
        } else if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
            return fv.getValue();
        } else
            return "absent ";
    }

    /**
     * @param ids
     * @return
     */
    private Collection<ExpressionExperiment> loadExperimentsByIds( Collection<Long> ids ) {
        Collection<ExpressionExperiment> experiments = expressionExperimentService.loadMultiple( ids );

        if ( experiments.isEmpty() ) {
            throw new EntityNotFoundException( "Could not access any experiments." );
        }

        return experiments;
    }

    
    /**
     * @param ids
     * @return
     */
    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.loadMultiple( ids );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }

// TODO: Dead code?
//    /*
//     * Handles the case exporting results as text.
//     * 
//     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
//     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
//     */
//    @Override
//    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
//            throws Exception {
//
//        if ( request.getParameter( "export" ) == null ) return new ModelAndView( this.getFormView() );
//
//        // -------------------------
//        // Download diff expression data for a specific diff expresion search
//
//        double threshold = DEFAULT_THRESHOLD;
//        try {
//            threshold = Double.parseDouble( request.getParameter( "t" ) );
//        } catch ( NumberFormatException e ) {
//            log.warn( "invalid threshold; using default " + threshold );
//        }
//
//        Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
//
//        Long eeSetId = null;
//        Collection<Long> eeIds = null;
//        try {
//            eeSetId = Long.parseLong( request.getParameter( "a" ) );
//        } catch ( NumberFormatException e ) {
//            //
//        }
//        if ( eeSetId == null ) {
//            eeIds = extractIds( request.getParameter( "ees" ) );
//        }
//
//        String fs = request.getParameter( "fm" );
//        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = extractFactorInfo( fs );
//
//        DiffExpressionSearchCommand command = new DiffExpressionSearchCommand();
//        command.setGeneIds( geneIds );
//        command.setEeSetId( eeSetId );
//        command.setEeIds( eeIds );
//        command.setSelectedFactors( selectedFactors );
//        command.setThreshold( threshold );
//
//        Collection<DifferentialExpressionMetaAnalysisValueObject> result = getDiffExpressionForGenes( command );
//
//        ModelAndView mav = new ModelAndView( new TextView() );
//
//        StringBuilder buf = new StringBuilder();
//
//        for ( DifferentialExpressionMetaAnalysisValueObject demavo : result ) {
//            buf.append( demavo );
//        }
//
//        String output = buf.toString();
//
//        mav.addObject( "text", output.length() > 0 ? output : "no results" );
//        return mav;
//
//    }
//    
//    
//    /**
//     * Returns a collection of {@link Long} ids from strings.
//     * 
//     * @param idString
//     * @return
//     */
//    protected Collection<Long> extractIds( String idString ) {
//        Collection<Long> ids = new ArrayList<Long>();
//        if ( idString != null ) {
//            for ( String s : idString.split( "," ) ) {
//                try {
//                    ids.add( Long.parseLong( s.trim() ) );
//                } catch ( NumberFormatException e ) {
//                    log.warn( "invalid id " + s );
//                }
//            }
//        }
//        return ids;
//    }

}