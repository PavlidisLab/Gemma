package ubic.gemma.persistence.util;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import org.springframework.lang.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a subquery right-hand side of a {@link Filter}.
 * <p>
 * A subquery has rather limited structure:
 * <p>
 * {@code select {rootAlias}.{propertyName} from {entityName} {rootAlias} join {aliases...} where {filters...}}
 * <p>
 * and is solely designed to nest one or more {@link Filter} in a subquery so that they can be applied to
 * one-to-many relations.
 * <p>
 * When more than one filter is carried they are <b>conjoined</b>, and the conjunction binds to a SINGLE
 * element of the relation. That is the difference that makes this type worth having: two separate
 * subqueries ask "some characteristic has X" AND "some characteristic has Y", whereas one subquery over
 * two filters asks "some characteristic has X AND Y". Only the latter can express "this gene URI, as a
 * genotype".
 * <p>
 * The root alias is used whenever {@code null} is used as object alias in the {@code aliases} or {@code filter}. It
 * can be declared by passing an {@link Alias} with a {@code null} object alias and an empty property name.
 * @author poirgui
 * @see Filter#by(String, String, Class, Filter.Operator, Subquery)
 * @see Filter#by(String, String, Class, Filter.Operator, Subquery, String)
 */
@Value
public class Subquery {

    @Value
    public static class Alias {
        @Nullable
        String objectAlias;
        String propertyName;
        String alias;
    }

    /**
     * The entity name being queried.
     */
    String entityName;
    /**
     * The property name being queried.
     */
    String propertyName;
    /**
     * List of aliases for resolving the object alias defined in {@code filter}.
     */
    List<Alias> aliases;
    /**
     * Root alias of this subquery.
     * <p>
     * If none are defined in {@code aliases}, the default {@code e} is used.
     */
    String rootAlias;
    /**
     * The filters being nested in the subquery, conjoined. Never empty.
     */
    List<Filter> filters;

    public Subquery( String entityName, String propertyName, List<Alias> aliases, Filter filter ) {
        this( entityName, propertyName, aliases, Collections.singletonList( filter ) );
    }

    public Subquery( String entityName, String propertyName, List<Alias> aliases, List<Filter> filters ) {
        Assert.isTrue( StringUtils.isNotEmpty( entityName ), "A subquery must have an entity name." );
        Assert.isTrue( StringUtils.isNotEmpty( propertyName ), "A subquery must have a property." );
        Assert.isTrue( !filters.isEmpty(), "A subquery must have at least one filter." );
        Set<String> declaredAliases = aliases.stream()
                .map( Subquery.Alias::getAlias )
                .collect( Collectors.toSet() );
        for ( Subquery.Alias a : aliases ) {
            Assert.isTrue( a.getObjectAlias() == null || declaredAliases.contains( a.getObjectAlias() ),
                    String.format( "The object alias %s is not resolvable in the subquery.", a.getObjectAlias() ) );
        }
        for ( Filter filter : filters ) {
            Assert.isTrue( filter.getObjectAlias() == null || declaredAliases.contains( filter.getObjectAlias() ),
                    String.format( "The object alias %s is not resolvable in the subquery.", filter.getObjectAlias() ) );
        }
        this.entityName = entityName;
        this.propertyName = propertyName;
        this.aliases = aliases;
        String rootAlias = "e";
        for ( Subquery.Alias a : aliases ) {
            if ( a.getObjectAlias() == null && a.getPropertyName().isEmpty() ) {
                rootAlias = a.getAlias();
                break;
            }
        }
        this.rootAlias = rootAlias;
        this.filters = Collections.unmodifiableList( filters );
    }

    /**
     * The sole filter carried by this subquery.
     *
     * @throws IllegalStateException if this subquery carries a conjunction; callers that can encounter
     *                               one must read {@link #getFilters()} and handle every conjunct.
     */
    public Filter getFilter() {
        Assert.state( filters.size() == 1,
                "This subquery carries " + filters.size() + " conjoined filters; use getFilters()." );
        return filters.get( 0 );
    }

    /**
     * Create a new subquery with a different property name for the filters.
     */
    public Subquery withFilterPropertyName( String newPropertyName, @Nullable String originalProperty ) {
        return new Subquery( entityName, propertyName, aliases, filters.stream()
                .map( f -> f.withPropertyName( newPropertyName, originalProperty ) )
                .collect( Collectors.toList() ) );
    }

    public String toString() {
        String rootAlias = getRootAlias();
        String jointures = aliases.stream()
                .filter( a -> !a.getPropertyName().isEmpty() )
                .map( a -> String.format( " join %s.%s %s", a.getObjectAlias() != null ? a.getObjectAlias() : rootAlias, a.getPropertyName(), a.getAlias() ) )
                .collect( Collectors.joining( "" ) );
        return String.format(
                "select %s.%s from %s %s%s where %s",
                rootAlias,
                propertyName,
                entityName,
                rootAlias,
                jointures,
                filters.stream()
                        .map( f -> f.getObjectAlias() == null ? rootAlias + "." + f : f.toString() )
                        .collect( Collectors.joining( " and " ) ) );
    }
}
