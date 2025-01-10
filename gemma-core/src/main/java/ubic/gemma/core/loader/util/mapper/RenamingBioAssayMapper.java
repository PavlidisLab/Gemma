package ubic.gemma.core.loader.util.mapper;

import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A BioAssay-to-sample-name matcher that renames samples before matching them.
 * @author poirigui
 */
public class RenamingBioAssayMapper implements BioAssayMapper {

    private final BioAssayMapper delegate;
    private final Map<String, String> sampleNameToBioAssayName;

    /**
     * @param delegate      a matcher that performs the underlying comparison of BA IDs
     * @param bioAssayNames the BioAssay identifiers to use
     * @param sampleNames   the corresponding sample identifiers to use
     */
    public RenamingBioAssayMapper( BioAssayMapper delegate, String[] bioAssayNames, String[] sampleNames ) {
        Assert.isTrue( bioAssayNames.length == sampleNames.length );
        this.delegate = delegate;
        this.sampleNameToBioAssayName = new HashMap<>();
        for ( int i = 0; i < bioAssayNames.length; i++ ) {
            this.sampleNameToBioAssayName.put( sampleNames[i], bioAssayNames[i] );
        }
    }

    @Override
    public String getName() {
        return "Rename sample names and delegate to " + delegate.getName();
    }

    @Override
    public boolean contains( Collection<BioAssay> bioAssays, String sampleName ) {
        return translate( sampleName )
                .map( bioAssayName -> delegate.contains( bioAssays, bioAssayName ) )
                .orElse( false );
    }

    @Override
    public boolean containsAny( Collection<BioAssay> bioAssays, Collection<String> sampleNames ) {
        return delegate.containsAny( bioAssays, sampleNames.stream()
                .map( sampleNameToBioAssayName::get )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() ) );
    }

    @Override
    public Optional<BioAssay> matchOne( Collection<BioAssay> bioAssays, String sampleName ) {
        return translate( sampleName )
                .flatMap( bioAssayName -> delegate.matchOne( bioAssays, bioAssayName ) );
    }

    @Override
    public Set<BioAssay> matchAll( Collection<BioAssay> bioAssays, String sampleName ) {
        return translate( sampleName )
                .map( bioAssayName -> delegate.matchAll( bioAssays, bioAssayName ) )
                .orElse( Collections.emptySet() );
    }

    @Override
    public StatefulEntityMapper<BioAssay> forCandidates( Collection<BioAssay> candidates ) {
        return new StatefulRenamingBioAssayMapper( delegate.forCandidates( candidates ) );
    }

    private class StatefulRenamingBioAssayMapper implements StatefulEntityMapper<BioAssay> {

        private final StatefulEntityMapper<BioAssay> delegate;

        private StatefulRenamingBioAssayMapper( StatefulEntityMapper<BioAssay> delegate ) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return "Rename sample names and delegate to " + delegate.getName();
        }

        @Override
        public boolean contains( String sampleName ) {
            return translate( sampleName )
                    .map( delegate::contains )
                    .orElse( false );
        }

        @Override
        public boolean containsAny( Collection<String> sampleNames ) {
            return delegate.containsAny( sampleNames.stream()
                    .map( sampleNameToBioAssayName::get )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() ) );
        }

        @Override
        public Optional<BioAssay> matchOne( String sampleName ) {
            return translate( sampleName )
                    .flatMap( delegate::matchOne );
        }

        @Override
        public Set<BioAssay> matchAll( String sampleName ) {
            return translate( sampleName )
                    .map( delegate::matchAll )
                    .orElse( Collections.emptySet() );
        }

        @Override
        public MappingStatistics getMappingStatistics( Collection<String> identifiers ) {
            return delegate.getMappingStatistics( identifiers.stream()
                    .map( sampleNameToBioAssayName::get )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() ) );
        }
    }

    private Optional<String> translate( String sampleName ) {
        return Optional.ofNullable( sampleNameToBioAssayName.get( sampleName ) );
    }
}
