/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author paul
 */
@Component
@Lazy
public class GeneCoexpressionSearchServiceImpl implements GeneCoexpressionSearchService {

    /**
     * If the query involves this many genes, we switch to looking only among the genes. The limit here is entirely
     * driven by performance considerations: too many genes and we get too many results.
     */
    private static final int THRESHOLD_TRIGGER_QUERY_GENES_ONLY = 50;

    private static final Log log = LogFactory.getLog( GeneCoexpressionSearchServiceImpl.class.getName() );

    private final ExpressionExperimentService expressionExperimentService;
    private final CoexpressionService coexpressionService;
    private final CoexpressionAnalysisService coexpressionAnalysisService;
    private final GeneService geneService;

    @Autowired
    public GeneCoexpressionSearchServiceImpl( ExpressionExperimentService expressionExperimentService,
            CoexpressionService coexpressionService, CoexpressionAnalysisService coexpressionAnalysisService,
            GeneService geneService ) {
        this.expressionExperimentService = expressionExperimentService;
        this.coexpressionService = coexpressionService;
        this.coexpressionAnalysisService = coexpressionAnalysisService;
        this.geneService = geneService;
    }

    @Override
    public CoexpressionMetaValueObject coexpressionSearch( Collection<Long> inputEeIds, Collection<Long> genes,
            int stringency, int maxResults, boolean queryGenesOnly ) {

        return this.doCoexpressionSearch( inputEeIds, genes, stringency, maxResults, queryGenesOnly, false );
    }

    @Override
    public CoexpressionMetaValueObject coexpressionSearchQuick( Collection<Long> eeIds, Collection<Long> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly ) {
        return this.doCoexpressionSearch( eeIds, queryGenes, stringency, maxResults, queryGenesOnly, true );
    }

    /**
     * Convert CoexpressionValueObjects to CoexpressionValueObjectExts
     *
     * @param queryGeneIds all those included in the query
     */
    private List<CoexpressionValueObjectExt> addExtCoexpressionValueObjects( GeneValueObject queryGene,
            Collection<CoexpressionValueObject> coexp, boolean queryGenesOnly, Collection<Long> queryGeneIds ) {

        List<CoexpressionValueObjectExt> results = new ArrayList<>();
        Collection<Long> coexpGenes = new HashSet<>();
        for ( CoexpressionValueObject cvo : coexp ) {
            coexpGenes.add( cvo.getCoexGeneId() );
        }

        // database hit. loadValueObjects is too slow.
        Map<Long, GeneValueObject> coexpedGenes = EntityUtils
                .getIdMap( geneService.loadValueObjectsByIds( coexpGenes ) );

        for ( CoexpressionValueObject cvo : coexp ) {

            /*
             * sanity check, this is sort of debug code. Each link has to contain at least one query gene.
             */
            if ( queryGenesOnly ) {
                if ( !queryGeneIds.contains( cvo.getCoexGeneId() ) ) {

                    GeneCoexpressionSearchServiceImpl.log
                            .warn( "coexpression for non-query genes obtained unexpectedly when doing queryGenesOnly "
                                    + cvo.getCoexGeneId() + " is not a query" );
                    continue;
                }

                if ( !queryGeneIds.contains( cvo.getQueryGeneId() ) ) {
                    GeneCoexpressionSearchServiceImpl.log
                            .warn( "coexpression for non-query genes obtained unexpectedly when doing queryGenesOnly "
                                    + cvo.getQueryGeneId() + " is not a query" );
                    continue;
                }
            }

            GeneValueObject foundGene = coexpedGenes.get( cvo.getCoexGeneId() );

            if ( foundGene == null ) {
                log.warn( "Could not load gene with ID " + cvo.getCoexGeneId() + " from the database, has it been removed?" );
                continue;
            }

            CoexpressionValueObjectExt ecVo = new CoexpressionValueObjectExt();
            ecVo.setQueryGene( queryGene );

            if ( !queryGeneIds.contains( foundGene.getId() ) ) {
                foundGene.setIsQuery( false );
            } else {
                foundGene.setIsQuery( true );

            }

            ecVo.setFoundGene( foundGene );

            if ( cvo.isPositiveCorrelation() ) {
                ecVo.setPosSupp( cvo.getNumDatasetsSupporting() );
            } else {
                ecVo.setNegSupp( cvo.getNumDatasetsSupporting() );
            }

            // when 'quick', these will not necessarily be set.
            ecVo.setNumTestedIn( cvo.getNumDatasetsTestedIn() );
            ecVo.setSupportingExperiments( cvo.getSupportingDatasets() );

            ecVo.setSortKey();
            results.add( ecVo );

            assert ecVo.getPosSupp() > 0 || ecVo.getNegSupp() > 0;
            assert ecVo.getFoundGene() != null;
            assert ecVo.getQueryGene() != null;
        }

        return results;
    }

    /**
     * @param genes          1 or more.
     * @param stringency;    this may be modified to control the number of results, unless "queryGenesOnly" is true.
     * @param maxResults     per gene, not including the query genes themselves. Ignored if this is 'queryGenesOnly'
     * @param queryGenesOnly will be ignored if number of genes is 1.
     * @return CoexpressionMetaValueObject, in which the results are already populated and sorted.
     */
    private CoexpressionMetaValueObject doCoexpressionSearch( Collection<Long> inputEeIds, Collection<Long> genes,
            int stringency, final int maxResults, final boolean queryGenesOnly, final boolean quick ) {
        if ( genes.isEmpty() ) {
            CoexpressionMetaValueObject r = new CoexpressionMetaValueObject();
            r.setErrorState( "No genes selected" );
            return r;
        }

        boolean actuallyUseQueryGeneOnly = queryGenesOnly && genes.size() > 1;

        Taxon taxon = this.geneService.loadOrFail( genes.iterator().next() ).getTaxon();
        List<ExpressionExperimentValueObject> eevos = this.getFilteredEEVos( inputEeIds, taxon );

        CoexpressionMetaValueObject result = this.initValueObject( genes, eevos );

        if ( eevos.isEmpty() ) {
            result = new CoexpressionMetaValueObject();
            result.setErrorState( "No experiments selected" );
            return result;
        }

        Collection<Long> eeIds = EntityUtils.getIds( eevos );

        Map<Long, List<CoexpressionValueObject>> allCoexpressions;

        // Note: auto-choose stringency on client size not always giving something reasonable. Also: not clear we want
        // to do this auto-adjust for 'query genes only'.

        if ( genes.size() > GeneCoexpressionSearchServiceImpl.THRESHOLD_TRIGGER_QUERY_GENES_ONLY ) {
            if ( !actuallyUseQueryGeneOnly ) {
                GeneCoexpressionSearchServiceImpl.log.info( "Switching to 'query genes only'" );
            }
            actuallyUseQueryGeneOnly = true;
        }

        if ( stringency < 1 )
            stringency = 1;

        if ( !queryGenesOnly ) {
            stringency = Math
                    .max( stringency, this.chooseStringency( actuallyUseQueryGeneOnly, eeIds.size(), genes.size() ) );
            GeneCoexpressionSearchServiceImpl.log
                    .info( "Stringency set to " + stringency + " based on number of experiments (" + eeIds.size()
                            + ") and genes (" + genes.size() + ") queried" );
        } else {
            GeneCoexpressionSearchServiceImpl.log
                    .info( "Query gene only: stringency maintained at requested value=" + stringency );
        }

        assert stringency >= 1 || eeIds.size() == 1;

        // HACK drop the stringency until we get some results.
        int stepSize = 3;
        while ( true ) {
            if ( actuallyUseQueryGeneOnly ) {
                // note that maxResults is ignored.
                if ( genes.size() < 2 ) {
                    // should be impossible - could assert.
                    throw new IllegalArgumentException( "cannot do inter-gene coexpression search with only one gene" );
                }
                allCoexpressions = coexpressionService
                        .findInterCoexpressionRelationships( taxon, genes, eeIds, stringency, quick );
            } else {
                allCoexpressions = coexpressionService
                        .findCoexpressionRelationships( taxon, genes, eeIds, stringency, maxResults, quick );
            }
            int minimum_stringency_for_requery = 2;

            if ( allCoexpressions.isEmpty() && stringency > minimum_stringency_for_requery ) {
                stringency -= stepSize; // step size completely made up.
                stringency = Math.max( minimum_stringency_for_requery, stringency ); // keep stringency at least 2.
                GeneCoexpressionSearchServiceImpl.log.info( "No results, re-querying with stringency=" + stringency );
            } else {
                // no results.
                break;
            }
        }

        GeneCoexpressionSearchServiceImpl.log.info( "Final actual stringency used was " + stringency );

        result.setQueryStringency( stringency );
        result.setQueryGenesOnly( actuallyUseQueryGeneOnly );

        Set<Long> queryGeneIds = allCoexpressions.keySet();
        assert genes.containsAll( queryGeneIds );
        Map<Long, GeneValueObject> idMap = EntityUtils.getIdMap( geneService.loadValueObjectsByIds( queryGeneIds ) );

        int k = 0;
        for ( Long queryGene : queryGeneIds ) {

            Collection<CoexpressionValueObject> coexpressions = allCoexpressions.get( queryGene );

            List<CoexpressionValueObjectExt> results = this
                    .addExtCoexpressionValueObjects( idMap.get( queryGene ), coexpressions, actuallyUseQueryGeneOnly,
                            genes );

            result.getResults().addAll( results );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject( queryGene );
            summary.setDatasetsAvailable( eevos.size() );
            summary.setLinksFound( coexpressions.size() );

            result.getSummaries().put( queryGene, summary );

            if ( ++k % 100 == 0 ) {
                GeneCoexpressionSearchServiceImpl.log.info( "Processed results for " + k + " query genes ..." );
            }

        }

        Collections.sort( result.getResults() );

        // FIXME we might want to suppress this in some situations
        if ( !queryGenesOnly ) {
            result.trim();
        }

        this.populateNodeDegrees( result );

        return result;
    }

    /**
     * Populate node degree. Note that this ignores the datasets that were used in the query - the statistics are for
     * 'all' data sets.
     */
    private void populateNodeDegrees( CoexpressionMetaValueObject result ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> allUsedGenes = new HashSet<>();
        for ( CoexpressionValueObjectExt coexp : result.getResults() ) {
            allUsedGenes.add( coexp.getQueryGene().getId() );
            allUsedGenes.add( coexp.getFoundGene().getId() );
        }

        Map<Long, GeneCoexpressionNodeDegreeValueObject> nodeDegrees = new HashMap<>();
        if ( allUsedGenes.size() > 0 ) {
            nodeDegrees = coexpressionService.getNodeDegrees( allUsedGenes );
        }

        for ( CoexpressionValueObjectExt coexp : result.getResults() ) {

            // if node degree info is out of date.
            if ( !nodeDegrees.containsKey( coexp.getQueryGene().getId() ) || !nodeDegrees
                    .containsKey( coexp.getFoundGene().getId() ) ) {
                continue;
            }

            GeneCoexpressionNodeDegreeValueObject queryGeneNodeDegree = nodeDegrees.get( coexp.getQueryGene().getId() );
            GeneCoexpressionNodeDegreeValueObject foundGeneNodeDegree = nodeDegrees.get( coexp.getFoundGene().getId() );

            boolean pos = coexp.getNegSupp() == 0;

            coexp.setQueryGeneNodeDegree( queryGeneNodeDegree.getLinksWithMinimumSupport( coexp.getSupport(), pos ) );
            coexp.setFoundGeneNodeDegree( foundGeneNodeDegree.getLinksWithMinimumSupport( coexp.getSupport(), pos ) );

            coexp.setQueryGeneNodeDegreeRank( queryGeneNodeDegree.getRankAtMinimumSupport( coexp.getSupport(), pos ) );
            coexp.setFoundGeneNodeDegreeRank( foundGeneNodeDegree.getRankAtMinimumSupport( coexp.getSupport(), pos ) );

        }

        assert result.getSummaries() != null;
        for ( Long g : nodeDegrees.keySet() ) {
            if ( !result.getSummaries().containsKey( g ) )
                continue;
            result.getSummaries().get( g ).setCoexpNodeDegree( nodeDegrees.get( g ) );
        }

        if ( timer.getTime() > 100 ) {
            GeneCoexpressionSearchServiceImpl.log
                    .info( "Populate node degree for " + result.getResults().size() + ": " + timer.getTime() + "ms" );
        }
    }

    /**
     * Security-filter the experiments, remove troubled ones, and retain only those that have an analysis.
     *
     * @param eeIds can be null, in which case all available (to user) IDs are gotten
     */
    private List<ExpressionExperimentValueObject> getFilteredEEVos( Collection<Long> eeIds, Taxon taxon ) {
        List<ExpressionExperimentValueObject> securityFilteredEevos;
        if ( eeIds == null || eeIds.isEmpty() ) {
            // all valid experiments for taxon.
            securityFilteredEevos = new ArrayList<>( expressionExperimentService
                    .loadValueObjectsByIds( coexpressionAnalysisService.getExperimentsWithAnalysis( taxon ) ) );
        } else {
            securityFilteredEevos = new ArrayList<>( expressionExperimentService
                    .loadValueObjectsByIds( coexpressionAnalysisService.getExperimentsWithAnalysis( eeIds ) ) );
        }

        List<ExpressionExperimentValueObject> eevos = new ArrayList<>();

        StopWatch timerFilterTroubled = new StopWatch();
        timerFilterTroubled.start();

        // only keep untroubled experiments
        for ( ExpressionExperimentValueObject eevo : securityFilteredEevos ) {
            if ( !eevo.getTroubled() ) {
                eevos.add( eevo );
            }
        }

        if ( timerFilterTroubled.getTime() > 100 ) {
            GeneCoexpressionSearchServiceImpl.log
                    .info( "Filtering eevos took " + timerFilterTroubled.getTime() + "ms" );
        }

        return eevos;
    }

    /**
     * Use some rough heuristics (based on manual testing) to choose an initial stringency for queries. This value could
     * get adjusted upwards during postprocessing to limit the result set sizes. But this should be set as high as
     * possible to speed up the first stages.
     */
    private Integer chooseStringency( boolean queryGenesOnly, int numExperimentsQueried, int numGenesQueried ) {

        /*
         * 0.01 means if we assay 800 genes the stringency will be increased by 8.
         */
        double geneSlope = 0.01;

        /*
         * If we're doing queryGeneOnly, we can leave the stringency a little lower.
         */
        double geneMinimum = 1 - ( queryGenesOnly ? 1 : 0 );

        /*
         * 0.05 means that we increase the stringency by 1 for every 20 data sets.
         */
        double expSlope = 0.05;

        double expMinimum = 2;

        if ( numExperimentsQueried < 5 ) {
            return ( int ) Math.min( numExperimentsQueried, expMinimum );
        }

        // choose initial level based on the number of genes selected
        int baseline = ( int ) ( Math.ceil( geneMinimum + geneSlope * numGenesQueried ) );

        if ( baseline > numExperimentsQueried ) {
            return numExperimentsQueried;
        }

        if ( queryGenesOnly )
            baseline--;

        return ( int ) ( Math.ceil( baseline + expSlope * numExperimentsQueried ) );
    }

    private CoexpressionMetaValueObject initValueObject( Collection<Long> genes,
            List<ExpressionExperimentValueObject> eevos ) {
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        result.setQueryGenes( new ArrayList<>( geneService.loadValueObjectsByIdsLiter( genes ) ) );
        result.setResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setSummaries( new HashMap<Long, CoexpressionSummaryValueObject>() );
        result.setNumDatasetsQueried( eevos.size() );
        return result;
    }

}
