package ubic.gemma.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by tesarst on 09/03/17.
 * Utility class containing code that would otherwise have to be duplicate in multiple Controllers.
 */
public class ControllerUtils {

    protected static Log log = LogFactory.getLog( ControllerUtils.class.getName() );

    /**
     * Returns a collection of {@link Long} ids from strings.
     */
    public static Collection<Long> extractIds( String idString ) {
        Collection<Long> ids = new ArrayList<>();
        if ( idString != null ) {
            for ( String s : idString.split( "," ) ) {
                try {
                    ids.add( Long.parseLong( s.trim() ) );
                } catch ( NumberFormatException e ) {
                    ControllerUtils.log.warn( "invalid id " + s );
                }
            }
        }
        return ids;
    }

}
