package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for design element mapper that use gene identifiers.
 * @author poirigui
 */
public abstract class AbstractGeneIdentifierBasedDesignElementMapper<K> implements DesignElementMapper {

    private final Map<CompositeSequence, Set<Gene>> cs2g;

    protected AbstractGeneIdentifierBasedDesignElementMapper( Map<CompositeSequence, Set<Gene>> cs2g ) {
        this.cs2g = new HashMap<>( cs2g );
    }

    @Override
    public EntityMapper.StatefulEntityMapper<CompositeSequence> forCandidates( Collection<CompositeSequence> candidates ) {
        return new StatefulDesignElementMapper( candidates );
    }

    private class StatefulDesignElementMapper implements StatefulEntityMapper<CompositeSequence> {

        private final Map<K, CompositeSequence> elementsMapping;

        private StatefulDesignElementMapper( Collection<CompositeSequence> designElements ) {
            Map<K, CompositeSequence> elementsMapping = new HashMap<>();
            for ( CompositeSequence cs : designElements ) {
                if ( !cs2g.containsKey( cs ) ) {
                    continue;
                }
                for ( Gene g : cs2g.get( cs ) ) {
                    K id = getIdentifier( g );
                    if ( id != null ) {
                        elementsMapping.putIfAbsent( id, cs );
                    }
                }
            }
            this.elementsMapping = elementsMapping;
        }

        @Override
        public String getName() {
            return AbstractGeneIdentifierBasedDesignElementMapper.this.getName() + " for " + elementsMapping + " candidates";
        }

        @Override
        public boolean contains( String geneIdentifier ) {
            K id = processIdentifier( geneIdentifier );
            return id != null && elementsMapping.containsKey( id );
        }

        @Override
        public boolean containsAny( Collection<String> geneIdentifiers ) {
            return geneIdentifiers.stream().anyMatch( this::contains );
        }

        @Override
        public Optional<CompositeSequence> matchOne( String geneIdentifier ) {
            K id = processIdentifier( geneIdentifier );
            if ( id == null ) {
                return Optional.empty();
            }
            return Optional.ofNullable( elementsMapping.get( id ) );
        }

        @Override
        public Set<CompositeSequence> matchAll( String geneIdentifier ) {
            K id = processIdentifier( geneIdentifier );
            if ( id == null ) {
                return Collections.emptySet();
            }
            CompositeSequence result = elementsMapping.get( id );
            return result != null ? Collections.singleton( result ) : Collections.emptySet();
        }

        @Override
        public MappingStatistics getMappingStatistics( Collection<String> identifiers ) {
            long overlap = identifiers.stream()
                    .map( AbstractGeneIdentifierBasedDesignElementMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .filter( elementsMapping::containsKey )
                    .count();
            Set<K> strippedGeneIdentifiers = identifiers.stream()
                    .map( AbstractGeneIdentifierBasedDesignElementMapper.this::processIdentifier )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            long coverage = elementsMapping.keySet().stream()
                    .filter( strippedGeneIdentifiers::contains )
                    .count();
            return new MappingStatistics( ( double ) overlap / identifiers.size(), ( double ) coverage / elementsMapping.size() );
        }
    }

    /**
     * Extract an identifier from the gene.
     */
    @Nullable
    protected abstract K getIdentifier( Gene gene );

    /**
     * Extract an identifier from an externally supplied string.
     * <p>
     * This may return null if the identifier cannot be parsed.
     */
    @Nullable
    protected abstract K processIdentifier( String identifier );
}
