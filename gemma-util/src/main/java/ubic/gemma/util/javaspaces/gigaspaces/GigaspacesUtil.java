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
package ubic.gemma.util.javaspaces.gigaspaces;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaspacesUtil implements BeanFactoryAware {

    private static Log log = LogFactory.getLog( GigaspacesUtil.class );

    private BeanFactory beanFactory = null;

    /**
     * Determines if the (@link BeanFactory) contains gigaspaces beans.
     * 
     * @param testing
     * @param compassOn
     * @param isWebApp
     * @return boolean
     */
    private boolean beanFactoryContainsGigaspaces() {

        return beanFactory.containsBean( "gigaspacesTemplate" );

    }

    /**
     * Checks if space is running at specified url.
     * 
     * @param ctx
     * @return boolean
     */
    public static boolean isSpaceRunning( String url ) {

        boolean running = true;
        try {
            SpaceFinder.find( url );
        } catch ( FinderException e ) {
            running = false;
            log.error( "Error finding space at: " + url + "." );
            // e.printStackTrace();
        } finally {
            return running;
        }
    }

    /**
     * Add the gigaspaces contexts to the other spring contexts
     * 
     * @param paths
     */
    public static void addGigaspacesContext( List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/gigaspaces-expressionExperiment.xml" );
    }

    // /**
    // * First checks if the space is running at url. If space is running, adds the gigaspaces beans to the bean factory
    // * if they do not exist. If space is not running, returns the original context.
    // *
    // * @param ctx
    // * @return BeanFactory
    // */
    // public BeanFactory addGigaspacesToBeanFactory( String url, boolean testing, boolean compassOn, boolean isWebApp )
    // {
    //
    // if ( !isSpaceRunning( url ) ) {
    // log.error( "Cannot add Gigaspaces to BeanFactory. Space not started at " + url
    // + ". Returning context without gigaspaces beans." );
    //
    // return beanFactory;
    //
    // }
    //
    // if ( !beanFactoryContainsGigaspaces() ) {
    //
    // // FIXME I cannot get the locations from the beanFactory directly
    // String[] locations = SpringContextUtil.getConfigLocations( testing, compassOn, isWebApp );
    //
    // List<String> paths = new ArrayList<String>( Arrays.asList( locations ) );
    //
    // paths.add( GIGASPACES_EXPRESSION_EXPERIMENT_BEAN_FACTORY );
    // // FIXME this could be dangerous (not to mention slow) as all beans are reloaded.
    // // What aboud adding a new "resource".
    // beanFactory = new ClassPathXmlApplicationContext( paths.toArray( new String[] {} ) );
    //
    // }
    //
    // else {
    // log.info( "Bean factory unchanged. Gigaspaces beans already existed." );
    // }
    //
    // return beanFactory;
    //
    // }

    public void setBeanFactory( BeanFactory beanFactory ) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
