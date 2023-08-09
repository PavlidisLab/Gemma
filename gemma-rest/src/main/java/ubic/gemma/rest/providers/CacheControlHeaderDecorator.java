package ubic.gemma.rest.providers;

import ubic.gemma.rest.annotations.CacheControl;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CacheControlHeaderDecorator implements WriterInterceptor {
    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws IOException, WebApplicationException {
        Optional<CacheControl> config = Arrays.stream( context.getAnnotations() )
                .filter( a -> a.annotationType().equals( CacheControl.class ) )
                .map( a -> ( CacheControl ) a )
                .findFirst();
        if ( config.isPresent() ) {
            javax.ws.rs.core.CacheControl header = new javax.ws.rs.core.CacheControl();
            header.setMaxAge( config.get().maxAge() );
            context.getHeaders().putSingle( "Cache-Control", header );
        }
        context.proceed();
    }
}
