package ubic.gemma.core.loader.entrez.pubmed;

import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class PubMedUtils {

    public static URL getUrl( String pubMedId ) {
        try {
            return new URL( "https://pubmed.ncbi.nlm.nih.gov/" + urlEncode( pubMedId ) + "/" );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
