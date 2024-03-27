package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.util.Version;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.FieldAwareSearchSource;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
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
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.QueryUtils.parseSafely;

/**
 * Search source based on Hibernate Search.
 * @author poirigui
 */
@Component
@CommonsLog
public class HibernateSearchSource implements FieldAwareSearchSource, InitializingBean {

    private static final double FULL_TEXT_SCORE_PENALTY = 0.9;

    private static final Class<?>[] SEARCHABLE_CLASSES = new Class[] {
            ExpressionExperiment.class,
            ArrayDesign.class,
            CompositeSequence.class,
            BioSequence.class,
            Gene.class,
            GeneSet.class,
            ExpressionExperimentSet.class,
            BibliographicReference.class
    };

    private static final String[] PLATFORM_FIELDS = { "shortName", "name", "description", "alternateNames.name", "externalReferences.accession" };
    private static final String[] PUBLICATION_FIELDS = new String[] { "name", "abstractText",
            "authorList", "chemicals.name", "chemicals.registryNumber",
            "fullTextUri", "keywords.term", "meshTerms.term", "pubAccession.accession", "title" };

    private static String[] DATASET_FIELDS = {
            "shortName", "name", "description", "accession.accession",
            "bioAssays.name", "bioAssays.description", "bioAssays.accession.accession", "bioAssays.sampleUsed.name",
            "bioAssays.sampleUsed.characteristics.value", "bioAssays.sampleUsed.characteristics.valueUri",
            "characteristics.value", "characteristics.valueUri",
            "experimentalDesign.name", "experimentalDesign.description", "experimentalDesign.experimentalFactors.name",
            "experimentalDesign.experimentalFactors.description",
            "experimentalDesign.experimentalFactors.category.categoryUri",
            "experimentalDesign.experimentalFactors.category.category",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.value",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.object",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.objectUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObject",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObjectUri"
    };

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

    private static final Map<Class<?>, Set<String>> ALL_FIELDS = new HashMap<>();

    static {
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "primaryPublication.", PUBLICATION_FIELDS ) );
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "otherRelevantPublications.", PUBLICATION_FIELDS ) );
        // TODO: EXPERIMENT_SET_FIELDS = ArrayUtils.addAll( EXPERIMENT_SET_FIELDS, prefix( "experiments.", DATASET_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "literatureSources.", PUBLICATION_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "members.gene.", GENE_FIELDS ) );
        COMPOSITE_SEQUENCE_FIELDS = ArrayUtils.addAll( COMPOSITE_SEQUENCE_FIELDS, prefix( "biologicalCharacteristic.", BIO_SEQUENCE_FIELDS ) );
        ALL_FIELDS.put( ExpressionExperiment.class, new HashSet<>( Arrays.asList( DATASET_FIELDS ) ) );
        ALL_FIELDS.put( ArrayDesign.class, new HashSet<>( Arrays.asList( PLATFORM_FIELDS ) ) );
        ALL_FIELDS.put( CompositeSequence.class, new HashSet<>( Arrays.asList( COMPOSITE_SEQUENCE_FIELDS ) ) );
        ALL_FIELDS.put( BioSequence.class, new HashSet<>( Arrays.asList( BIO_SEQUENCE_FIELDS ) ) );
        ALL_FIELDS.put( Gene.class, new HashSet<>( Arrays.asList( GENE_FIELDS ) ) );
        ALL_FIELDS.put( GeneSet.class, new HashSet<>( Arrays.asList( GENE_SET_FIELDS ) ) );
    }

    private static String[] prefix( String p, String... fields ) {
        return Arrays.stream( fields ).map( f -> p + f ).toArray( String[]::new );
    }

    @Autowired
    private SessionFactory sessionFactory;

    private final Map<Class<?>, Analyzer> analyzers = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );
        try {
            for ( Class<?> clazz : SEARCHABLE_CLASSES ) {
                analyzers.put( clazz, fullTextSession.getSearchFactory().getAnalyzer( clazz ) );
            }
        } finally {
            fullTextSession.close();
        }
    }

    @Override
    public Set<String> getFields( Class<? extends Identifiable> entityClass ) {
        return ALL_FIELDS.getOrDefault( entityClass, Collections.emptySet() );
    }

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        return searchFor( settings, ArrayDesign.class, PLATFORM_FIELDS );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException {
        return searchFor( settings, BibliographicReference.class, PUBLICATION_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return searchFor( settings, ExpressionExperimentSet.class, EXPERIMENT_SET_FIELDS );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        return searchFor( settings, BioSequence.class, BIO_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return searchFor( settings, CompositeSequence.class, COMPOSITE_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        return searchFor( settings, ExpressionExperiment.class, DATASET_FIELDS );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        return searchFor( settings, Gene.class, GENE_FIELDS );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return searchFor( settings, GeneSet.class, GENE_SET_FIELDS );
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchFor( SearchSettings settings, Class<T> clazz, String... fields ) throws SearchException {
        try {
            FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.getCurrentSession() );
            Analyzer analyzer = analyzers.get( clazz );
            QueryParser queryParser = new MultiFieldQueryParser( Version.LUCENE_36, fields, analyzer );
            Query query = parseSafely( settings, queryParser );
            Highlighter highlighter = settings.getHighlighter() != null ? new Highlighter( settings.getHighlighter().getFormatter(), new QueryScorer( query ) ) : null;
            String[] projection;
            if ( highlighter != null ) {
                projection = new String[] { settings.isFillResults() ? FullTextQuery.THIS : FullTextQuery.ID, FullTextQuery.SCORE, FullTextQuery.DOCUMENT };
            } else {
                projection = new String[] { settings.isFillResults() ? FullTextQuery.THIS : FullTextQuery.ID, FullTextQuery.SCORE };
            }
            //noinspection unchecked
            List<Object[]> results = fullTextSession
                    .createFullTextQuery( query, clazz )
                    .setProjection( projection )
                    .setMaxResults( settings.getMaxResults() )
                    .setCacheable( true )
                    .list();
            StopWatch timer = StopWatch.createStarted();
            try {
                DoubleSummaryStatistics stats = results.stream().mapToDouble( r -> ( Float ) r[1] ).summaryStatistics();
                return results.stream()
                        .map( r -> searchResultFromRow( r, settings, highlighter, analyzer, clazz, stats ) )
                        .filter( Objects::nonNull )
                        .collect( Collectors.toList() );
            } finally {
                if ( timer.getTime() > 100 ) {
                    log.warn( String.format( "Highlighting %d results took %d ms", results.size(), timer.getTime() ) );
                }
            }
        } catch ( org.hibernate.search.SearchException e ) {
            throw new HibernateSearchException( String.format( "Error while searching for %s.", clazz.getName() ), e );
        }
    }

    @Nullable
    private <T extends Identifiable> SearchResult<T> searchResultFromRow( Object[] row, SearchSettings settings, @Nullable Highlighter highlighter, Analyzer analyzer, Class<T> clazz, DoubleSummaryStatistics stats ) {
        double score;
        if ( stats.getMax() == stats.getMin() ) {
            score = FULL_TEXT_SCORE_PENALTY;
        } else {
            score = FULL_TEXT_SCORE_PENALTY * ( ( Float ) row[1] - stats.getMin() ) / ( stats.getMax() - stats.getMin() );
        }
        if ( settings.isFillResults() ) {
            //noinspection unchecked
            T entity = ( T ) row[0];
            if ( row[0] == null ) {
                // this happens if an entity is still in the cache, but was removed from the database
                return null;
            }
            return SearchResult.from( clazz, entity, score, highlighter != null ? settings.highlightDocument( ( Document ) row[2], highlighter, analyzer ) : null, "hibernateSearch" );
        } else {
            return SearchResult.from( clazz, ( Long ) row[0], score, highlighter != null ? settings.highlightDocument( ( Document ) row[2], highlighter, analyzer ) : null, "hibernateSearch" );
        }
    }
}
