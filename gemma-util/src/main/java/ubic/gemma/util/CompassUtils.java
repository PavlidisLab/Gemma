/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.Compass;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility methods to manipulate compass (and lucene).
 * 
 * @author keshav
 * @version $Id$
 */
public class CompassUtils {

    private static Log log = LogFactory.getLog( CompassUtils.class );

    /**
     * Deletes compass lock file(s).
     * 
     * @throws IOException
     */
    public static void deleteCompassLocks() {
        /*
         * FIXME Note that normally the lock files are not present - only during indexing. So I am turning this into a
         * no-op for now until we evaluate the necessity. - PP
         * 
         * The locks are stored in the same directoy as the indexes, so to do this we need to know the location of the
         * indexes (there are multiple locations)
         */

        // Collection<File> lockFiles = FileUtils.listFiles( /* FIXME */,
        // FileFilterUtils.suffixFileFilter( "lock" ), null );
        //
        // if ( lockFiles.size() == 0 ) {
        // log.debug( "Lucene lock files do not exist." );
        // return;
        // }
        //
        // for ( File file : lockFiles ) {
        // log.debug( "Removing Lucene lock file " + file );
        // file.delete();
        // }
    }

    /**
     * disables the index mirroring operation.
     * 
     * @param device
     */
    public static void disableIndexMirroring( CompassGpsInterfaceDevice device ) {
        device.stop();
    }

    /**
     * enables the index mirroring operation.
     * 
     * @param device
     */
    public static void enableIndexMirroring( CompassGpsInterfaceDevice device ) {
        device.start();
    }

    /**
     * Deletes and re-creates the index. See the IndexService
     * 
     * @param gps
     * @throws IOException
     */
    public static synchronized boolean rebuildCompassIndex( CompassGpsInterfaceDevice gps ) {
        boolean wasRunningBefore = gps.isRunning();

        log.debug( "CompassGps was running? " + wasRunningBefore );

        /*
         * Check state of device. If not running and you try to index, you will get a device exception.
         */
        if ( !wasRunningBefore ) {
            enableIndexMirroring( gps );
        }

        if ( gps.getIndexCompass().getSearchEngineIndexManager().indexExists() ) {
            if ( wasRunningBefore ) gps.stop();

            gps.getIndexCompass().getSearchEngineIndexManager().deleteIndex();
            log.info( "Deleting old index" );

            if ( wasRunningBefore ) gps.start();
        }

        gps.getIndexCompass().getSearchEngineIndexManager().createIndex();
        log.info( "indexing now ... " );
        try {
            gps.index();
        } catch ( Exception e ) {
            String bodyText = "Failed to build Index. Error is:  " + e;
            log.error( bodyText, e );
            return false;
        }

        log.info( "Indexing done. Now Optimizing index" );
        try {
            gps.getIndexCompass().getSearchEngineOptimizer().optimize();
        } catch ( Exception e ) {
            String bodyText = "Failed to optimize Index. Error is:  " + e;
            log.error( bodyText, e );
            return false;
        }

        log.info( "Optimizing complete" );
        /* Return state of device */
        if ( !wasRunningBefore ) {
            disableIndexMirroring( gps );
        }

        return true;
    }

    /**
     * @param compass eg: InternalCompass expressionBean = (InternalCompass) this.getBean("compassExpression"); Need
     *        this for replacing the indexes and it contains the path to the indexes to replace
     * @param pathToIndex An absolute path to the directory where the new indexes are located. Path should end at index
     *        sub dir.
     * @throws IOException
     */
    public static synchronized void swapCompassIndex( Compass compass, String pathToIndex ) throws IOException {

        log.info( "Attempting to swap indexes. From " + pathToIndex );
        final File srcDir = new File( pathToIndex );
        String engineSetting = compass.getSettings().getSetting( "compass.engine.connection" );

        assert engineSetting != null;

        final File targetDir = new File( engineSetting.replaceFirst( "file:", "" ) + "/index/" );

        log.info( "Swapping to: " + targetDir );
        // Validate that the new indexes exist and can read from them
        if ( !srcDir.canRead() ) {
            log.error( "Unable to read from specified directory: " + srcDir.getAbsolutePath() );
            return;
        }

        // Validate that we can write where we are copying the file to.
        if ( !targetDir.canWrite() ) {
            log.error( "Unable to read from specified directory: " + targetDir.getAbsolutePath() );
            return;
        }

        compass.getSearchEngineIndexManager().stop();

        log.info( "Deleting old index...." );
        compass.getSearchEngineIndexManager().deleteIndex();

        log.info( "Clearing Cache.... " );
        compass.getSearchEngineIndexManager().clearCache();

        log.info( "swapping index....." );
        FileUtils.copyDirectory( srcDir, targetDir );

        compass.getSearchEngineIndexManager().start();

    }

    /**
     * "Turning on" means adding the compass context to our spring context, as well as creating the compass index
     * directory. This does not turn on index mirroring to automatically update the index while persisting data (to a
     * database). To do this, call enableIndexMirroring after running this.
     * 
     * @param testEnv
     * @param paths
     */
    public static void turnOnCompass( boolean testEnv, List<String> paths ) {
        deleteCompassLocks();
        if ( testEnv ) {
            addCompassTestContext( paths );
        } else {
            addCompassContext( paths );
        }

    }

    /**
     * Add the compass contexts to the other spring contexts
     * 
     * @param paths
     */
    private static void addCompassContext( List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/applicationContext-search.xml" );
    }

    /**
     * Add the compass test contexts to the other spring contexts.
     * 
     * @param paths
     */
    private static void addCompassTestContext( List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/applicationContext-search.xml" );
    }
}
