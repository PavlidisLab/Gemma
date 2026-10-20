package ubic.gemma.core.loader.expression.zenodo;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class ZenodoUtils {

    public static String getUri( String recordId ) {
        return "https://zenodo.org/records/" + urlEncode( recordId );
    }
}
