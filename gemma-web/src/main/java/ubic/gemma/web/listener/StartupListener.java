/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.listener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.persistence.util.SpringProfiles;
import ubic.gemma.web.scheduler.SchedulerUtils;
import ubic.gemma.web.util.Constants;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * StartupListener class used to initialize the spring context and make it available to the servlet context, so filters
 * that need the spring context can be configured. It also fills in parameters used by the application:
 * <ul>
 * <li>Theme (for styling pages)
 * <li>The version number of the application
 * <li>Ontologies that need to be preloaded.
 * <li>Google analytics tracking
 * </ul>
 *
 * @author keshav
 * @author pavlidis
 * @author Matt Raible (original version)
 */
public class StartupListener extends ContextLoaderListener {

    /**
     * The style to be used if one is not defined in web.xml.
     */
    private static final String DEFAULT_THEME = "simplicity";

    private static final Log log = LogFactory.getLog( StartupListener.class );

    @Override
    protected WebApplicationContext createWebApplicationContext( ServletContext servletContext ) {
        WebApplicationContext ctx = super.createWebApplicationContext( servletContext );

        // setup active profiles
        if ( ctx instanceof ConfigurableApplicationContext ) {
            ConfigurableApplicationContext cac = ( ConfigurableApplicationContext ) ctx;
            // FIXME: I think this is added in a later version of Spring (maybe https://github.com/PavlidisLab/Gemma/pull/508 will fix this?)
            if ( servletContext.getInitParameter( "spring.profiles.active" ) != null ) {
                for ( String activeProfile : servletContext.getInitParameter( "spring.profiles.active" ).split( "," ) ) {
                    cac.getEnvironment().addActiveProfile( activeProfile.trim() );
                }
            }
            if ( !cac.getEnvironment().acceptsProfiles( SpringProfiles.PRODUCTION, SpringProfiles.DEV, SpringProfiles.TEST ) ) {
                log.warn( "No profiles were detected, activating the 'dev' profile as a fallback. Use -Dspring.profiles.active=dev explicitly to remove this warning." );
                cac.getEnvironment().addActiveProfile( SpringProfiles.DEV );
            }
        }

        return ctx;
    }

    @Override
    public void contextInitialized( ServletContextEvent event ) {
        StartupListener.log.info( "Initializing Gemma Web context..." );
        StopWatch sw = StopWatch.createStarted();

        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        // call Spring's context ContextLoaderListener to initialize
        // all the context files specified in web.xml
        super.contextInitialized( event );

        ServletContext servletContext = event.getServletContext();

        Map<String, Object> config = this.initializeConfiguration( servletContext );

        this.loadTheme( servletContext, config );

        this.loadVersionInformation( config );

        this.loadTrackerInformation( config );

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

        servletContext.setAttribute( Constants.CONFIG, config );

        this.configureJawr( ctx );

        this.configureScheduler( ctx );

        sw.stop();

        StartupListener.log.info( String.format( "Initialization of Gemma Web context took %d s. The following profiles are active: %s.",
                sw.getTime( TimeUnit.SECONDS ),
                String.join( ", ", ctx.getEnvironment().getActiveProfiles() ) ) );
    }

    private static final String JAWR_DEBUG_ON_SYSTEM_PROPERTY = "net.jawr.debug.on";

    /**
     * Set the net.jawr.debug.on system property to true if the current profile is {@link SpringProfiles#DEV} and no
     * value has been explicitly set by the user.
     */
    private void configureJawr( ApplicationContext ctx ) {
        if ( ctx.getEnvironment().acceptsProfiles( SpringProfiles.DEV )
                && System.getProperty( JAWR_DEBUG_ON_SYSTEM_PROPERTY ) == null ) {
            log.info( String.format( "Enabling debug mode for JAWR by setting %s=true.", JAWR_DEBUG_ON_SYSTEM_PROPERTY ) );
            System.setProperty( JAWR_DEBUG_ON_SYSTEM_PROPERTY, "true" );
        }
    }

    private void configureScheduler( ApplicationContext ctx ) {
        if ( !Settings.isSchedulerEnabled() ) {
            SchedulerUtils.disableScheduler( ctx.getBean( "schedulerFactoryBean", Scheduler.class ) );
            StartupListener.log.info( "Quartz scheduling disabled.  Set quartzOn=true in Gemma.properties to enable" );
        } else {
            StartupListener.log.info( "Quartz scheduling enabled.  Set quartzOn=false in Gemma.properties to disable" );
        }

    }

    private Map<String, Object> initializeConfiguration( ServletContext context ) {
        // Check if the config
        // object already exists
        @SuppressWarnings("unchecked") Map<String, Object> config = ( Map<String, Object> ) context
                .getAttribute( Constants.CONFIG );

        if ( config == null ) {
            config = new HashMap<>();
        }

        for ( Iterator<String> it = Settings.getKeys(); it.hasNext(); ) {
            String o = it.next();
            config.put( o, Settings.getProperty( o ) );
        }

        return config;
    }

    /**
     * Load the style theme for the site.
     *
     * @param context context
     * @param config  config
     */
    private void loadTheme( ServletContext context, Map<String, Object> config ) {
        if ( context.getInitParameter( "theme" ) != null ) {
            StartupListener.log.debug( "Found theme " + context.getInitParameter( "theme" ) );
            config.put( "theme", context.getInitParameter( "theme" ) );
        } else {
            StartupListener.log.warn( "No theme found, using default=" + StartupListener.DEFAULT_THEME );
            config.put( "theme", StartupListener.DEFAULT_THEME );
        }
    }

    /**
     * For google analytics
     *
     * @param config config
     */
    private void loadTrackerInformation( Map<String, Object> config ) {
        String gaTrackerKey = Settings.getAnalyticsKey();
        if ( StringUtils.isNotBlank( gaTrackerKey ) ) {
            config.put( "ga.tracker", gaTrackerKey );

            String gaTrackerDomain = Settings.getAnalyticsDomain();
            if ( StringUtils.isNotBlank( gaTrackerDomain ) ) {
                StartupListener.log.debug( "Tracker domain is " + gaTrackerDomain );
                config.put( "ga.domain", gaTrackerDomain );
                StartupListener.log.info( "Enabled Google analytics tracking with key " + gaTrackerKey + ", domain="
                        + gaTrackerDomain );

            } else {
                StartupListener.log.warn( "Google analytics will not work unless you also define the domain." );
            }
        }

    }

    private void loadVersionInformation( Map<String, Object> config ) {
        StartupListener.log.debug( "Version is " + Settings.getAppVersion() );
        config.put( "version", Settings.getAppVersion() );
    }
}
