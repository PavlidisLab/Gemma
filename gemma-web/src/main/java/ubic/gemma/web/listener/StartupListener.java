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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.web.scheduler.SchedulerUtils;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.util.Constants;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ubic.gemma.persistence.util.SpringContextUtil.prepareContext;

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
        prepareContext( ctx );
        return ctx;
    }

    @Override
    public void contextInitialized( ServletContextEvent event ) {
        StopWatch sw = new StopWatch();
        sw.start();

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

        this.initializeHomologene( ctx );

        this.configureScheduler( ctx );

        sw.stop();

        double time = sw.getTime() / 1000.00;
        StartupListener.log.info( "Initialization of Gemma Spring web context in " + time + " s " );
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

    private void initializeHomologene( ApplicationContext ctx ) {
        HomologeneService ho = ( HomologeneService ) ctx.getBean( "homologeneService" );
        ho.init( false );

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
            StartupListener.log.info( "Enabled Google analytics tracking with key " + gaTrackerKey );
        }
    }

    private void loadVersionInformation( Map<String, Object> config ) {
        StartupListener.log.debug( "Version is " + Settings.getAppVersion() );
        config.put( "version", Settings.getAppVersion() );
    }
}
