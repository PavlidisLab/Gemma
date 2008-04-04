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
import javax.servlet.ServletContextListener;

import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.rememberme.RememberMeAuthenticationProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
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
public class StartupListener extends ContextLoaderListener implements ServletContextListener {

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

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( servletContext );

        CompassUtils.deleteCompassLocks();

        loadRememberMeStatus( config, ctx );

        servletContext.setAttribute( Constants.CONFIG, config );

        populateDropDowns( servletContext );

        copyWorkerJars( servletContext );

        initializeOntologies( ctx );

        sw.stop();

        double time = sw.getTime() / 1000.00;
        log.info( "Startup of Gemma took " + time + " s " );
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
     * @param config
     */
    private void loadVersionInformation( Map<String, Object> config ) {
        log.debug( "Version is " + ConfigUtils.getAppVersion() );
        config.put( "version", ConfigUtils.getAppVersion() );
    }

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

    /**
     * Copy the JAR files required by the JavaSpaces workers to the defined shared location.
     */
    @SuppressWarnings("unchecked")
    private void copyWorkerJars( ServletContext servletContext ) {
        String libpath = ConfigUtils.getString( "gemma.lib.path" );
        File targetLibdir = new File( libpath );
        if ( !targetLibdir.exists() ) {
            if ( !targetLibdir.mkdirs() ) {
                log.error( "destination directory " + targetLibdir + " does not exist and could not be created" );
                return;
            }
        }

        File sourceLibdir = null;
        Collection<File> jars = new HashSet<File>();
        for ( Enumeration e = servletContext.getAttributeNames(); e.hasMoreElements(); ) {
            String key = ( String ) e.nextElement();
            if ( key.endsWith( "classpath" ) ) {
                String classpath = ( String ) servletContext.getAttribute( key );
                for ( String entry : classpath.split( ":" ) ) {
                    if ( entry.endsWith( ".jar" ) ) {
                        File jar = new File( entry );
                        jars.add( jar );
                        if ( sourceLibdir == null && entry.matches( ".*Gemma/WEB-INF/lib.*" ) )
                            sourceLibdir = jar.getParentFile();
                    }
                }
            }
        }
        Map<String, String> appConfig = ( Map<String, String> ) servletContext.getAttribute( "appConfig" );
        String version = appConfig.get( "version" );
        jars.add( new File( sourceLibdir, String.format( "%s-%s.jar", "gemma-core", version ) ) );
        jars.add( new File( sourceLibdir, String.format( "%s-%s.jar", "gemma-mda", version ) ) );
        jars.add( new File( sourceLibdir, String.format( "%s-%s.jar", "gemma-testing", version ) ) );
        jars.add( new File( sourceLibdir, String.format( "%s-%s.jar", "gemma-util", version ) ) );

        Collection<File> copiedJars = new HashSet<File>();
        for ( File sourceJar : jars ) {
            try {
                File targetJar = new File( targetLibdir, sourceJar.getName() );
                FileUtils.copyFile( sourceJar, targetJar );
                copiedJars.add( targetJar );
            } catch ( IOException e ) {
                log.error( "error copying " + sourceJar + " to " + targetLibdir + ": " + e );
            }
        }

        try {
            File classpathFile = new File( targetLibdir, "CLASSPATH" );
            String classpath = StringUtils.join( copiedJars, ":" );
            FileUtils.writeStringToFile( classpathFile, classpath, null );
        } catch ( IOException e ) {
            log.error( "error creating classpath file in " + targetLibdir );
        }
    }
}
