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
package ubic.gemma.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.CoexpressionService;
import ubic.gemma.model.association.coexpression.CoexpressionValueObject;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.util.EntityUtils;

/**
 * @author paul
 * @version $Id$
 */
@Component
@Lazy
public class GeneCoexpressionSearchServiceImpl implements GeneCoexpressionSearchService {

    /**
     * If the query involves this many genes, we switch to looking only among the genes. The limit here is entirely
     * driven by performance considerations: too many genes and we get too many results.
     */
    private static final int THRESHOLD_TRIGGER_QUERYGENESONLY = 50;

    private static Log log = LogFactory.getLog( GeneCoexpressionSearchServiceImpl.class.getName() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private CoexpressionService coexpressionService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

    @Autowired
    private GeneService geneService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService#
     * coexpressionSearch(java.util.Collection, java.util.Collection, int, int, boolean, boolean)
     */
    @Override
    public CoexpressionMetaValueObject coexpressionSearch( Collection<Long> inputEeIds, Collection<Long> genes,
            int stringency, int maxResults, boolean queryGenesOnly ) {

        return doCoexpressionSearch( inputEeIds, genes, stringency, maxResults, queryGenesOnly, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.coexpression.GeneCoexpressionSearchService#coexpressionSearchQuick(java.util.
     * Collection, java.util.Collection, int, int, boolean)
     */
    @Override
    public CoexpressionMetaValueObject coexpressionSearchQuick( Collection<Long> eeIds, Collection<Long> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly ) {
        return doCoexpressionSearch( eeIds, queryGenes, stringency, maxResults, queryGenesOnly, true );
    }

    /**
     * Convert Gene2GeneCoexpressionValueObjects to CoexpressionValueObjectExts
     * 
     * @param queryGene
     * @param coexp
     * @param stringency
     * @param queryGenesOnly
     * @param queryGeneIds all those included in the query
     * @return results
     */
    private List<CoexpressionValueObjectExt> addExtCoexpressionValueObjects( GeneValueObject queryGene,
            Collection<CoexpressionValueObject> coexp, int stringency, boolean queryGenesOnly,
            Collection<Long> queryGeneIds ) {

        List<CoexpressionValueObjectExt> results = new ArrayList<>();
        Collection<Long> coxpGenes = new HashSet<>();
        for ( CoexpressionValueObject cvo : coexp ) {
            coxpGenes.add( cvo.getCoexGeneId() );
        }

        // database hit. loadValueObjects is too slow.
        Map<Long, GeneValueObject> coexpedGenes = EntityUtils.getIdMap( geneService.loadValueObjectsLiter( coxpGenes ) );

        for ( CoexpressionValueObject cvo : coexp ) {

            /*
             * sanity check, this is sort of debug code. Each link has to contain at least one query gene.
             */
            if ( queryGenesOnly ) {
                if ( !queryGeneIds.contains( cvo.getCoexGeneId() ) ) {

                    log.warn( "coexpression for non-query genes obtained unexpectedly when doing queryGenesOnly "
                            + cvo.getCoexGeneId() + " is not a query" );
                    continue;
                }

                if ( !queryGeneIds.contains( cvo.getQueryGeneId() ) ) {
                    log.warn( "coexpression for non-query genes obtained unexpectedly when doing queryGenesOnly "
                            + cvo.getQueryGeneId() + " is not a query" );
                    continue;
                }
            }

            CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
            ecvo.setQueryGene( queryGene );
            GeneValueObject foundGene = coexpedGenes.get( cvo.getCoexGeneId() );

            if ( !queryGeneIds.contains( foundGene.getId() ) ) {
                foundGene.setIsQuery( false );
            } else {
                foundGene.setIsQuery( true );

            }

            ecvo.setFoundGene( foundGene );

            if ( cvo.isPositiveCorrelation() ) {
                ecvo.setPosSupp( cvo.getNumDatasetsSupporting() );
            } else {
                ecvo.setNegSupp( cvo.getNumDatasetsSupporting() );
            }

            // when 'quick', these will not necessarily be set.
            ecvo.setNumTestedIn( cvo.getNumDatasetsTestedIn() );
            ecvo.setSupportingExperiments( cvo.getSupportingDatasets() );

            ecvo.setSortKey();
            results.add( ecvo );

            assert ecvo.getPosSupp() > 0 || ecvo.getNegSupp() > 0;
            assert ecvo.getFoundGene() != null;
            assert ecvo.getQueryGene() != null;
        }

        return results;
    }

    /**
     * @param inputEeIds
     * @param genes
     * @param stringency if set to 1, may be adjusted
     * @param maxResults per gene, not including the query genes themselves. Ignored if this is 'querygenesonly'
     * @param queryGenesOnly
     * @param quick
     * @return CoexpressionMetaValueObject, in which the results are already populated and sorted.
     */
    private CoexpressionMetaValueObject doCoexpressionSearch( Collection<Long> inputEeIds, Collection<Long> genes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean quick ) {
        if ( genes.isEmpty() ) {
            CoexpressionMetaValueObject r = new CoexpressionMetaValueObject();
            r.setErrorState( "No genes selected" );
            return r;
        }

        Taxon taxon = this.geneService.load( genes.iterator().next() ).getTaxon();
        List<ExpressionExperimentValueObject> eevos = getFilteredEEvos( inputEeIds, taxon );

        CoexpressionMetaValueObject result = initValueObject( genes, eevos );

        if ( eevos.isEmpty() ) {
            result = new CoexpressionMetaValueObject();
            result.setErrorState( "No experiments selected" );
            return result;
        }

        Collection<Long> eeIds = EntityUtils.getIds( eevos );

        Map<Long, List<CoexpressionValueObject>> allCoexpressions = new HashMap<>();

        // Note: auto-choose stringency on client size not always giving something reasonable. Also: not clear we want
        // to do this auto-adjust for 'query genes only'.

        if ( genes.size() > THRESHOLD_TRIGGER_QUERYGENESONLY ) {
            if ( !queryGenesOnly ) {
                log.info( "Switching to 'query genes only'" );
            }
            queryGenesOnly = true;
        }

        stringency = Math.max( stringency, chooseStringency( queryGenesOnly, eeIds.size(), genes.size() ) );

        assert stringency >= 1 || eeIds.size() == 1;

        log.info( "Stringency set to " + stringency + " based on number of experiments (" + eeIds.size()
                + ") and genes (" + genes.size() + ") queried" );

        // HACK drop the stringency until we get some results.
        int stepSize = 3;
        while ( true ) {
            if ( queryGenesOnly ) {
                // note that maxResults is ignored.
                if ( genes.size() < 2 ) {
                    throw new IllegalArgumentException( "cannot do inter-gene coexpression search with only one gene" );
                }
                allCoexpressions = coexpressionService.findInterCoexpressionRelationships( taxon, genes, eeIds,
                        stringency, quick );
            } else {
                allCoexpressions = coexpressionService.findCoexpressionRelationships( taxon, genes, eeIds, stringency,
                        maxResults, quick );
            }
            int minimum_stringency_for_requery = 2;

            if ( allCoexpressions.isEmpty() && stringency > minimum_stringency_for_requery ) {
                stringency -= stepSize; // step size completely made up.
                stringency = Math.max( minimum_stringency_for_requery, stringency ); // keep stringency at least 2.
                log.info( "No results, requerying with stringeny=" + stringency );
            } else {
                // no results.
                break;
            }
        }

        log.info( "Final actual stringency used was " + stringency );

        result.setQueryStringency( stringency );
        result.setQueryGenesOnly( queryGenesOnly );

        Set<Long> queryGeneIds = allCoexpressions.keySet();
        assert genes.containsAll( queryGeneIds );
        Map<Long, GeneValueObject> idMap = EntityUtils.getIdMap( geneService.loadValueObjects( queryGeneIds ) );

        int k = 0;
        for ( Long queryGene : queryGeneIds ) {

            Collection<CoexpressionValueObject> coexpressions = allCoexpressions.get( queryGene );

            List<CoexpressionValueObjectExt> results = addExtCoexpressionValueObjects( idMap.get( queryGene ),
                    coexpressions, stringency, queryGenesOnly, genes );

            // test for bug 4036
            // for ( CoexpressionValueObjectExt cvo : results ) {
            // assert cvo.getNumTestedIn() <= eevos.size() : "Expected max testedin of " + eevos.size() + " but got "
            // + cvo.getNumTestedIn() + " for query gene " + queryGene;
            // }

            result.getResults().addAll( results );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject( queryGene );
            summary.setDatasetsAvailable( eevos.size() );
            summary.setLinksFound( coexpressions.size() );

            result.getSummaries().put( queryGene, summary );

            if ( ++k % 100 == 0 ) {
                log.info( "Processed results for " + k + " query genes ..." );
            }

        }

        Collections.sort( result.getResults() );

        // FIXME we might want to suppress this in some situations
        result.trim();

        populateNodeDegrees( result );
        // if ( searchOptions.isUseMyDatasets() ) {
        // addMyDataFlag( result, myEE );
        // }

        return result;
    }

    /**
     * Populate node degree. Note that this ignores the datasets that were used in the query - the statistics are for
     * 'all' data sets.
     * 
     * @param result
     */
    public void populateNodeDegrees( CoexpressionMetaValueObject result ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<Long> allUsedGenes = new HashSet<>();
        for ( CoexpressionValueObjectExt coex : result.getResults() ) {
            allUsedGenes.add( coex.getQueryGene().getId() );
            allUsedGenes.add( coex.getFoundGene().getId() );
        }

        Map<Long, GeneCoexpressionNodeDegreeValueObject> nodeDegrees = coexpressionService
                .getNodeDegrees( allUsedGenes );

        for ( CoexpressionValueObjectExt coex : result.getResults() ) {

            // if node degree info is out of date.
            if ( !nodeDegrees.containsKey( coex.getQueryGene().getId() )
                    || !nodeDegrees.containsKey( coex.getFoundGene().getId() ) ) {
                continue;
            }

            GeneCoexpressionNodeDegreeValueObject queryGeneNodeDegree = nodeDegrees.get( coex.getQueryGene().getId() );
            GeneCoexpressionNodeDegreeValueObject foundGeneNodeDegree = nodeDegrees.get( coex.getFoundGene().getId() );

            boolean pos = coex.getNegSupp() == 0;

            coex.setQueryGeneNodeDegree( queryGeneNodeDegree.getLinksWithMinimumSupport( coex.getSupport(), pos ) );
            coex.setFoundGeneNodeDegree( foundGeneNodeDegree.getLinksWithMinimumSupport( coex.getSupport(), pos ) );

            coex.setQueryGeneNodeDegreeRank( queryGeneNodeDegree.getRankAtMinimumSupport( coex.getSupport(), pos ) );
            coex.setFoundGeneNodeDegreeRank( foundGeneNodeDegree.getRankAtMinimumSupport( coex.getSupport(), pos ) );

        }

        assert result.getSummaries() != null;
        for ( Long g : nodeDegrees.keySet() ) {
            if ( !result.getSummaries().containsKey( g ) ) continue;
            result.getSummaries().get( g ).setCoexpNodeDegree( nodeDegrees.get( g ) );
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Populate node degree for " + result.getResults().size() + ": " + timer.getTime() + "ms" );
        }
    }

    /**
     * Security-filter the experiments, remove troubled ones, and retain only those that have an analysis.
     * 
     * @param eeIds can be null, in which case all available (to user) IDs are gotten
     * @return
     */
    private List<ExpressionExperimentValueObject> getFilteredEEvos( Collection<Long> eeIds, Taxon taxon ) {
        List<ExpressionExperimentValueObject> securityFilteredEevos;
        if ( eeIds == null || eeIds.isEmpty() ) {
            // all valid experiments for taxon.
            securityFilteredEevos = new ArrayList<>( expressionExperimentService.loadValueObjects(
                    coexpressionAnalysisService.getExperimentsWithAnalysis( taxon ), false ) );
        } else {
            securityFilteredEevos = new ArrayList<>( expressionExperimentService.loadValueObjects(
                    coexpressionAnalysisService.getExperimentsWithAnalysis( eeIds ), false ) );
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
            log.info( "Filtering eevos took " + timerFilterTroubled.getTime() + "ms" );
        }

        return eevos;
    }

    /**
     * Use some rough heuristics (based on manual testing) to choose an initial stringency for queries. This value could
     * get adjusted upwards during postprocessing to limit the result set sizes. But this should be set as high as
     * possible to speed up the first stages.
     * 
     * @param queryGenesOnly
     * @param numExperimentsQueried
     * @return
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

        if ( queryGenesOnly ) baseline--;

        return ( int ) ( Math.ceil( baseline + expSlope * numExperimentsQueried ) );
    }

    /**
     * @param genes
     * @param eevos
     * @return
     */
    private CoexpressionMetaValueObject initValueObject( Collection<Long> genes,
            List<ExpressionExperimentValueObject> eevos ) {
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        result.setQueryGenes( new ArrayList<GeneValueObject>( geneService.loadValueObjectsLiter( genes ) ) );
        result.setResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setSummaries( new HashMap<Long, CoexpressionSummaryValueObject>() );
        result.setNumDatasetsQueried( eevos.size() );
        return result;
    }

}
