package ubic.gemma.web.services.rest.swagger.resolver;

import io.swagger.v3.core.converter.ModelConverters;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers our customized {@link io.swagger.v3.core.jackson.ModelResolver}.
 *
 * @author poirigui
 * @see CustomModelResolver
 */
@CommonsLog
@WebListener
public class CustomModelResolverRegistrationListener implements ServletContextListener {

    private CustomModelResolver customModelResolver;

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContextEvent.getServletContext() );
        // these must be sorted by ascending precedence because addConverter prepends
        customModelResolver = applicationContext.getBean( CustomModelResolver.class );
        ModelConverters.getInstance().addConverter( customModelResolver );
        log.info( String.format( "Registered %s to Swagger's custom model converters.", CustomModelResolver.class.getName() ) );
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        ModelConverters.getInstance().removeConverter( customModelResolver );
        log.info( String.format( "Unregistered %s to Swagger's custom model converters.", CustomModelResolver.class.getName() ) );
    }
}
