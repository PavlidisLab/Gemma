package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.MediaType;

/**
 * Utilities for {@link MediaType}.
 *
 * @author poirigui
 */
public class MediaTypeUtils {

    public static final String TEXT_TAB_SEPARATED_VALUES_UTF8 = "text/tab-separated-values; charset=UTF-8";

    @SuppressWarnings("unused")
    public static final MediaType TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE = new MediaType( "text", "tab-separated-values", "UTF-8" );
}
