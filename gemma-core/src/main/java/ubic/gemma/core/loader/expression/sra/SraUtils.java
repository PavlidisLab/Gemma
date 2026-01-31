package ubic.gemma.core.loader.expression.sra;

import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

public class SraUtils {

    public static URL getUrl( String accession ) {
        try {
            return new URL( "https://www.ncbi.nlm.nih.gov/sra?term=" + urlEncode( accession ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
