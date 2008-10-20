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
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.impl.StdScheduler;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.ProviderManager;
import org.springframework.security.providers.rememberme.RememberMeAuthenticationProvider;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.ontology.AbstractOntologyService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.LabelValue;
import ubic.gemma.util.QuartzUtils;

/**
 * StartupListener class used to initialize the spring context and make it available to the servlet context, so filters
 * that need the spring context can be configured. It also fills in parameters used by the application:
 * <ul>
 * <li>Theme (for styling pages)
 * <li>The version number of the application
 * <li>Whether 'remember me' functionality is enabled
 * <li>Whether and how to encrypt passwords
 * <li>Static information used to populate drop-downs, e.g., the list of user roles
 * </ul>
 * 
 * @author keshav
 * @author pavlidis
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a> (original version)
 * @version $Id$
 */
public class StartupListener extends ContextLoaderListener {

    /**
     * Configuration parameter for lib directory.
     */
    private static final String GEMMA_LIB_DIR = "gemma.lib.dir";

    /**
     * Key for the tracker ID in your configuration file. Tracker id for Google is something like 'UA-12441-1'. In your
     * Gemma.properties file add a line like
     * 
     * <pre>
     * ga.tracker = UA_123456_1
     * </pre>
     */
    private static final String ANALYTICS_TRACKER_PROPERTY = "ga.tracker";

    /**
     * The style to be used if one is not defined in web.xml.
     */
    private static final String DEFAULT_THEME = "simplicity";

    private static final Log log = LogFactory.getLog( StartupListener.class );

    private static final String[] modules = new String[] { "core", "mda", "util" };

    private static final String QUARTZ = "quartzOn";

    /**
     * This is used to get information from the system that does not change and which can be reused throughout -
     * typically used in drop-down menus.
     * 
     * @param context
     */
    @SuppressWarnings("unchecked")
    public static void populateDropDowns( ServletContext context ) {
        log.debug( "Populating drop-downs..." );
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( context );

        // mimic the functionality of the LookupManager in Appfuse.
        UserService mgr = ( UserService ) ctx.getBean( "userService" );
        Set<LabelValue> roleList = new HashSet<LabelValue>();

        // get list of possible roles, used to populate admin tool where roles can be altered.
        Collection<UserRole> roles = mgr.loadAllRoles();
        for ( UserRole role : roles ) {
            roleList.add( new LabelValue( role.getName(), role.getName() ) );
        }

        context.setAttribute( Constants.AVAILABLE_ROLES, roleList );

        if ( log.isDebugEnabled() ) {
            log.debug( "Drop-down initialization complete [OK]" );
        }

        assert ( context.getAttribute( Constants.AVAILABLE_ROLES ) != null );

    }

    /*
     * (non-Javadoc)
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

        ServletContext servletContext = event.getServletContext();

        Map<String, Object> config = initializeConfiguration( servletContext );

        loadTheme( servletContext, config );

        loadVersionInformation( config );

        loadTrackerInformation( config );

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

        CompassUtils.deleteCompassLocks();

        loadRememberMeStatus( config, ctx );

        servletContext.setAttribute( Constants.CONFIG, config );

        populateDropDowns( servletContext );

        copyWorkerJars( servletContext );

        initializeOntologies( ctx );

        configureScheduler( ctx );

        sw.stop();

        double time = sw.getTime() / 1000.00;
        log.info( "Initialization of Gemma Spring context in " + time + " s " );
    }

    /**
     * @param ctx
     */
    private void configureScheduler( ApplicationContext ctx ) {
        if ( !ConfigUtils.getBoolean( QUARTZ, false ) ) {
            QuartzUtils.disableQuartzScheduler( ( StdScheduler ) ctx.getBean( "schedulerFactoryBean" ) );
            log.info( "Quartz scheduling disabled.  Set quartzOn=true in Gemma.properties to enable" );
        } else
            log.info( "Quartz scheduling enableded.  Set quartzOn=false in Gemma.properties to disable" );
    }

    /**
     * Copy the JAR files required by the JavaSpaces workers to the defined shared location. This includes all jars in
     * the WEB-INF/lib directory.
     */
    @SuppressWarnings("unchecked")
    private void copyWorkerJars( ServletContext servletContext ) {

        String appName = "gemma";
        Map<String, String> appConfig = ( Map<String, String> ) servletContext.getAttribute( "appConfig" );
        String version = appConfig.get( "version" );

        File targetLibdir = null;
        String libpath = ConfigUtils.getString( GEMMA_LIB_DIR );
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
        for ( Enumeration e = servletContext.getAttributeNames(); e.hasMoreElements(); ) {
            String key = ( String ) e.nextElement();
            // Looking for something like org.apache.catalina.jsp_classpath.
            // FIXME: will this work with other engines? Can we use java.class.path instead?
            if ( key.endsWith( "classpath" ) ) {
                String classpath = ( String ) servletContext.getAttribute( key );
                for ( String entry : classpath.split( File.pathSeparator ) ) {
                    if ( entry.endsWith( "jar" ) ) {
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
    @SuppressWarnings("unchecked")
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
     * Intialize ontologies as configured by the user's configuration file (Gemma.properties).
     * <p>
     * FIXME make this smarter so it can figure this out without hard-coding ontology names.
     * 
     * @param ctx
     */
    private void initializeOntologies( ApplicationContext ctx ) {

        GeneOntologyService go = ( GeneOntologyService ) ctx.getBean( "geneOntologyService" );
        go.init( false );

        String[] otherOntologies = new String[] { "mged", "disease", "fma", "birnLex", "chebi" };

        for ( String ont : otherOntologies ) {
            AbstractOntologyService os = ( AbstractOntologyService ) ctx.getBean( ont + "OntologyService" );
            os.init( false );
        }

    }

    /**
     * @param config
     * @param ctx
     */
    @SuppressWarnings("unchecked")
    private void loadRememberMeStatus( Map<String, Object> config, ApplicationContext ctx ) {
        try {
            ProviderManager provider = ( ProviderManager ) ctx.getBean( "authenticationManager" );
            for ( Iterator<AuthenticationProvider> it = provider.getProviders().iterator(); it.hasNext(); ) {
                AuthenticationProvider p = it.next();
                if ( p instanceof RememberMeAuthenticationProvider ) {
                    config.put( "rememberMeEnabled", Boolean.TRUE );
                    log.debug( "Remember Me is enabled" );
                    break;
                }
            }

        } catch ( NoSuchBeanDefinitionException n ) {
            // ignore, should only happen when testing
        }
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
        String gaTrackerKey = ConfigUtils.getString( ANALYTICS_TRACKER_PROPERTY );
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
