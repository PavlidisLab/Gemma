package ubic.gemma.persistence.service.expression.experiment;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.ListUtils.partition;
import static ubic.gemma.persistence.util.SubqueryUtils.guessAliases;

/**
 * Helper service for rewriting {@link Filters} to provide more rich results.
 * @author poirigui
 */
@Service
class ExpressionExperimentFilterRewriteHelperService {

    /**
     * List of properties that will be rewritten to include an additional sub-clause for the "second" property.
     */
    private static final Set<String> PROPERTIES_WITH_SECOND_PROPERTIES = new HashSet<>( Arrays.asList(
            "allCharacteristics.object",
            "allCharacteristics.objectUri",
            "allCharacteristics.predicate",
            "allCharacteristics.predicateUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.predicate",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.predicateUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.object",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.objectUri"
    ) );

    /**
     * Only the mention of these properties will result in inferred term expansion.
     * <p>
     * Note: we do not apply inference to category URIs as they are (a) too broad and (b) their sub-terms are never used.
     */
    private static final Set<String> PROPERTIES_USED_FOR_ANNOTATIONS = new HashSet<>( Arrays.asList(
            "allCharacteristics.valueUri",
            "allCharacteristics.subjectUri",
            "allCharacteristics.predicateUri",
            "allCharacteristics.objectUri",
            "characteristics.valueUri",
            "bioAssays.sampleUsed.characteristics.valueUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.subjectUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.predicateUri",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.objectUri"
    ) );

    private final OntologyService ontologyService;

    @Autowired
    public ExpressionExperimentFilterRewriteHelperService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    public Filters getFiltersWithAdditionalProperties( Filters f ) {
        Filters f2 = Filters.empty();
        for ( List<Filter> clause : f ) {
            Filters.FiltersClauseBuilder builder = f2.and();
            for ( Filter subClause : clause ) {
                builder = builder.or( subClause );
                String originalProperty = FiltersUtils.unnestSubquery( subClause )
                        .getOriginalProperty();
                if ( originalProperty == null ) {
                    continue; // only rewrite clauses that have an original property
                }
                String propertyName = FiltersUtils.unnestSubquery( subClause )
                        .getPropertyName();
                if ( PROPERTIES_WITH_SECOND_PROPERTIES.contains( originalProperty ) ) {
                    // keep the original property as it is, the Filter.toOriginalString() only renders distinct clauses,
                    // so the "second" clause will not show up to the user
                    builder = builder.or( subClause.withPropertyName( "second" + StringUtils.capitalize( propertyName ), originalProperty ) );
                }
            }
            builder.build();
        }
        return f2;
    }

    public boolean supportsInferredAnnotations( String propertyName ) {
        return PROPERTIES_USED_FOR_ANNOTATIONS.contains( propertyName );
    }

    /**
     * The approach here is to construct a collection for each sub-clause in the expression that regroups all the
     * predicates that apply to characteristics as well as their inferred terms.
     * <p>
     * The transformation only applies to properties that represent {@link Characteristic} objects such as {@code characteristics},
     * {@code allCharacteristics}, {@code bioAssays.sample.characteristics} and {@code experimentalDesign.experimentalFactors.factorValues.characteristics}
     * <p>
     * Given {@code characteristics.valueUri = a}, we construct a collection clause such as
     * {@code characteristics.valueUri in (a, children of a...)}.
     * <p>
     * For efficiency, all the terms mentioned in a sub-clause are grouped by {@link SubClauseKey} and aggregated in a
     * single collection. If a term is mentioned multiple times, it is simplified as a single appearance in the
     * collection.
     * <p>
     * For example, {@code characteristics.termUri = a or characteristics.termUri = b} will be transformed into {@code characteristics.termUri in (a, b, children of a and b...)}.
     */
    public Filters getFiltersWithInferredAnnotations( Filters f, String rootObjectAlias, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
        Filters f2 = Filters.empty();
        // apply inference to terms
        // collect clauses mentioning terms
        final Map<SubClauseKey, Set<String>> termUrisBySubClause = new HashMap<>();
        for ( List<Filter> clause : f ) {
            Filters.FiltersClauseBuilder clauseBuilder = f2.and();
            for ( Filter subClause : clause ) {
                if ( PROPERTIES_USED_FOR_ANNOTATIONS.contains( subClause.getOriginalProperty() ) ) {
                    clauseBuilder = addSubClausesWithInferredAnnotations( clauseBuilder, rootObjectAlias, subClause, termUrisBySubClause,
                            Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), timer,
                            mentionedTerms, inferredTerms );
                } else {
                    // clause is irrelevant, so we add it as it is
                    clauseBuilder = clauseBuilder.or( subClause );
                }
            }
            // recreate a clause with inferred terms
            for ( Map.Entry<SubClauseKey, Set<String>> e : termUrisBySubClause.entrySet() ) {
                clauseBuilder = addSubClausesWithInferredAnnotations( clauseBuilder, rootObjectAlias,
                        requireNonNull( e.getKey().getObjectAlias() ), e.getKey().getPropertyName(),
                        requireNonNull( e.getKey().getOriginalProperty() ), true, true, e.getValue(),
                        Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), timer,
                        mentionedTerms, inferredTerms );
            }
            f2 = clauseBuilder.build();
            termUrisBySubClause.clear();
        }
        return f2;
    }

    private Filters.FiltersClauseBuilder addSubClausesWithInferredAnnotations( Filters.FiltersClauseBuilder clauseBuilder,
            String rootObjectAlias,
            Filter subClause,
            Map<SubClauseKey, Set<String>> termUrisBySubClause,
            long timeoutMillis, StopWatch timer,
            @Nullable Collection<OntologyTerm> mentionedTerms,
            @Nullable Collection<OntologyTerm> inferredTerms ) throws TimeoutException {
        if ( canBeGroupedWithOtherSubClauses( subClause ) ) {
            // this is the best case scenario where the sub-clause is simple or is a simple subquery that can be
            // unnested and grouped with other subclauses
            // rewrite positive clauses to contain all the inferred terms
            subClause = FiltersUtils.unnestSubquery( subClause );
            SubClauseKey key = SubClauseKey.from( subClause.getObjectAlias(), subClause.getPropertyName(), subClause.getOriginalProperty() );
            if ( subClause.getRequiredValue() instanceof Collection ) {
                //noinspection unchecked
                termUrisBySubClause.computeIfAbsent( key, k -> new HashSet<>() )
                        .addAll( ( Collection<String> ) subClause.getRequiredValue() );
            } else if ( subClause.getRequiredValue() instanceof String ) {
                termUrisBySubClause.computeIfAbsent( key, k -> new HashSet<>() )
                        .add( ( String ) subClause.getRequiredValue() );
            } else {
                clauseBuilder = clauseBuilder.or( subClause );
            }
        } else if ( subClause.getOperator() == Filter.Operator.notEq
                || subClause.getOperator() == Filter.Operator.notIn
                // handle subqueries with a non-nested clause
                || ( subClause.getRequiredValue() instanceof Subquery && eqOrIn( ( ( Subquery ) subClause.getRequiredValue() ).getFilter() ) ) ) {
            boolean inSubquery;
            boolean inClause;
            if ( subClause.getRequiredValue() instanceof Subquery ) {
                Subquery subquery = ( Subquery ) subClause.getRequiredValue();
                inSubquery = subClause.getOperator() == Filter.Operator.inSubquery;
                inClause = subquery.getFilter().getOperator() == Filter.Operator.eq || subquery.getFilter().getOperator() == Filter.Operator.in;
                // unnest a single level
                subClause = ( ( Subquery ) subClause.getRequiredValue() ).getFilter();
            } else {
                // for negative operator, do a negative subquery with a positive clause
                inSubquery = false;
                inClause = true;
            }
            if ( subClause.getRequiredValue() instanceof Collection ) {
                //noinspection unchecked
                clauseBuilder = addSubClausesWithInferredAnnotations( clauseBuilder, rootObjectAlias, requireNonNull( subClause.getObjectAlias() ), subClause.getPropertyName(), requireNonNull( subClause.getOriginalProperty() ), inSubquery, inClause, ( Collection<String> ) subClause.getRequiredValue(), timeoutMillis, timer, mentionedTerms, inferredTerms );
            } else if ( subClause.getRequiredValue() instanceof String ) {
                clauseBuilder = addSubClausesWithInferredAnnotations( clauseBuilder, rootObjectAlias, requireNonNull( subClause.getObjectAlias() ), subClause.getPropertyName(), requireNonNull( subClause.getOriginalProperty() ), inSubquery, inClause, Collections.singleton( ( String ) subClause.getRequiredValue() ), timeoutMillis, timer, mentionedTerms, inferredTerms );
            } else {
                clauseBuilder = clauseBuilder.or( subClause );
            }
        } else {
            // none of the supported cases, no inference will be done
            clauseBuilder = clauseBuilder.or( subClause );
        }
        return clauseBuilder;
    }

    /**
     * Check if a sub-clause can be grouped and merged with other sub-clauses on the same object alias and property.
     * <p>
     * For example, if you have {@code valueUri = a or valueUri = b}, the two can be grouped into {@code valueUri in (a, b)}.
     */
    private boolean canBeGroupedWithOtherSubClauses( Filter filter ) {
        return filter.getOperator() == Filter.Operator.eq
                || filter.getOperator() == Filter.Operator.in
                || ( filter.getOperator() == Filter.Operator.inSubquery && filter.getRequiredValue() instanceof Subquery && canBeGroupedWithOtherSubClauses( ( ( Subquery ) filter.getRequiredValue() ).getFilter() ) );
    }

    private boolean eqOrIn( Filter filter ) {
        return filter.getOperator() == Filter.Operator.eq
                || filter.getOperator() == Filter.Operator.in
                || filter.getOperator() == Filter.Operator.notEq
                || filter.getOperator() == Filter.Operator.notIn;
    }

    /**
     * Add sub-clauses with inferred annotations to the clause builder.
     * <p>
     * More than one sub-clause can be added if the collection of URIs is too large and batching is necessary. Note that
     * batching is only supported for positive subqueries.
     */
    private Filters.FiltersClauseBuilder addSubClausesWithInferredAnnotations(
            Filters.FiltersClauseBuilder clauseBuilder,
            String rootObjectAlias,
            String objectAlias,
            String propertyName,
            String originalProperty,
            boolean inSubquery,
            boolean inClause,
            Collection<String> uris,
            long timeoutMillis, StopWatch timer,
            @Nullable Collection<OntologyTerm> mentionedTerms,
            @Nullable Collection<OntologyTerm> inferredTerms ) throws TimeoutException {
        Set<OntologyTerm> terms = ontologyService.getTerms( uris, Math.max( TimeUnit.MILLISECONDS.toMillis( timeoutMillis ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        if ( mentionedTerms != null ) {
            mentionedTerms.addAll( terms );
        }
        Set<OntologyTerm> c = ontologyService.getChildren( terms, false, true, Math.max( TimeUnit.MILLISECONDS.toMillis( timeoutMillis ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        if ( inferredTerms != null ) {
            inferredTerms.addAll( terms );
            inferredTerms.addAll( c );
        }
        Set<String> termAndChildrenUris = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        termAndChildrenUris.addAll( uris );
        termAndChildrenUris.addAll( c.stream()
                .map( OntologyTerm::getUri )
                .collect( Collectors.toList() ) );
        if ( inSubquery ) {
            for ( List<String> termAndChildrenUrisBatch : partition( new ArrayList<>( termAndChildrenUris ), QueryUtils.MAX_PARAMETER_LIST_SIZE ) ) {
                clauseBuilder = clauseBuilder.or( getFilterWithInferredAnnotations( rootObjectAlias, objectAlias, propertyName, originalProperty, true, inClause, termAndChildrenUrisBatch ) );
            }
        } else {
            // FIXME: support batching for negative subqueries
            clauseBuilder = clauseBuilder.or( getFilterWithInferredAnnotations( rootObjectAlias, objectAlias, propertyName, originalProperty, false, inClause, termAndChildrenUris ) );
        }
        return clauseBuilder;
    }

    /**
     * Create a filter with inferred annotations.
     */
    private Filter getFilterWithInferredAnnotations( String rootObjectAlias, String objectAlias, String propertyName, String originalProperty, boolean inSubquery, boolean inClause, Collection<String> uris ) {
        Assert.isTrue( !uris.isEmpty(), "Cannot create a filter with an empty clause" );
        Filter g;
        if ( uris.size() == 1 ) {
            g = Filter.by( objectAlias, propertyName, String.class, inClause ? Filter.Operator.eq : Filter.Operator.notEq, uris.iterator().next(), originalProperty );
        } else {
            g = Filter.by( objectAlias, propertyName, String.class, inClause ? Filter.Operator.in : Filter.Operator.notIn, uris, originalProperty );
        }
        // nest the filter in a subquery, all the applicable properties are one-to-many
        String prefix = originalProperty.substring( 0, originalProperty.lastIndexOf( '.' ) + 1 );
        return Filter.by( rootObjectAlias, "id", Long.class,
                inSubquery ? Filter.Operator.inSubquery : Filter.Operator.notInSubquery, new Subquery( "ExpressionExperiment", "id", guessAliases( prefix, objectAlias ), g ) );
    }

    /**
     * Identifies a sub-clause in a filter.
     */
    @Value(staticConstructor = "from")
    // ignore the originalProperty when grouping sub-clauses, it's only cosmetic
    @EqualsAndHashCode(of = { "objectAlias", "propertyName" })
    private static class SubClauseKey {
        @Nullable
        String objectAlias;
        String propertyName;
        @Nullable
        String originalProperty;
    }
}
