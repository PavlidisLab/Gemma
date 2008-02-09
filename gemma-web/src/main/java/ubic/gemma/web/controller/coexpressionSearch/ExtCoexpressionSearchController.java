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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.BitUtil;
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
 */
public class ExtCoexpressionSearchController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExtCoexpressionSearchController.class.getName() );

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer = null;
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService = null;
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService = null;

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        return new ModelAndView( this.getFormView() );
    }
    
    public ExtCoexpressionMetaValueObject doSearch( ExtCoexpressionSearchCommand searchOptions ) {
        Collection<Gene> genes = geneService.loadMultiple( searchOptions.getGeneIds() );
        ExtCoexpressionMetaValueObject result;
        if ( genes == null || genes.isEmpty() )
            return new ExtCoexpressionMetaValueObject();
        else if ( searchOptions.getCannedAnalysisId() != null )
            result = getCannedAnalysisResults( searchOptions.getCannedAnalysisId(), genes, searchOptions.getStringency() );
        else
            result = getCustomAnalysisResults( searchOptions.getEeIds(), genes, searchOptions.getStringency() );
        return result;
    }
    
    private Collection<ExpressionExperiment> getPossibleExpressionExperiments( Collection<Gene> genes ) {
        Collection<Long> eeIds = new HashSet<Long>();
        for ( Gene g : genes ) {
            eeIds.addAll( expressionExperimentService.findByGene( g ) );
        }
        return expressionExperimentService.loadMultiple( eeIds );
    }

    private ExtCoexpressionMetaValueObject getCannedAnalysisResults( Long cannedAnalysisId, Collection<Gene> genes, int stringency ) {
        ExtCoexpressionMetaValueObject result = new ExtCoexpressionMetaValueObject();
        GeneCoexpressionAnalysis analysis = (GeneCoexpressionAnalysis)geneCoexpressionAnalysisService.load( cannedAnalysisId );
        List<Long> eeIds = getSortedIdList( analysis.getExperimentsAnalyzed() );
        List<ExpressionExperimentValueObject> eevos = new ArrayList( expressionExperimentService.loadValueObjects( eeIds ) );
        result.setDatasets( eevos );
        result.setKnownGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setPredictedGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<ExtCoexpressionValueObject>() );
        
        for ( Gene queryGene : genes ) {
            CountingMap<Long> supportCount = new CountingMap<Long>(); 
            for ( Object o : gene2GeneCoexpressionService.findCoexpressionRelationships( queryGene, analysis, stringency ) ) {
                Gene2GeneCoexpression g2g = (Gene2GeneCoexpression)o;
                ExtCoexpressionValueObject ecvo = new ExtCoexpressionValueObject();
                ecvo.setQueryGene( g2g.getFirstGene() );
                ecvo.setFoundGene( g2g.getSecondGene() );
                ecvo.setTestedDatasetVector( convertG2GBitVector( g2g.getDatasetsTestedVector(), eeIds ) );
                ecvo.setSupportingDatasetVector( convertG2GBitVector( g2g.getDatasetsSupportingVector(), eeIds ) );
                int numTestingDatasets = countBits( ecvo.getTestedDatasetVector() );
                int numSupportingDatasets = countBits( ecvo.getSupportingDatasetVector() );
                if ( g2g.getEffect() < 0 ) {
                    ecvo.setPositiveLinks( 0 );
                    ecvo.setNegativeLinks( numSupportingDatasets );
                } else {
                    ecvo.setPositiveLinks( numSupportingDatasets );
                    ecvo.setNegativeLinks( 0 );
                }
                ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
                ecvo.setNumDatasetsLinkTestedIn( numTestingDatasets );
                ecvo.setGoOverlap( 0 );
                ecvo.setPossibleOverlap( 0 );
                result.getKnownGeneResults().add( ecvo );
                
                for ( Long id : Arrays.asList( ecvo.getSupportingDatasetVector() ) ) {
                    supportCount.increment( id );
                }
            }
            for ( ExpressionExperimentValueObject eevo : eevos ) {
                ExtCoexpressionDatasetValueObject ecdvo = new ExtCoexpressionDatasetValueObject();
                ecdvo.setId( eevo.getId() );
                ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
                ecdvo.setCoexpressionLinkCount( supportCount.get( eevo.getId() ).longValue() );
                ecdvo.setRawCoexpressionLinkCount( ecdvo.getCoexpressionLinkCount() );
                ecdvo.setProbeSpecificForQueryGene( null );
                ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
                ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
                result.getKnownGeneDatasets().add( ecdvo );
            }
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

    private Long[] convertG2GBitVector( byte[] datasetsTestedVector, List<Long> eeIds ) {
        Long[] result = new Long[ eeIds.size() ];
        for ( int i=0; i<eeIds.size(); ++i ) {
            result[i] = BitUtil.get( datasetsTestedVector, i ) ? eeIds.get( i ) : 0;
        }
        return result;
    }

    private int countBits( Long[] vector ) {
        int n=0;
        for ( int i=0; i<vector.length; ++i )
            if ( vector[i] > 0 )
                ++n;
        return n;
    }

    private ExtCoexpressionMetaValueObject getCustomAnalysisResults( Collection<Long> eeIds, Collection<Gene> genes, int stringency ) {
        ExtCoexpressionMetaValueObject result = new ExtCoexpressionMetaValueObject();
        Collection<ExpressionExperiment> ees = ( eeIds != null && !eeIds.isEmpty() ) ?
                expressionExperimentService.loadMultiple( eeIds ) :
                getPossibleExpressionExperiments( genes );
        
        /* repopulate eeIds with the actual eeIds we'll be searching through and load
         * ExpressionExperimentValueObjects to get summary information about the datasets...
         */
        eeIds.clear();
        for ( ExpressionExperiment ee : ees ) {
            eeIds.add( ee.getId() );
        }
        List<ExpressionExperimentValueObject> eevos = new ArrayList( expressionExperimentService.loadValueObjects( eeIds ) );
        
        result.setQueryGenes( genes );
        result.setDatasets( eevos );
        result.setKnownGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setPredictedGeneDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<ExtCoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<ExtCoexpressionValueObject>() );
        
        /* TODO this is done just naively right now.
         * allow the user to show only interactions among their genes of interest and
         * filter the results before the time-consuming analysis is done...
         */
        for ( Gene queryGene : genes ) {
            CoexpressionCollectionValueObject coexpressions =
                probeLinkCoexpressionAnalyzer.linkAnalysis( queryGene, ees, stringency, !SecurityService.isUserAdmin() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getKnownGeneCoexpression(), stringency, result.getKnownGeneResults(), result.getKnownGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getPredictedCoexpressionType(), stringency, result.getPredictedGeneResults(), result.getPredictedGeneDatasets() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getProbeAlignedCoexpressionType(), stringency, result.getProbeAlignedRegionResults(), result.getProbeAlignedRegionDatasets() );
        }
        
        return result;
    }
    
    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperimentValueObject> eevos, CoexpressedGenesDetails coexp, int stringency, Collection<ExtCoexpressionValueObject> results, Collection<ExtCoexpressionDatasetValueObject> datasetResults ) {
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
            
            Long[] tested = new Long[ eevos.size() ];
            Long[] supported = new Long[ eevos.size() ];
            for ( int i=0; i<eevos.size(); ++i ) {
                ExpressionExperimentValueObject eevo = eevos.get( i );
                tested[i] = cvo.getDatasetsTestedIn().contains( eevo.getId() ) ? eevo.getId() : 0;
                supported[i] = cvo.getExperimentBitIds().contains( eevo.getId() ) ? eevo.getId() : 0;
            }
            ecvo.setTestedDatasetVector( tested );
            ecvo.setSupportingDatasetVector( supported );
            
            results.add( ecvo );
        }
        
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            ExpressionExperimentValueObject coexpEevo = coexp.getExpressionExperiment( eevo.getId() );
            if ( coexpEevo == null )
                continue;
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

    /* I assume the reason Genes weren't being loaded before is that it was too time consuming, so
     * we'll do this instead...
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
            Taxon taxon = (Taxon)o;
            for ( Object p : geneCoexpressionAnalysisService.findByTaxon( taxon ) ) {
                GeneCoexpressionAnalysis analysis = (GeneCoexpressionAnalysis)p;
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
    
    public Collection<Long> findExpressionExperiments( String query ) {
        Collection<Long> eeIds = new HashSet<Long>();
        List<SearchResult> results = searchService.search( SearchSettings.ExpressionExperimentSearch( query ), false ).get( ExpressionExperiment.class );
        for ( SearchResult result : results ) {
            eeIds.add( result.getId() );
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
    
}