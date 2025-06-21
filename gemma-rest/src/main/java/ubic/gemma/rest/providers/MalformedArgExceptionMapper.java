package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.List;

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
        return WellComposedErrorBody.builder()
                .code( Response.Status.BAD_REQUEST.getStatusCode() )
                .message( exception.getMessage() )
                .errors( getErrors( exception ) )
                .build();
    }

    private List<WellComposedError> getErrors( MalformedArgException exception ) {
        if ( exception.getCause() != null ) {
            // TODO: report the exact request parameter that caused this
            Throwable t = ExceptionUtils.getRootCause( exception );
            return Collections.singletonList( WellComposedError.builder().reason( t.getClass().getName() ).message( t.getMessage() ).location( null ).locationType( null ).build() );
        } else {
            return Collections.emptyList();
        }
    }
}
