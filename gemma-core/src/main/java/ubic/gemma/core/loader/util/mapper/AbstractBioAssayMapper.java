package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

public abstract class AbstractBioAssayMapper implements BioAssayMapper {

    @Override
    public StatefulEntityMapper<BioAssay> forCandidates( Collection<BioAssay> candidates ) {
        throw new UnsupportedOperationException( "Stateful mapping of BioAssays is not supported." );
    }

    @Override
    public boolean contains( Collection<BioAssay> candidates, String identifier ) {
        return !matchAll( candidates, identifier ).isEmpty();
    }

    @Override
    public boolean containsAny( Collection<BioAssay> candidates, Collection<String> identifiers ) {
        return identifiers.stream()
                .anyMatch( identifier -> contains( candidates, identifier ) );
    }

    @Override
    public Optional<BioAssay> matchOne( Collection<BioAssay> candidates, String identifier ) {
        Set<BioAssay> results = matchAll( candidates, identifier );
        if ( results.size() == 1 ) {
            return Optional.of( results.iterator().next() );
        } else {
            return Optional.empty();
        }
    }

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
}
