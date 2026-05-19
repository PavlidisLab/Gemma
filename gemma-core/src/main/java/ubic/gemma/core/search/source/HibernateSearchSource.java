package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.FieldAwareSearchSource;
import ubic.gemma.core.search.Highlighter;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search source backed by Hibernate Search 7's local-Lucene backend.
 *
 * <p>This is a Hibernate Search 7 reimplementation of the pre-strip
 * {@code HibernateSearchSource}. Key API changes vs the HS 5 original:
 * <ul>
 *   <li>{@code Search.getFullTextSession(session)} →
 *       {@link Search#session(org.hibernate.engine.spi.SessionImplementor)} returning a
 *       {@link SearchSession} that operates against the current ORM session.</li>
 *   <li>{@code MultiFieldQueryParser} + raw Lucene {@code Query} → HS 7's
 *       {@code SearchScope.predicate()} DSL: per-class predicate over the historical
 *       field list ({@code ALL_FIELDS}/{@code ALL_EXACT_FIELDS}).</li>
 *   <li>{@code FullTextQuery.list()} → {@link org.hibernate.search.engine.search.query.SearchResult#hits()}.</li>
 *   <li>Highlighter integration is deferred to Step 5 of the recce; this build
 *       returns no highlights.</li>
 * </ul>
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class HibernateSearchSource implements FieldAwareSearchSource {

    private static final double FULL_TEXT_SCORE_PENALTY = 0.9;

    private static final String[] PLATFORM_FIELDS = { "shortName", "name", "description", "alternateNames.name", "externalReferences.accession" };
    private static final String[] PLATFORM_EXACT_FIELDS = { "shortName", "name", "alternateNames.name", "externalReferences.accession" };
    private static final String[] PUBLICATION_FIELDS = new String[] { "name", "abstractText",
            "authorList", "chemicals.name", "chemicals.registryNumber",
            "fullTextUri", "keywords.term", "meshTerms.term", "pubAccession.accession", "title" };
    private static final String[] PUBLICATION_EXACT_FIELDS = new String[] { "name", "fullTextUri", "pubAccession.accession" };

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
            "experimentalDesign.experimentalFactors.factorValues.characteristics.predicate",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.predicateUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.object",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.objectUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondPredicate",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondPredicateUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObject",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.secondObjectUri"
    };
    private static String[] DATASET_EXACT_FIELDS = {
            "shortName", "name", "accession.accession"
    };

    private static final String[] GENE_FIELDS = {
            "name", "accessions.accession", "aliases.alias",
            "ensemblId", "ncbiGeneId", "officialName", "officialSymbol", "products.name",
            "products.ncbiGi", "products.accessions.accession", "products.previousNcbiId"
    };
    private static final String[] GENE_EXACT_FIELDS = {
            "name", "accessions.accession", "aliases.alias", "ensemblId", "ncbiGeneId", "officialName", "officialSymbol"
    };

    private static String[] GENE_SET_FIELDS = {
            "name", "description", "characteristics.value", "characteristics.valueUri", "sourceAccession.accession"
    };
    private static final String[] GENE_SET_EXACT_FIELDS = { "name" };

    private static final String[] EXPERIMENT_SET_FIELDS = { "name", "description" };
    private static final String[] EXPERIMENT_SET_EXACT_FIELDS = { "name" };

    private static final String[] BIO_SEQUENCE_FIELDS = { "name", "sequenceDatabaseEntry.accession" };
    private static final String[] BIO_SEQUENCE_EXACT_FIELDS = { "name", "sequenceDatabaseEntry.accession" };

    private static String[] COMPOSITE_SEQUENCE_FIELDS = { "name", "description" };
    private static final String[] COMPOSITE_SEQUENCE_EXACT_FIELDS = { "name" };

    private static final Map<Class<?>, Set<String>> ALL_FIELDS = new HashMap<>();
    private static final Map<Class<?>, Set<String>> ALL_EXACT_FIELDS = new HashMap<>();

    /**
     * Per-indexed-root list of {@code projectable = Projectable.YES} fields. Mirrors the
     * {@code @FullTextField(projectable = Projectable.YES)} annotations on the entity classes.
     *
     * <p>The Step-5 highlighter path projects these out of the Lucene document and routes the
     * raw value through {@link Highlighter#highlight(String, String)} on the SearchContext. The
     * HS 7 native {@code f.highlight(field)} projection is intentionally avoided here because it
     * requires the field schema to declare {@code highlightable = Highlightable.ANY}, a change
     * paired with the Step-6 reindex.</p>
     */
    private static final Map<Class<?>, String[]> PROJECTABLE_FIELDS = new HashMap<>();

    static {
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "primaryPublication.", PUBLICATION_FIELDS ) );
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "otherRelevantPublications.", PUBLICATION_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "literatureSources.", PUBLICATION_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "members.gene.", GENE_FIELDS ) );
        COMPOSITE_SEQUENCE_FIELDS = ArrayUtils.addAll( COMPOSITE_SEQUENCE_FIELDS, prefix( "biologicalCharacteristic.", BIO_SEQUENCE_FIELDS ) );
        ALL_FIELDS.put( ExpressionExperiment.class, new HashSet<>( Arrays.asList( DATASET_FIELDS ) ) );
        ALL_FIELDS.put( ArrayDesign.class, new HashSet<>( Arrays.asList( PLATFORM_FIELDS ) ) );
        ALL_FIELDS.put( CompositeSequence.class, new HashSet<>( Arrays.asList( COMPOSITE_SEQUENCE_FIELDS ) ) );
        ALL_FIELDS.put( BioSequence.class, new HashSet<>( Arrays.asList( BIO_SEQUENCE_FIELDS ) ) );
        ALL_FIELDS.put( Gene.class, new HashSet<>( Arrays.asList( GENE_FIELDS ) ) );
        ALL_FIELDS.put( GeneSet.class, new HashSet<>( Arrays.asList( GENE_SET_FIELDS ) ) );
        ALL_FIELDS.put( ExpressionExperimentSet.class, new HashSet<>( Arrays.asList( EXPERIMENT_SET_FIELDS ) ) );
        ALL_FIELDS.put( BibliographicReference.class, new HashSet<>( Arrays.asList( PUBLICATION_FIELDS ) ) );

        DATASET_EXACT_FIELDS = ArrayUtils.addAll( DATASET_EXACT_FIELDS, prefix( "primaryPublication.", PUBLICATION_EXACT_FIELDS ) );
        DATASET_EXACT_FIELDS = ArrayUtils.addAll( DATASET_EXACT_FIELDS, prefix( "otherRelevantPublications.", PUBLICATION_EXACT_FIELDS ) );
        ALL_EXACT_FIELDS.put( ExpressionExperiment.class, new HashSet<>( Arrays.asList( DATASET_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( ArrayDesign.class, new HashSet<>( Arrays.asList( PLATFORM_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( CompositeSequence.class, new HashSet<>( Arrays.asList( COMPOSITE_SEQUENCE_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( BioSequence.class, new HashSet<>( Arrays.asList( BIO_SEQUENCE_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( Gene.class, new HashSet<>( Arrays.asList( GENE_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( GeneSet.class, new HashSet<>( Arrays.asList( GENE_SET_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( ExpressionExperimentSet.class, new HashSet<>( Arrays.asList( EXPERIMENT_SET_EXACT_FIELDS ) ) );
        ALL_EXACT_FIELDS.put( BibliographicReference.class, new HashSet<>( Arrays.asList( PUBLICATION_EXACT_FIELDS ) ) );

        // Projectable fields per @Indexed root. These match the @FullTextField(projectable = Projectable.YES)
        // annotations on the entity classes; keep in sync if you flip more fields to projectable.
        PROJECTABLE_FIELDS.put( ExpressionExperiment.class, new String[] { "description" } );
        PROJECTABLE_FIELDS.put( ArrayDesign.class, new String[] { "description" } );
        PROJECTABLE_FIELDS.put( CompositeSequence.class, new String[] { "description" } );
        PROJECTABLE_FIELDS.put( GeneSet.class, new String[] { "description" } );
        PROJECTABLE_FIELDS.put( ExpressionExperimentSet.class, new String[] { "description" } );
        PROJECTABLE_FIELDS.put( BibliographicReference.class, new String[] { "abstractText", "authorList", "title" } );
        // Gene + BioSequence have no projectable text fields today.
    }

    private static String[] prefix( String p, String... fields ) {
        return Arrays.stream( fields ).map( f -> p + f ).toArray( String[]::new );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private AclService aclService;

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Override
    public Set<String> getFields( Class<? extends Identifiable> resultType, SearchSettings.SearchMode searchMode ) {
        return searchMode == SearchSettings.SearchMode.EXACT
                ? ALL_EXACT_FIELDS.getOrDefault( resultType, Collections.emptySet() )
                : ALL_FIELDS.getOrDefault( resultType, Collections.emptySet() );
    }

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseFullTextIndex() && settings.getMode().isAtLeast( SearchSettings.SearchMode.FAST );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, ArrayDesign.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? PLATFORM_EXACT_FIELDS : PLATFORM_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, BibliographicReference.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? PUBLICATION_EXACT_FIELDS : PUBLICATION_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, ExpressionExperimentSet.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? EXPERIMENT_SET_EXACT_FIELDS : EXPERIMENT_SET_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<BioSequence>> searchBioSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, BioSequence.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? BIO_SEQUENCE_EXACT_FIELDS : BIO_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, CompositeSequence.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? COMPOSITE_SEQUENCE_EXACT_FIELDS : COMPOSITE_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, ExpressionExperiment.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? DATASET_EXACT_FIELDS : DATASET_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<Gene>> searchGene( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, Gene.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? GENE_EXACT_FIELDS : GENE_FIELDS );
    }

    @Override
    public Collection<ubic.gemma.model.common.search.SearchResult<GeneSet>> searchGeneSet( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchFor( settings, context, GeneSet.class, settings.getMode() == SearchSettings.SearchMode.EXACT ? GENE_SET_EXACT_FIELDS : GENE_SET_FIELDS );
    }

    /**
     * HS 7 search implementation: build a per-class scope, parse the user query through a
     * {@code simpleQueryString} predicate over the field list (an HS-7 native, ASCII-safe
     * alternative to HS 5's {@code MultiFieldQueryParser}), then project entity reference,
     * Lucene score, and (when a {@link Highlighter} is supplied via {@link SearchContext})
     * the values of the per-class {@link #PROJECTABLE_FIELDS} so they can be post-processed
     * into highlight snippets.
     */
    private <T extends Identifiable> Collection<ubic.gemma.model.common.search.SearchResult<T>> searchFor(
            SearchSettings settings, SearchContext context, Class<T> clazz, String... fields ) throws SearchException {
        if ( settings.getQuery() == null || settings.getQuery().trim().isEmpty() ) {
            return Collections.emptyList();
        }
        try {
            Session session = sessionFactory.getCurrentSession();
            SearchSession searchSession = Search.session( session );

            final Highlighter highlighter = context != null ? context.getHighlighter() : null;
            final String[] highlightFields = highlighter != null
                    ? PROJECTABLE_FIELDS.getOrDefault( clazz, new String[0] )
                    : new String[0];

            SearchResult<List<?>> hits = searchSession.search( clazz )
                    .select( f -> {
                        org.hibernate.search.engine.search.projection.SearchProjection<?>[] projections =
                                new org.hibernate.search.engine.search.projection.SearchProjection<?>[2 + highlightFields.length];
                        projections[0] = f.entityReference().toProjection();
                        projections[1] = f.score().toProjection();
                        for ( int i = 0; i < highlightFields.length; i++ ) {
                            projections[2 + i] = f.field( highlightFields[i], String.class ).toProjection();
                        }
                        return f.composite( projections );
                    } )
                    .where( f -> f.simpleQueryString()
                            .fields( fields )
                            .matching( settings.getQuery() )
                            // tolerate Lucene-reserved characters; mirrors the pre-strip parseSafely behaviour.
                            .defaultOperator( org.hibernate.search.engine.search.common.BooleanOperator.OR ) )
                    .fetch( Math.max( settings.getMaxResults(), 1 ) );

            List<List<?>> rows = hits.hits();
            DoubleSummaryStatistics stats = rows.stream().mapToDouble( r -> ( Float ) r.get( 1 ) ).summaryStatistics();

            List<ubic.gemma.model.common.search.SearchResult<T>> results = rows.stream()
                    .map( r -> rowToSearchResult( r, settings, clazz, stats, highlighter, highlightFields ) )
                    .filter( java.util.Objects::nonNull )
                    .collect( Collectors.toList() );

            if ( Securable.class.isAssignableFrom( clazz ) ) {
                //noinspection unchecked
                return filterByAcls( results, ( Class<? extends Securable> ) clazz );
            }
            return results;
        } catch ( org.hibernate.search.util.common.SearchException e ) {
            throw new HibernateSearchException( String.format( "Error while searching for %s.", clazz.getName() ), e );
        }
    }

    private <T extends Identifiable> ubic.gemma.model.common.search.SearchResult<T> rowToSearchResult(
            List<?> row, SearchSettings settings, Class<T> clazz, DoubleSummaryStatistics stats,
            Highlighter highlighter, String[] highlightFields ) {
        Object refObj = row.get( 0 );
        Float scoreF = ( Float ) row.get( 1 );
        double score;
        if ( stats.getMax() == stats.getMin() ) {
            score = FULL_TEXT_SCORE_PENALTY;
        } else {
            score = FULL_TEXT_SCORE_PENALTY * ( scoreF - stats.getMin() ) / ( stats.getMax() - stats.getMin() );
        }
        // entity reference exposes the id as Object — entities use Long primary keys throughout Gemma.
        Long id;
        if ( refObj instanceof org.hibernate.search.engine.common.EntityReference ) {
            Object raw = ( ( org.hibernate.search.engine.common.EntityReference ) refObj ).id();
            id = ( raw instanceof Long ) ? ( Long ) raw : Long.valueOf( raw.toString() );
        } else {
            return null;
        }
        Map<String, String> highlights = null;
        if ( highlighter != null && highlightFields.length > 0 ) {
            highlights = new HashMap<>();
            for ( int i = 0; i < highlightFields.length; i++ ) {
                Object v = row.get( 2 + i );
                if ( v instanceof String && !( ( String ) v ).isEmpty() ) {
                    highlights.putAll( highlighter.highlight( ( String ) v, highlightFields[i] ) );
                }
            }
            if ( highlights.isEmpty() ) {
                highlights = null;
            }
        }
        if ( settings.isFillResults() ) {
            T entity = sessionFactory.getCurrentSession().get( clazz, id );
            if ( entity == null || entity.getId() == null ) {
                // entity vanished out from under the index — skip the stale hit.
                return null;
            }
            return ubic.gemma.model.common.search.SearchResult.from( clazz, entity, score, highlights, "hibernateSearch" );
        } else {
            return ubic.gemma.model.common.search.SearchResult.from( clazz, id, score, highlights, "hibernateSearch" );
        }
    }

    /**
     * Filter search results by ACLs (gsec → ubic.gemma.core.security rename has shifted the
     * imports above; the algorithm is unchanged).
     */
    private <T extends Identifiable> Collection<ubic.gemma.model.common.search.SearchResult<T>> filterByAcls(
            Collection<ubic.gemma.model.common.search.SearchResult<T>> results, Class<? extends Securable> resultType ) {
        if ( results.isEmpty() ) {
            return results;
        }
        List<Sid> sids = sidRetrievalStrategy.getSids( SecurityContextHolder.getContext().getAuthentication() );
        List<ObjectIdentity> aclIdentities = results.stream()
                .map( r -> new AclObjectIdentity( resultType, r.getResultId() ) )
                .collect( Collectors.toList() );
        Set<Long> filteredIds = aclService.readAclsById( aclIdentities ).values().stream()
                .filter( acl -> acl.isGranted( Collections.singletonList( BasePermission.READ ), sids, false ) )
                .map( acl -> ( Long ) acl.getObjectIdentity().getIdentifier() )
                .collect( Collectors.toSet() );
        return results.stream()
                .filter( s -> filteredIds.contains( s.getResultId() ) )
                .collect( Collectors.toList() );
    }
}
