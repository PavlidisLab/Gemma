package ubic.gemma.web.services.rest.util;

import ubic.gemma.web.services.rest.util.args.Arg;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

public class ArgUtils {

    public static void checkReqArg( Arg arg, String name ) throws BadRequestException {
        if ( arg == null || arg.toString().isEmpty() ) {
            throw new BadRequestException( String.format( "Value for required parameter '%s' not found.", name ) );
        }
    }
}
