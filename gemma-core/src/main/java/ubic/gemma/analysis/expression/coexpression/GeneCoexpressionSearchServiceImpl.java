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
     * @param geneIds
     * @return results
     */
    private List<CoexpressionValueObjectExt> addExtCoexpressionValueObjects( GeneValueObject queryGene,
            Collection<CoexpressionValueObject> coexp, int stringency, boolean queryGenesOnly, Collection<Long> geneIds ) {

        List<CoexpressionValueObjectExt> results = new ArrayList<>();
        Collection<Long> coexpIds = new HashSet<Long>();
        for ( CoexpressionValueObject cvo : coexp ) {
            coexpIds.add( cvo.getCoexGeneId() );
        }

        Map<Long, GeneValueObject> coexpedGenes = EntityUtils.getIdMap( geneService.loadValueObjects( coexpIds ) );

        for ( CoexpressionValueObject cvo : coexp ) {

            /*
             * sanity check
             */
            if ( queryGenesOnly && !geneIds.contains( cvo.getCoexGeneId() ) ) {
                log.warn( "coexpression for non-query genes obtained unexpectedly when doing queryGenesOnly" );
                continue;
            }

            CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
            ecvo.setQueryGene( queryGene );
            ecvo.setFoundGene( coexpedGenes.get( cvo.getCoexGeneId() ) );

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
     * @param maxResults per gene, not including the query genes themselves.
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

        if ( queryGenesOnly ) {
            if ( genes.size() < 2 ) {
                throw new IllegalArgumentException( "cannot do inter-gene coexpression search with only one gene" );
            }
            allCoexpressions = coexpressionService.findInterCoexpressionRelationships( taxon, genes, eeIds, stringency,
                    quick );
        } else {
            if ( stringency == 1 ) stringency = chooseStringency( eeIds.size() );
            allCoexpressions = coexpressionService.findCoexpressionRelationships( taxon, genes, eeIds, stringency,
                    maxResults, quick );
        }

        result.setAppliedStringency( stringency );
        Set<Long> queryGeneIds = allCoexpressions.keySet();
        Map<Long, GeneValueObject> idMap = EntityUtils.getIdMap( geneService.loadValueObjects( queryGeneIds ) );

        for ( Long queryGene : queryGeneIds ) {

            Collection<CoexpressionValueObject> coexpressions = allCoexpressions.get( queryGene );

            List<CoexpressionValueObjectExt> results = addExtCoexpressionValueObjects( idMap.get( queryGene ),
                    coexpressions, stringency, queryGenesOnly, genes );

            // test for bug 4036
            for ( CoexpressionValueObjectExt cvo : results ) {
                assert cvo.getNumTestedIn() <= eevos.size() : "Expected max testedin of " + eevos.size() + " but got "
                        + cvo.getNumTestedIn() + " for query gene " + queryGene;
            }

            result.getResults().addAll( results );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject( queryGene );
            summary.setDatasetsAvailable( eevos.size() );
            summary.setLinksFound( coexpressions.size() );

            result.getSummaries().put( queryGene, summary );
        }

        Collections.sort( result.getResults() );

        populateNodeDegrees( result );

        return result;
    }

    /**
     * @param result
     */
    public void populateNodeDegrees( CoexpressionMetaValueObject result ) {
        /*
         * Populate node degree
         */
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

            coex.setQueryGeneNodeDegree( queryGeneNodeDegree.getLinksWithMinimumSupport( coex.getSupport() ) );
            coex.setFoundGeneNodeDegree( foundGeneNodeDegree.getLinksWithMinimumSupport( coex.getSupport() ) );

            coex.setQueryGeneNodeDegreeRank( queryGeneNodeDegree.getRankAtMinimumSupport( coex.getSupport() ) );
            coex.setFoundGeneNodeDegreeRank( foundGeneNodeDegree.getRankAtMinimumSupport( coex.getSupport() ) );

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
            log.info( "Filtering troubled eevos took " + timerFilterTroubled.getTime() + "ms" );
        }

        return eevos;
    }

    /**
     * If the user has not set a stringency higher than 1, we set it for them.
     * 
     * @param size
     * @return
     */
    private Integer chooseStringency( int size ) {
        // this is completely made up...
        if ( size < 5 ) {
            return 2;
        } else if ( size < 20 ) {
            return 3;
        } else if ( size < 50 ) {
            return 4;
        } else if ( size < 100 ) {
            return 6;
        } else if ( size < 200 ) {
            return 8;
        } else if ( size < 300 ) {
            return 10;
        } else if ( size < 400 ) {
            return 15;
        } else if ( size < 600 ) {
            return 20;
        } else if ( size < 800 ) {
            return 25;
        } else if ( size < 1000 ) {
            return 35;
        } else if ( size < 1200 ) {
            return 45;
        } else if ( size < 1500 ) {
            return 55;
        }
        return 65;
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
