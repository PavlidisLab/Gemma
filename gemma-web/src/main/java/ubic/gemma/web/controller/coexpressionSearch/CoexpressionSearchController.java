/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.coexpression.GeneLinkCoexpressionAnalyzer;
import ubic.gemma.analysis.coexpression.ProbeLinkCoexpressionAnalyzer;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.util.CountingMap;
import ubic.gemma.util.GemmaLinkUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.view.TextView;

/**
 * @author luke
 * @version $Id$
 * @spring.bean id="coexpressionSearchController"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "probeLinkCoexpressionAnalyzer" ref="probeLinkCoexpressionAnalyzer"
 * @spring.property name = "gene2GeneCoexpressionService" ref="gene2GeneCoexpressionService"
 * @spring.property name = "geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name = "geneOntologyService" ref="geneOntologyService"
 */
public class CoexpressionSearchController extends BaseFormController {

    /**
     * How many genes to fill in the "expression experiments tested in" and "go overlap" info for.
     */
    private static final int NUM_GENES_TO_DETAIL = 100;

    private static final int DEFAULT_STRINGENCY = 2;

    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer = null;
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService = null;
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService = null;
    private GeneOntologyService geneOntologyService = null;

    /**
     * @param searchOptions
     * @return
     */
    @SuppressWarnings("unchecked")
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {
        Collection<Gene> genes = geneService.loadMultiple( searchOptions.getGeneIds() );
        CoexpressionMetaValueObject result;
        if ( genes == null || genes.isEmpty() )
            return getEmptyResult();
        else if ( searchOptions.getCannedAnalysisId() != null )
            result = getCannedAnalysisResults( searchOptions.getCannedAnalysisId(), genes, searchOptions
                    .getStringency(), searchOptions.getQueryGenesOnly() );
        else
            result = getCustomAnalysisResults( searchOptions.getEeIds(), genes, searchOptions.getStringency(),
                    searchOptions.getQueryGenesOnly() );
        return result;
    }

    /**
     * @param query
     * @param taxonId
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        /*
         * TODO this might be too slow, given that we just need the ids, but I don't want to add a service method until
         * that's proven to be the case... --- this is actually usually pretty fast, as there is a cache in the EE
         * dao.-- PP
         */
        Taxon taxon = taxonService.load( taxonId );
        Collection<Long> eeIds = new HashSet<Long>();
        if ( query.length() > 0 ) {
            List<SearchResult> results = searchService.search( SearchSettings.ExpressionExperimentSearch( query ),
                    false ).get( ExpressionExperiment.class );
            for ( SearchResult result : results ) {
                eeIds.add( result.getId() );
            }
            if ( taxon != null ) {
                Collection<Long> eeIdsToKeep = new HashSet<Long>();
                Collection<ExpressionExperiment> ees = expressionExperimentService.findByTaxon( taxon );
                for ( ExpressionExperiment ee : ees ) {
                    if ( eeIds.contains( ee.getId() ) ) eeIdsToKeep.add( ee.getId() );
                }
                eeIds.retainAll( eeIdsToKeep );
            }
        } else {

            Collection<ExpressionExperiment> ees = ( taxon != null ) ? expressionExperimentService.findByTaxon( taxon )
                    : expressionExperimentService.loadAll();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
        }
        return eeIds;
    }

    /**
     * @return collection of the available caned analyses, for all taxa.
     */
    public Collection<CannedAnalysisValueObject> getCannedAnalyses() {
        Collection<CannedAnalysisValueObject> analyses = new ArrayList<CannedAnalysisValueObject>();
        for ( Object o : taxonService.loadAll() ) {
            Taxon taxon = ( Taxon ) o;
            for ( Object p : geneCoexpressionAnalysisService.findByTaxon( taxon ) ) {
                GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) p;
                CannedAnalysisValueObject cavo = new CannedAnalysisValueObject();
                cavo.setId( analysis.getId() );
                cavo.setName( analysis.getName() );
                cavo.setDescription( analysis.getDescription() );
                cavo.setTaxon( taxon );

                /*
                 * FIXME this number isn't quite right if there are 'troubled' data sets we filter out.
                 */
                cavo.setNumDatasets( geneCoexpressionAnalysisService.getNumDatasetsAnalyzed( analysis ) );

                analyses.add( cavo );
            }
        }
        return analyses;
    }

    public CoexpressionMetaValueObject getEmptyResult() {
        return new CoexpressionMetaValueObject();
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
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

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /*
     * Handle case of text export of the results.
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
            Collection<Gene> genes = geneService.loadMultiple( geneIds );

            boolean queryGenesOnly = request.getParameter( "q" ) != null;
            int stringency = DEFAULT_STRINGENCY;
            try {
                stringency = Integer.parseInt( request.getParameter( "s" ) );
            } catch ( Exception e ) {
                log.warn( "invalid stringency; using default " + stringency );
            }

            Long cannedAnalysisId = null;
            String cannedAnalysisString = request.getParameter( "a" );
            if ( cannedAnalysisString != null ) {
                try {
                    cannedAnalysisId = Long.parseLong( cannedAnalysisString );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid canned analysis id" );
                }
            }

            CoexpressionMetaValueObject result;
            if ( cannedAnalysisId != null ) {
                result = getCannedAnalysisResults( cannedAnalysisId, genes, stringency, queryGenesOnly );
            } else {
                Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) );
                result = getCustomAnalysisResults( eeIds, genes, stringency, queryGenesOnly );
            }

            ModelAndView mav = new ModelAndView( new TextView() );
            String output = result.toString();
            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;

        } else {
            return new ModelAndView( this.getFormView() );
        }
    }

    /**
     * @param queryGene
     * @param eevos
     * @param coexp
     * @param stringency
     * @param queryGenesOnly
     * @param geneIds
     * @param results
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

            ecvo.setPositiveLinks( cvo.getPositiveLinkSupport() );
            ecvo.setNegativeLinks( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( 10 * ( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() ) );

            /*
             * this logic is taken from CoexpressionWrapper; I don't understand it, but that's where it comes from...
             */
            if ( !cvo.getExpressionExperiments().isEmpty() ) {
                ecvo.setNonSpecificPositiveLinks( getNonSpecificLinkCount( cvo.getEEContributing2PositiveLinks(), cvo
                        .getNonspecificEE() ) );
                ecvo.setNonSpecificNegativeLinks( getNonSpecificLinkCount( cvo.getEEContributing2NegativeLinks(), cvo
                        .getNonspecificEE() ) );
                ecvo.setHybridizesWithQueryGene( cvo.isHybridizesWithQueryGene() );
                ecvo.setSupportKey( ecvo.getSupportKey() - ecvo.getNonSpecificPositiveLinks()
                        + ecvo.getNonSpecificNegativeLinks() );
            }

            ecvo.setNumDatasetsLinkTestedIn( cvo.getNumDatasetsTestedIn() );

            ecvo.setGoOverlap( cvo.getGoOverlap() != null ? cvo.getGoOverlap().size() : 0 );
            ecvo.setPossibleOverlap( cvo.getPossibleOverlap() );

            Long[] tested = new Long[eevos.size()];
            Long[] supported = new Long[eevos.size()];
            for ( int i = 0; i < eevos.size(); ++i ) {
                ExpressionExperimentValueObject eevo = eevos.get( i );
                tested[i] = ( cvo.getDatasetsTestedIn() != null && cvo.getDatasetsTestedIn().contains( eevo.getId() ) ) ? eevo
                        .getId()
                        : 0;
                supported[i] = ( cvo.getExperimentBitIds() != null && cvo.getExperimentBitIds().contains( eevo.getId() ) ) ? eevo
                        .getId()
                        : 0;
            }
            ecvo.setTestedDatasetVector( tested );
            ecvo.setSupportingDatasetVector( supported );

            ecvo.setSortKey();
            results.add( ecvo );
        }

        results.size(); // for breakpoint

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            if ( !coexp.getExpressionExperimentIds().contains( eevo.getId() ) ) continue;
            ExpressionExperimentValueObject coexpEevo = coexp.getExpressionExperiment( eevo.getId() );
            if ( coexpEevo == null ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
            ecdvo.setCoexpressionLinkCount( coexp.getLinkCountForEE( coexpEevo.getId() ) );
            ecdvo.setRawCoexpressionLinkCount( coexp.getRawLinkCountForEE( coexpEevo.getId() ) );
            ecdvo.setProbeSpecificForQueryGene( coexpEevo.isProbeSpecificForQueryGene() );
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            datasetResults.add( ecdvo );
        }
    }

    /**
     * @param idString
     * @return
     */
    private Collection<Long> extractIds( String idString ) {
        Collection<Long> ids = new ArrayList<Long>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
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
     * @param cannedAnalysisId
     * @param queryGenes
     * @param stringency
     * @param queryGenesOnly
     * @return
     */
    @SuppressWarnings("unchecked")
    private CoexpressionMetaValueObject getCannedAnalysisResults( Long cannedAnalysisId, Collection<Gene> queryGenes,
            int stringency, boolean queryGenesOnly ) {

        GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService
                .load( cannedAnalysisId );

        // FIXME I'm not sure this needs to be a list and also sorted.
        Collection<ExpressionExperiment> datasetsAnalyzed = geneCoexpressionAnalysisService
                .getDatasetsAnalyzed( analysis );
        List<Long> eeIds = getSortedFilteredIdList( datasetsAnalyzed );

        // This sort is necessary.(?)
        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        Map<Integer, Long> posToId = GeneLinkCoexpressionAnalyzer.getPositionToIdMap( eeIds );

        /*
         * FIXME I'm lazy and rushed, so I'm using an existing field for this info; probably better to add another field
         * to the value object...
         */
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );
        }

        CoexpressionMetaValueObject result = initValueObject( queryGenes, eevos, true );

        /*
         * find coexpression data for the query genes.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = null;
        if ( queryGenesOnly ) {
            gg2gs = gene2GeneCoexpressionService.findInterCoexpressionRelationship( queryGenes, analysis, stringency );
        } else {
            gg2gs = gene2GeneCoexpressionService.findCoexpressionRelationships( queryGenes, analysis, stringency );
        }

        Collection<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Gene2GeneCoexpression> seen = new HashSet<Gene2GeneCoexpression>();

        // populate the value objects.
        for ( Gene queryGene : gg2gs.keySet() ) {
            CountingMap<Long> supportCount = new CountingMap<Long>();
            Collection<Long> supportingExperimentIds = new HashSet<Long>();
            Collection<Long> datasetsTested = new HashSet<Long>();
            int linksMetPositiveStringency = 0;
            int linksMetNegativeStringency = 0;

            Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );

            for ( Gene2GeneCoexpression g2g : g2gs ) {
                Gene foundGene = g2g.getFirstGene().equals( queryGene ) ? g2g.getSecondGene() : g2g.getFirstGene();
                CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
                ecvo.setQueryGene( queryGene );
                ecvo.setFoundGene( foundGene );

                Collection<Long> testingDatasets = GeneLinkCoexpressionAnalyzer.getTestedExperimentIds( g2g, posToId );
                testingDatasets.retainAll( eeIds ); // necesssary in case any were filtered out
                Collection<Long> supportingDatasets = GeneLinkCoexpressionAnalyzer.getSupportingExperimentIds( g2g,
                        posToId );
                supportingDatasets.retainAll( eeIds ); // necessary in case any were filtered out.

                ecvo.setTestedDatasetVector( getDatasetVector( testingDatasets, eeIds ) );
                ecvo.setSupportingDatasetVector( getDatasetVector( supportingDatasets, eeIds ) );

                int numTestingDatasets = testingDatasets.size();
                int numSupportingDatasets = supportingDatasets.size();

                /*
                 * This check is necessary in case any data sets were filtered out. (i.e., we're not interested in the
                 * full set of data sets that were used in the original analysis.
                 */
                if ( numSupportingDatasets < stringency ) {
                    continue;
                }

                datasetsTested.addAll( testingDatasets );

                if ( g2g.getEffect() < 0 ) {
                    ecvo.setPositiveLinks( 0 );
                    ecvo.setNegativeLinks( numSupportingDatasets );
                    ++linksMetNegativeStringency;
                } else {
                    ecvo.setPositiveLinks( numSupportingDatasets );
                    ecvo.setNegativeLinks( 0 );
                    ++linksMetPositiveStringency;
                }
                ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
                ecvo.setNumDatasetsLinkTestedIn( numTestingDatasets );

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

                supportingExperimentIds.addAll( supportingDatasets );
                seen.add( g2g );
            }

            CoexpressionSummaryValueObject summary = makeSummary( eevos, datasetsTested, linksMetPositiveStringency,
                    linksMetNegativeStringency );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );

            generateDatasetSummary( eevos, result, supportCount, supportingExperimentIds, queryGene );

            getGoOverlap( ecvos, queryGene );
        }

        result.getKnownGeneResults().addAll( ecvos );

        return result;
    }

    /**
     * @param eeIds
     * @return
     */
    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private CoexpressionMetaValueObject getCustomAnalysisResults( Collection<Long> eeIds, Collection<Gene> genes,
            int stringency, boolean queryGenesOnly ) {

        if ( true ) {
            throw new RuntimeException(
                    "We're sorry. Custom analysis is not available at this time. Please select one of the other analysis options." );
        }

        if ( eeIds == null ) eeIds = new HashSet<Long>();
        Collection<ExpressionExperiment> ees = getPossibleExpressionExperiments( genes );

        if ( !eeIds.isEmpty() ) {
            // remove the expression experiments we're not interested in...
            Collection<ExpressionExperiment> eesToRemove = new HashSet<ExpressionExperiment>();
            for ( ExpressionExperiment ee : ees ) {
                if ( !eeIds.contains( ee.getId() ) ) eesToRemove.add( ee );
            }
            ees.removeAll( eesToRemove );
        }

        /*
         * repopulate eeIds with the actual eeIds we'll be searching through and load ExpressionExperimentValueObjects
         * to get summary information about the datasets...
         */
        eeIds.clear();
        for ( ExpressionExperiment ee : ees ) {
            eeIds.add( ee.getId() );
        }
        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        /*
         * I'm lazy and rushed, so I'm using an existing field for this info; probably better to add another field to
         * the value object...
         */
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );
        }

        CoexpressionMetaValueObject result = initValueObject( genes, eevos, false );

        boolean knownGenesOnly = true; // !SecurityService.isUserAdmin();
        result.setKnownGenesOnly( knownGenesOnly );

        /*
         * TODO this is done just naively right now. allow the user to show only interactions among their genes of
         * interest and filter the results before the time-consuming analysis is done...
         */
        Collection<Long> geneIds = new HashSet<Long>( genes.size() );
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId() );
        }
        for ( Gene queryGene : genes ) {
            CoexpressionCollectionValueObject coexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( queryGene,
                    ees, stringency, knownGenesOnly, NUM_GENES_TO_DETAIL );

            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getKnownGeneCoexpression(),
                    stringency, queryGenesOnly, geneIds, result.getKnownGeneResults(), result.getKnownGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getPredictedCoexpressionType(), stringency, queryGenesOnly, geneIds, result
                    .getPredictedGeneResults(), result.getPredictedGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getProbeAlignedCoexpressionType(), stringency, queryGenesOnly, geneIds, result
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
     * @param presentIds
     * @param allIds
     * @return
     */
    private Long[] getDatasetVector( Collection<Long> presentIds, List<Long> allIds ) {
        Long[] result = new Long[allIds.size()];
        int i = 0;
        for ( Long id : allIds ) {
            result[i++] = presentIds.contains( id ) ? id : 0;
        }
        return result;
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
                ecvo.setPossibleOverlap( numQueryGeneGoTerms );
                Collection<OntologyTerm> overlap = goOverlap.get( ecvo.getFoundGene().getId() );
                ecvo.setGoOverlap( overlap == null ? 0 : overlap.size() );
            }
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
     * @param genes
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperiment> getPossibleExpressionExperiments( Collection<Gene> genes ) {
        Collection<Long> eeIds = new HashSet<Long>();
        for ( Gene g : genes ) {
            eeIds.addAll( expressionExperimentService.findByGene( g ) );
        }
        return eeIds.isEmpty() ? new HashSet<ExpressionExperiment>() : expressionExperimentService.loadMultiple( eeIds );
    }

    /**
     * Remove data sets that are 'troubled' and sort the list.
     * 
     * @param datasets
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Long> getSortedFilteredIdList( Collection<ExpressionExperiment> datasets ) {

        List<Long> ids = new ArrayList<Long>( datasets.size() );
        for ( Securable dataset : datasets ) {
            ids.add( dataset.getId() );
        }

        // filter out the data sets that are troubled.
        removeTroubledEes( datasets );

        /*
         * FIXME this is also a good place to filter out data sets that are to be excluded for other reasons (e.g., so
         * we can do just "brain" on a canned analysis)
         */

        // rebuild the id list.
        ids = new ArrayList<Long>( datasets.size() );
        for ( Securable dataset : datasets ) {
            ids.add( dataset.getId() );
        }

        Collections.sort( ids );
        return ids;
    }

    /**
     * FIXME this duplicates code from ExpressionExperimentManipulatingCLI
     * 
     * @param ees
     */
    @SuppressWarnings("unchecked")
    private void removeTroubledEes( Collection<ExpressionExperiment> ees ) {
        if ( ees == null || ees.size() == 0 ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }
        ExpressionExperiment theOnlyOne = null;
        if ( ees.size() == 1 ) {
            theOnlyOne = ees.iterator().next();
        }
        int size = ees.size();
        final Map<Long, AuditEvent> trouble = expressionExperimentService.getLastTroubleEvent( CollectionUtils.collect(
                ees, new Transformer() {
                    public Object transform( Object input ) {
                        return ( ( ExpressionExperiment ) input ).getId();
                    }
                } ) );
        CollectionUtils.filter( ees, new Predicate() {
            public boolean evaluate( Object object ) {
                boolean hasTrouble = trouble.containsKey( ( ( ExpressionExperiment ) object ).getId() );
                return !hasTrouble;
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            if ( size == 0 && theOnlyOne != null ) {
                log.info( theOnlyOne.getShortName() + " has an active trouble flag" );
            } else {
                log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving " + newSize );
            }
        }
    }

    /**
     * @param genes
     * @param eevos
     * @param isCannedF
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
     * @param linksMetPositiveStringency
     * @param linksMetNegativeStringency
     * @return
     */
    private CoexpressionSummaryValueObject makeSummary( List<ExpressionExperimentValueObject> eevos,
            Collection<Long> datasetsTested, int linksMetPositiveStringency, int linksMetNegativeStringency ) {
        CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
        summary.setDatasetsAvailable( eevos.size() );
        summary.setDatasetsTested( datasetsTested.size() );
        summary.setLinksFound( linksMetPositiveStringency + linksMetNegativeStringency );
        summary.setLinksMetPositiveStringency( linksMetPositiveStringency );
        summary.setLinksMetNegativeStringency( linksMetNegativeStringency );
        return summary;
    }

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

}