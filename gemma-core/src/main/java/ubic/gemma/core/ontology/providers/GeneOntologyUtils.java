package ubic.gemma.core.ontology.providers;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;

public class GeneOntologyUtils {

    /**
     * Check if a given string is a GO ID.
     */
    public static boolean isGoId( String s ) {
        String goId;
        if ( s.startsWith( GeneOntologyService.BASE_GO_URI ) ) {
            goId = s.substring( GeneOntologyService.BASE_GO_URI.length() );
        } else {
            goId = s;
        }
        return goId.matches( "GO[:_]\\d+" );
    }

    /**
     * Convert various GO IDs formats to the usual format, e.g., GO:0039392.
     * @param  term ontology term
     * @return Usual formatted GO id, e.g., GO:0039392 or null for a free-text term
     */
    @Nullable
    public static String asRegularGoId( OntologyTerm term ) {
        return asRegularGoId( term.getUri() );
    }

    /**
     * Convert a characteristic to a regular GO ID.
     */
    @Nullable
    public static String asRegularGoId( Characteristic c ) {
        return asRegularGoId( c.getValueUri() );
    }

    /**
     * Convert various GO IDs formats to the usual format, e.g., GO:0039392.
     * @return a regular GO ID or null if the input is null or not a reckognized GO ID format
     */
    @Nullable
    public static String asRegularGoId( String uri ) {
        if ( uri == null ) {
            return null;
        }
        String goId;
        if ( uri.startsWith( GeneOntologyService.BASE_GO_URI ) ) {
            goId = uri.substring( GeneOntologyService.BASE_GO_URI.length() );
        } else {
            goId = uri;
        }
        if ( goId.startsWith( "GO:" ) || goId.startsWith( "GO_" ) ) {
            return goId.replace( "_", ":" );
        } else {
            return null;
        }
    }
}
