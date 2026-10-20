package ubic.gemma.rest.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.annotations.CacheControl;
import ubic.gemma.rest.annotations.CacheControls;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

@Provider
@Component
@Priority(Priorities.HEADER_DECORATOR)
public class CacheControlHeaderDecorator implements WriterInterceptor {

    @Autowired
    private AccessDecisionManager accessDecisionManager;

    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws IOException, WebApplicationException {
        for ( Annotation a : context.getAnnotations() ) {
            if ( a instanceof CacheControl ) {
                addCacheControl( context, ( CacheControl ) a );
                break;
            }
            if ( a instanceof CacheControls ) {
                for ( CacheControl config : ( ( CacheControls ) a ).value() ) {
                    addCacheControl( context, config );
                }
            }
        }
        context.proceed();
    }

    private void addCacheControl( WriterInterceptorContext context, CacheControl config ) {
        if ( config.authorities().length > 0 ) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                List<ConfigAttribute> configAttributes = SecurityConfig.createList( config.authorities() );
                accessDecisionManager.decide( auth, null, configAttributes );
            } catch ( AccessDeniedException | InsufficientAuthenticationException e ) {
                return;
            }
        }
        jakarta.ws.rs.core.CacheControl header = new jakarta.ws.rs.core.CacheControl();
        header.setMaxAge( config.maxAge() );
        header.setNoTransform( false );
        header.setPrivate( config.isPrivate() );
        context.getHeaders().add( "Cache-Control", header );
    }
}
