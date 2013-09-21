/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods to create Spring contexts for Gemma. This is not used for webapps except under Test environments. It is
 * normally used for CLIs only.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );

    private static BeanFactory ctx = null;

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @param isWebApp If true, configuration specific to the web application will be included.
     * @param additionalConfigurationLocations, like "classpath*:/myproject/applicationContext-mine.xml"
     * @return BeanFactory or null if no context could be created.
     */
    public static BeanFactory getApplicationContext( boolean testing, boolean isWebApp,
            String[] additionalConfigurationLocations ) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations( testing, isWebApp );

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

    // /**
    // * @param testing If true, it will get a test configured-BeanFactory
    // * @param compassOn Include the compass (search) configuration. This is usually false for CLIs and tests.
    // * @param isWebApp If true, configuration specific to the web application will be included.
    // * @return BeanFactory or null if no context could be created.
    // */
    // public static BeanFactory getApplicationContext( boolean testing, boolean isWebApp ) {
    // return getApplicationContext( testing, isWebApp, new String[] {} );
    // }

    // /**
    // * @param additionalConfigurationPaths
    // * @return a minimally-configured standard BeanFactory: no Compass, no Web config, but with the additional
    // * configuration paths.
    // */
    // public static BeanFactory getApplicationContext( String[] additionalConfigurationPaths ) {
    // return getApplicationContext( false, false, additionalConfigurationPaths );
    // }

    // /**
    // * @return a minimally-configured standard BeanFactory: no Web config.
    // * @see getApplicationContext( boolean testing, boolean compassOn , boolean isWebApp)
    // */
    // public static BeanFactory getApplicationContext() {
    // return getApplicationContext( false, false );
    // }

    // /**
    // * Find the configuration file locations. The files must be in your class path for this to work.
    // *
    // * @return
    // */
    // public static String[] getConfigLocations() {
    // return getConfigLocations( false, true );
    // }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @param testing - if true, it will use the test configuration.
     * @return
     * @see getApplicationContext
     */
    public static String[] getConfigLocations( boolean testing, boolean isWebapp ) {
        if ( testing ) {
            return getTestConfigLocations( isWebapp );
        }
        return getStandardConfigLocations( isWebapp );

    }

    /**
     * @return
     */
    private static String getGemmaHomeProperty() {
        String gemmaHome = Settings.getString( "gemma.home" );
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
         * search is not listed first (?).
         */
        paths.add( "classpath*:gemma/gsec/acl/security-bean-baseconfig.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-security.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-hibernate.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-serviceBeans.xml" );
        if ( isWebapp ) {
            paths.add( "classpath*:ubic/gemma/applicationContext-component-scan.xml" );
            paths.add( "classpath*:ubic/gemma/applicationContext-schedule.xml" );
        }

        /*
         * When using a web application, we get the config locations from the web.xml files --- not using this class.
         * However, this IS used during tests, so we need it.
         */
        File f = new File( getGemmaHomeProperty() );
        try {
            if ( isWebapp ) {
                paths.add( f.toURI().toURL() + "gemma-web/target/Gemma/WEB-INF/gemma-servlet.xml" );
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
    private static String[] getStandardConfigLocations( boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/dataSource.xml" );

        CompassUtils.turnOnCompass( false, paths );

        addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

    /**
     * @param compassOn
     * @param isWebapp
     * @return
     */
    private static String[] getTestConfigLocations( boolean isWebapp ) {
        List<String> paths = new ArrayList<String>();
        paths.add( "classpath*:ubic/gemma/testDataSource.xml" );

        CompassUtils.turnOnCompass( true, paths );

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
        GenericWebApplicationContext spacesBeans = new GenericWebApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( spacesBeans );
        xmlReader.loadBeanDefinitions( resource );

        spacesBeans.setParent( parentContext );

        CommonsConfigurationPropertyPlaceholderConfigurer configurationPropertyConfigurer = ( CommonsConfigurationPropertyPlaceholderConfigurer ) spacesBeans
                .getBean( "configurationPropertyConfigurer" );
        configurationPropertyConfigurer.postProcessBeanFactory( spacesBeans.getBeanFactory() );

        spacesBeans.refresh();

        return spacesBeans;
    }

}