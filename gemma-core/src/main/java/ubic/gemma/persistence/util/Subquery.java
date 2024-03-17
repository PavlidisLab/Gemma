package ubic.gemma.persistence.util;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a subquery right-hand side of a {@link Filter}.
 * <p>
 * A subquery has rather limited structure:
 * <p>
 * {@code select {rootAlias}.{propertyName} from {entityName} {rootAlias} join {aliases...} where {filter}}
 * <p>
 * and is solely designed to nest a {@link Filter} in a subquery so that it can be applied to one-to-many relations.
 * <p>
 * The root alias is used whenever {@code null} is used as object alias in the {@link #aliases} or {@link #filter}. It
 * can be declared by passing an {@link Alias} with a {@code null} object alias and an empty property name.
 * @author poirgui
 * @see Filter#by(String, String, Class, Filter.Operator, Subquery)
 * @see Filter#by(String, String, Class, Filter.Operator, Subquery, String)
 */
@Value
public class Subquery implements Comparable<Subquery> {

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
     * List of aliases for resolving the object alias defined in {@link #filter}.
     */
    List<Alias> aliases;
    /**
     * Root alias of this subquery.
     * <p>
     * If none are defined in {@link #aliases}, the default {@code e} is used.
     */
    String rootAlias;
    /**
     * A filter being nested in the subquery.
     */
    Filter filter;

    public Subquery( String entityName, String propertyName, List<Alias> aliases, Filter filter ) {
        Assert.isTrue( StringUtils.isNotEmpty( entityName ), "A subquery must have an entity name." );
        Assert.isTrue( StringUtils.isNotEmpty( propertyName ), "A subquery must have a property." );
        Set<String> declaredAliases = aliases.stream()
                .map( Subquery.Alias::getAlias )
                .collect( Collectors.toSet() );
        for ( Subquery.Alias a : aliases ) {
            Assert.isTrue( a.getObjectAlias() == null || declaredAliases.contains( a.getObjectAlias() ),
                    String.format( "The object alias %s is not resolvable in the subquery.", a.getObjectAlias() ) );
        }
        Assert.isTrue( filter.getObjectAlias() == null || declaredAliases.contains( filter.getObjectAlias() ),
                String.format( "The object alias %s is not resolvable in the subquery.", filter.getObjectAlias() ) );
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
        this.filter = filter;
    }

    @Override
    public int compareTo( Subquery subquery ) {
        return filter.compareTo( subquery.filter );
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
                filter.getObjectAlias() == null ? rootAlias + "." + filter : filter.toString() );
    }
}
