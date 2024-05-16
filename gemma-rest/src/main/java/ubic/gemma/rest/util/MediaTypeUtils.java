package ubic.gemma.rest.util;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for {@link MediaType}.
 *
 * @author poirigui
 */
public class MediaTypeUtils {

    public static final String TEXT_TAB_SEPARATED_VALUES_UTF8 = "text/tab-separated-values; charset=UTF-8";

    public static final MediaType TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE = new MediaType( "text", "tab-separated-values", "UTF-8" );

    public static MediaType withQuality( MediaType mediaType, double quality ) {
        Map<String, String> parameters;
        if ( mediaType.getParameters() != null ) {
            parameters = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
            parameters.putAll( mediaType.getParameters() );
            parameters.put( "q", String.valueOf( quality ) );
        } else {
            parameters = Collections.singletonMap( "q", String.valueOf( quality ) );
        }
        return new MediaType( mediaType.getType(), mediaType.getSubtype(), parameters );
    }

    public static MediaType negotiate( HttpHeaders headers, MediaType... types ) throws NotAcceptableException {
        MediaType bestMediaType = null;
        double bestScore = 0.0;
        double[] typeScores = new double[types.length];
        for ( int i = 0; i < types.length; i++ ) {
            typeScores[i] = Double.parseDouble( types[i].getParameters().getOrDefault( "q", "1.0" ) );
        }
        for ( MediaType acceptableMediaType : headers.getAcceptableMediaTypes() ) {
            double q1;
            try {
                q1 = Double.parseDouble( acceptableMediaType.getParameters().getOrDefault( "q", "1.0" ) );
            } catch ( NumberFormatException e ) {
                throw new BadRequestException( "Invalid q-value for media type in 'Accept' header." );
            }
            for ( int i = 0; i < types.length; i++ ) {
                MediaType type = types[i];
                double q2 = typeScores[i];
                if ( acceptableMediaType.isCompatible( type ) ) {
                    double score = q1 * q2;
                    if ( score > bestScore ) {
                        bestScore = score;
                        bestMediaType = type;
                    }
                }
            }
        }
        if ( bestMediaType == null ) {
            throw new NotAcceptableException( String.format( "None of the accepted media type are compatible with those produced: %s.",
                    Stream.of( types ).map( MediaType::toString ).collect( Collectors.joining( ", " ) ) ) );
        }
        return bestMediaType;
    }
}
