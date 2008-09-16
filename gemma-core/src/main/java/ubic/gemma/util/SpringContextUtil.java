package ubic.gemma.util;


import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.support.GenericWebApplicationContext;

import ubic.gemma.util.grid.javaspaces.SpacesUtil;

/**
 * Methods to create Spring contexts for Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );
    private static BeanFactory ctx = null;

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @param compassOn Include the compass (search) configuration. This is usually false for CLIs and tests.
     * @param gigaspacesOn Include the gigaspaces (grid) configuration. This is usually false for CLIs and tests.
     * @param isWebApp If true, configuration specific to the web application will be included.
     * @param additionalConfigurationLocations, like "classpath*:/myproject/applicationContext-mine.xml"
     * @return BeanFactory or null if no context could be created.
     */
    public static BeanFactory getApplicationContext( boolean testing, boolean compassOn, boolean gigaspacesOn,
            boolean isWebApp, String[] additionalConfigurationLocations ) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations( testing, compassOn, gigaspacesOn, isWebApp );

            if ( additionalConfigurationLocations != null ) {
                paths = addPaths( additionalConfigurationLocations, paths );
            }

            StopWatch timer = new StopWatch();
            timer.start();
            ctx = new ClassPathXmlApplicationContext( paths );
            timer.stop();
            if ( ctx != null ) {
                log.info( "Got context in " + timer.getTime() + "ms" );
            } else {
                log.fatal( "Failed to load context!" );
            }
        }
        return ctx;
    }

    /**
     * @param additionalConfigurationLocations
     * @param paths
     * @return
     */
    private static String[] addPaths( String[] additionalConfigurationLocations, String[] paths ) {
        Object[] allPaths = ArrayUtils.addAll( paths, additionalConfigurationLocations );
        paths = new String[allPaths.length];
        for ( int i = 0; i < allPaths.length; i++ ) {
            paths[i] = ( String ) allPaths[i];
        }
        return paths;
    }

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @param compassOn Include the compass (search) configuration. This is usually false for CLIs and tests.
     * @param gigaspacesOn Include the gigaspaces (grid) configuration. This is usually false for CLIs and tests.
     * @param isWebApp If true, configuration specific to the web application will be included.
     * @return BeanFactory or null if no context could be created.
     */
    public static BeanFactory getApplicationContext( boolean testing, boolean compassOn, boolean gigaspacesOn,
            boolean isWebApp ) {
        return getApplicationContext( testing, compassOn, gigaspacesOn, isWebApp, new String[] {} );
    }

    /**
     * @param additionalConfigurationPaths
     * @return a minimally-configured standard BeanFactory: no Compass, no Gigaspaces, no Web config, but with the
     *         additional configuration paths.
     */
    public static BeanFactory getApplicationContext( String[] additionalConfigurationPaths ) {
        return getApplicationContext( false, false, false, false, additionalConfigurationPaths );
    }

    /**
     * @return a minimally-configured standard BeanFactory: no Compass, no Gigaspaces, no Web config.
     * @see getApplicationContext( boolean testing, boolean compassOn, boolean gigaspacesOn, boolean isWebApp)
     */
    public static BeanFactory getApplicationContext() {
        return getApplicationContext( false, false, false, false );
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @return
     */
    public static String[] getConfigLocations() {
        return getConfigLocations( false, false, false, true );
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @param testing - if true, it will use the test configuration.
     * @return
     * @see getApplicationContext
     */
    public static String[] getConfigLocations( boolean testing, boolean compassOn, boolean gigaspacesOn,
            boolean isWebapp ) {
        if ( testing ) {
            return getTestConfigLocations( compassOn, isWebapp );
        }
        return getStandardConfigLocations( compassOn, gigaspacesOn, isWebapp );

    }

    /**
     * @return
     */
    private static String getGemmaHomeProperty() {
        String gemmaHome = ConfigUtils.getString( "gemma.home" );
        if ( gemmaHome == null ) {
            throw new RuntimeException( "You must set 'gemma.home' in your Gemma.properties" );
        }
        return gemmaHome;
    }

    /**
     * @param isWebapp
     * @param paths
     */
    private static void addCommonConfig( boolean isWebapp, List<String> paths ) {
        /*
         * Note that the order here matters, somewhat - in some environments, configuring beans in schedule fails if
         * search is not listed first.
         */
        paths.add( "classpath*:ubic/gemma/applicationContext-security.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-hibernate.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-serviceBeans.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-search.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-schedule.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-persisterBeans.xml" );
        File f = new File( getGemmaHomeProperty() );
        try {
            if ( isWebapp ) {
                paths.add( "classpath*:ubic/gemma/applicationContext-validation.xml" );
                paths.add( f.toURL() + "gemma-web/target/Gemma/WEB-INF/" + "action-servlet.xml" );
            }
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Could not form valid URL for " + f.getAbsolutePath(), e );
        }
    }

    /**
     * @param compassOn
     * @param isWebapp
     * @return
     */
    private static String[] getStandardConfigLocations( boolean compassOn, boolean gigaspacesOn, boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/dataSource.xml" );

        if ( compassOn ) {
            CompassUtils.turnOnCompass( false, paths );
        }

        if ( gigaspacesOn ) {
            SpacesUtil.addGigaspacesContextToPaths( paths );
        }

        addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

    /**
     * @param compassOn
     * @param isWebapp
     * @return
     */
    private static String[] getTestConfigLocations( boolean compassOn, boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/localTestDataSource.xml" );

        if ( compassOn ) {
            CompassUtils.turnOnCompass( true, paths );
        }
        addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

    /**
     * Adds the resource to the application context and sets the parentContext as the parent of the resource
     * 
     * @param parentContext
     * @param resource
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext addResourceToContext( ApplicationContext parentContext, ClassPathResource resource ) {
        GenericWebApplicationContext genericCtx = new GenericWebApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( genericCtx );
        xmlReader.loadBeanDefinitions( resource );

        genericCtx.setParent( parentContext );

        CommonsConfigurationPropertyPlaceholderConfigurer configurationPropertyConfigurer = ( CommonsConfigurationPropertyPlaceholderConfigurer ) genericCtx
                .getBean( "configurationPropertyConfigurer" );
        if ( configurationPropertyConfigurer != null )
            configurationPropertyConfigurer.postProcessBeanFactory( genericCtx.getBeanFactory() );

        genericCtx.refresh();

        return genericCtx;
    }
}
