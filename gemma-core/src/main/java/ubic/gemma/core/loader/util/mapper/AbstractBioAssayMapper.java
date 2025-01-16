package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

public abstract class AbstractBioAssayMapper implements BioAssayMapper {

    @Override
    public StatefulEntityMapper<BioAssay> forCandidates( Collection<BioAssay> candidates ) {
        return new StatefulBioAssayMapper( candidates );
    }

    @Override
    public boolean contains( Collection<BioAssay> candidates, String identifier ) {
        return !matchAllInternal( candidates, identifier ).isEmpty();
    }

    @Override
    public boolean containsAny( Collection<BioAssay> candidates, Collection<String> identifiers ) {
        return identifiers.stream()
                .anyMatch( identifier -> !matchAllInternal( candidates, identifier ).isEmpty() );
    }

    @Override
    public Optional<BioAssay> matchOne( Collection<BioAssay> candidates, String identifier ) {
        Set<BioAssay> results = matchAllInternal( candidates, identifier );
        if ( results.size() == 1 ) {
            return Optional.of( results.iterator().next() );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Set<BioAssay> matchAll( Collection<BioAssay> candidates, String identifier ) {
        return matchAllInternal( candidates, identifier );
    }

    @Override
    public String toString() {
        return getName();
    }

    protected abstract Set<BioAssay> matchAllInternal( Collection<BioAssay> candidates, String identifier );

    protected boolean matchId( @Nullable Long a, String b ) {
        try {
            return a != null && a.equals( Long.parseLong( StringUtils.strip( b ) ) );
        } catch ( NumberFormatException e ) {
            return false;
        }
    }

    protected boolean matchName( String a, String b ) {
        return normalizeSpace( a ).equals( normalizeSpace( b ) );
    }

    protected boolean matchNameIgnoreCase( String a, String b ) {
        return normalizeSpace( a ).equalsIgnoreCase( normalizeSpace( b ) );
    }

    private class StatefulBioAssayMapper implements StatefulEntityMapper<BioAssay> {

        private final Collection<BioAssay> candidates;

        public StatefulBioAssayMapper( Collection<BioAssay> candidates ) {
            this.candidates = candidates;
        }

        @Override
        public String getName() {
            return AbstractBioAssayMapper.this.getName() + " for " + candidates.size() + " candidates";
        }

        @Override
        public boolean contains( String identifier ) {
            return AbstractBioAssayMapper.this.contains( candidates, identifier );
        }

        @Override
        public boolean containsAny( Collection<String> identifiers ) {
            return AbstractBioAssayMapper.this.containsAny( candidates, identifiers );
        }

        @Override
        public Optional<BioAssay> matchOne( String identifier ) {
            return AbstractBioAssayMapper.this.matchOne( candidates, identifier );
        }

        @Override
        public Map<String, BioAssay> matchOne( Collection<String> identifiers ) {
            return AbstractBioAssayMapper.this.matchOne( candidates, identifiers );
        }

        @Override
        public Set<BioAssay> matchAll( String identifier ) {
            return AbstractBioAssayMapper.this.matchAll( candidates, identifier );
        }

        @Override
        public Map<String, Set<BioAssay>> matchAll( Collection<String> identifiers ) {
            return AbstractBioAssayMapper.this.matchAll( candidates, identifiers );
        }

        @Override
        public MappingStatistics getMappingStatistics( Collection<String> identifiers ) {
            return AbstractBioAssayMapper.this.getMappingStatistics( candidates, identifiers );
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
