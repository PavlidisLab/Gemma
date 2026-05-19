package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Strategy for matching {@link GeoSample} from a given sample name.
 */
@CommonsLog
class GeoSampleToSampleNameMatcher {

    private enum Tier {
        EXACT, EXACT_CI, SUBSTRING, SUBSTRING_CI
    }

    private static class MatchResult {
        final Set<GeoSample> results;
        final Tier tier; // null when results is empty

        MatchResult( Set<GeoSample> results, Tier tier ) {
            this.results = results;
            this.tier = tier;
        }
    }

    public Set<GeoSample> match( Collection<GeoSample> samples, String sampleName ) {
        return cascadeMatch( samples, sampleName ).results;
    }

    /**
     * Match a collection of candidate sample names from a single column against GEO samples,
     * applying a longest-wins invalidation rule on substring matches.
     * <p>
     * If a value {@code v} substring-matches a title {@code t}, but some other value {@code v'} in
     * the same column is <em>strictly longer</em> than {@code v} and is also a substring of
     * {@code t}, then {@code (v, t)} is dropped — {@code v'} more specifically claims {@code t}.
     * Same-length collisions are preserved (and therefore cause the column to remain ambiguous,
     * which is the honest outcome). The rule is applied to both the case-sensitive and
     * case-insensitive substring tiers; exact-match tiers are never invalidated.
     */
    public Map<String, Set<GeoSample>> matchAll( Collection<GeoSample> samples, Collection<String> sampleNames ) {
        Map<String, MatchResult> raw = new LinkedHashMap<>();
        for ( String name : sampleNames ) {
            raw.put( name, cascadeMatch( samples, name ) );
        }
        Map<String, Set<GeoSample>> result = new LinkedHashMap<>();
        for ( Map.Entry<String, MatchResult> entry : raw.entrySet() ) {
            String value = entry.getKey();
            MatchResult mr = entry.getValue();
            if ( mr.tier == null || mr.tier == Tier.EXACT || mr.tier == Tier.EXACT_CI ) {
                result.put( value, mr.results );
                continue;
            }
            boolean caseInsensitive = ( mr.tier == Tier.SUBSTRING_CI );
            String normalizedValue = StringUtils.normalizeSpace( value );
            Set<GeoSample> filtered = mr.results.stream()
                    .filter( s -> {
                        String title = StringUtils.normalizeSpace( s.getTitle() );
                        for ( String other : sampleNames ) {
                            if ( other.equals( value ) ) {
                                continue;
                            }
                            String normalizedOther = StringUtils.normalizeSpace( other );
                            if ( normalizedOther.length() <= normalizedValue.length() ) {
                                continue;
                            }
                            boolean contains = caseInsensitive
                                    ? StringUtils.containsIgnoreCase( title, normalizedOther )
                                    : StringUtils.contains( title, normalizedOther );
                            if ( contains ) {
                                return false;
                            }
                        }
                        return true;
                    } )
                    .collect( Collectors.toSet() );
            result.put( value, filtered );
        }
        return result;
    }

    private MatchResult cascadeMatch( Collection<GeoSample> samples, String sampleName ) {
        Map<GeoSample, String> sampleNamesInGeo = samples.stream()
                .collect( Collectors.toMap( s -> s, s -> StringUtils.normalizeSpace( s.getTitle() ) ) );
        String normalizedSampleName = StringUtils.normalizeSpace( sampleName );

        Set<GeoSample> results = filterByTitle( sampleNamesInGeo, t -> StringUtils.equals( t, normalizedSampleName ) );
        if ( !results.isEmpty() ) {
            log.info( "Exact match found for '" + sampleName + "' in " + results );
            return new MatchResult( results, Tier.EXACT );
        }

        results = filterByTitle( sampleNamesInGeo, t -> StringUtils.equalsIgnoreCase( t, normalizedSampleName ) );
        if ( !results.isEmpty() ) {
            log.info( "Exact match, case-insensitive found for '" + sampleName + "' in " + results );
            return new MatchResult( results, Tier.EXACT_CI );
        }

        results = filterByTitle( sampleNamesInGeo, t -> StringUtils.contains( t, normalizedSampleName ) );
        if ( !results.isEmpty() ) {
            log.info( "Substring match for '" + sampleName + "' in " + results );
            return new MatchResult( results, Tier.SUBSTRING );
        }

        results = filterByTitle( sampleNamesInGeo, t -> StringUtils.containsIgnoreCase( t, normalizedSampleName ) );
        if ( !results.isEmpty() ) {
            log.info( "Substring match, case-insensitive for '" + sampleName + "' in " + results );
            return new MatchResult( results, Tier.SUBSTRING_CI );
        }

        return new MatchResult( Collections.emptySet(), null );
    }

    private Set<GeoSample> filterByTitle( Map<GeoSample, String> sampleNamesInGeo, Predicate<String> predicate ) {
        return sampleNamesInGeo.entrySet().stream()
                .filter( e -> predicate.test( e.getValue() ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );
    }
}