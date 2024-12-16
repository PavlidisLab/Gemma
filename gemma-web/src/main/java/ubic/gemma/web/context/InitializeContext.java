package ubic.gemma.web.context;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import ubic.gemma.core.config.Settings;
import ubic.gemma.web.util.Constants;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ubic.gemma.core.context.SpringContextUtils.prepareContext;

/**
 * Performs the standard context preparation from {@link ubic.gemma.core.context.SpringContextUtils#prepareContext(ApplicationContext)}
 * as well as some more specific Web-related setups.
 * @author keshav
 * @author pavlidis
 * @author Matt Raible (original version)
 * @author poirigui
 */
@CommonsLog
public class InitializeContext implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    /**
     * The style to be used if one is not defined in web.xml.
     */
    private static final String DEFAULT_THEME = "simplicity";

    @Override
    public void initialize( ConfigurableWebApplicationContext applicationContext ) {
        enableActiveProfiles( applicationContext );
        initializeConfiguration( applicationContext );
        prepareContext( applicationContext );
    }

    /**
     * Activate the Spring profiles declared in {@code spring.profiles.active} servlet init parameter.
     * <p>
     * FIXME: I think this is added in a later version of Spring (maybe <a href="https://github.com/PavlidisLab/Gemma/pull/508">#508<a> will fix this?)
     * @author poirigui
     */
    private void enableActiveProfiles( ConfigurableWebApplicationContext cac ) {
        ServletContext servletContext = cac.getServletContext();
        // setup active profiles
        if ( servletContext.getInitParameter( "spring.profiles.active" ) != null ) {
            String[] profiles = servletContext.getInitParameter( "spring.profiles.active" ).split( "," );
            log.debug( "The spring.profiles.active parameter is set, activating the following profiles: " + String.join( ", ", profiles ) + "." );
            for ( String activeProfile : profiles ) {
                cac.getEnvironment().addActiveProfile( activeProfile.trim() );
            }
        }
    }

    /**
     * Fills in parameters used by the application:
     * <ul>
     * <li>Theme (for styling pages) available under {@code theme}</li>
     * <li>All the settings from {@link Settings} available as a mapping under {@code appConfig[...]}</li>
     * </ul>
     */
    private void initializeConfiguration( ConfigurableWebApplicationContext applicationContext ) {
        ServletContext servletContext = applicationContext.getServletContext();
        Map<String, Object> config = new HashMap<>();
        lintConfiguration();
        loadSettings( config );
        loadTheme( servletContext, config );
        loadTrackerInformation( config );
        servletContext.setAttribute( Constants.CONFIG, config );
    }

    /**
     * Perform some basic sanity checks for the configuration.
     */
    private void lintConfiguration() {
        if ( !Settings.getBoolean( "load.ontologies" ) ) {
            log.warn( "Auto-loading of ontologies is disabled, enable it by setting load.ontologies=true in Gemma.properties." );
        }
        if ( !Settings.getBoolean( "load.homologene" ) ) {
            log.warn( "Homologene is not enabled, set load.homologene=true in Gemma.properties to load it on startup." );
        }
    }

    private void loadSettings( Map<String, Object> config ) {
        for ( Iterator<String> it = Settings.getKeys(); it.hasNext(); ) {
            String o = it.next();
            config.put( o, Settings.getProperty( o ) );
        }
    }

    /**
     * Load the style theme for the site.
     */
    private void loadTheme( ServletContext context, Map<String, Object> config ) {
        if ( context.getInitParameter( "theme" ) != null ) {
            log.debug( "Found theme " + context.getInitParameter( "theme" ) );
            config.put( "theme", context.getInitParameter( "theme" ) );
        } else {
            log.warn( "No theme found, using default=" + DEFAULT_THEME );
            config.put( "theme", DEFAULT_THEME );
        }
    }

    /**
     * For Google Analytics
     */
    private void loadTrackerInformation( Map<String, Object> config ) {
        String gaTrackerKey = ( String ) config.get( "ga.tracker" );
        if ( StringUtils.isNotBlank( gaTrackerKey ) ) {
            log.info( "Enabled Google Analytics tracking with key " + gaTrackerKey );
        }
    }
}
