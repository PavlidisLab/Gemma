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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaspacesUtil {

    private static Log log = LogFactory.getLog( GigaspacesUtil.class );

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
     * Start the space at the given url.
     * 
     * @param ctx
     */
    public static void startSpace( BeanFactory ctx, String url ) {

        throw new UnsupportedOperationException( "Method not yet implemented!" );

        // if ( !isSpaceRunning( url ) ) {
        //
        // }

    }

    /**
     * Stop the space at the given url.
     * 
     * @param ctx
     */
    public static void stopSpace( BeanFactory ctx, String url ) {
        throw new UnsupportedOperationException( "Method not yet implemented!" );

    }

    /**
     * Tests if workers are listening to the space at the given url.
     * 
     * @param default_remoting_space
     */
    public static void areWorkersListening( String default_remoting_space ) {
        throw new UnsupportedOperationException( "Method not yet implemented!" );

    }

}
