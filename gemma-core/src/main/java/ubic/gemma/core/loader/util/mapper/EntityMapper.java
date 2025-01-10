package ubic.gemma.core.loader.util.mapper;

import lombok.Value;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Map external identifiers to Gemma entities.
 * @param <T>
 * @author poirigui
 */
public interface EntityMapper<T extends Identifiable> {

    /**
     * Obtain the name of the mapping strategy.
     */
    String getName();

    /**
     * Create a stateful mapper for a set of candidates.
     */
    StatefulEntityMapper<T> forCandidates( Collection<T> candidates );

    /**
     * Check if any of the candidates can be mapped to the identifier.
     */
    default boolean contains( Collection<T> candidates, String identifier ) {
        return forCandidates( candidates ).contains( identifier );
    }

    /**
     * Check if any of the candidates can be mapped to any of the identifiers.
     */
    default boolean containsAny( Collection<T> candidates, Collection<String> identifiers ) {
        return forCandidates( candidates ).containsAny( identifiers );
    }

    /**
     * Map the identifier to a single candidate.
     * <p>
     * If more than one candidate matches the identifier, {@link Optional#empty()} is returned.
     */
    default Optional<T> matchOne( Collection<T> candidates, String identifier ) {
        return forCandidates( candidates ).matchOne( identifier );
    }

    /**
     * Match the identifier to all the candidates.
     */
    default Set<T> matchAll( Collection<T> candidates, String identifier ) {
        return forCandidates( candidates ).matchAll( identifier );
    }

    default MappingStatistics getMappingStatistics( Collection<T> candidates, Collection<String> identifiers ) {
        return forCandidates( candidates ).getMappingStatistics( identifiers );
    }

    /**
     * A stateful entity mapper keeps an internal state for a set of candidates to optimize the mapping process.
     * @param <T>
     */
    interface StatefulEntityMapper<T extends Identifiable> {

        String getName();

        /**
         * Check if any of the candidates can be mapped to the identifier.
         */
        boolean contains( String identifier );

        /**
         * Check if any of the candidates can be mapped to any of the identifiers.
         */
        boolean containsAny( Collection<String> identifiers );

        /**
         * Map the identifier to a candidate.
         */
        Optional<T> matchOne( String identifier );

        /**
         * Match the identifier to all the candidates.
         */
        Set<T> matchAll( String identifier );

        /**
         * Calculate mapping statistics for a set of gene identifiers.
         */
        MappingStatistics getMappingStatistics( Collection<String> identifiers );
    }

    /**
     * Mapping statistics computed from a given set of entities and identifiers.
     */
    @Value
    class MappingStatistics {
        /**
         * Proportion of the gene identifiers that were mapped to a {@link CompositeSequence}.
         */
        double overlap;
        /**
         * Proportion of the composite sequences that were mapped by a gene identifier.
         */
        double coverage;
    }
}
