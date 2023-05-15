package ubic.gemma.core.ontology;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyTerm;

import java.util.List;

public class OntologyUtils {

    /**
     * Extract a label for a term URI as per {@link OntologyTerm#getLabel()}.
     */
    public static String getLabelFromTermUri( String termUri ) {
        UriComponents components = UriComponentsBuilder.fromUriString( termUri ).build();
        List<String> segments = components.getPathSegments();
        // use the fragment
        if ( !StringUtils.isEmpty( components.getFragment() ) ) {
            return partToTerm( components.getFragment() );
        }
        // pick the last non-empty segment
        for ( int i = segments.size() - 1; i >= 0; i-- ) {
            if ( !StringUtils.isEmpty( segments.get( i ) ) ) {
                return partToTerm( segments.get( i ) );
            }
        }
        // as a last resort, return the parsed URI (this will remove excessive trailing slashes)
        return components.toUriString();
    }

    private static String pickSegment( List<String> segments ) {
        return segments.get( segments.size() - 1 );
    }

    private static String partToTerm( String part ) {
        return part.replaceFirst( "_", ":" ).toUpperCase();
    }
}
