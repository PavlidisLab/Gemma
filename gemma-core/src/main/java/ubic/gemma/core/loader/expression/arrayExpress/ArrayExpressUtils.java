package ubic.gemma.core.loader.expression.arrayExpress;

import java.net.MalformedURLException;
import java.net.URL;

import static ubic.gemma.core.util.StringUtils.urlEncode;

public class ArrayExpressUtils {

    public static URL getUrl( String accession ) {
        try {
            return new URL( "https://www.ebi.ac.uk/biostudies/ArrayExpress/studies/" + urlEncode( accession ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
