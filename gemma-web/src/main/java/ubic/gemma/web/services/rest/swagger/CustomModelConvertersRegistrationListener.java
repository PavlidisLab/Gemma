package ubic.gemma.web.services.rest.swagger;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Map;

/**
 * Registers all beans defined as {@link ModelConverter} to Swagger's {@link io.swagger.v3.core.converter.ModelConverters}
 * singleton.
 *
 * @author poirigui
 */
@CommonsLog
@WebListener
public class CustomModelConvertersRegistrationListener implements ServletContextListener {

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContextEvent.getServletContext() );
        for ( Map.Entry<String, ModelConverter> entry : applicationContext.getBeansOfType( ModelConverter.class ).entrySet() ) {
            ModelConverters.getInstance().addConverter( entry.getValue() );
            log.info( String.format( "Registered %s to Swagger's custom model converters.", entry.getKey() ) );
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
    }
}
