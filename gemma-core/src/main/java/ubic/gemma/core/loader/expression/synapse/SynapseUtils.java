package ubic.gemma.core.loader.expression.synapse;

import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class SynapseUtils {

    public static URL getUrl( String accession ) {
        try {
            return new URL( "https://www.synapse.org/Synapse:" + urlEncode( accession ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
