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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.CountingMap;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author luke
 * @version $Id$
 * @spring.bean id="extCoexpressionSearchController"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "probeLinkCoexpressionAnalyzer" ref="probeLinkCoexpressionAnalyzer"
 * @spring.property name = "gene2GeneCoexpressionService" ref="gene2GeneCoexpressionService"
 * @spring.property name = "geneCoexpressionAnalysisService" ref="geneCoexpressionAnalysisService"
 * @spring.property name = "geneOntologyService" ref="geneOntologyService"
 */
public class ExtCoexpressionSearchController extends BaseFormController {

    /**
     * How many genes to fill in the "expression experiments tested in" and "go overlap" info for.
     */
    private static final int NUM_GENES_TO_DETAIL = 100;

    private static Log log = LogFactory.getLog( ExtCoexpressionSearchController.class.getName() );

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer = null;
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService = null;
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService = null;
    private GeneOntologyService geneOntologyService = null;

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        return new ModelAndView( this.getFormView() );
    }
    
    public ExtCoexpressionMetaValueObject getEmptyResult() {
        return new ExtCoexpressionMetaValueObject();
    }

    public ExtCoexpressionMetaValueObject doSearch( ExtCoexpressionSearchCommand searchOptions ) {
        Collection<Gene> genes = geneService.loadMultiple( searchOptions.getGeneIds() );
        ExtCoexpressionMetaValueObject result;
        if ( genes == null || genes.isEmpty() )
            return getEmptyResult();
        else if ( searchOptions.getCannedAnalysisId() != null )
            result = getCannedAnalysisResults( searchOptions.getCannedAnalysisId(), genes, searchOptions
                    .getStringency() );
        else
            result = getCustomAnalysisResults( searchOptions.getEeIds(), genes, searchOptions.getStringency() );
        return result;
    }

    private Collection<ExpressionExperiment> getPossibleExpressionExperiments( Collection<Gene> genes ) {
        Collection<Long> eeIds = new HashSet<Long>();
        for ( Gene g : genes ) {
            eeIds.addAll( expressionExperimentService.findByGene( g ) );
        }
        return eeIds.isEmpty() ? new HashSet<ExpressionExperiment>() : expressionExperimentService.loadMultiple( eeIds );
    }

    private ExtCoexpressionMetaValueObject getCannedAnalysisResults( Long cannedAnalysisId, Collection<Gene> genes,
            int stringency ) {
        
        GeneCoexpressionAnalysis analysis = ( GeneCoexpressionAnalysis ) geneCoexpressionAnalysisService
                .load( cannedAnalysisId );
        List<Long> eeIds = getSortedIdList( analysis.getExperimentsAnalyzed() );
        List<ExpressionExperimentValueObject> eevos = new ArrayList( expressionExperimentService
                .loadValueObjects( eeIds ) );

        ExtCoexpressionMetaValueObject result = new ExtCoexpressionMetaValueObject();
        result.setQueryGenes( genes );
        result.setDatasets( eevos );
        result.setIsCannedAnalysis( true );
        result.setKnownGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setPredictedGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<ExtCoexpressionValueObject>() );        

        for ( Gene queryGene : genes ) {
            
            /* find coexpression data for this query gene...
             */
            CountingMap<Long> supportCount = new CountingMap<Long>();
            Collection<Long> supportingExperimentIds = new HashSet<Long>();
            Collection<ExtCoexpressionValueObject> ecvos = new ArrayList<ExtCoexpressionValueObject>();
            Collection<Gene2GeneCoexpression> g2gs =
                gene2GeneCoexpressionService.findCoexpressionRelationships( queryGene, analysis, stringency );
            
            for ( Gene2GeneCoexpression g2g : g2gs ) {
                ExtCoexpressionValueObject ecvo = new ExtCoexpressionValueObject();
                ecvo.setQueryGene( g2g.getFirstGene() );
                ecvo.setFoundGene( g2g.getSecondGene() );
                
                Map<Integer, Long> posToId = GeneLinkCoexpressionAnalyzer.getPositionToIdMap( eeIds );
                Collection<Long> testingDatasets = GeneLinkCoexpressionAnalyzer.getTestedExperimentIds( g2g, posToId );
                Collection<Long> supportingDatasets = GeneLinkCoexpressionAnalyzer.getSupportingExperimentIds( g2g, posToId );
                ecvo.setTestedDatasetVector( getDatasetVector( testingDatasets, eeIds ) );
                ecvo.setSupportingDatasetVector( getDatasetVector( supportingDatasets, eeIds ) );
                
                int numTestingDatasets = testingDatasets.size();
                int numSupportingDatasets = supportingDatasets.size();
                if ( g2g.getEffect() < 0 ) {
                    ecvo.setPositiveLinks( 0 );
                    ecvo.setNegativeLinks( numSupportingDatasets );
                } else {
                    ecvo.setPositiveLinks( numSupportingDatasets );
                    ecvo.setNegativeLinks( 0 );
                }
                ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
                ecvo.setNumDatasetsLinkTestedIn( numTestingDatasets );
                
                for ( Long id : supportingDatasets ) {
                    supportCount.increment( id );
                }
                supportingExperimentIds.addAll( supportingDatasets );
                
                ecvos.add( ecvo );
            }
            
            /* generate dataset summary info for this query gene...
             */
            for ( ExpressionExperimentValueObject eevo : eevos ) {
                if ( ! supportingExperimentIds.contains( eevo.getId() ) )
                    continue;
                ExtCoexpressionDatasetValueObject ecdvo = new ExtCoexpressionDatasetValueObject();
                ecdvo.setId( eevo.getId() );
                ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
                ecdvo.setCoexpressionLinkCount( supportCount.get( eevo.getId() ).longValue() );
                ecdvo.setRawCoexpressionLinkCount( null ); // not available
                ecdvo.setProbeSpecificForQueryGene( true ); // only specific probes in these results
                ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
                ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
                result.getKnownGeneDatasets().add( ecdvo );
            }
            
            /* get GO overlap info for this query gene...
             */
            if ( geneOntologyService.isGeneOntologyLoaded() ) {
                int numQueryGeneGoTerms = geneOntologyService.getGOTerms( queryGene ).size();
                Collection<Long> overlapIds = new ArrayList<Long>();
                int i=0;
                for ( ExtCoexpressionValueObject ecvo : ecvos ) {
                    overlapIds.add( ecvo.getFoundGene().getId() );
                    if ( i++ > NUM_GENES_TO_DETAIL ) break;
                }
                Map<Long, Collection<OntologyTerm>> goOverlap =
                    geneOntologyService.calculateGoTermOverlap( queryGene, overlapIds );
                for ( ExtCoexpressionValueObject ecvo : ecvos ) {
                    ecvo.setPossibleOverlap( numQueryGeneGoTerms );
                    ecvo.setGoOverlap( goOverlap.get( ecvo.getFoundGene().getId() ).size() );
                }
            }

            /* add results for this query gene...
             */
            result.getKnownGeneResults().addAll( ecvos );
        }
        
        return result;
        
    }

    private List<Long> getSortedIdList( Collection<ExpressionExperiment> datasets ) {
        List<Long> ids = new ArrayList<Long>( datasets.size() );
        for ( Securable dataset : datasets ) {
            ids.add( dataset.getId() );
        }
        Collections.sort( ids );
        return ids;
    }

    private Long[] getDatasetVector( Collection<Long> presentIds, List<Long> allIds ) {
        Long[] result = new Long[ allIds.size() ];
        int i = 0;
        for ( Long id : allIds ) {
            result[ i++ ] = presentIds.contains( id ) ? id : 0;
        }
        return result;
    }

    private ExtCoexpressionMetaValueObject getCustomAnalysisResults( Collection<Long> eeIds, Collection<Gene> genes,
            int stringency ) {
        ExtCoexpressionMetaValueObject result = new ExtCoexpressionMetaValueObject();
        if ( eeIds == null ) eeIds = new HashSet<Long>();
        Collection<ExpressionExperiment> ees = getPossibleExpressionExperiments( genes );

        if ( eeIds == null ) {
            eeIds = new HashSet<Long>();
        } else if ( !eeIds.isEmpty() ) {
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
        List<ExpressionExperimentValueObject> eevos = new ArrayList( expressionExperimentService
                .loadValueObjects( eeIds ) );

        result.setQueryGenes( genes );
        result.setDatasets( eevos );
        result.setKnownGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setPredictedGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<ExtCoexpressionValueObject>() );

        /*
         * TODO this is done just naively right now. allow the user to show only interactions among their genes of
         * interest and filter the results before the time-consuming analysis is done...
         */
        for ( Gene queryGene : genes ) {
            CoexpressionCollectionValueObject coexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( queryGene,
                    ees, stringency, !SecurityService.isUserAdmin(), NUM_GENES_TO_DETAIL );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getKnownGeneCoexpression(),
                    stringency, result.getKnownGeneResults(), result.getKnownGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getPredictedCoexpressionType(), stringency, result.getPredictedGeneResults(), result
                    .getPredictedGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions
                    .getProbeAlignedCoexpressionType(), stringency, result.getProbeAlignedRegionResults(), result
                    .getProbeAlignedRegionDatasets() );
        }

        return result;
    }

    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperimentValueObject> eevos,
            CoexpressedGenesDetails coexp, int stringency, Collection<ExtCoexpressionValueObject> results,
            Collection<ExtCoexpressionDatasetValueObject> datasetResults ) {
        for ( CoexpressionValueObject cvo : coexp.getCoexpressionData( stringency ) ) {
            ExtCoexpressionValueObject ecvo = new ExtCoexpressionValueObject();
            ecvo.setQueryGene( queryGene );
            ecvo.setFoundGene( new SimpleGene( cvo.getGeneId(), cvo.getGeneName(), cvo.getGeneOfficialName() ) );

            ecvo.setPositiveLinks( cvo.getPositiveLinkSupport() );
            ecvo.setNegativeLinks( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
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

            results.add( ecvo );
        }

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            ExpressionExperimentValueObject coexpEevo = coexp.getExpressionExperiment( eevo.getId() );
            if ( coexpEevo == null ) continue;
            ExtCoexpressionDatasetValueObject ecdvo = new ExtCoexpressionDatasetValueObject();
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
                cavo.setNumDatasets( analysis.getExperimentsAnalyzed().size() );
                analyses.add( cavo );
            }
        }
        return analyses;
    }
    
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        /* TODO think about caching EEs by taxon, since we're calling it every time here...
         */
        Taxon taxon = taxonService.load( taxonId );
        Collection<Long> eeIds = new HashSet<Long>();
        if ( query.length() > 0 ) {
            List<SearchResult> results = searchService.search( SearchSettings.ExpressionExperimentSearch( query ), false )
                .get( ExpressionExperiment.class );
            for ( SearchResult result : results ) {
                eeIds.add( result.getId() );
            }
            if ( taxon != null ) {
                Collection<Long> eeIdsToKeep = new HashSet<Long>();
                Collection<ExpressionExperiment> ees = expressionExperimentService.findByTaxon( taxon );
                for ( ExpressionExperiment ee : ees ) {
                    if ( eeIds.contains( ee.getId() ) )
                        eeIdsToKeep.add( ee.getId() );
                }
                eeIds.retainAll( eeIdsToKeep );
            }
        } else {
            /* TODO this might be too slow, given that we just need the ids, but I don't want
             * to add a service method until that's proven to be the case...
             */
            Collection<ExpressionExperiment> ees = ( taxon != null ) ?
                    expressionExperimentService.findByTaxon( taxon ) :
                    expressionExperimentService.loadAll();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
        }
        return eeIds;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    public void setProbeLinkCoexpressionAnalyzer( ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer ) {
        this.probeLinkCoexpressionAnalyzer = probeLinkCoexpressionAnalyzer;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

}