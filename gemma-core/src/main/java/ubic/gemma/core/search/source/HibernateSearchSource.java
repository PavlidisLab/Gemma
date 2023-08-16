package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Search source based on Hibernate Search.
 * @author poirigui
 */
@Component
@CommonsLog
public class HibernateSearchSource implements SearchSource {


    private static final String[] PLATFORM_FIELDS = { "shortName", "name", "description", "alternateNames.name", "externalReferences.accession" };
    private static final String[] PUBLICATION_FIELDS = new String[] { "name", "abstractText",
            "authorList", "chemicals.name", "chemicals.registryNumber",
            "fullTextUri", "keywords.term", "meshTerms.term", "pubAccession.accession", "title" };

    private static String[] DATASET_FIELDS = {
            "shortName", "name", "description", "bioAssays.name", "bioAssays.description", "bioAssays.accession.accession",
            "bioAssays.sampleUsed.name", "bioAssays.sampleUsed.characteristics.value",
            "bioAssays.sampleUsed.characteristics.valueUri", "characteristics.value", "characteristics.valueUri",
            "experimentalDesign.name", "experimentalDesign.description", "experimentalDesign.experimentalFactors.name",
            "experimentalDesign.experimentalFactors.description",
            "experimentalDesign.experimentalFactors.category.categoryUri",
            "experimentalDesign.experimentalFactors.category.category",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.value",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri" };

    private static final String[] GENE_FIELDS = {
            "name", "accessions.accession", "aliases.alias",
            "ensemblId", "ncbiGeneId", "officialName", "officialSymbol", "products.name",
            "products.ncbiGi", "products.accessions.accession", "products.previousNcbiId"
    };

    private static String[] GENE_SET_FIELDS = {
            "name", "description", "characteristics.value", "characteristics.valueUri", "sourceAccession.accession"
    };

    private static final String[] EXPERIMENT_SET_FIELDS = { "name", "description" };

    private static final String[] BIO_SEQUENCE_FIELDS = { "name", "sequenceDatabaseEntry.accession" };

    private static String[] COMPOSITE_SEQUENCE_FIELDS = { "name", "description" };

    private static String[] prefix( String p, String... fields ) {
        return Arrays.stream( fields ).map( f -> p + f ).toArray( String[]::new );
    }

    static {
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "primaryPublication.", PUBLICATION_FIELDS ) );
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "otherRelevantPublications.", PUBLICATION_FIELDS ) );
        // TODO: EXPERIMENT_SET_FIELDS = ArrayUtils.addAll( EXPERIMENT_SET_FIELDS, prefix( "experiments.", DATASET_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "literatureSources.", PUBLICATION_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "members.gene.", GENE_FIELDS ) );
        COMPOSITE_SEQUENCE_FIELDS = ArrayUtils.addAll( COMPOSITE_SEQUENCE_FIELDS, prefix( "biologicalCharacteristic.", BIO_SEQUENCE_FIELDS ) );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) {
        return searchFor( settings, ArrayDesign.class, PLATFORM_FIELDS );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) {
        return searchFor( settings, BibliographicReference.class, PUBLICATION_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) {
        return searchFor( settings, ExpressionExperimentSet.class, EXPERIMENT_SET_FIELDS );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) {
        return searchFor( settings, BioSequence.class, BIO_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) {
        return searchFor( settings, CompositeSequence.class, COMPOSITE_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) {
        return searchFor( settings, ExpressionExperiment.class, DATASET_FIELDS );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) {
        return searchFor( settings, Gene.class, GENE_FIELDS );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) {
        return searchFor( settings, GeneSet.class, GENE_SET_FIELDS );
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchFor( SearchSettings settings, Class<T> clazz, String... fields ) {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.getCurrentSession() );
        QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( clazz )
                .get();
        Query query = queryBuilder.keyword()
                .onFields( fields )
                .matching( settings.getQuery() )
                .createQuery();
        Highlighter highlighter;
        Analyzer analyzer;
        String[] projection;
        if ( settings.getHighlighter() != null ) {
            highlighter = new Highlighter( new SimpleHTMLFormatter(), new QueryScorer( query ) );
            analyzer = fullTextSession.getSearchFactory().getAnalyzer( clazz );
            projection = new String[] { settings.isFillResults() ? FullTextQuery.THIS : FullTextQuery.ID, FullTextQuery.SCORE, FullTextQuery.DOCUMENT };
        } else {
            highlighter = null;
            analyzer = null;
            projection = new String[] { settings.isFillResults() ? FullTextQuery.THIS : FullTextQuery.ID, FullTextQuery.SCORE };
        }
        //noinspection unchecked
        List<Object[]> results = fullTextSession
                .createFullTextQuery( query, clazz )
                .setProjection( projection )
                .list();
        return results.stream()
                .map( r -> searchResultFromRow( r, settings, highlighter, analyzer, fields, clazz ) )
                .collect( Collectors.toList() );
    }

    private <T extends Identifiable> SearchResult<T> searchResultFromRow( Object[] row, SearchSettings settings, @Nullable Highlighter highlighter, @Nullable Analyzer analyzer, String[] fields, Class<T> clazz ) {
        if ( settings.isFillResults() ) {
            //noinspection unchecked
            return SearchResult.from( clazz, ( T ) row[0], ( Float ) row[1], highlighter != null && analyzer != null ? highlightDocument( ( Document ) row[2], highlighter, analyzer, fields ) : null, "hibernateSearch" );
        } else {
            return SearchResult.from( clazz, ( Long ) row[0], ( Float ) row[1], highlighter != null && analyzer != null ? highlightDocument( ( Document ) row[2], highlighter, analyzer, fields ) : null, "hibernateSearch" );
        }
    }

    private Map<String, String> highlightDocument( Document document, Highlighter highlighter, Analyzer analyzer, String[] fields ) {
        Map<String, String> highlights = new HashMap<>();
        for ( Fieldable field : document.getFields() ) {
            if ( !field.isTokenized() || field.isBinary() || !ArrayUtils.contains( fields, field.name() ) ) {
                continue;
            }
            try {
                String bestFragment = highlighter.getBestFragment( analyzer, field.name(), field.stringValue() );
                if ( bestFragment != null ) {
                    highlights.put( field.name(), bestFragment );
                }
            } catch ( IOException | InvalidTokenOffsetsException e ) {
                log.warn( String.format( "Failed to highlight field %s.", field.name() ) );
            }
        }
        return highlights;
    }

    private static class SimpleHTMLFormatter implements Formatter {
        @Override
        public String highlightTerm( String originalText, TokenGroup tokenGroup ) {
            if ( tokenGroup.getTotalScore() <= 0 ) {
                return escapeHtml4( originalText );
            }
            return "<b>" + escapeHtml4( originalText ) + "</b>";
        }
    }
}
