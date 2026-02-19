package ubic.gemma.core.loader.expression.arrayExpress;

import static ubic.gemma.core.util.StringUtils.urlEncode;

public class ArrayExpressUtils {

    public static String getUri( String accession ) {
        return "https://www.ebi.ac.uk/biostudies/ArrayExpress/studies/" + urlEncode( accession );
    }
}
