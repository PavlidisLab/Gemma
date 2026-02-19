package ubic.gemma.core.loader.entrez.pubmed;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class PubMedUtils {

    public static String getUri( String pubMedId ) {
        return "https://pubmed.ncbi.nlm.nih.gov/" + urlEncode( pubMedId ) + "/";
    }
}
