package ubic.gemma.core.loader.expression.synapse;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author poirigui
 */
public class SynapseUtils {

    public static String getUri( String accession ) {
        return "https://www.synapse.org/Synapse:" + urlEncode( accession );
    }
}
