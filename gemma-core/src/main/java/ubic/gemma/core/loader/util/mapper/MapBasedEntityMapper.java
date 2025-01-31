package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple implementation of {@link EntityMapper} that uses a {@link Map} to store association between identifiers and
 * entities.
 * @author poirigui
 */
public class MapBasedEntityMapper<T extends Identifiable> implements EntityMapper<T> {

    private final String name;
    private final Map<String, T> elementsMapping;

    public MapBasedEntityMapper( String name, Map<String, T> elementsMapping ) {
        this.name = name;
        this.elementsMapping = new HashMap<>( elementsMapping );
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StatefulEntityMapper<T> forCandidates( Collection<T> candidates ) {
        Set<T> candidatesSet = candidates instanceof Set ? ( ( Set<T> ) candidates ) : new HashSet<>( candidates );
        return new StatefulMapBasedEntityMapper( elementsMapping.entrySet().stream()
                .filter( e -> candidatesSet.contains( e.getValue() ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
    }

    private class StatefulMapBasedEntityMapper implements StatefulEntityMapper<T> {

        private final Map<String, T> elementsMapping;

        private StatefulMapBasedEntityMapper( Map<String, T> elementsMapping ) {
            this.elementsMapping = elementsMapping;
        }

        @Override
        public String getName() {
            return MapBasedEntityMapper.this.getName() + " for " + elementsMapping.size() + " candidates";
        }

        @Override
        public boolean contains( String identifier ) {
            String id = processIdentifier( identifier );
            return id != null && elementsMapping.containsKey( id );
        }

        @Override
        public boolean containsAny( Collection<String> identifier ) {
            return CollectionUtils.containsAny( elementsMapping.keySet(),
                    identifier.stream()
                            .map( MapBasedEntityMapper.this::processIdentifier )
                            .filter( Objects::nonNull )
                            .collect( Collectors.toSet() ) );
        }

        @Override
        public Optional<T> matchOne( String identifier ) {
            String id = processIdentifier( identifier );
            if ( id == null ) {
                return Optional.empty();
            }
            return Optional.ofNullable( elementsMapping.get( id ) );
        }

        @Override
        public Map<String, T> matchOne( Collection<String> identifiers ) {
            return identifiers.stream()
                    .map( MapBasedEntityMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .filter( elementsMapping::containsKey )
                    .collect( Collectors.toMap( id -> id, elementsMapping::get ) );
        }

        @Override
        public Set<T> matchAll( String identifier ) {
            String id = processIdentifier( identifier );
            if ( id == null ) {
                return Collections.emptySet();
            }
            T result = elementsMapping.get( id );
            return result != null ? Collections.singleton( result ) : Collections.emptySet();
        }

        @Override
        public Map<String, Set<T>> matchAll( Collection<String> identifiers ) {
            return identifiers.stream()
                    .map( MapBasedEntityMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .filter( elementsMapping::containsKey )
                    .collect( Collectors.toMap( id -> id, id -> Collections.singleton( elementsMapping.get( id ) ) ) );
        }

        @Override
        public MappingStatistics getMappingStatistics( Collection<String> identifiers ) {
            // the original identifiers might contain duplicated elements, so it would not reflect the true overlap to
            // create a set out of it
            long overlap = identifiers.stream()
                    .map( MapBasedEntityMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .filter( elementsMapping::containsKey )
                    .count();
            Set<String> processedIdentifiers = identifiers.stream()
                    .map( MapBasedEntityMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            long coverage = elementsMapping.keySet().stream()
                    .filter( processedIdentifiers::contains )
                    .count();
            return new MappingStatistics( ( double ) overlap / identifiers.size(),
                    ( double ) coverage / elementsMapping.size() );
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Process the identifier before using it to query the mapping.
     */
    @Nullable
    protected String processIdentifier( String identifier ) {
        return StringUtils.strip( identifier );
    }
}
