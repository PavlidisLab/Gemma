package ubic.gemma.core.loader.expression.sra;

import static ubic.gemma.core.util.StringUtils.urlEncode;

public class SraUtils {

    public static String getUri( String accession ) {
        return "https://www.ncbi.nlm.nih.gov/sra?term=" + urlEncode( accession );
    }
}
