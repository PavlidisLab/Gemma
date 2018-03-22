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
package ubic.gemma.persistence.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods to create Spring contexts for Gemma. This is not used for webapps except under Test environments. It is
 * normally used for CLIs only.
 *
 * @author pavlidis
 */
public class SpringContextUtil {
    private static final Log log = LogFactory.getLog( SpringContextUtil.class.getName() );

    private static BeanFactory ctx = null;

    /**
     * @param testing                           If true, it will get a test configured-BeanFactory
     * @param isWebApp                          If true, configuration specific to the web application will be included.
     * @param additionalConfigurationLocations, like "classpath*:/myproject/applicationContext-mine.xml"
     * @return BeanFactory or null if no context could be created.
     */
    public static BeanFactory getApplicationContext( boolean testing, boolean isWebApp,
            String[] additionalConfigurationLocations ) {
        if ( SpringContextUtil.ctx == null ) {
            String[] paths = SpringContextUtil.getConfigLocations( testing, isWebApp );

            if ( additionalConfigurationLocations != null ) {
                paths = SpringContextUtil.addPaths( additionalConfigurationLocations, paths );
            }

            StopWatch timer = new StopWatch();
            timer.start();
            SpringContextUtil.ctx = new ClassPathXmlApplicationContext( paths );
            timer.stop();
            if ( SpringContextUtil.ctx != null ) {
                SpringContextUtil.log.info( "Got context in " + timer.getTime() + "ms" );
            } else {
                SpringContextUtil.log.fatal( "Failed to load context!" );
            }
        }
        return SpringContextUtil.ctx;
    }

    private static String[] addPaths( String[] additionalConfigurationLocations, String[] paths ) {
        Object[] allPaths = ArrayUtils.addAll( paths, additionalConfigurationLocations );
        paths = new String[allPaths.length];
        for ( int i = 0; i < allPaths.length; i++ ) {
            paths[i] = ( String ) allPaths[i];
        }
        return paths;
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     *
     * @param testing  - if true, it will use the test configuration.
     * @param isWebapp is webapp
     * @return string[]
     */
    private static String[] getConfigLocations( boolean testing, boolean isWebapp ) {
        if ( testing ) {
            return SpringContextUtil.getTestConfigLocations( isWebapp );
        }
        return SpringContextUtil.getStandardConfigLocations( isWebapp );

    }

    private static String getGemmaHomeProperty() {
        String gemmaHome = Settings.getString( "gemma.home" );
        if ( gemmaHome == null ) {
            throw new RuntimeException( "You must set 'gemma.home' in your Gemma.properties" );
        }
        return gemmaHome;
    }

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
        File f = new File( SpringContextUtil.getGemmaHomeProperty() );
        try {
            if ( isWebapp ) {
                paths.add( f.toURI().toURL() + "gemma-web/target/Gemma/WEB-INF/gemma-servlet.xml" );
            }
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Could not form valid URL for " + f.getAbsolutePath(), e );
        }
    }

    private static String[] getStandardConfigLocations( boolean isWebapp ) {
        List<String> paths = new ArrayList<>();
        paths.add( "classpath*:ubic/gemma/dataSource.xml" );

        CompassUtils.turnOnCompass( false, paths );

        SpringContextUtil.addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

    private static String[] getTestConfigLocations( boolean isWebapp ) {
        List<String> paths = new ArrayList<>();
        paths.add( "classpath*:ubic/gemma/testDataSource.xml" );

        CompassUtils.turnOnCompass( true, paths );

        SpringContextUtil.addCommonConfig( isWebapp, paths );
        return paths.toArray( new String[] {} );
    }

}