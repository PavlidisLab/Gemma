package ubic.gemma.web.services.rest.util;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import lombok.extern.apachecommons.CommonsLog;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Register the {@link ArgModelResolver} to Swagger's {@link io.swagger.v3.core.converter.ModelConverters} singleton.
 *
 * @author poirigui
 */
@CommonsLog
@WebListener
public class ArgModelResolverRegistrationListener implements ServletContextListener {

    private final ArgModelResolver argModelConverter = new ArgModelResolver( Json.mapper() );

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        ModelConverters.getInstance().addConverter( argModelConverter );
        log.info( "Registered ArgModelConverter." );
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        ModelConverters.getInstance().removeConverter( argModelConverter );
        log.info( "Unregistered ArgModelConverter." );
    }
}
