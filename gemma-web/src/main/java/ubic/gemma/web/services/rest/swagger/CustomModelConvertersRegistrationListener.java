package ubic.gemma.web.services.rest.swagger;

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
 * Registers all beans defined as {@link CustomModelConverter} to Swagger's {@link io.swagger.v3.core.converter.ModelConverters}
 * singleton.
 *
 * @author poirigui
 */
@CommonsLog
@WebListener
public class CustomModelConvertersRegistrationListener implements ServletContextListener {

    private List<Pair<String, CustomModelConverter>> converters = new ArrayList<>();

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContextEvent.getServletContext() );
        // these must be sorted by ascending precedence because addConverter prepends
        this.converters = applicationContext.getBeansOfType( CustomModelConverter.class )
                .entrySet().stream()
                .map( Pair::of )
                // sorted in reverse order because of how addConverter works (thus negative sign)
                .sorted( Comparator.comparingInt( pair -> -pair.getValue().getOrder() ) )
                .collect( Collectors.toList() );
        for ( Pair<String, CustomModelConverter> converter : converters ) {
            ModelConverters.getInstance().addConverter( converter.getValue() );
            log.info( String.format( "Registered %s to Swagger's custom model converters.", converter.getKey() ) );
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
        for ( Pair<String, CustomModelConverter> converter : converters ) {
            ModelConverters.getInstance().removeConverter( converter.getValue() );
            log.info( String.format( "Unregistered %s to Swagger's custom model converters.", converter.getKey() ) );
        }
    }
}
