package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Component
public class MalformedArgExceptionMapper extends AbstractExceptionMapper<MalformedArgException> {

    @Autowired
    public MalformedArgExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, OpenAPI spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( MalformedArgException exception ) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( MalformedArgException exception ) {
        WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST.getStatusCode(), exception.getMessage() );
        if ( exception.getCause() != null ) {
            // TODO: report the exact request parameter that caused this
            body.addError( ExceptionUtils.getRootCause( exception ), null, null );
        }
        return body;
    }
}
