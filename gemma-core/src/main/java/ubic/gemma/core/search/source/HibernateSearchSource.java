package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Search source based on Hibernate Search.
 * @author poirigui
 */
@Component
@CommonsLog
public class HibernateSearchSource implements SearchSource, InitializingBean {

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
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, ArrayDesign.class, PLATFORM_FIELDS );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, BibliographicReference.class, PUBLICATION_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, ExpressionExperimentSet.class, EXPERIMENT_SET_FIELDS );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, BioSequence.class, BIO_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, CompositeSequence.class, COMPOSITE_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, ExpressionExperiment.class, DATASET_FIELDS );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, Gene.class, GENE_FIELDS );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws HibernateSearchException {
        return searchFor( settings, GeneSet.class, GENE_SET_FIELDS );
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchFor( SearchSettings settings, Class<T> clazz, String... fields ) throws HibernateSearchException {
        try {
            FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.getCurrentSession() );
            Analyzer analyzer = analyzers.get( clazz );
            QueryParser queryParser = new MultiFieldQueryParser( Version.LUCENE_36, fields, analyzer );
            Query query;
            try {
                query = queryParser.parse( settings.getQuery() );
            } catch ( ParseException e ) {
                throw new org.hibernate.search.SearchException( e );
            }
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
                Set<String> fieldsSet = new HashSet<>( Arrays.asList( fields ) );
                return results.stream()
                        .map( r -> searchResultFromRow( r, settings, highlighter, analyzer, fieldsSet, clazz ) )
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
    private <T extends Identifiable> SearchResult<T> searchResultFromRow( Object[] row, SearchSettings settings, @Nullable Highlighter highlighter, Analyzer analyzer, Set<String> fields, Class<T> clazz ) {
        if ( settings.isFillResults() ) {
            //noinspection unchecked
            T entity = ( T ) row[0];
            if ( row[0] == null ) {
                // this happens if an entity is still in the cache, but was removed from the database
                return null;
            }
            return SearchResult.from( clazz, entity, ( Float ) row[1], highlighter != null ? settings.highlightDocument( ( Document ) row[2], highlighter, analyzer ) : null, "hibernateSearch" );
        } else {
            return SearchResult.from( clazz, ( Long ) row[0], ( Float ) row[1], highlighter != null ? settings.highlightDocument( ( Document ) row[2], highlighter, analyzer ) : null, "hibernateSearch" );
        }
    }
}
