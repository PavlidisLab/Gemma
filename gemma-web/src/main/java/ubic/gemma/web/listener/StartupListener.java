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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.impl.StdScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.Settings;
import ubic.gemma.util.QuartzUtils;
import ubic.gemma.web.util.Constants;

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
 * @version $Id$
 */
public class StartupListener extends ContextLoaderListener {

    /**
     * The style to be used if one is not defined in web.xml.
     */
    private static final String DEFAULT_THEME = "simplicity";

    private static final Log log = LogFactory.getLog( StartupListener.class );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.context.ContextLoaderListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized( ServletContextEvent event ) {
        log.info( "Initializing Gemma web context ..." );
        StopWatch sw = new StopWatch();
        sw.start();

        // call Spring's context ContextLoaderListener to initialize
        // all the context files specified in web.xml
        super.contextInitialized( event );

        ServletContext servletContext = event.getServletContext();

        Map<String, Object> config = initializeConfiguration( servletContext );

        loadTheme( servletContext, config );

        loadVersionInformation( config );

        loadTrackerInformation( config );

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        CompassUtils.deleteCompassLocks();

        servletContext.setAttribute( Constants.CONFIG, config );

        initializeHomologene( ctx );

        configureScheduler( ctx );

        sw.stop();

        double time = sw.getTime() / 1000.00;
        log.info( "Initialization of Gemma Spring web context in " + time + " s " );
    }

    /**
     * @param ctx
     */
    private void configureScheduler( ApplicationContext ctx ) {
        if ( !Settings.isSchedulerEnabled() ) {
            QuartzUtils.disableQuartzScheduler( ( StdScheduler ) ctx.getBean( "schedulerFactoryBean" ) );
            log.info( "Quartz scheduling disabled.  Set quartzOn=true in Gemma.properties to enable" );
        } else {
            log.info( "Quartz scheduling enabled.  Set quartzOn=false in Gemma.properties to disable" );
        }

    }

    /**
     * @param context
     * @return
     */
    private Map<String, Object> initializeConfiguration( ServletContext context ) {
        // Check if the config
        // object already exists
        Map<String, Object> config = ( Map<String, Object> ) context.getAttribute( Constants.CONFIG );

        if ( config == null ) {
            config = new HashMap<String, Object>();
        }

        for ( Iterator<String> it = Settings.getKeys(); it.hasNext(); ) {
            String o = it.next();
            config.put( o, Settings.getProperty( o ) );
        }

        return config;
    }

    /**
     * @param ctx
     */
    private void initializeHomologene( ApplicationContext ctx ) {
        HomologeneService ho = ( HomologeneService ) ctx.getBean( "homologeneService" );
        ho.init( false );

    }

    /**
     * Load the style theme for the site.
     * 
     * @param context
     * @param config
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
     * For google analytics
     * 
     * @param config
     */
    private void loadTrackerInformation( Map<String, Object> config ) {
        String gaTrackerKey = Settings.getAnalyticsKey();
        if ( StringUtils.isNotBlank( gaTrackerKey ) ) {
            config.put( "ga.tracker", gaTrackerKey );

            String gaTrackerDomain = Settings.getAnalyticsDomain();
            if ( StringUtils.isNotBlank( gaTrackerDomain ) ) {
                log.debug( "Tracker domain is " + gaTrackerDomain );
                config.put( "ga.domain", gaTrackerDomain );
                log.info( "Enabled Google analytics tracking with key " + gaTrackerKey + ", domain=" + gaTrackerDomain );

            } else {
                log.warn( "Google analytics will not work unless you also define the domain." );
            }
        }

    }

    /**
     * @param config
     */
    private void loadVersionInformation( Map<String, Object> config ) {
        log.debug( "Version is " + Settings.getAppVersion() );
        config.put( "version", Settings.getAppVersion() );
    }
}
