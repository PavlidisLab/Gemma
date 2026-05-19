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
 *
 * <p>This does not exactly fit the {@link OntologySearchSource} because it is
 * specialized for the {@link GeneOntologyService} and uses higher-level methods
 * to retrieve GO-gene associations.
 *
 * <p><b>Phase 3 restoration.</b> This class was deleted wholesale in the Phase 2
 * "stub/delete search subsystem cascade" commit (ed93c2f023) and is restored
 * here per {@code SEARCH_RECCE.md} Section 6.3 Step 6. The body is the
 * pre-strip implementation (167 LoC) restored verbatim.
 *
 * <p><b>Known runtime gap (2026-05-19).</b> Like {@link OntologySearchSource},
 * this source consumes baseCode's {@link GeneOntologyService#findTerm(String, int)},
 * which routes through baseCode's pre-renovations Lucene-3 indexer (now stubbed
 * to always return null). GO full-text search will therefore return empty
 * results until either baseCode gains a public hook for the in-Gemma
 * {@link ubic.gemma.core.ontology.search.OntologySearchService} (jena-text /
 * Lucene 9), or {@code GeneOntologyServiceImpl.findTerm} is rewired in Gemma to
 * consult that service directly over a TDB / OntModel it controls. The
 * 3 disabled tests in {@code GeneOntologyServiceTest} are the witness for this
 * gap. The {@link GeneOntologyService#getGenes(String, ubic.gemma.model.genome.Taxon)}
 * exact-GO-ID path remains functional.
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
            Collection<Gene> exactMatchResults = filterGenesByExperimentAndPlatformConstraints( geneOntologyService.getGenes( settings.getQuery(), settings.getTaxonConstraint() ), settings );
            for ( Gene g : exactMatchResults ) {
                results.add( SearchResult.from( Gene.class, g, 1.0, Collections.emptyMap(), "GeneOntologyService.getGenes using a GO URI" ) );
            }
            return results;
        }

        Collection<OntologySearchResult<OntologyTerm>> terms = findTerms( settings.getQuery() );
        findGenesByTerms( terms, settings, results );

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
            genes.retainAll( arrayDesignService.getGenes( settings.getPlatformConstraint(), true ) );
        }
        if ( settings.getDatasetConstraint() != null ) {
            genes.retainAll( expressionExperimentService.getGenesUsedByPreferredVectors( settings.getDatasetConstraint() ) );
        }
        return genes;
    }
}
