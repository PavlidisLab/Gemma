package ubic.gemma.core.loader.util.mapper;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

/**
 *
 */
@CommonsLog
public class TabularDataBioAssayMapper extends AbstractBioAssayMapper implements HintingEntityMapper<BioAssay> {

    private static final char[] DELIMITERS = { ' ', '_', '-' };

    @Override
    public String getName() {
        return "tabular";
    }

    @Override
    protected Set<BioAssay> matchAllInternal( Collection<BioAssay> candidates, String identifier ) {
        Set<BioAssay> result;

        // exact match
        if ( !( result = matchExactly( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by exact match." );
            return result;
        }

        if ( !( result = matchExactlyCaseInsensitive( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by exact match (case insensitive)." );
            return result;
        }

        // exact match (via biomaterial)
        if ( !( result = matchExactly( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by exact match via its biomaterial." );
            return result;
        }

        if ( !( result = matchExactlyCaseInsensitive( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by exact match (case insensitive) via its biomaterial." );
            return result;
        }

        // substring
        if ( !( result = matchBySubstring( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by substring." );
            return result;
        }

        if ( !( result = matchBySubstringCaseInsensitive( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by substring (case insensitive)." );
            return result;
        }

        // substring (via biomaterial)
        if ( !( result = matchBySubstring( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by substring via its biomaterial." );
            return result;
        }

        if ( !( result = matchBySubstringCaseInsensitive( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by substring (case insensitive) via its biomaterial." );
            return result;
        }

        // common substring
        if ( !( result = matchByCommonSubstring( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by longest common subsequence." );
            return result;
        }

        if ( !( result = matchByCommonSubstringCaseInsensitive( candidates, BioAssay::getName, identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by longest common subsequence (case insensitive)." );
            return result;
        }

        // common substring (via biomaterial)
        if ( !( result = matchByCommonSubstring( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by longest common subsequence via its biomaterial." );
            return result;
        }

        if ( !( result = matchByCommonSubstringCaseInsensitive( candidates, ba -> ba.getSampleUsed().getName(), identifier ) ).isEmpty() ) {
            log.debug( "Match " + identifier + " by longest common subsequence (case insensitive) via its biomaterial." );
            return result;
        }

        return Collections.emptySet();
    }

    private Set<BioAssay> matchExactly( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) );
            return candidates.stream()
                    .filter( bioAssay -> id.equals( normalizeSpace( func.apply( bioAssay ) ) ) )
                    .collect( Collectors.toSet() );
        }
        return Collections.emptySet();
    }

    private Set<BioAssay> matchExactlyCaseInsensitive( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) );
            return candidates.stream()
                    .filter( bioAssay -> id.equalsIgnoreCase( normalizeSpace( func.apply( bioAssay ) ) ) )
                    .collect( Collectors.toSet() );
        }
        return Collections.emptySet();
    }

    private Set<BioAssay> matchBySubstring( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) );
            return candidates.stream()
                    .filter( bioAssay -> id.contains( normalizeSpace( func.apply( bioAssay ) ) ) )
                    .collect( Collectors.toSet() );
        }
        return Collections.emptySet();
    }

    private Set<BioAssay> matchBySubstringCaseInsensitive( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) );
            return candidates.stream()
                    .filter( bioAssay -> Strings.CI.contains( id, normalizeSpace( func.apply( bioAssay ) ) ) )
                    .collect( Collectors.toSet() );
        }
        return Collections.emptySet();
    }

    private static final LongestCommonSubsequence lcs = new LongestCommonSubsequence();

    private Set<BioAssay> matchByCommonSubstring( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) );
            return candidates.stream()
                    .max( Comparator.comparingInt( bioAssay -> lcs.apply( id, normalizeSpace( func.apply( bioAssay ) ) ) ) )
                    .map( Collections::singleton )
                    .orElse( Collections.emptySet() );
        }
        return Collections.emptySet();
    }

    private Set<BioAssay> matchByCommonSubstringCaseInsensitive( Collection<BioAssay> candidates, Function<BioAssay, String> func, String identifier ) {
        for ( char delimiter : DELIMITERS ) {
            String id = normalizeSpace( identifier.replace( delimiter, ' ' ) ).toLowerCase();
            return candidates.stream()
                    .max( Comparator.comparingInt( bioAssay -> lcs.apply( id, normalizeSpace( func.apply( bioAssay ) ).toLowerCase() ) ) )
                    .map( Collections::singleton )
                    .orElse( Collections.emptySet() );
        }
        return Collections.emptySet();
    }

    @Override
    public List<String> getCandidateIdentifiers( BioAssay entity ) {
        ArrayList<String> candidates = new ArrayList<>();
        for ( char delimiter : DELIMITERS ) {
            candidates.add( entity.getName().replace( ' ', delimiter ) );
            candidates.add( entity.getSampleUsed().getName().replace( ' ', delimiter ) );
        }
        return candidates;
    }
}
