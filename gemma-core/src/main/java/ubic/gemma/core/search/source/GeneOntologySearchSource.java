package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.BaseCodeOntologySearchException;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchResultSet;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneSearchService;

import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.Set;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.isGoId;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.extractTermsDnf;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.quote;
import static ubic.gemma.core.search.source.SearchSourceUtils.isFilled;

/**
 * GO-based search source.
 * <p>
 * This does not exactly fit the {@link OntologySearchSource} because it is specialized for the {@link GeneOntologyService}
 * and uses higher-level method to retrieve GO-gene associations.
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class GeneOntologySearchSource implements SearchSource {

    /**
     * Penalty applied on a full-text result.
     */
    private static final double FULL_TEXT_SCORE_PENALTY = 0.9;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneSearchService geneSearchService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseGeneOntology()
                && settings.hasResultType( Gene.class )
                && settings.getMode().isAtLeast( SearchSettings.SearchMode.FAST );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings, SearchContext context ) throws SearchException {
        Collection<OntologySearchResult<OntologyTerm>> terms = findTerms( quote( settings.getQuery() ) );
        if ( !terms.isEmpty() ) {
            SearchResultSet<Gene> results = new SearchResultSet<>( settings );
            findGenesByTerms( terms, settings, results );
            return results;
        }

        SearchResultSet<Gene> results = new SearchResultSet<>( settings );
        Set<Set<String>> dnf = extractTermsDnf( settings, context.getIssueReporter() );
        for ( Set<String> clause : dnf ) {
            SearchResultSet<Gene> clauseResults = new SearchResultSet<>( settings );
            for ( String term : clause ) {
                if ( clauseResults.isEmpty() ) {
                    clauseResults.addAll( doSearchGene( settings.withQuery( term ) ) );
                } else {
                    clauseResults.retainAll( doSearchGene( settings.withQuery( term ) ) );
                }
                // if there are no elements in common, we can move on to the next clause
                if ( clauseResults.isEmpty() ) {
                    break;
                }
            }
            results.addAll( clauseResults );
            if ( isFilled( results, settings ) )
                break;
        }
        return results;
    }

    private Collection<SearchResult<Gene>> doSearchGene( SearchSettings settings ) throws SearchException {
        SearchResultSet<Gene> results = new SearchResultSet<>( settings );

        if ( isGoId( settings.getQuery() ) ) {
            // find via a full URI or GO identifier
            Collection<Gene> exactMatchResults = filterGenesByExperimentAndPlatformConstraints( geneOntologyService.getGenes( settings.getQuery(), settings.getTaxonConstraint() ), settings );
            for ( Gene g : exactMatchResults ) {
                results.add( SearchResult.from( Gene.class, g, 1.0, Collections.emptyMap(), "GeneOntologyService.getGenes using a GO URI" ) );
            }
            return results;
        }

        // find inexact match using full-text query of GO terms
        Collection<OntologySearchResult<OntologyTerm>> terms = findTerms( settings.getQuery() );
        findGenesByTerms( terms, settings, results );

        // find via GO-annotated GeneSet
        for ( Gene g : geneSearchService.getGOGroupGenes( settings.getQuery(), settings.getTaxonConstraint() ) ) {
            results.add( SearchResult.from( Gene.class, g, 0.8, Collections.singletonMap( "GO Group", "From GO group" ), "GeneSearchService.getGOGroupGenes" ) );
        }

        return results;
    }

    private Collection<OntologySearchResult<OntologyTerm>> findTerms( String query ) throws BaseCodeOntologySearchException {
        try {
            return geneOntologyService.findTerm( query, 2000 );
        } catch ( OntologySearchException e ) {
            try {
                return geneOntologyService.findTerm( LuceneQueryUtils.escape( query ), 2000 );
            } catch ( OntologySearchException ex ) {
                throw new BaseCodeOntologySearchException( e );
            }
        }
    }

    private void findGenesByTerms( Collection<OntologySearchResult<OntologyTerm>> terms, SearchSettings settings, SearchResultSet<Gene> results ) {
        // rescale the scores in a [0, 1] range
        DoubleSummaryStatistics summaryStatistics = terms.stream()
                .mapToDouble( OntologySearchResult::getScore )
                .summaryStatistics();
        double m = summaryStatistics.getMin();
        double d = summaryStatistics.getMax() - summaryStatistics.getMin();
        for ( OntologySearchResult<OntologyTerm> osr : terms ) {
            for ( Gene g : filterGenesByExperimentAndPlatformConstraints( geneOntologyService.getGenes( osr.getResult(), settings.getTaxonConstraint() ), settings ) ) {
                double score;
                if ( d == 0 ) {
                    score = FULL_TEXT_SCORE_PENALTY;
                } else {
                    score = FULL_TEXT_SCORE_PENALTY * ( osr.getScore() - m ) / d;
                }
                results.add( SearchResult.from( Gene.class, g, score, Collections.emptyMap(), "GeneOntologyService.getGenes via full-text matches" ) );
            }
        }
    }

    private Collection<Gene> filterGenesByExperimentAndPlatformConstraints( Collection<Gene> genes, SearchSettings settings ) {
        if ( settings.getPlatformConstraint() != null ) {
            // query all genes for the given platform?
            genes.retainAll( arrayDesignService.getGenes( settings.getPlatformConstraint() ) );
        }
        if ( settings.getDatasetConstraint() != null ) {
            // query all the genes used by the *preferred* set of vectors, or any vector?
            genes.retainAll( expressionExperimentService.getGenesUsedByPreferredVectors( settings.getDatasetConstraint() ) );
        }
        return genes;
    }
}
