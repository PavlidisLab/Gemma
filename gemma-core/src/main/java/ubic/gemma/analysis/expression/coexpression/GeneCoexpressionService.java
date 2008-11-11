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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.CountingMap;
import ubic.gemma.util.AnchorTagUtil;

/**
 * Provides access to Gene2Gene and Probe2Probe links. The use of this service provides 'high-level' access to
 * functionality in the Gene2GeneCoexpressionService and the ProbeLinkCoexpressionAnalyzer.
 * 
 * @author paul
 * @version $Id$
 * @spring.bean id="geneCoexpressionService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="gene2GeneCoexpressionService" ref="gene2GeneCoexpressionService"
 * @spring.property name = "geneOntologyService" ref="geneOntologyService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "probeLinkCoexpressionAnalyzer" ref="probeLinkCoexpressionAnalyzer"
 * @spring.property name="expressionExperimentSetService" ref="expressionExperimentSetService"
 * @spring.property name="geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 */
public class GeneCoexpressionService {

    /*
     * I assume the reason Genes weren't being loaded before is that it was too time consuming, so we'll do this
     * instead...
     */
    private static class SimpleGene extends Gene {
        public SimpleGene( Long id, String name, String officialName ) {
            super();
            this.setId( id );
            this.setOfficialSymbol( name );
            this.setOfficialName( officialName );
        }
    }

    private static Log log = LogFactory.getLog( GeneCoexpressionService.class.getName() );
    /**
     * How many genes to fill in the "expression experiments tested in" and "go overlap" info for.
     */
    private static final int NUM_GENES_TO_DETAIL = 100;
    GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;
    private ExpressionExperimentService expressionExperimentService;
    private ExpressionExperimentSetService expressionExperimentSetService;
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService;
    private GeneOntologyService geneOntologyService;
    private GeneService geneService;

    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    /**
     * This is the entry point for queries starting from a preset ExpressionExperimentSet.
     * 
     * @param eeSetId expressionExperimentSetId
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @param queryGenesOnly
     * @return
     */
    public CoexpressionMetaValueObject coexpressionSearch( Long eeSetId, Collection<Gene> queryGenes, int stringency,
            int maxResults, boolean queryGenesOnly ) {
        return getFilteredCannedAnalysisResults( eeSetId, null, queryGenes, stringency, maxResults, queryGenesOnly );
    }

    /**
     * Perform a "custom" analysis, using an ad-hoc set of expreriments. Note that if possible, the query will be done
     * using results from an ExpressionExperimentSet.
     * 
     * @param eeIds Expression experiments to consider
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @param forceProbeLevelSearch If a probe-level search should always be done. This is primarily a testing and
     *        debugging feature. If false, searches will be done using 'canned' results if possible.
     * @return
     */
    public CoexpressionMetaValueObject coexpressionSearch( Collection<Long> eeIds, Collection<Gene> genes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean forceProbeLevelSearch ) {

        if ( eeIds == null ) eeIds = new HashSet<Long>();
        Collection<BioAssaySet> ees = getPossibleExpressionExperiments( genes );

        if ( ees.isEmpty() ) {
            CoexpressionMetaValueObject r = new CoexpressionMetaValueObject();
            r.setErrorState( "Gene(s) are not tested in any experiments" );
            return r;
        }

        if ( !eeIds.isEmpty() ) {
            // remove the expression experiments we're not interested in...
            Collection<BioAssaySet> eesToRemove = new HashSet<BioAssaySet>();
            for ( BioAssaySet ee : ees ) {
                if ( !eeIds.contains( ee.getId() ) ) eesToRemove.add( ee );
            }
            ees.removeAll( eesToRemove );
        }

        /*
         * repopulate eeIds with the actual eeIds we'll be searching through and load ExpressionExperimentValueObjects
         * to get summary information about the datasets...
         */
        eeIds.clear();
        for ( BioAssaySet ee : ees ) {
            eeIds.add( ee.getId() );
        }

        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        CoexpressionMetaValueObject result = initValueObject( genes, eevos, false );

        if ( eeIds.isEmpty() ) {
            result = new CoexpressionMetaValueObject();
            result.setErrorState( "No experiments selected" );
            return result;
        }

        /*
         * If possible: instead of using the probeLinkCoexpressionAnalyzer, Use a canned analysis with a filter.
         */
        if ( !forceProbeLevelSearch ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.loadAll();

            for ( ExpressionExperimentSet eeSet : eeSets ) {

                Collection<Long> eeSetIds = new ArrayList<Long>();
                for ( BioAssaySet baSet : eeSet.getExperiments() ) {
                    eeSetIds.add( baSet.getId() );
                }

                if ( eeSetIds.containsAll( eeIds ) ) {
                    log.info( "Using canned analysis to conduct customized analysis" );
                    return getFilteredCannedAnalysisResults( eeSet.getId(), eeIds, genes, stringency, maxResults,
                            queryGenesOnly );
                }

                /*
                 * FIXME: if there is an analysis that contains 'most' of the experiments, it may be performant to split
                 * it up: get the expression results for some datasets from gene2gene, and some via probe-level query.
                 * This will be necessary to allow inclusion of 'new' data sets like ones uploaded by users, that aren't
                 * included in the gene2gene analysis. (Bug 1398, 1399)
                 */
            }
        }

        /*
         * If we get this far, there was no matching analysis so we do it using the probe2probe table. This is
         * relatively slow so should be avoided.
         */

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            // FIXME don't reuse this field.
            eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        }

        boolean knownGenesOnly = true; // !SecurityService.isUserAdmin();
        result.setKnownGenesOnly( knownGenesOnly );

        Collection<Long> geneIds = new HashSet<Long>( genes.size() );
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId() );
        }

        Map<Gene, CoexpressionCollectionValueObject> allCoexpressions = new HashMap<Gene, CoexpressionCollectionValueObject>();

        if ( genes.size() == 1 ) {
            Gene soleQueryGene = genes.iterator().next();
            allCoexpressions.put( soleQueryGene, probeLinkCoexpressionAnalyzer.linkAnalysis( soleQueryGene, ees,
                    stringency, knownGenesOnly, maxResults ) );
        } else {
            /*
             * Batch mode
             */
            allCoexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( genes, ees, stringency, knownGenesOnly,
                    queryGenesOnly, maxResults );
        }

        for ( Gene queryGene : allCoexpressions.keySet() ) {

            CoexpressionCollectionValueObject coexpressions = allCoexpressions.get( queryGene );

            result.setErrorState( coexpressions.getErrorState() );

            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getKnownGeneCoexpression(),
                    stringency, queryGenesOnly, geneIds, result.getKnownGeneResults(), result.getKnownGeneDatasets() );

            // FIXME only do this part if the user is logged in?
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getPredictedGeneCoexpression(), stringency, queryGenesOnly, geneIds, result
                    .getPredictedGeneResults(), result.getPredictedGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getProbeAlignedRegionCoexpression(), stringency, queryGenesOnly, geneIds, result
                    .getProbeAlignedRegionResults(), result.getProbeAlignedRegionDatasets() );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
            summary.setDatasetsAvailable( eevos.size() );
            summary.setDatasetsTested( coexpressions.getEesQueryTestedIn().size() );
            summary.setLinksFound( coexpressions.getNumKnownGenes() );
            summary.setLinksMetPositiveStringency( coexpressions.getKnownGeneCoexpression()
                    .getPositiveStringencyLinkCount() );
            summary.setLinksMetNegativeStringency( coexpressions.getKnownGeneCoexpression()
                    .getNegativeStringencyLinkCount() );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );
        }

        return result;
    }

    /**
     * Get coexpression results using a pure gene2gene query (without visiting the probe2probe tables. This is generally
     * faster, probably even if we're only interested in data from a subset of the exeriments.
     * 
     * @param eeSetId the base expression experimnent set to refer to for analysis results.
     * @param eeIds Experiments to limit the results to (can be null)
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly return links among the query genes only.
     * @return
     */
    private CoexpressionMetaValueObject getFilteredCannedAnalysisResults( Long eeSetId, Collection<Long> eeIds,
            Collection<Gene> queryGenes, int stringency, int maxResults, boolean queryGenesOnly ) {

        ExpressionExperimentSet baseSet = expressionExperimentSetService.load( eeSetId );

        if ( baseSet == null ) {
            throw new IllegalArgumentException( "No such expressionexperiment set with id=" + eeSetId );
        }

        /*
         * This set of links must be filtered to include those in the data sets being analyzed.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = getRawCoexpression( queryGenes, stringency, maxResults,
                queryGenesOnly );

        Collection<Long> eeIdsFromAnalysis = getIds( baseSet );

        /*
         * We get this prior to filtering so it matches the vectors stored with the analysis.
         */
        Map<Integer, Long> positionToIDMap = GeneLinkCoexpressionAnalyzer.getPositionToIdMap( eeIdsFromAnalysis );

        /*
         * Now we get the data sets we area actually concerned with.
         */
        Collection<Long> eeIdsTouse = null;
        if ( eeIds == null ) {
            eeIdsTouse = eeIdsFromAnalysis;
        } else {
            eeIdsTouse = eeIds;
        }

        List<Long> filteredEeIds = getSortedFilteredIdList( eeIdsTouse );

        // This sort is necessary.(?)
        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIdsTouse );

        CoexpressionMetaValueObject result = initValueObject( queryGenes, eevos, true );

        Collection<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Gene2GeneCoexpression> seen = new HashSet<Gene2GeneCoexpression>();

        // populate the value objects.
        for ( Gene queryGene : gg2gs.keySet() ) {
            geneService.thaw( queryGene );

            /*
             * For summary statistics
             */
            CountingMap<Long> supportCount = new CountingMap<Long>();
            Collection<Long> allSupportingDatasets = new HashSet<Long>();
            Collection<Long> allDatasetsWithSpecificProbes = new HashSet<Long>();
            Collection<Long> allTestedDataSets = new HashSet<Long>();

            int linksMetPositiveStringency = 0;
            int linksMetNegativeStringency = 0;

            Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );

            assert g2gs != null;

            for ( Gene2GeneCoexpression g2g : g2gs ) {
                Gene foundGene = g2g.getFirstGene().equals( queryGene ) ? g2g.getSecondGene() : g2g.getFirstGene();
                CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();

                Collection<Gene> geneToThaw = new ArrayList<Gene>();
                geneToThaw.add( foundGene );

                // The thaw needs to be done here because building the value object
                // calls methods that require the gene's info (setSortKey, hashCode)
               //  geneService.thaw( foundGene );
                ecvo.setQueryGene( queryGene );
                ecvo.setFoundGene( foundGene );

                Collection<Long> testingDatasets = GeneLinkCoexpressionAnalyzer.getTestedExperimentIds( g2g,
                        positionToIDMap );
                testingDatasets.retainAll( filteredEeIds );

                /*
                 * necesssary in case any were filtered out (for example, if this is a virtual analysis; or there were
                 * 'troubled' ees. Note that 'supporting' includes 'non-specific' if they were recorded by the analyzer.
                 */
                Collection<Long> supportingDatasets = GeneLinkCoexpressionAnalyzer.getSupportingExperimentIds( g2g,
                        positionToIDMap );

                // necessary in case any were filtered out.
                supportingDatasets.retainAll( filteredEeIds );

                ecvo.setSupportingExperiments( supportingDatasets );

                Collection<Long> specificDatasets = GeneLinkCoexpressionAnalyzer.getSpecificExperimentIds( g2g,
                        positionToIDMap );

                /*
                 * Specific probe EEids contains 1 even if the data set wasn't supporting.
                 */
                specificDatasets.retainAll( supportingDatasets );

                int numTestingDatasets = testingDatasets.size();
                int numSupportingDatasets = supportingDatasets.size();

                /*
                 * SANITY CHECKS
                 */
                assert specificDatasets.size() <= numSupportingDatasets;
                assert numTestingDatasets >= numSupportingDatasets;
                assert numTestingDatasets <= eevos.size();

                ecvo.setDatasetVector( getDatasetVector( supportingDatasets, testingDatasets, specificDatasets,
                        filteredEeIds ) );

                /*
                 * This check is necessary in case any data sets were filtered out. (i.e., we're not interested in the
                 * full set of data sets that were used in the original analysis.
                 */
                if ( numSupportingDatasets < stringency ) {
                    continue;
                }

                allTestedDataSets.addAll( testingDatasets );

                int supportFromSpecificProbes = specificDatasets.size();
                if ( g2g.getEffect() < 0 ) {
                    ecvo.setPosSupp( 0 );
                    ecvo.setNegSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        ecvo.setNonSpecNegSupp( numSupportingDatasets - supportFromSpecificProbes );

                    ++linksMetNegativeStringency;
                } else {
                    ecvo.setPosSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        ecvo.setNonSpecPosSupp( numSupportingDatasets - supportFromSpecificProbes );
                    ecvo.setNegSupp( 0 );
                    ++linksMetPositiveStringency;
                }
                ecvo.setSupportKey( Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );
                ecvo.setNumTestedIn( numTestingDatasets );

                for ( Long id : supportingDatasets ) {
                    supportCount.increment( id );
                }

                ecvo.setSortKey();

                /*
                 * This check prevents links from being shown twice when we do "among query genes". We don't skip
                 * entirely so we get the counts for the summary table populated correctly.
                 */
                if ( !seen.contains( g2g ) ) {
                    ecvos.add( ecvo );
                }

                seen.add( g2g );

                allSupportingDatasets.addAll( supportingDatasets );
                allDatasetsWithSpecificProbes.addAll( specificDatasets );
            }

            CoexpressionSummaryValueObject summary = makeSummary( eevos, allTestedDataSets,
                    allDatasetsWithSpecificProbes, linksMetPositiveStringency, linksMetNegativeStringency );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );

            generateDatasetSummary( eevos, result, supportCount, allSupportingDatasets, queryGene );

            /*
             * FIXME I'm lazy and rushed, so I'm using an existing field for this info; probably better to add another
             * field to the value object...
             */
            for ( ExpressionExperimentValueObject eevo : eevos ) {
                eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
            }

            getGoOverlap( ecvos, queryGene );
        }

        result.getKnownGeneResults().addAll( ecvos );
        return result;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setProbeLinkCoexpressionAnalyzer( ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer ) {
        this.probeLinkCoexpressionAnalyzer = probeLinkCoexpressionAnalyzer;
    }

    /**
     * Convert CoexpressionValueObject into CoexpressionValueObjectExt objects to be passed to the client for display.
     * This is used for probe-level queries.
     * 
     * @param queryGene
     * @param eevos
     * @param coexp
     * @param stringency
     * @param queryGenesOnly
     * @param geneIds
     * @param results object we are adding to
     * @param datasetResults
     */
    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperimentValueObject> eevos,
            CoexpressedGenesDetails coexp, int stringency, boolean queryGenesOnly, Collection<Long> geneIds,
            Collection<CoexpressionValueObjectExt> results, Collection<CoexpressionDatasetValueObject> datasetResults ) {

        for ( CoexpressionValueObject cvo : coexp.getCoexpressionData( stringency ) ) {
            if ( queryGenesOnly && !geneIds.contains( cvo.getGeneId() ) ) continue;
            CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
            ecvo.setQueryGene( queryGene );
            ecvo.setFoundGene( new SimpleGene( cvo.getGeneId(), cvo.getGeneName(), cvo.getGeneOfficialName() ) );

            ecvo.setPosSupp( cvo.getPositiveLinkSupport() );
            ecvo.setNegSupp( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( 10 * Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );

            /*
             * Fill in the support based on 'non-specific' probes.
             */
            if ( !cvo.getExpressionExperiments().isEmpty() ) {
                ecvo.setNonSpecPosSupp( getNonSpecificLinkCount( cvo.getEEContributing2PositiveLinks(), cvo
                        .getNonspecificEE() ) );
                ecvo.setNonSpecNegSupp( getNonSpecificLinkCount( cvo.getEEContributing2NegativeLinks(), cvo
                        .getNonspecificEE() ) );
            }

            ecvo.setNumTestedIn( cvo.getNumDatasetsTestedIn() );

            ecvo.setGoSim( cvo.getGoOverlap() != null ? cvo.getGoOverlap().size() : 0 );
            ecvo.setMaxGoSim( cvo.getPossibleOverlap() );

            StringBuilder datasetVector = new StringBuilder();
            Collection<Long> supportingEEs = new ArrayList<Long>();

            for ( int i = 0; i < eevos.size(); ++i ) {
                ExpressionExperimentValueObject eevo = eevos.get( i );

                boolean tested = cvo.getDatasetsTestedIn() != null && cvo.getDatasetsTestedIn().contains( eevo.getId() );

                assert cvo.getExpressionExperiments().size() <= cvo.getPositiveLinkSupport()
                        + cvo.getNegativeLinkSupport() : "got " + cvo.getExpressionExperiments().size() + " expected "
                        + ( cvo.getPositiveLinkSupport() + cvo.getNegativeLinkSupport() );

                boolean supported = cvo.getExpressionExperiments().contains( eevo.getId() );

                boolean specific = !cvo.getNonspecificEE().contains( eevo.getId() );

                if ( supported ) {
                    if ( specific ) {
                        datasetVector.append( "3" );
                    } else {
                        datasetVector.append( "2" );
                    }
                    supportingEEs.add( eevo.getId() );
                } else if ( tested ) {
                    datasetVector.append( "1" );
                } else {
                    datasetVector.append( "0" );
                }
            }

            ecvo.setDatasetVector( datasetVector.toString() );
            ecvo.setSupportingExperiments( supportingEEs );

            ecvo.setSortKey();
            results.add( ecvo );
        }

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            if ( !coexp.getExpressionExperimentIds().contains( eevo.getId() ) ) continue;
            ExpressionExperimentValueObject coexpEevo = coexp.getExpressionExperiment( eevo.getId() );
            if ( coexpEevo == null ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
            ecdvo.setCoexpressionLinkCount( coexp.getLinkCountForEE( coexpEevo.getId() ) );
            ecdvo.setRawCoexpressionLinkCount( coexp.getRawLinkCountForEE( coexpEevo.getId() ) );
            ecdvo.setProbeSpecificForQueryGene( coexpEevo.getHasProbeSpecificForQueryGene() );
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            datasetResults.add( ecdvo );
        }
    }

    /**
     * This is necessary in case there is more than one gene2gene analysis in the system. The common case is when a new
     * analysis is in progress. Only one analysis should be enableed at any given time.
     * 
     * @param queryGenes
     * @return
     */
    private GeneCoexpressionAnalysis findEnabledCoexpressionAnalysis( Collection<Gene> queryGenes ) {
        GeneCoexpressionAnalysis gA = null;
        Gene g = queryGenes.iterator().next();
        // note: we assume they all come from one taxon.
        Taxon t = g.getTaxon();
        Collection<? extends Analysis> analyses = geneCoexpressionAnalysisService.findByTaxon( t );
        if ( analyses.size() == 0 ) {
            throw new IllegalStateException( "No gene coexpression analysis is available for " + t.getScientificName() );
        } else if ( analyses.size() == 1 ) {
            gA = ( GeneCoexpressionAnalysis ) analyses.iterator().next();
        } else {
            for ( Analysis analysis : analyses ) {
                GeneCoexpressionAnalysis c = ( GeneCoexpressionAnalysis ) analysis;
                if ( c.getEnabled() ) {
                    if ( gA == null ) {
                        gA = c;
                    } else {
                        throw new IllegalStateException(
                                "System should only have a single gene2gene coexpression analysis enabled per taxon, found more than one for "
                                        + t );
                    }
                }
            }
        }
        return gA;
    }

    /**
     * @param eevos
     * @param result
     * @param supportCount
     * @param supportingExperimentIds
     * @param queryGene
     */
    private void generateDatasetSummary( List<ExpressionExperimentValueObject> eevos,
            CoexpressionMetaValueObject result, CountingMap<Long> supportCount,
            Collection<Long> supportingExperimentIds, Gene queryGene ) {
        /*
         * generate dataset summary info for this query gene...
         */
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            if ( !supportingExperimentIds.contains( eevo.getId() ) ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
            ecdvo.setCoexpressionLinkCount( supportCount.get( eevo.getId() ).longValue() );
            ecdvo.setRawCoexpressionLinkCount( null ); // not available
            ecdvo.setProbeSpecificForQueryGene( true ); // only specific probes in these results
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            result.getKnownGeneDatasets().add( ecdvo );
        }
    }

    /**
     * @param supporting
     * @param testing
     * @param allIds
     * @return String representation of binary vector (might as well be a string, as it gets sent to the browser that
     *         way). 0 = not tested; 1 = tested but not supporting; 2 = supporting but not specific; 3 supporting and
     *         specific.
     */
    private String getDatasetVector( Collection<Long> supporting, Collection<Long> testing, Collection<Long> specific,
            List<Long> allIds ) {
        StringBuilder datasetVector = new StringBuilder();
        for ( Long id : allIds ) {
            boolean tested = testing.contains( id );
            boolean supported = supporting.contains( id );
            boolean s = specific.contains( id );

            if ( supported ) {
                if ( s ) {
                    datasetVector.append( "3" );
                } else {
                    datasetVector.append( "2" );
                }
            } else if ( tested ) {
                datasetVector.append( "1" );
            } else {
                datasetVector.append( "0" );
            }
        }
        return datasetVector.toString();
    }

    /**
     * @param ecvos
     * @param queryGene
     */
    private void getGoOverlap( Collection<CoexpressionValueObjectExt> ecvos, Gene queryGene ) {
        /*
         * get GO overlap info for this query gene...
         */
        if ( geneOntologyService.isGeneOntologyLoaded() ) {
            int numQueryGeneGoTerms = geneOntologyService.getGOTerms( queryGene ).size();
            Collection<Long> overlapIds = new ArrayList<Long>();
            int i = 0;
            for ( CoexpressionValueObjectExt ecvo : ecvos ) {
                overlapIds.add( ecvo.getFoundGene().getId() );
                if ( i++ > NUM_GENES_TO_DETAIL ) break;
            }
            Map<Long, Collection<OntologyTerm>> goOverlap = geneOntologyService.calculateGoTermOverlap( queryGene,
                    overlapIds );
            for ( CoexpressionValueObjectExt ecvo : ecvos ) {
                ecvo.setMaxGoSim( numQueryGeneGoTerms );
                Collection<OntologyTerm> overlap = goOverlap.get( ecvo.getFoundGene().getId() );
                ecvo.setGoSim( overlap == null ? 0 : overlap.size() );
            }
        }
    }

    /**
     * @param expressionExperimentSet
     */
    private List<Long> getIds( ExpressionExperimentSet expressionExperimentSet ) {
        List<Long> ids = new ArrayList<Long>( expressionExperimentSet.getExperiments().size() );
        for ( Securable dataset : expressionExperimentSet.getExperiments() ) {
            ids.add( dataset.getId() );
        }
        return ids;
    }

    /**
     * @param contributingEEs
     * @param nonSpecificEEs
     * @return
     */
    private int getNonSpecificLinkCount( Collection<Long> contributingEEs, Collection<Long> nonSpecificEEs ) {
        int n = 0;
        for ( Long id : contributingEEs ) {
            if ( nonSpecificEEs.contains( id ) ) ++n;
        }
        return n;
    }

    /**
     * @param genes
     * @return
     */
    private Collection<BioAssaySet> getPossibleExpressionExperiments( Collection<Gene> genes ) {
        Collection<BioAssaySet> result = new HashSet<BioAssaySet>();
        if ( genes.isEmpty() ) {
            return result;
        }

        for ( Gene g : genes ) {
            result.addAll( expressionExperimentService.findByGene( g ) );
        }
        if ( result.size() == 0 ) {
            log.warn( "No datasets for gene. If this is unexpected, check that the GENE2CS table is up to date." );
        }
        return result;
    }

    /**
     * Retrieve all gene2gene coexpression information for the genes at the specified stringency, using methods that
     * don't filter by experiment.
     * 
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<Gene, Collection<Gene2GeneCoexpression>> getRawCoexpression( Collection<Gene> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly ) {
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        if ( queryGenes.size() == 0 ) {
            return gg2gs;
        }

        GeneCoexpressionAnalysis gA = findEnabledCoexpressionAnalysis( queryGenes );

        if ( queryGenesOnly ) {
            gg2gs = gene2GeneCoexpressionService.findInterCoexpressionRelationship( queryGenes, stringency, gA );
        } else {
            gg2gs = gene2GeneCoexpressionService.findCoexpressionRelationships( queryGenes, stringency, maxResults, gA );
        }
        return gg2gs;
    }

    /**
     * @param eeIds
     * @return
     */
    private List<ExpressionExperimentValueObject> getSortedEEvos( Collection<Long> eeIds ) {
        List<ExpressionExperimentValueObject> eevos = new ArrayList<ExpressionExperimentValueObject>(
                expressionExperimentService.loadValueObjects( eeIds ) );
        Collections.sort( eevos, new Comparator<ExpressionExperimentValueObject>() {
            public int compare( ExpressionExperimentValueObject eevo1, ExpressionExperimentValueObject eevo2 ) {
                return eevo1.getId().compareTo( eevo2.getId() );
            }
        } );
        return eevos;
    }

    /**
     * Remove data sets that are 'troubled' and sort the list.
     * 
     * @param datasets
     * @return
     */
    private List<Long> getSortedFilteredIdList( Collection<Long> datasets ) {

        removeTroubledEes( datasets );

        List<Long> ids = new ArrayList<Long>( datasets );
        Collections.sort( ids );
        return ids;
    }

    /**
     * @param genes
     * @param eevos
     * @param isCanned
     * @return
     */
    private CoexpressionMetaValueObject initValueObject( Collection<Gene> genes,
            List<ExpressionExperimentValueObject> eevos, boolean isCanned ) {
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        result.setQueryGenes( new ArrayList<Gene>( genes ) );
        result.setDatasets( eevos );
        result.setIsCannedAnalysis( isCanned );
        result.setKnownGeneDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setPredictedGeneDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setSummary( new HashMap<String, CoexpressionSummaryValueObject>() );
        return result;
    }

    /**
     * @param eevos
     * @param datasetsTested
     * @param datasetsWithSpecificProbes
     * @param linksMetPositiveStringency
     * @param linksMetNegativeStringency
     * @return
     */
    private CoexpressionSummaryValueObject makeSummary( List<ExpressionExperimentValueObject> eevos,
            Collection<Long> datasetsTested, Collection<Long> datasetsWithSpecificProbes,
            int linksMetPositiveStringency, int linksMetNegativeStringency ) {
        CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
        summary.setDatasetsAvailable( eevos.size() );
        summary.setDatasetsTested( datasetsTested.size() );
        summary.setDatasetsWithSpecificProbes( datasetsWithSpecificProbes.size() );
        summary.setLinksFound( linksMetPositiveStringency + linksMetNegativeStringency );
        summary.setLinksMetPositiveStringency( linksMetPositiveStringency );
        summary.setLinksMetNegativeStringency( linksMetNegativeStringency );
        return summary;
    }

    /**
     * FIXME partly duplicates code from ExpressionExperimentManipulatingCLI
     * 
     * @param ees
     */
    @SuppressWarnings("unchecked")
    private void removeTroubledEes( Collection<Long> ees ) {
        if ( ees == null || ees.size() == 0 ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }

        int size = ees.size();
        final Map<Long, AuditEvent> trouble = expressionExperimentService.getLastTroubleEvent( ees );
        CollectionUtils.filter( ees, new Predicate() {
            public boolean evaluate( Object id ) {
                boolean hasTrouble = trouble.containsKey( id );
                return !hasTrouble;
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving " + newSize );
        }
    }

}
