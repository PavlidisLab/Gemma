package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.gene.GeneSearchService;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.Set;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.isGoId;
import static ubic.gemma.core.search.SearchSettingsUtils.isFilled;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.extractTermsDnf;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareTermUriQuery;

/**
 * GO-based search source.
 * <p>
 * This does not exactly fit the {@link OntologySearchSource} because it is specialized for the {@link GeneOntologyService}
 * and uses higher-level method to retrieve GO-gene associations.
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

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseGo();
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        SearchResultSet<Gene> results = new SearchResultSet<>( settings );
        Set<Set<String>> dnf = extractTermsDnf( settings );
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
            // find via a full URI
            URI termUri = prepareTermUriQuery( settings );
            if ( termUri != null ) {
                OntologyTerm term = geneOntologyService.getTerm( termUri.toString() );
                if ( term != null ) {
                    for ( Gene g : geneOntologyService.getGenes( term, settings.getTaxon() ) ) {
                        results.add( SearchResult.from( Gene.class, g, 1.0, Collections.emptyMap(), "GeneOntologyService.getGenes using a GO URI" ) );
                    }
                }
            } else {
                // find exact matches of the GO ID
                Collection<Gene> exactMatchResults = geneOntologyService.getGenes( settings.getQuery(), settings.getTaxon() );
                for ( Gene g : exactMatchResults ) {
                    results.add( SearchResult.from( Gene.class, g, 1.0, Collections.emptyMap(), "GeneOntologyService.getGenes using a GO ID" ) );
                }
            }

            return results;
        }

        // find inexact match using full-text query of GO terms
        try {
            Collection<OntologySearchResult<OntologyTerm>> terms = geneOntologyService.findTerm( settings.getQuery(), 2000 );
            // rescale the scores in a [0, 1] range
            DoubleSummaryStatistics summaryStatistics = terms.stream()
                    .mapToDouble( OntologySearchResult::getScore )
                    .summaryStatistics();
            double m = summaryStatistics.getMin();
            double d = summaryStatistics.getMax() - summaryStatistics.getMin();
            for ( OntologySearchResult<OntologyTerm> osr : terms ) {
                for ( Gene g : geneOntologyService.getGenes( osr.getResult(), settings.getTaxon() ) ) {
                    double score;
                    if ( d == 0 ) {
                        score = FULL_TEXT_SCORE_PENALTY;
                    } else {
                        score = FULL_TEXT_SCORE_PENALTY * ( osr.getScore() - m ) / d;
                    }
                    results.add( SearchResult.from( Gene.class, g, score, Collections.emptyMap(), "GeneOntologyService.getGenes via full-text matches" ) );
                }
            }
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( e );
        }

        // find via GO-annotated GeneSet
        for ( Gene g : geneSearchService.getGOGroupGenes( settings.getQuery(), settings.getTaxon() ) ) {
            results.add( SearchResult.from( Gene.class, g, 0.8, Collections.singletonMap( "GO Group", "From GO group" ), "GeneSearchService.getGOGroupGenes" ) );
        }

        return results;
    }
}
