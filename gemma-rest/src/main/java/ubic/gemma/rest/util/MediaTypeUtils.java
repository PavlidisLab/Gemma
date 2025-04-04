package ubic.gemma.rest.util;

import org.springframework.util.Assert;

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

    /**
     * Create a media type with a quality indicator.
     */
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

    /**
     * Create a media type without a quality indicator.
     */
    public static MediaType withoutQuality( MediaType mediaType ) {
        Map<String, String> parameters;
        if ( mediaType.getParameters() != null ) {
            parameters = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
            parameters.putAll( mediaType.getParameters() );
            parameters.remove( "q" );
        } else {
            parameters = null;
        }
        return new MediaType( mediaType.getType(), mediaType.getSubtype(), parameters );
    }

    /**
     * Perform content negotiation of a given set of HTTP headers against a list of possible media types.
     * <p>
     * Media type may include a quality score by setting the {@code q} parameter. If omitted, this score defaults to
     * {@code 1.0}. The pair of compatible accepted and produced types with the highest product is returned.
     * @param headers HTTP headers from the client, {@link HttpHeaders#getAcceptableMediaTypes()} is used to determine
     *                acceptable media types
     * @param types   an array of produced media types
     * @throws NotAcceptableException if the negotiation fails (i.e. no media types can be satisfied)
     * @return one of the produced media type, stripped if its quality score with {@link #withoutQuality(MediaType)}
     */
    public static MediaType negotiate( HttpHeaders headers, MediaType... types ) throws NotAcceptableException {
        Assert.isTrue( types.length > 0, "At least one media type must be provided" );
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
        return withoutQuality( bestMediaType );
    }
}
