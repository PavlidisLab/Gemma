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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.impl.StdScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ubic.gemma.Constants;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.QuartzUtils;

/**
 * StartupListener class used to initialize the spring context and make it available to the servlet context, so filters
 * that need the spring context can be configured. It also fills in parameters used by the application:
 * <ul>
 * <li>Theme (for styling pages)
 * <li>The version number of the application
 * <li>Ontologies that need to be preloaded.
 * <li>Google analytics tracking
 * </ul>
 * It also performs some initial setup needed to allow the compute grid to work, making sure the jar files are all
 * available.
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

    private static final String[] modules = new String[] { "core", "mda", "util" };

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.context.ContextLoaderListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized( ServletContextEvent event ) {
        log.info( "Initializing application context..." );
        StopWatch sw = new StopWatch();
        sw.start();

        // call Spring's context ContextLoaderListener to initialize
        // all the context files specified in web.xml
        super.contextInitialized( event );

        // /
        ServletContext servletContext = event.getServletContext();

        Map<String, Object> config = initializeConfiguration( servletContext );

        loadTheme( servletContext, config );

        loadVersionInformation( config );

        loadTrackerInformation( config );

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );

        CompassUtils.deleteCompassLocks();

        servletContext.setAttribute( Constants.CONFIG, config );

        if ( ConfigUtils.isRemoteTasksEnabled() ) copyWorkerJars( servletContext );

        initializeHomologene( ctx );

        configureScheduler( ctx );

        sw.stop();

        double time = sw.getTime() / 1000.00;
        log.info( "Initialization of Gemma Spring context in " + time + " s " );
    }

    /**
     * Remove old jars from the dir.
     * 
     * @param targetLibdir
     */
    private void clearOldJars( File targetLibdir ) {
        File[] oldJars = targetLibdir.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( File dir, String name ) {
                if ( name.endsWith( ".jar" ) ) {
                    return true;
                }
                return false;
            }
        } );
        for ( File jar : oldJars ) {
            if ( !jar.delete() ) {
                log.warn( "Unable to delete: " + jar );
            }
        }
    }

    /**
     * @param ctx
     */
    private void configureScheduler( ApplicationContext ctx ) {
        if ( !ConfigUtils.isSchedulerEnabled() ) {
            QuartzUtils.disableQuartzScheduler( ( StdScheduler ) ctx.getBean( "schedulerFactoryBean" ) );
            log.info( "Quartz scheduling disabled.  Set quartzOn=true in Gemma.properties to enable" );
        } else {
            log.info( "Quartz scheduling enabled.  Set quartzOn=false in Gemma.properties to disable" );
        }

    }

    /**
     * Copy the JAR files required by the JavaSpaces workers to the defined shared location. This includes all jars in
     * the WEB-INF/lib directory.
     */
    private void copyWorkerJars( ServletContext servletContext ) {

        String appName = "gemma";
        Map<String, String> appConfig = ( Map<String, String> ) servletContext.getAttribute( "appConfig" );
        String version = appConfig.get( "version" );

        File targetLibdir = null;
        String libpath = ConfigUtils.getLibDirectoryPath();
        if ( StringUtils.isNotBlank( libpath ) ) {
            targetLibdir = new File( libpath );
            if ( !targetLibdir.exists() ) {
                if ( !targetLibdir.mkdirs() ) {
                    log.warn( "destination directory " + targetLibdir
                            + " does not exist and could not be created; using " );
                    return;
                }
            }
        } else {
            targetLibdir = new File( System.getProperty( "java.io.tmpdir" ) + "Gemma" + File.separator + "lib" );
            targetLibdir.mkdirs();
            log.info( "Using " + targetLibdir + " as worker jar file target location" );
        }

        Collection<File> jars = new HashSet<File>();

        /*
         * Locate all the dependency jar files in the webapp's lib directory.
         */
        File sourceLibdir = null;
        for ( Enumeration<?> e = servletContext.getAttributeNames(); e.hasMoreElements(); ) {
            String key = ( String ) e.nextElement();
            // Looking for something like org.apache.catalina.jsp_classpath.
            // FIXME: will this work with other engines? Can we use java.class.path instead?
            if ( key.endsWith( "classpath" ) ) {
                String classpath = ( String ) servletContext.getAttribute( key );
                for ( String entry : classpath.split( File.pathSeparator ) ) {
                    if ( entry.endsWith( "jar" ) ) {
                        /*
                         * exclude the JSpaces jars, because they are provided by the space server as well, which can
                         * cause conflicts.
                         */
                        if ( entry.matches( ".*Gemma/WEB-INF/lib.*JSpaces.*\\.jar" ) ) {
                            // log.info( "Skipping: " + entry );
                            continue;
                        }
                        File jar = new File( entry );
                        jars.add( jar );
                        if ( entry.matches( ".*Gemma/WEB-INF/lib.*\\.jar" ) ) {
                            sourceLibdir = jar.getParentFile();
                        }
                    }
                }
            }
        }

        boolean foundAppJars = false;
        if ( sourceLibdir != null ) {
            for ( String module : modules ) {
                File jar = new File( sourceLibdir, String.format( appName + "-%s-%s.jar", module, version ) );
                if ( jar.canRead() && jars.contains( jar ) ) {
                    // jars.add( jar );
                    // log.info( "Gemma jar: " + jar );
                    foundAppJars = true; // if we found one, we probably found them all.
                }
            }
        }

        /*
         * In a development environment, the jar files are not relevant for the webapp (they won't be in WEB-INF/lib),
         * and might have been built. However, we'd still like to copy them if they exist. Let's assume that the user
         * has run 'mvn install' and therefore the jars will be in their maven target directories.
         */
        if ( !foundAppJars ) {
            log.warn( "Web application seems to be running in a development or test environment. "
                    + "Jars for javaspaces workers will be copied from the maven build directories. "
                    + "Make sure these are up to date if you are testing javaspaces." );
            String workDir = ConfigUtils.getString( "gemma.home" );
            if ( StringUtils.isNotBlank( workDir ) ) {
                for ( String module : modules ) {
                    File moduleDir = new File( workDir, appName + "-" + module );
                    if ( moduleDir.exists() ) {
                        File buildDir = new File( moduleDir, "target" );
                        if ( buildDir.exists() ) {
                            File jar = new File( buildDir, String.format( appName + "-%s-%s.jar", module, version ) );
                            if ( jar.canRead() ) {
                                // log.info( "Gemma jar: " + jar );
                                foundAppJars = true;
                                jars.add( jar );
                            }
                        }
                    }
                }
            }
        }

        if ( jars.isEmpty() ) {
            log.warn( "Unable to locate any jar files for copying to grid worker location. "
                    + "Javaspaces initialization may fail." );
            return;
        }

        if ( !foundAppJars ) {
            log.warn( "Gemma jar files for grid workers were not located -- you may need to build them. "
                    + "Javaspaces initializion may fail." );
        }

        Collection<File> copiedJars = new HashSet<File>();

        clearOldJars( targetLibdir );

        for ( File sourceJar : jars ) {
            try {
                File targetJar = new File( targetLibdir, sourceJar.getName() );
                if ( sourceJar.exists() ) {
                    FileUtils.copyFile( sourceJar, targetJar );
                    copiedJars.add( targetJar );
                } else {
                    log.warn( "Grid config: Cannot locate " + sourceJar );
                }
            } catch ( IOException e ) {
                log.error( "Error copying " + sourceJar + " to " + targetLibdir + ": " + e );
            }
        }

        /*
         * Create a text file that contains the list of jar files in java classpath format.
         */
        try {
            File classpathFile = new File( targetLibdir, "CLASSPATH" );

            String classpath = StringUtils.join( copiedJars, File.pathSeparator );
            FileUtils.writeStringToFile( classpathFile, classpath, null );
        } catch ( IOException e ) {
            log.error( "Error creating classpath file in " + targetLibdir );
        }

        log.info( copiedJars.size() + " jar files copied to " + targetLibdir + " for grid configuration" );
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
        String gaTrackerKey = ConfigUtils.getAnalyticsKey();
        if ( StringUtils.isNotBlank( gaTrackerKey ) ) {
            log.debug( "Tracker is " + gaTrackerKey );
            config.put( "ga.tracker", gaTrackerKey );
        }
    }

    /**
     * @param config
     */
    private void loadVersionInformation( Map<String, Object> config ) {
        log.debug( "Version is " + ConfigUtils.getAppVersion() );
        config.put( "version", ConfigUtils.getAppVersion() );
    }
}
