package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.StringUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Strategy for matching {@link GeoSample} from a given sample name.
 */
@CommonsLog
public class GeoSampleToSampleNameMatcher {

    public Set<GeoSample> match( Collection<GeoSample> samples, String sampleName ) {
        Map<GeoSample, String> sampleNamesInGeo = samples.stream()
                .collect( Collectors.toMap( s -> s, s -> StringUtils.normalizeSpace( s.getTitle() ) ) );
        String normalizedSampleName = StringUtils.normalizeSpace( sampleName );

        // exact matches
        Set<GeoSample> results = sampleNamesInGeo.entrySet().stream()
                .filter( sn -> StringUtils.equals( sn.getValue(), normalizedSampleName ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );
        if ( !results.isEmpty() ) {
            log.info( "Exact match found for '" + sampleName + "' in " + results );
            return results;
        }

        // exact matches, case-insensitive
        results = sampleNamesInGeo.entrySet().stream()
                .filter( sn -> StringUtils.equalsIgnoreCase( sn.getValue(), normalizedSampleName ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );
        if ( !results.isEmpty() ) {
            log.info( "Exact match, case-insensitive found for '" + sampleName + "' in " + results );
            return results;
        }

        // substring
        results = sampleNamesInGeo.entrySet().stream()
                .filter( sn -> StringUtils.contains( sn.getValue(), normalizedSampleName ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );
        if ( !results.isEmpty() ) {
            log.info( "Substring match for '" + sampleName + "' in " + results );
            return results;
        }

        // substring, case-insensitive
        results = sampleNamesInGeo.entrySet().stream()
                .filter( sn -> StringUtils.containsIgnoreCase( sn.getValue(), normalizedSampleName ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );
        if ( !results.isEmpty() ) {
            log.info( "Substring match, case-insensitive for '" + sampleName + "' in " + results );
            return results;
        }

        return Collections.emptySet();
    }
}
