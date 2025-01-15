package ubic.gemma.core.loader.util.mapper;

import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of a chain of entity mappers.
 * @param <T>
 * @author poirigui
 */
public class ChainedEntityMapper<T extends Identifiable> implements EntityMapper<T> {

    private final EntityMapper<T>[] chain;

    public ChainedEntityMapper( EntityMapper<T>... chain ) {
        Assert.isTrue( chain.length > 0, "The chain cannot be empty." );
        this.chain = chain;
    }

    @Override
    public String getName() {
        return Arrays.stream( chain ).map( EntityMapper::getName ).collect( Collectors.joining( " → " ) );
    }

    @Override
    public StatefulEntityMapper<T> forCandidates( Collection<T> candidates ) {
        //noinspection unchecked
        return new ChainedStatefulEntityMapper( Arrays.stream( chain ).map( e -> e.forCandidates( candidates ) ).toArray( StatefulEntityMapper[]::new ) );
    }

    @Override
    public boolean contains( Collection<T> candidates, String identifier ) {
        return Arrays.stream( chain ).anyMatch( e -> e.contains( candidates, identifier ) );
    }

    @Override
    public boolean containsAny( Collection<T> candidates, Collection<String> identifiers ) {
        return Arrays.stream( chain ).anyMatch( e -> e.containsAny( candidates, identifiers ) );
    }

    @Override
    public Optional<T> matchOne( Collection<T> candidates, String identifier ) {
        return Arrays.stream( chain ).map( e -> e.matchOne( candidates, identifier ) ).filter( Optional::isPresent ).map( Optional::get ).findFirst();
    }

    @Override
    public Set<T> matchAll( Collection<T> candidates, String identifier ) {
        return Arrays.stream( chain )
                .map( e -> e.matchAll( candidates, identifier ) )
                .filter( ts -> !ts.isEmpty() )
                .findFirst()
                .orElse( Collections.emptySet() );
    }

    @Override
    public MappingStatistics getMappingStatistics( Collection<T> candidates, Collection<String> identifiers ) {
        return Arrays.stream( chain )
                .map( e -> e.getMappingStatistics( candidates, identifiers ) )
                .filter( s -> s.getOverlap() > 0 )
                .findFirst()
                .orElse( new MappingStatistics( 0, 0 ) );
    }

    private class ChainedStatefulEntityMapper implements StatefulEntityMapper<T> {

        private final StatefulEntityMapper<T>[] chain;

        private ChainedStatefulEntityMapper( StatefulEntityMapper<T>... chain ) {
            this.chain = chain;
        }

        @Override
        public String getName() {
            return Arrays.stream( chain ).map( StatefulEntityMapper::getName ).collect( Collectors.joining( " → " ) );
        }

        @Override
        public boolean contains( String identifier ) {
            return Arrays.stream( chain ).anyMatch( e -> e.contains( identifier ) );
        }

        @Override
        public boolean containsAny( Collection<String> identifiers ) {
            return Arrays.stream( chain ).anyMatch( e -> e.containsAny( identifiers ) );
        }

        @Override
        public Optional<T> matchOne( String identifier ) {
            return Arrays.stream( chain ).map( e -> e.matchOne( identifier ) ).filter( Optional::isPresent ).map( Optional::get ).findFirst();
        }

        @Override
        public Set<T> matchAll( String identifier ) {
            return Arrays.stream( chain )
                    .map( e -> e.matchAll( identifier ) )
                    .filter( ts -> !ts.isEmpty() )
                    .findFirst()
                    .orElse( Collections.emptySet() );
        }

        @Override
        public MappingStatistics getMappingStatistics( Collection<String> identifiers ) {
            return Arrays.stream( chain )
                    .map( e -> e.getMappingStatistics( identifiers ) )
                    .filter( s -> s.getOverlap() > 0 )
                    .findFirst()
                    .orElse( new MappingStatistics( 0, 0 ) );
        }
    }
}
