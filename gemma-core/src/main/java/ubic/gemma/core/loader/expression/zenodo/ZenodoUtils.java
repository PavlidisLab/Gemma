package ubic.gemma.core.loader.expression.zenodo;

import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class ZenodoUtils {

    public static URL getUrl( String recordId ) {
        try {
            return new URL( "https://zenodo.org/records/" + urlEncode( recordId ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
