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
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.coexpression.ProbeLinkCoexpressionAnalyzer;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
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
        result.setDatasets( new ArrayList( analysis.getExperimentsAnalyzed() ) );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        for ( Gene queryGene : genes ) {
            for ( Object o : gene2GeneCoexpressionService.findCoexpressionRelationships( queryGene, analysis, stringency ) ) {
                Gene2GeneCoexpression g2g = (Gene2GeneCoexpression)o;
                ExtCoexpressionValueObject ecvo = new ExtCoexpressionValueObject();
                ecvo.setQueryGene( g2g.getFirstGene() );
                ecvo.setFoundGene( g2g.getSecondGene() );
                if ( g2g.getEffect() < 0 ) {
                    ecvo.setPositiveLinks( 0 );
                    ecvo.setNegativeLinks( countBits( g2g.getDatasetsSupportingVector() ) );
                } else {
                    ecvo.setPositiveLinks( countBits( g2g.getDatasetsSupportingVector() ) );
                    ecvo.setNegativeLinks( 0 );
                }
                ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
                ecvo.setNumDatasetsLinkTestedIn( countBits( g2g.getDatasetsTestedVector() ) );
                ecvo.setGoOverlap( 0 );
                ecvo.setPossibleOverlap( 0 );
                ecvo.setTestedDatasetVector( g2g.getDatasetsTestedVector() );
                ecvo.setSupportingDatasetVector( g2g.getDatasetsSupportingVector() );
                result.getKnownGeneResults().add( ecvo );
            }
        }
        return result;
    }
    
    private int countBits( byte[] vector ) {
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
        result.setDatasets( new ArrayList( ees ) );
        result.setKnownGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setPredictedGeneResults( new ArrayList<ExtCoexpressionValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<ExtCoexpressionValueObject>() );
        
        /* TODO this is done just naively right now.
         * allow the user to show only interactions among their genes of interest and
         * filter the results before the time-consuming analysis is done...
         */
        for ( Gene queryGene : genes ) {
            CoexpressionCollectionValueObject coexpressions =
                probeLinkCoexpressionAnalyzer.linkAnalysis( queryGene, ees, stringency, !SecurityService.isUserAdmin() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getKnownGeneCoexpressionData( stringency ), result.getKnownGeneResults() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getPredictedCoexpressionData( stringency ), result.getPredictedGeneResults() );
            addExtCoexpressionValueObjects( queryGene, result.getDatasets(), coexpressions.getProbeAlignedCoexpressionData( stringency ), result.getProbeAlignedRegionResults() );
        }
        
        return result;
    }
    
    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperiment> ees, List<CoexpressionValueObject> coexp, Collection<ExtCoexpressionValueObject> results ) {
        for ( CoexpressionValueObject cvo : coexp ) {
            ExtCoexpressionValueObject ecvo = new ExtCoexpressionValueObject();
            ecvo.setQueryGene( queryGene );
            ecvo.setFoundGene( new SimpleGene( cvo.getGeneId(), cvo.getGeneName(), cvo.getGeneOfficialName() ) );
            
            ecvo.setPositiveLinks( cvo.getPositiveLinkSupport() );
            ecvo.setNegativeLinks( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( ecvo.getPositiveLinks() - ecvo.getNegativeLinks() );
            ecvo.setNumDatasetsLinkTestedIn( cvo.getNumDatasetsTestedIn() );
            
            ecvo.setGoOverlap( cvo.getGoOverlap() != null ? cvo.getGoOverlap().size() : 0 );
            ecvo.setPossibleOverlap( cvo.getPossibleOverlap() );
            
            // these are here because I have to cast if I assign directly in the loop; try it, it's weird...
            final byte ON=1, OFF=0;
            byte[] tested = new byte[ ees.size() ];
            byte[] supported = new byte[ ees.size() ];
            for ( int i=0; i<ees.size(); ++i ) {
                ExpressionExperiment ee = ees.get( i );
                tested[i] = cvo.getDatasetsTestedIn().contains( ee.getId() ) ? ON : OFF;
                supported[i] = cvo.getExperimentBitIds().contains( ee.getId() ) ? ON : OFF;
            }
            ecvo.setTestedDatasetVector( tested );
            ecvo.setSupportingDatasetVector( supported );
            results.add( ecvo );
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
        for ( SearchResult result : searchService.search( SearchSettings.ExpressionExperimentSearch( query ) ).get( ExpressionExperiment.class ) ) {
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