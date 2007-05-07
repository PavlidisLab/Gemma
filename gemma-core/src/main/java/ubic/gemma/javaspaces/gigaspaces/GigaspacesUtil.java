/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.javaspaces.gigaspaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springmodules.javaspaces.DelegatingWorker;

import ubic.gemma.util.SpringContextUtil;

import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaspacesUtil {

    private static final String GIGASPACES_EXPRESSION_EXPERIMENT_BEAN_FACTORY = "classpath*:/ubic/gemma/gigaspaces-expressionExperiment.xml";
    private static Log log = LogFactory.getLog( GigaspacesUtil.class );

    /**
     * Determines if the (@link BeanFactory) contains gigaspaces beans.
     * 
     * @param testing
     * @param compassOn
     * @param isWebApp
     * @return
     */
    private static boolean beanFactoryContainsGigaspaces( boolean testing, boolean compassOn, boolean isWebApp ) {

        BeanFactory factory = SpringContextUtil.getApplicationContext( testing, compassOn, isWebApp );

        return factory.containsBean( "gigaspacesTemplate" );

    }

    /**
     * First checks if the bean exists in context, then if it is running.
     * 
     * @param ctx
     * @return
     */
    public static boolean isSpaceRunning( String url ) {

        boolean running = true;
        try {
            SpaceFinder.find( url );
        } catch ( FinderException e ) {
            running = false;
            log.error( "Error finding space at: " + url + ".  Exception is: " );
            e.printStackTrace();
        } finally {
            return running;
        }

    }

    /**
     * First checks if the space is running at url. If space is running, adds the gigaspaces beans to the bean factory.
     * 
     * @param ctx
     */
    public static BeanFactory addGigaspacesBeanFactory( String url, boolean testing, boolean compassOn, boolean isWebApp ) {

        if ( !isSpaceRunning( url ) ) {
            throw new RuntimeException( "Cannot add Gigaspaces BeanFactory.  Space not started at " + url );
        }

        BeanFactory ctx = SpringContextUtil.getApplicationContext( testing, compassOn, isWebApp );

        if ( !beanFactoryContainsGigaspaces( testing, compassOn, isWebApp ) ) {

            String[] locations = SpringContextUtil.getConfigLocations( testing, compassOn, isWebApp );

            List<String> paths = new ArrayList<String>( Arrays.asList( locations ) );

            paths.add( GIGASPACES_EXPRESSION_EXPERIMENT_BEAN_FACTORY );
            // FIXME this could be dangerous as all beans are reloaded.
            // just add a new "resource".
            ctx = new ClassPathXmlApplicationContext( paths.toArray( new String[] {} ) );

        }

        else {
            log.info( "Bean factory unchanged.  Gigaspaces beans already existed." );
        }

        return ctx;

    }

    // /**
    // * Tests if workers are listening to the space at the given url.
    // *
    // * @param url
    // */
    // public static boolean areWorkersListening( String url, boolean testing, boolean compassOn, boolean isWebApp ) {
    //
    // if ( !isSpaceRunning( url ) ) {
    // throw new RuntimeException( "Space not running at: " + url );
    // }
    //
    // if ( !beanFactoryContainsGigaspaces( testing, compassOn, isWebApp ) ) {
    // throw new RuntimeException( "Application context does not contain Gigaspaces beans." );
    // }
    //
    // // TODO finish this - should be task specific (cannot just use a dummy task)
    // BeanFactory factory = SpringContextUtil.getApplicationContext( testing, compassOn, isWebApp );
    //
    // // factory.getBean( "" )
    //
    // return false;
    // }

}
