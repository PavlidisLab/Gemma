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

import net.sf.ehcache.Element;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.loader.protein.ProteinLinkOutFormatter;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressedGenePairValueObject;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationService;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.model.association.TfGeneAssociationService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.common.auditAndSecurity.Status;
import ubic.gemma.model.common.auditAndSecurity.StatusService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneLightWeightCache;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.util.AnchorTagUtil;
import ubic.gemma.util.EntityUtils;

/**
 * Provides access to Gene2Gene and Probe2Probe links. The use of this service provides 'high-level' access to
 * functionality in the Gene2GeneCoexpressionService and the ProbeLinkCoexpressionAnalyzer.
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Lazy
public class GeneCoexpressionServiceImpl implements GeneCoexpressionService {

    private static Log log = LogFactory.getLog( GeneCoexpressionServiceImpl.class.getName() );

    /**
     * How many genes to fill in the "go overlap" info for.
     */
    private static final int NUM_GENES_TO_DETAIL = 25;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService;

    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService = null;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    @Autowired
    private TfGeneAssociationService tfGeneAssociationService;

    @Autowired
    private StatusService statusService;
    
    @Autowired
    private GeneLightWeightCache geneLightWeightCache;
    
    
    public GeneLightWeightCache getGeneLightWeightCache(){
    	return geneLightWeightCache;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService#coexpressionSearch(java.util.Collection,
     * java.util.Collection, int, int, boolean, boolean)
     */
    @Override
    public CoexpressionMetaValueObject coexpressionSearch( Collection<Long> inputEeIds, Collection<Gene> genes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean forceProbeLevelSearch ) {

        if ( genes.isEmpty() ) {
            CoexpressionMetaValueObject r = new CoexpressionMetaValueObject();
            r.setErrorState( "No genes selected" );
            return r;
        }

        /*
         * repopulate eeIds with the actual eeIds we'll be searching through and load ExpressionExperimentValueObjects
         * to get summary information about the datasets...
         */
        Collection<? extends BioAssaySet> ees = expressionExperimentService.loadMultiple( inputEeIds );
        Collection<Long> eeIds = EntityUtils.getIds( ees );

        /*
         * If possible: instead of using the probeLinkCoexpressionAnalyzer, Use a canned analysis with a filter.
         */
        if ( !forceProbeLevelSearch ) {
            Taxon taxon = genes.iterator().next().getTaxon();
            ExpressionExperimentSet eeSet = geneCoexpressionAnalysisService.findCurrent( taxon )
                    .getExpressionExperimentSetAnalyzed();

            if ( eeSet != null
                    && EntityUtils.getIds( expressionExperimentSetService.getExperimentsInSet( eeSet.getId() ) )
                            .containsAll( eeIds ) ) {
                return getFilteredCannedAnalysisResults( eeSet, eeIds, genes, stringency, maxResults, queryGenesOnly );
            }
        }

        /*
         * If we get this far, there was no matching analysis so we do it using the probe2probe table. This is
         * relatively slow so should be avoided.
         */
        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        CoexpressionMetaValueObject result = initValueObject( genes, eevos, false );

        if ( eeIds.isEmpty() ) {
            result = new CoexpressionMetaValueObject();
            result.setErrorState( "No experiments selected" );
            return result;
        }

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            // FIXME don't reuse this field.
            eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        }

        boolean knownGenesOnly = true; // used to be:
                                       // !SecurityService.isUserAdmin();
        result.setKnownGenesOnly( knownGenesOnly );

        Collection<Long> geneIds = new HashSet<Long>( genes.size() );
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId() );
        }

        Map<Gene, QueryGeneCoexpression> allCoexpressions = new HashMap<Gene, QueryGeneCoexpression>();

        if ( genes.size() == 1 ) {
            Gene soleQueryGene = genes.iterator().next();
            allCoexpressions.put( soleQueryGene,
                    probeLinkCoexpressionAnalyzer.linkAnalysis( soleQueryGene, ees, stringency, maxResults ) );
        } else {
            /*
             * Batch mode
             */
            allCoexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( genes, ees, stringency, queryGenesOnly,
                    maxResults );
        }

        Collection<Long> allUsedGenes = new HashSet<Long>();

        for ( Gene queryGene : allCoexpressions.keySet() ) {

            allUsedGenes.add( queryGene.getId() );

            QueryGeneCoexpression coexpressions = allCoexpressions.get( queryGene );

            result.setErrorState( coexpressions.getErrorState() );

            // fill in the protein interaction details if present
            Map<Long, Gene2GeneProteinAssociation> proteinInteractionsForQueryGene = this
                    .getGene2GeneProteinAssociationForQueryGene( queryGene );

            addExtCoexpressionValueObjects( queryGene, eevos, coexpressions, stringency, queryGenesOnly, geneIds,
                    result.getKnownGeneResults(), result.getKnownGeneDatasets(), proteinInteractionsForQueryGene );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
            summary.setDatasetsAvailable( eevos.size() );
            summary.setDatasetsTested( coexpressions.getNumDataSetsQueryGeneTestedIn() );
            summary.setLinksFound( coexpressions.getNumberOfGenes() );
            summary.setLinksMetPositiveStringency( coexpressions.getPositiveStringencyLinkCount() );
            summary.setLinksMetNegativeStringency( coexpressions.getNegativeStringencyLinkCount() );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );
            coexpressions.finalize();
        }

        Collection<Long> allSupportingDatasets = new HashSet<Long>();
        for ( CoexpressionValueObjectExt c : result.getKnownGeneResults() ) {
            allSupportingDatasets.addAll( c.getSupportingExperiments() );
            allUsedGenes.add( c.getFoundGene().getId() );
        }

        if ( !allSupportingDatasets.isEmpty() ) {

            Map<Gene, GeneCoexpressionNodeDegree> geneNodeDegrees = geneService
                    .getGeneCoexpressionNodeDegree( geneService.loadMultiple( allUsedGenes ) );

            Map<Long, Gene> idMap = EntityUtils.getIdMap( geneNodeDegrees.keySet() );

            for ( CoexpressionValueObjectExt c : result.getKnownGeneResults() ) {

                GeneCoexpressionNodeDegree queryGeneNodeDegree = geneNodeDegrees.get( idMap.get( c.getQueryGene()
                        .getId() ) );
                if ( queryGeneNodeDegree.getNumTests() < 20 ) {
                    c.setQueryGeneNodeDegree( 0.5 );
                } else {
                    c.setQueryGeneNodeDegree( queryGeneNodeDegree.getRank() );
                }

                GeneCoexpressionNodeDegree foundGeneNodeDegree = geneNodeDegrees.get( idMap.get( c.getFoundGene()
                        .getId() ) );
                if ( foundGeneNodeDegree.getNumTests() < 20 ) {
                    c.setQueryGeneNodeDegree( 0.5 );
                } else {
                    c.setFoundGeneNodeDegree( foundGeneNodeDegree.getRank() );
                }
            }
        } else {
            for ( CoexpressionValueObjectExt c : result.getKnownGeneResults() ) {
                c.setQueryGeneNodeDegree( 0d );
                c.setFoundGeneNodeDegree( 0d );
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService#coexpressionSearchQuick(java.util.Collection,
     * java.util.Collection, int, int, boolean, boolean)
     */
    @Override
    public Collection<CoexpressionValueObjectExt> coexpressionSearchQuick( Collection<Long> inputEeIds,
            Collection<Gene> genes, int stringency, int maxResults, boolean queryGenesOnly, boolean skipDetails ) {

        if ( genes.isEmpty() ) {
            return new HashSet<CoexpressionValueObjectExt>();
        }

        /*
         * If possible: instead of using the probeLinkCoexpressionAnalyzer, Use a canned analysis with a filter.
         */
        Taxon taxon = genes.iterator().next().getTaxon();
        ExpressionExperimentSet eeSet = geneCoexpressionAnalysisService.findCurrent( taxon )
                .getExpressionExperimentSetAnalyzed();

        assert !inputEeIds.isEmpty();

        return getFilteredCannedAnalysisResults2( eeSet, inputEeIds, genes, stringency, maxResults, queryGenesOnly,
                skipDetails );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService#coexpressionSearchQuick(java.lang.Long,
     * java.util.Collection, int, int, boolean, boolean)
     */
    @Override
    public Collection<CoexpressionValueObjectExt> coexpressionSearchQuick( Long eeSetId, Collection<Gene> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean skipDetails ) {

        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( eeSetId );
        expressionExperimentSetService.thaw( eeSet );
        Collection<Long> allEEIdsInSet = EntityUtils.getIds( eeSet.getExperiments() );

        return coexpressionSearchQuick( allEEIdsInSet, queryGenes, stringency, maxResults, queryGenesOnly, skipDetails );
    }

    /**
     * For a given query gene retrieve it's protein protein interactions. Iterating through those interactions create a
     * map keyed on the gene association that was retreived for that given gene. E.g. query gene 'AB' has interactions
     * with 'BB' and 'CC' then create a map using the ids as keys from BB and CC. and the value using the String url for
     * that interaction
     * 
     * @param gene The gene to find associations for
     * @return Map of gene ids and their protein protein interactions
     */
    protected Map<Long, Gene2GeneProteinAssociation> getGene2GeneProteinAssociationForQueryGene( Gene gene ) {
        Map<Long, Gene2GeneProteinAssociation> stringUrlsMappedByGeneID = new HashMap<Long, Gene2GeneProteinAssociation>();
        Collection<Gene2GeneProteinAssociation> proteinInteractions = this.gene2GeneProteinAssociationService
                .findProteinInteractionsForGene( gene );
        // check if found any interactions
        if ( proteinInteractions != null && !proteinInteractions.isEmpty() ) {

            for ( Gene2GeneProteinAssociation proteinInteraction : proteinInteractions ) {
                gene2GeneProteinAssociationService.thaw( proteinInteraction );
                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + proteinInteraction.getFirstGene() + " and "
                            + proteinInteraction.getSecondGene() );
                }

                if ( proteinInteraction.getDatabaseEntry() != null
                        && proteinInteraction.getSecondGene().getId() != null
                        && proteinInteraction.getFirstGene().getId() != null ) {
                    // can append extra details to link if required this
                    // formating code should be somewhere else?

                    if ( proteinInteraction.getFirstGene().getId().equals( gene.getId() ) ) {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getSecondGene().getId(), proteinInteraction );
                    } else {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getFirstGene().getId(), proteinInteraction );
                    }
                }
            }
        }
        return stringUrlsMappedByGeneID;

    }

    /**
     * @param gene which is to be treated as a "target"
     * @return map of the transcription factor to the interaction details
     */
    protected Map<Long, TfGeneAssociation> getTfGeneAssociationsforTargetGene( Gene gene ) {
        Map<Long, TfGeneAssociation> associationsMappedByGeneId = new HashMap<Long, TfGeneAssociation>();
        Collection<? extends TfGeneAssociation> interactions = this.tfGeneAssociationService.findByTargetGene( gene );

        if ( interactions != null && !interactions.isEmpty() ) {

            for ( TfGeneAssociation interaction : interactions ) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + interaction.getFirstGene() + " and "
                            + interaction.getSecondGene() );
                }

                associationsMappedByGeneId.put( interaction.getFirstGene().getId(), interaction );

            }
        }
        return associationsMappedByGeneId;

    }

    /**
     * @param gene which is to be treated as a "transcription factor"
     * @return map of the target genes to the interaction details.
     */
    protected Map<Long, TfGeneAssociation> getTfGeneAssociationsforTf( Gene gene ) {
        Map<Long, TfGeneAssociation> associationsMappedByGeneId = new HashMap<Long, TfGeneAssociation>();
        Collection<? extends TfGeneAssociation> interactions = this.tfGeneAssociationService.findByTf( gene );
        if ( interactions != null && !interactions.isEmpty() ) {

            for ( TfGeneAssociation interaction : interactions ) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "found interaction for gene " + interaction.getFirstGene() + " and "
                            + interaction.getSecondGene() );
                }
                associationsMappedByGeneId.put( interaction.getSecondGene().getId(), interaction );
            }
        }
        return associationsMappedByGeneId;
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
     * @param proteinInteractionsForQueryGene map keyed on geneid of string url for protein interaction
     */
    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperimentValueObject> eevos,
            QueryGeneCoexpression coexp, int stringency, boolean queryGenesOnly, Collection<Long> geneIds,
            Collection<CoexpressionValueObjectExt> results, Collection<CoexpressionDatasetValueObject> datasetResults,
            Map<Long, Gene2GeneProteinAssociation> proteinInteractionsForQueryGene ) {

        Collection<Long> coexpIds = new HashSet<Long>();
        for ( CoexpressedGenePairValueObject cvo : coexp.getCoexpressionData( stringency ) ) {
            coexpIds.add( cvo.getCoexpressedGeneId() );
        }
        Map<Long, GeneValueObject> coexpedGenes = EntityUtils.getIdMap( geneService.loadValueObjects( coexpIds ) );

        for ( CoexpressedGenePairValueObject cvo : coexp.getCoexpressionData( stringency ) ) {
            if ( queryGenesOnly && !geneIds.contains( cvo.getCoexpressedGeneId() ) ) continue;

            CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
            ecvo.setQueryGene( new GeneValueObject( queryGene ) );
            ecvo.setFoundGene( coexpedGenes.get( cvo.getCoexpressedGeneId() ) ); // FIXME too slow,

            ecvo.setPosSupp( cvo.getPositiveLinkSupport() );
            ecvo.setNegSupp( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( 10 * Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );

            // if there are some protein protein interactions for this gene see
            // if the given coexpressed gene is in the
            // map of interactions and if so get the value for the URL.
            if ( proteinInteractionsForQueryGene != null && !( proteinInteractionsForQueryGene.isEmpty() ) ) {
                Gene2GeneProteinAssociation proteinProteinInteraction = proteinInteractionsForQueryGene.get( cvo
                        .getCoexpressedGeneId() );
                this.addProteinDetailsToValueObject( proteinProteinInteraction, ecvo );
            }
            /*
             * Fill in the support based on 'non-specific' probes.
             */
            if ( !cvo.getExpressionExperiments().isEmpty() ) {
                ecvo.setNonSpecPosSupp( getNonSpecificLinkCount( cvo.getEEContributing2PositiveLinks(),
                        cvo.getNonspecificEE() ) );
                ecvo.setNonSpecNegSupp( getNonSpecificLinkCount( cvo.getEEContributing2NegativeLinks(),
                        cvo.getNonspecificEE() ) );
            }

            ecvo.setNumTestedIn( cvo.getNumDatasetsTestedIn() );

            StringBuilder datasetVector = new StringBuilder();
            Collection<Long> supportingEEs = new ArrayList<Long>();

            for ( int i = 0; i < eevos.size(); ++i ) {
                ExpressionExperimentValueObject eevo = eevos.get( i );

                Long eeid = eevo.getId();

                // this information will not be filled in if the cvo was built in 'batch' mode, but that's not the case,
                // here.
                boolean tested = cvo.getDatasetsTestedIn() != null && cvo.getDatasetsTestedIn().contains( eeid );

                assert cvo.getExpressionExperiments().size() <= cvo.getPositiveLinkSupport()
                        + cvo.getNegativeLinkSupport() : "got " + cvo.getExpressionExperiments().size() + " expected "
                        + ( cvo.getPositiveLinkSupport() + cvo.getNegativeLinkSupport() );

                boolean supported = cvo.getExpressionExperiments().contains( eeid );

                boolean specific = !cvo.getNonspecificEE().contains( eeid );

                if ( supported ) {
                    if ( specific ) {
                        datasetVector.append( "3" );
                    } else {
                        datasetVector.append( "2" );
                    }
                    supportingEEs.add( eeid );
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
            if ( !coexp.getDataSetsQueryGeneTestedIn().contains( eevo.getId() ) ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );

            // NOTE should be accurate (probe-level query) but we won't show it.
            // See bug 1564 FIXME
            // ecdvo.setProbeSpecificForQueryGene( coexpEevo.getHasProbeSpecificForQueryGene() );
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            datasetResults.add( ecdvo );
        }
    }

    /**
     * Adds the protein protein interaction data to the value object, that is the url link for string the evidence for
     * that interaction and the confidence score.
     * 
     * @param proteinProteinInteraction Protein Protein interaction for the coexpression link
     * @param cvo The value object used to display coexpression data
     */
    private void addProteinDetailsToValueObject( Gene2GeneProteinAssociation proteinProteinInteraction,
            CoexpressionValueObjectExt cvo ) {

        if ( proteinProteinInteraction == null ) return;

        ProteinLinkOutFormatter proteinFormatter = new ProteinLinkOutFormatter();
        String proteinProteinIdUrl = proteinFormatter
                .getStringProteinProteinInteractionLinkGemmaDefault( proteinProteinInteraction.getDatabaseEntry() );

        String evidenceText = proteinFormatter.getEvidenceDisplayText( proteinProteinInteraction.getEvidenceVector() );

        String confidenceText = proteinFormatter.getConfidenceScoreAsPercentage( proteinProteinInteraction
                .getConfidenceScore() );

        log.debug( "A coexpression link in GEMMA has a interaction in STRING " + proteinProteinIdUrl + " evidence of "
                + evidenceText );

        cvo.setGene2GeneProteinAssociationStringUrl( proteinProteinIdUrl );
        cvo.setGene2GeneProteinInteractionConfidenceScore( confidenceText );
        cvo.setGene2GeneProteinInteractionEvidence( evidenceText );

    }

    /**
     * @param tfGeneAssociation
     * @param cvo
     */
    private void addTfInteractionToValueObject( TfGeneAssociation tfGeneAssociation, CoexpressionValueObjectExt cvo ) {
        if ( tfGeneAssociation == null ) return;

        if ( tfGeneAssociation.getFirstGene().getId().equals( cvo.getQueryGene().getId() ) ) {
            cvo.setQueryRegulatesFound( true );
        } else if ( tfGeneAssociation.getFirstGene().getId().equals( cvo.getFoundGene().getId() ) ) {
            cvo.setFoundRegulatesQuery( true );
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * This is necessary in case there is more than one gene2gene analysis in the system. The common case is when a new
     * analysis is in progress. Only one analysis should be enabled at any given time.
     * 
     * @param queryGenes
     * @return
     */
    private GeneCoexpressionAnalysis findEnabledCoexpressionAnalysis( Collection<Gene> queryGenes ) {

        GeneCoexpressionAnalysis gA = null;
        Gene g = queryGenes.iterator().next();
        // note: we assume they all come from one taxon.
        Taxon t = g.getTaxon();
        Collection<? extends Analysis> analyses = null;
        // check if the taxon is a species if it is not then it is a parent
        // taxon and need to get child taxa
        // coexpression analyses.
        if ( !t.getIsSpecies() ) {
            analyses = geneCoexpressionAnalysisService.findByParentTaxon( t );
        } else {
            analyses = geneCoexpressionAnalysisService.findByTaxon( t );
        }

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
            ecdvo.setProbeSpecificForQueryGene( true ); // we shouldn't display
                                                        // this. See bug 1564.
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            result.getKnownGeneDatasets().add( ecdvo );
        }
    }

    /**
     * @param supporting
     * @param testing
     * @param specific
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
     * Get coexpression results using a pure gene2gene query (without visiting the probe2probe tables. This is generally
     * faster, probably even if we're only interested in data from a subset of the experiments.
     * 
     * @param baseSet
     * @param eeIds Experiments to limit the results to (must not be null, and should already be security-filtered)
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly return links among the query genes only.
     * @return
     */
    private CoexpressionMetaValueObject getFilteredCannedAnalysisResults( ExpressionExperimentSet baseSet,
            Collection<Long> eeIds, Collection<Gene> queryGenes, int stringency, int maxResults, boolean queryGenesOnly ) {

        if ( queryGenes.isEmpty() ) {
            throw new IllegalArgumentException( "No genes in query" );
        }

        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        if ( eevos.isEmpty() ) {
            throw new IllegalArgumentException( "There are no usable experiments in the selected set" );
        }

        /*
         * We get this prior to filtering so it matches the vectors stored with the analysis.
         */
        expressionExperimentSetService.thaw( baseSet );
        List<Long> positionToIDMap = Gene2GenePopulationServiceImpl.getPositionToIdMap( EntityUtils.getIds( baseSet
                .getExperiments() ) );

        /*
         * This set of links must be filtered to include those in the data sets being analyzed.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = getRawCoexpression( queryGenes, stringency, maxResults,
                queryGenesOnly );

        List<Long> filteredEeIds = ( List<Long> ) EntityUtils.getIds( eevos );

        CoexpressionMetaValueObject result = initValueObject( queryGenes, eevos, true );

        List<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Gene2GeneCoexpression> seen = new HashSet<Gene2GeneCoexpression>();

        queryGenes = geneService.thawLite( gg2gs.keySet() );

        // populate the value objects.
        StopWatch timer = new StopWatch();
        Collection<Gene> allUsedGenes = new HashSet<Gene>();
        for ( Gene queryGene : queryGenes ) {
            timer.start();

            if ( !queryGene.getTaxon().equals( baseSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        "Mismatch between taxon for expression experiment set selected and gene queries" );
            }

            allUsedGenes.add( queryGene );

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

            List<Long> relevantEEIdList = getRelevantEEidsForBitVector( positionToIDMap, g2gs );
            relevantEEIdList.retainAll( filteredEeIds );

            GeneValueObject queryGeneValueObject = new GeneValueObject( queryGene );

            HashMap<Gene, Collection<Gene2GeneCoexpression>> foundGenes = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

            // for queryGene get the interactions
            Map<Long, Gene2GeneProteinAssociation> proteinInteractionMap = this
                    .getGene2GeneProteinAssociationForQueryGene( queryGene );

            Map<Long, TfGeneAssociation> regulatedBy = this.getTfGeneAssociationsforTargetGene( queryGene );
            Map<Long, TfGeneAssociation> regulates = this.getTfGeneAssociationsforTf( queryGene );

            if ( timer.getTime() > 100 ) {
                log.info( "Postprocess " + queryGene.getOfficialSymbol() + " Phase I: " + timer.getTime() + "ms" );
            }
            timer.stop();
            timer.reset();
            timer.start();

            for ( Gene2GeneCoexpression g2g : g2gs ) {
                StopWatch timer2 = new StopWatch();
                timer2.start();

                Gene foundGene = g2g.getFirstGene().equals( queryGene ) ? g2g.getSecondGene() : g2g.getFirstGene();

                allUsedGenes.add( foundGene );

                // FIXME Symptom fix for duplicate found genes
                // Keep track of the found genes that we can correctly identify
                // duplicates.
                // All keep the g2g object for debugging purposes.
                if ( foundGenes.containsKey( foundGene ) ) {
                    foundGenes.get( foundGene ).add( g2g );
                    log.warn( "Duplicate gene found in coexpression results, skipping: " + foundGene
                            + " From analysis: " + g2g.getSourceAnalysis().getId() );
                    continue; // Found a duplicate gene, don't add to results
                              // just our debugging list

                }

                foundGenes.put( foundGene, new ArrayList<Gene2GeneCoexpression>() );
                foundGenes.get( foundGene ).add( g2g );

                CoexpressionValueObjectExt cvo = new CoexpressionValueObjectExt();

                /*
                 * This Thaw is a big time sink and _should not_ be necessary.
                 */
                // foundGene = geneService.thawLite( foundGene ); // db hit

                cvo.setQueryGene( queryGeneValueObject );
                cvo.setFoundGene( new GeneValueObject( foundGene ) );

                if ( timer2.getTime() > 10 ) log.info( "Coexp. Gene processing phase I:" + timer2.getTime() + "ms" );
                timer2.stop();
                timer2.reset();
                timer2.start();

                populateInteractions( proteinInteractionMap, regulatedBy, regulates, foundGene, cvo );

                Collection<Long> testingDatasets = Gene2GenePopulationServiceImpl.getTestedExperimentIds( g2g,
                        positionToIDMap );
                testingDatasets.retainAll( filteredEeIds );

                /*
                 * necesssary in case any were filtered out (for example, if this is a virtual analysis; or there were
                 * 'troubled' ees. Note that 'supporting' includes 'non-specific' if they were recorded by the analyzer.
                 */
                Collection<Long> supportingDatasets = Gene2GenePopulationServiceImpl.getSupportingExperimentIds( g2g,
                        positionToIDMap );

                // necessary in case any were filtered out.
                supportingDatasets.retainAll( filteredEeIds );

                cvo.setSupportingExperiments( supportingDatasets );

                Collection<Long> specificDatasets = Gene2GenePopulationServiceImpl.getSpecificExperimentIds( g2g,
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

                cvo.setDatasetVector( getDatasetVector( supportingDatasets, testingDatasets, specificDatasets,
                        relevantEEIdList ) );

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
                    cvo.setPosSupp( 0 );
                    cvo.setNegSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        cvo.setNonSpecNegSupp( numSupportingDatasets - supportFromSpecificProbes );

                    ++linksMetNegativeStringency;
                } else {
                    cvo.setPosSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        cvo.setNonSpecPosSupp( numSupportingDatasets - supportFromSpecificProbes );
                    cvo.setNegSupp( 0 );
                    ++linksMetPositiveStringency;
                }
                cvo.setSupportKey( Math.max( cvo.getPosSupp(), cvo.getNegSupp() ) );
                cvo.setNumTestedIn( numTestingDatasets );

                for ( Long id : supportingDatasets ) {
                    supportCount.increment( id );
                }

                cvo.setSortKey();

                /*
                 * This check prevents links from being shown twice when we do "among query genes". We don't skip
                 * entirely so we get the counts for the summary table populated correctly.
                 */
                if ( !seen.contains( g2g ) ) {
                    ecvos.add( cvo );
                }

                seen.add( g2g );

                allSupportingDatasets.addAll( supportingDatasets );
                allDatasetsWithSpecificProbes.addAll( specificDatasets );

            }
            
            Collection<Long> geneIds = new ArrayList<Long>();

            for (Gene g: allUsedGenes){
            	
            	geneIds.add(g.getId());
            	
            }
            
            populateNodeDegree( ecvos, geneIds, allTestedDataSets );

            if ( timer.getTime() > 1000 ) {
                log.info( "Postprocess " + g2gs.size() + " results for " + queryGene.getOfficialSymbol() + "Phase II: "
                        + timer.getTime() + "ms" );
            }
            timer.stop();
            timer.reset();
            timer.start();

            // This is only necessary for debugging purposes. Helps us keep
            // track of duplicate genes found above.
            if ( log.isDebugEnabled() ) {
                for ( Gene foundGene : foundGenes.keySet() ) {
                    if ( foundGenes.get( foundGene ).size() > 1 ) {
                        log.debug( "** DUPLICATE: " + foundGene.getOfficialSymbol()
                                + " found multiple times. Gene2Genes objects are: " );
                        for ( Gene2GeneCoexpression g1g : foundGenes.get( foundGene ) ) {
                            log.debug( " ============ Gene2Gene Id: " + g1g.getId() + " 1st gene: "
                                    + g1g.getFirstGene().getOfficialSymbol() + " 2nd gene: "
                                    + g1g.getSecondGene().getOfficialSymbol() + " Source Analysis: "
                                    + g1g.getSourceAnalysis().getId() + " # of dataSets: " + g1g.getNumDataSets() );
                        }
                    }
                }
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

            Collections.sort( ecvos );
            getGoOverlap( ecvos, queryGene );

            timer.stop();
            if ( timer.getTime() > 1000 ) {
                log.info( "Postprocess " + g2gs.size() + " results for " + queryGene.getOfficialSymbol()
                        + " PhaseIII: " + timer.getTime() + "ms" );
            }
            timer.reset();
        } // Over results.

        result.getKnownGeneResults().addAll( ecvos );
        return result;
    }

    /**
     * @param baseSet
     * @param eeIds
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    private Collection<CoexpressionValueObjectExt> getFilteredCannedAnalysisResults2( ExpressionExperimentSet baseSet,
            Collection<Long> eeIds, Collection<Gene> queryGenes, int stringency, int maxResults,
            boolean queryGenesOnly, boolean skipDetails ) {

        if ( queryGenes.isEmpty() ) {
            throw new IllegalArgumentException( "No genes in query" );
        }

        List<ExpressionExperimentValueObject> eevos = null;
        List<Long> filteredEeIds = null;

        eevos = getSortedEEvos( eeIds );

        if ( eevos.isEmpty() ) {
            throw new IllegalArgumentException( "There are no usable experiments in the selected set" );
        }

        filteredEeIds = ( List<Long> ) EntityUtils.getIds( eevos );
        /*
         * We get this prior to filtering so it matches the vectors stored with the analysis.
         */
        expressionExperimentSetService.thaw( baseSet );
        List<Long> positionToIDMap = Gene2GenePopulationServiceImpl.getPositionToIdMap( EntityUtils.getIds( baseSet
                .getExperiments() ) );

        /*
         * This set of links must be filtered to include those in the data sets being analyzed.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = getRawCoexpression( queryGenes, stringency, maxResults,
                queryGenesOnly );

        List<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Long> seenGene2Gene = new HashSet<Long>();
        
        queryGenes = new HashSet<Gene>();
        
        Collection<Long> gidsNeeded = new HashSet<Long>();
        
        StopWatch timerGeneLoad = new StopWatch();
        
        for (Gene g :  gg2gs.keySet()){
        	
        	Element e = this.getGeneLightWeightCache().getCache().get(g.getId());
        	
        	if ( e != null){
        		queryGenes.add((Gene)e.getValue());
        	} else {
        		gidsNeeded.add(g.getId());
        	}        	
        	
        }        

        if (!gidsNeeded.isEmpty()){
        	
        	Collection<Gene> justLoadedGenes = geneService.loadThawedLiter( gidsNeeded );
        	
        	queryGenes.addAll(justLoadedGenes);
        	
        	for (Gene g: justLoadedGenes){
        		this.getGeneLightWeightCache().getCache().put(new Element(g.getId(), g));
        	}
        }
        // return empty collection if no coexpression results
        if ( queryGenes.isEmpty() ) {
            return ecvos;
        }
        
        if ( timerGeneLoad.getTime() > 100 ) {
        	//TODO make this logging more descriptive
            log.info( "Loading and caching query genes took "
                    + timerGeneLoad.getTime() + "ms" );
        }
        
        
        
        timerGeneLoad.reset();
        timerGeneLoad.start();
        
        gidsNeeded = new HashSet<Long>();
        
        //Map<Long, Gene> genesForValueObjects = new HashMap<Long, Gene>();

        //load all genes first
        for ( Gene queryGene : queryGenes ) {
        	
        	Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );
        	
        	for ( Gene2GeneCoexpression g2g : g2gs ) {
        		
        		Gene foundGene = g2g.getFirstGene().getId().equals( queryGene.getId() ) ? g2g.getSecondGene() : g2g.getFirstGene();
        		
        		 //TODO results for the graph get trimmed on the front end, we can make this even faster if we only load the gvo after we do the trimming. Maybe move trimming into here
                if (this.getGeneLightWeightCache().getCache().get(foundGene.getId()) == null){
                	gidsNeeded.add(foundGene.getId());
                }
        		
        	}
        	
        }
        
        //Put all needed genes in Cache to that they are guaranteed to be there for this method call
      //TODO results for the graph get trimmed on the front end, we can make this even faster if we only load the gvo after we do the trimming. Maybe move trimming into here
        
        if (!gidsNeeded.isEmpty()){
        	
        	//TODO change loadThawedLiter to not join the taxon table, get taxon info from a previous call
        	Collection<Gene> forCache = geneService.loadThawedLiter(gidsNeeded);
        	
        	for (Gene g: forCache){
        		this.getGeneLightWeightCache().getCache().put(new Element(g.getId(), g));      		
        	}
        	
        }
        if ( timerGeneLoad.getTime() > 100 ) {
        	//TODO make this logging more descriptive
            log.info( "Loading and caching found genes took "
                    + timerGeneLoad.getTime() + "ms" );
        }
        

        // populate the value objects.
        StopWatch timer = new StopWatch();
        Collection<Long> allUsedGenes = new HashSet<Long>();

        for ( Gene queryGene : queryGenes ) {
            timer.start();

            if ( !queryGene.getTaxon().equals( baseSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        "Mismatch between taxon for expression experiment set selected and gene queries" );
            }

            allUsedGenes.add( queryGene.getId() );

            /*
             * For summary statistics
             */
            CountingMap<Long> supportCount = new CountingMap<Long>();

            Collection<Long> allDatasetsWithSpecificProbes = new HashSet<Long>();
            Collection<Long> allTestedDataSets = new HashSet<Long>();

            Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );

            assert g2gs != null;

            List<Long> relevantEEIdList = null;

            if ( !skipDetails ) {
                relevantEEIdList = getRelevantEEidsForBitVector( positionToIDMap, g2gs );
                relevantEEIdList.retainAll( filteredEeIds );
            }
            GeneValueObject queryGeneValueObject = new GeneValueObject( queryGene );

            Map<Long, Collection<Gene2GeneCoexpression>> foundGenes = new HashMap<Long, Collection<Gene2GeneCoexpression>>();

            if ( timer.getTime() > 100 ) {
                log.info( "Postprocess " + queryGene.getOfficialSymbol() + " Phase I: " + timer.getTime() + "ms" );
            }
            timer.stop();
            timer.reset();
            timer.start();
            
            for ( Gene2GeneCoexpression g2g : g2gs ) {
            	
                StopWatch timer2 = new StopWatch();
                timer2.start();

                Gene foundGene = g2g.getFirstGene().getId().equals( queryGene.getId() ) ? g2g.getSecondGene() : g2g.getFirstGene();
                
                allUsedGenes.add( foundGene.getId() );

                // use this flag to test for a duplicate link with opposite stringency support (positive/negative)
                boolean testForDuplicateFlag = false;

                // duplicate found genes can occur when there is both positive and negative support for the same link
                // Keep track of the found genes that we can correctly identify
                // duplicates.
                // All keep the g2g object for debugging purposes.
                if ( foundGenes.containsKey( foundGene.getId() ) ) {                   

                    testForDuplicateFlag = true;

                }else{
                	foundGenes.put( foundGene.getId(), new ArrayList<Gene2GeneCoexpression>() );
                }                
                
                foundGenes.get( foundGene.getId() ).add( g2g );

                CoexpressionValueObjectExt cvo = new CoexpressionValueObjectExt();               
                
                Long idToGet= foundGene.getId();
                
                foundGene = (Gene)this.getGeneLightWeightCache().getCache().get(idToGet).getValue();
                
                if (foundGene == null){
                	log.error("Gene id:"+idToGet+" Not in the GeneLightWeightCache, something is wrong");
                	continue;
                }
               

                cvo.setQueryGene( queryGeneValueObject );
                cvo.setFoundGene( new GeneValueObject( foundGene ) );
                
                if ( timer2.getTime() > 100 ) log.info( "Coexp. Gene processing phase I:" + timer2.getTime() + "ms" );
                timer2.stop();
                timer2.reset();
                timer2.start();

                List<Long> supportingDatasets = Gene2GenePopulationServiceImpl.getSupportingExperimentIds( g2g,
                        positionToIDMap );
                // necessary in case any were filtered out.
                supportingDatasets.retainAll( filteredEeIds );
                cvo.setSupportingExperiments( supportingDatasets );

                List<Long> testingDatasets;
                List<Long> specificDatasets;
                if ( !skipDetails ) {

                    testingDatasets = Gene2GenePopulationServiceImpl.getTestedExperimentIds( g2g, positionToIDMap );
                    testingDatasets.retainAll( filteredEeIds );

                    /*
                     * necesssary in case any were filtered out (for example, if this is a virtual analysis; or there
                     * were 'troubled' ees. Note that 'supporting' includes 'non-specific' if they were recorded by the
                     * analyzer.
                     */

                    specificDatasets = Gene2GenePopulationServiceImpl.getSpecificExperimentIds( g2g, positionToIDMap );

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

                    cvo.setDatasetVector( getDatasetVector( supportingDatasets, testingDatasets, specificDatasets,
                            relevantEEIdList ) );

                    if ( testForDuplicateFlag ) {

                        testAndModifyDuplicateResultForOppositeStringency( ecvos, queryGene, foundGene,
                                g2g.getEffect(), numSupportingDatasets, specificDatasets.size(), numTestingDatasets );

                        continue;

                    } else if ( numSupportingDatasets < stringency ) {// check in case any data sets were filtered
                                                                      // out.(i.e., we're not interested in the full set
                                                                      // of data sets that were used in the original
                                                                      // analysis.
                        continue;
                    }

                    allTestedDataSets.addAll( testingDatasets );

                    int supportFromSpecificProbes = specificDatasets.size();

                    if ( g2g.getEffect() < 0 ) {
                        cvo.setPosSupp( 0 );
                        cvo.setNegSupp( numSupportingDatasets );
                        if ( numSupportingDatasets != supportFromSpecificProbes )
                            cvo.setNonSpecNegSupp( numSupportingDatasets - supportFromSpecificProbes );

                    } else {
                        cvo.setPosSupp( numSupportingDatasets );
                        if ( numSupportingDatasets != supportFromSpecificProbes )
                            cvo.setNonSpecPosSupp( numSupportingDatasets - supportFromSpecificProbes );
                        cvo.setNegSupp( 0 );
                    }
                    cvo.setSupportKey( Math.max( cvo.getPosSupp(), cvo.getNegSupp() ) );
                    cvo.setNumTestedIn( numTestingDatasets );

                    for ( Long id : supportingDatasets ) {
                        supportCount.increment( id );
                    }

                    allDatasetsWithSpecificProbes.addAll( specificDatasets );

                } else {

                    int numSupportingDatasets = supportingDatasets.size();

                    if ( testForDuplicateFlag ) {

                        testAndModifyDuplicateResultForOppositeStringency( ecvos, queryGene, foundGene,
                                g2g.getEffect(), numSupportingDatasets, null, null );

                        continue;

                    } else if ( numSupportingDatasets < stringency ) {
                        continue;
                    }

                    if ( g2g.getEffect() < 0 ) {
                        cvo.setPosSupp( 0 );
                        cvo.setNegSupp( numSupportingDatasets );
                    } else {
                        cvo.setPosSupp( numSupportingDatasets );
                        cvo.setNegSupp( 0 );
                    }

                    cvo.setSupportKey( Math.max( cvo.getPosSupp(), cvo.getNegSupp() ) );

                }

                cvo.setSortKey();

                /*
                 * This check prevents links from being shown twice when we do "among query genes". We don't skip
                 * entirely so we get the counts for the summary table populated correctly.
                 */
                if ( !seenGene2Gene.contains( g2g.getId() ) ) {
                    ecvos.add( cvo );
                }

                seenGene2Gene.add( g2g.getId() );
                
                if ( timer2.getTime() > 100 ) log.info( "Coexp. Gene processing phase I.5:" + timer2.getTime() + "ms" );

            }

            if ( timer.getTime() > 100 ) {
                log.info( "Postprocess " + g2gs.size() + " results for " + queryGene.getOfficialSymbol() + "Phase II: "
                        + timer.getTime() + "ms" );
            }
            timer.stop();
            timer.reset();
            timer.start();

            Collections.sort( ecvos );

            timer.stop();
            if ( timer.getTime() > 100 ) {
                log.info( "Postprocess " + g2gs.size() + " results for " + queryGene.getOfficialSymbol()
                        + " PhaseIII: " + timer.getTime() + "ms" );
            }
            timer.reset();
        } // Over querygenes

        populateNodeDegree( ecvos, allUsedGenes, filteredEeIds );
        return ecvos;

    }

    /**
     * @param ecvos (sorted)
     * @param queryGene
     */
    private void getGoOverlap( List<CoexpressionValueObjectExt> ecvos, Gene queryGene ) {
        if ( !geneOntologyService.isGeneOntologyLoaded() ) {
            return;
        }

        /*
         * get GO overlap info for this query gene...
         */
        StopWatch timer = new StopWatch();
        timer.start();

        int numQueryGeneGoTerms = geneOntologyService.getGOTerms( queryGene ).size();
        Collection<Long> overlapIds = new HashSet<Long>();
        for ( CoexpressionValueObjectExt ecvo : ecvos ) {
            overlapIds.add( ecvo.getFoundGene().getId() );
            if ( overlapIds.size() >= NUM_GENES_TO_DETAIL ) break;
        }
        Map<Long, Collection<OntologyTerm>> goOverlap = geneOntologyService.calculateGoTermOverlap( queryGene,
                overlapIds );
        int i = 0;
        for ( CoexpressionValueObjectExt ecvo : ecvos ) {
            ecvo.setMaxGoSim( numQueryGeneGoTerms );
            Collection<OntologyTerm> overlap = goOverlap.get( ecvo.getFoundGene().getId() );
            ecvo.setGoSim( overlap == null ? null : overlap.size() );
            if ( ++i >= NUM_GENES_TO_DETAIL ) break;
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "GO stats for " + queryGene.getName() + " + " + overlapIds.size() + " coexpressed genes :"
                    + timer.getTime() + "ms" );
        }
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
     * Retrieve all gene2gene coexpression information for the genes at the specified stringency, using methods that
     * don't filter by experiment.
     * 
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    private Map<Gene, Collection<Gene2GeneCoexpression>> getRawCoexpression( Collection<Gene> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly ) {
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        if ( queryGenes.size() == 0 ) {
            return gg2gs;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        GeneCoexpressionAnalysis gA = findEnabledCoexpressionAnalysis( queryGenes );
        timer.stop();
        if ( timer.getTime() > 100 ) {
            log.info( "Get analysis: " + timer.getTime() + "ms" );
        }
        timer.reset();
        timer.start();

        if ( queryGenesOnly ) {
            if ( queryGenes.size() < 2 ) {
                throw new IllegalArgumentException( "Must have at least two genes to do 'my genes only'" );
            }
            gg2gs = gene2GeneCoexpressionService.findInterCoexpressionRelationship( queryGenes, stringency, gA );
        } else {
            gg2gs = gene2GeneCoexpressionService.findCoexpressionRelationships( queryGenes, stringency, maxResults, gA );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Get raw coexpression: " + timer.getTime() + "ms" );
        }

        return gg2gs;
    }

    /**
     * @param positionToIDMap
     * @param g2gs
     * @return
     */
    private List<Long> getRelevantEEidsForBitVector( List<Long> positionToIDMap, Collection<Gene2GeneCoexpression> g2gs ) {
        Collection<Long> relevantEEIds = new HashSet<Long>();
        List<Long> relevantEEIdList = new ArrayList<Long>();
        for ( Gene2GeneCoexpression g2g : g2gs ) {
            relevantEEIds.addAll( Gene2GenePopulationServiceImpl.getTestedExperimentIds( g2g, positionToIDMap ) );
        }
        relevantEEIdList.addAll( relevantEEIds );
        Collections.sort( relevantEEIdList );
        return relevantEEIdList;
    }

    /**
     * @param eeIds
     * @return
     */
    private List<ExpressionExperimentValueObject> getSortedEEvos( Collection<Long> eeIds ) {

        /* security will filter experiments */
        Collection<ExpressionExperiment> filteredExperiments = expressionExperimentService.loadMultiple( eeIds );
        
        StopWatch timerFilterTroubled = new StopWatch(); 
        timerFilterTroubled.start();
        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : filteredExperiments ) {

        	//TODO this is extremely slow, commenting it out for now, will need to ask how important it is
            //Status eestatus = statusService.getStatus( ee );
            //if ( eestatus.getTroubled() ) continue;
        	
            filteredIds.add( ee.getId() );
        }

        if ( timerFilterTroubled.getTime() > 100 ) {
        	//TODO make this logging more descriptive
            log.info( "Filtering troubled experiments took "
                    + timerFilterTroubled.getTime() + "ms" );
        }
        
        List<ExpressionExperimentValueObject> eevos = new ArrayList<ExpressionExperimentValueObject>(
                expressionExperimentService.loadValueObjects( filteredIds, false ) );

        Collections.sort( eevos, new Comparator<ExpressionExperimentValueObject>() {
            @Override
            public int compare( ExpressionExperimentValueObject eevo1, ExpressionExperimentValueObject eevo2 ) {
                return eevo1.getId().compareTo( eevo2.getId() );
            }
        } );
        return eevos;
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
        result.setQueryGenes( new ArrayList<GeneValueObject>( GeneValueObject.convert2ValueObjects( genes ) ) );
        result.setDatasets( eevos );
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
     * @param proteinInteractionMap
     * @param regulatedBy
     * @param regulates
     * @param foundGene
     * @param cvo
     */
    private void populateInteractions( Map<Long, Gene2GeneProteinAssociation> proteinInteractionMap,
            Map<Long, TfGeneAssociation> regulatedBy, Map<Long, TfGeneAssociation> regulates, Gene foundGene,
            CoexpressionValueObjectExt cvo ) {

        StopWatch timer = new StopWatch();
        timer.start();

        // set the interaction if none null will be put
        if ( proteinInteractionMap != null && !( proteinInteractionMap.isEmpty() ) ) {
            Gene2GeneProteinAssociation association = proteinInteractionMap.get( foundGene.getId() );
            if ( association != null ) this.addProteinDetailsToValueObject( association, cvo );
        }

        if ( regulatedBy != null && !regulatedBy.isEmpty() ) {
            TfGeneAssociation tfGeneAssociation = regulatedBy.get( foundGene.getId() );
            if ( tfGeneAssociation != null ) this.addTfInteractionToValueObject( tfGeneAssociation, cvo );
        }

        if ( regulates != null && !regulates.isEmpty() ) {
            TfGeneAssociation tfGeneAssociation = regulates.get( foundGene.getId() );
            if ( tfGeneAssociation != null ) this.addTfInteractionToValueObject( tfGeneAssociation, cvo );
        }
        if ( timer.getTime() > 10 ) log.info( "Iteraction population:" + timer.getTime() + "ms" );

    }

    /**
     * @param ecvos
     * @param allUsedGenes
     * @param ees
     */
    private void populateNodeDegree( List<CoexpressionValueObjectExt> ecvos, Collection<Long> allUsedGenes,
            Collection<Long> ees ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Map<Long, GeneCoexpressionNodeDegree> geneNodeDegrees = geneService
                .getGeneIdCoexpressionNodeDegree( allUsedGenes );

        

        for ( CoexpressionValueObjectExt coexp : ecvos ) {

            GeneCoexpressionNodeDegree queryGeneNodeDegree = geneNodeDegrees.get(  coexp.getQueryGene()
                    .getId()  );
            if ( queryGeneNodeDegree == null ) {
                coexp.setQueryGeneNodeDegree( -1.0 );
            } else {
                coexp.setQueryGeneNodeDegree( queryGeneNodeDegree.getRankNumLinks() );
            }

            GeneCoexpressionNodeDegree foundGeneNodeDegree = geneNodeDegrees.get(  coexp.getFoundGene()
                    .getId() );
            if ( foundGeneNodeDegree == null ) {
                coexp.setFoundGeneNodeDegree( -1.0 );
            } else {
                coexp.setFoundGeneNodeDegree( foundGeneNodeDegree.getRankNumLinks() );
            }
        }

        /*
         * Old, slower way
         */
        // if ( !ees.isEmpty() ) {
        // Map<Gene, Double> geneNodeDegrees =
        // geneService.getGeneCoexpressionNodeDegree( allUsedGenes,
        // expressionExperimentService.loadMultiple( ees ) );
        // Map<Long, Gene> idMap = EntityUtils.getIdMap(
        // geneNodeDegrees.keySet() );
        // for ( CoexpressionValueObjectExt coexp : ecvos ) {
        // coexp.setQueryGeneNodeDegree( geneNodeDegrees.get( idMap.get(
        // coexp.getQueryGene().getId() ) ) );
        // coexp.setFoundGeneNodeDegree( geneNodeDegrees.get( idMap.get(
        // coexp.getFoundGene().getId() ) ) );
        // }
        // } else {
        // for ( CoexpressionValueObjectExt coexp : ecvos ) {
        // coexp.setQueryGeneNodeDegree( 0d );
        // coexp.setFoundGeneNodeDegree( 0d );
        // }
        // }

        if ( timer.getTime() > 10 ) log.info( "Node degree population:" + timer.getTime() + "ms" );
    }

    /**
     * Sometimes a coexpression link can have both negative and positive support. When this happens two results are
     * returned and need to be merged. If there is a duplicate(same coexpression link between two genes seen again)
     * result, this method merges the duplicate result with the result in the List<CoexpressionValueObjectExt> ecvos
     * passed in
     */
    private boolean testAndModifyDuplicateResultForOppositeStringency( List<CoexpressionValueObjectExt> ecvos,
            Gene queryGene, Gene foundGene, Double effect, Integer numSupportingDatasets,
            Integer supportFromSpecificProbes, Integer numTestedIn ) {

        for ( CoexpressionValueObjectExt ecvo : ecvos ) {

            if ( ecvo.getFoundGene().getId().equals( foundGene.getId() )
                    && ecvo.getQueryGene().getId().equals( queryGene.getId() ) ) {
                if ( ecvo.getNegSupp() > 0 && effect > 0 ) {
                    ecvo.setPosSupp( numSupportingDatasets );
                    if ( supportFromSpecificProbes != null && numSupportingDatasets != supportFromSpecificProbes ) {
                        ecvo.setNonSpecPosSupp( numSupportingDatasets - supportFromSpecificProbes );
                    }
                    // this may not be necessary, putting in just in case of a difference
                    if ( numTestedIn != null ) {
                        ecvo.setNumTestedIn( Math.max( ecvo.getNumTestedIn(), numTestedIn ) );
                    }
                    ecvo.setSupportKey( Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );
                    return true;
                } else if ( ecvo.getPosSupp() > 0 && effect < 0 ) {
                    ecvo.setNegSupp( numSupportingDatasets );
                    if ( supportFromSpecificProbes != null && numSupportingDatasets != supportFromSpecificProbes ) {
                        ecvo.setNonSpecNegSupp( numSupportingDatasets - supportFromSpecificProbes );
                    }
                    // this may not be necessary, putting in just in case of a difference
                    if ( numTestedIn != null ) {
                        ecvo.setNumTestedIn( Math.max( ecvo.getNumTestedIn(), numTestedIn ) );
                    }
                    ecvo.setSupportKey( Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );
                    return true;
                }

            }

        }

        return false;
    }

}
