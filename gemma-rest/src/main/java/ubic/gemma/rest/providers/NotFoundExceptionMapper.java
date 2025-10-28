package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedError;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * This mapper ensures that raised {@link NotFoundException} throughout the API contain well-formed {@link ResponseErrorObject}
 * entity.
 * <p>
 * Normally, this would be handled by {@link WebApplicationExceptionMapper}, but we also want to expose the stack trace
 * in the case of a missing entity.
 *
 * @author poirigui
 */
@Provider
@Component
public class NotFoundExceptionMapper extends AbstractExceptionMapper<NotFoundException> {

    @Autowired
    public NotFoundExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, @Qualifier("openApi") Future<OpenAPI> spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( NotFoundException exception ) {
        return Response.Status.NOT_FOUND;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( NotFoundException exception ) {
        return WellComposedErrorBody.builder()
                .code( Response.Status.NOT_FOUND.getStatusCode() )
                .message( exception.getMessage() )
                .errors( getErrors( exception ) )
                .build();
    }

    private List<WellComposedError> getErrors( NotFoundException exception ) {
        if ( exception.getCause() != null ) {
            // TODO: report the exact request parameter that caused this
            Throwable t = ExceptionUtils.getRootCause( exception );
            return Collections.singletonList( WellComposedError.builder().reason( t.getClass().getName() ).message( t.getMessage() ).location( null ).locationType( null ).build() );
        } else {
            return Collections.emptyList();
        }
    }

}
