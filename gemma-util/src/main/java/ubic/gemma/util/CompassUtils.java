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
package ubic.gemma.util;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.FSDirectory;
import org.compass.gps.device.AbstractGpsDevice;
import org.compass.gps.spi.CompassGpsInterfaceDevice; 

/**
 * Utility methods to manipulate compass (and lucene).
 * 
 * @author keshav
 * @version $Id$
 */
public class CompassUtils {

    private static Log log = LogFactory.getLog( CompassUtils.class );

    /**
     * Disables lucene locking mechanism. Alternatively, you see deleteCompassLock to delete the compass lock file.
     * 
     * @throws IOException
     */
    public static void disableLuceneLocks() {
        log.debug( "lock directory is " + FSDirectory.LOCK_DIR );

        log.debug( "disabling lucene locks" );
        FSDirectory.setDisableLocks( true );
    }

    /**
     * Deletes compass lock file(s).
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static void deleteCompassLocks() {
        log.debug( "compass lock dir: " + FSDirectory.LOCK_DIR );

        Collection<File> lockFiles = FileUtils.listFiles( new File( FSDirectory.LOCK_DIR ), FileFilterUtils
                .suffixFileFilter( "lock" ), null );

        if ( lockFiles.size() == 0 ) {
            log.debug( "Compass lock files do not exist." );
            return;
        }

        for ( File file : lockFiles ) {
            log.warn( "removing file " + file );
            // FileUtils.forceDeleteOnExit( file ); //delete on jvm term.
            file.delete(); // delete right away, not on jvm termination (not forcing).
        }
    }

    /**
     * Deletes and re-creates the index.
     * 
     * @param gps
     * @throws IOException
     */
    public static void rebuildCompassIndex( CompassGpsInterfaceDevice gps ) {
        gps.getIndexCompass().getSearchEngineIndexManager().deleteIndex();
        gps.getIndexCompass().getSearchEngineIndexManager().createIndex();
    }

    /**
     * disables the index mirroring operation.
     * 
     * @param device
     */
    public static void disableIndexMirroring( AbstractGpsDevice device ) {
        device.stop();
    }

    /**
     * enables the index mirroring operation.
     * 
     * @param device
     */
    public static void enableIndexMirroring( AbstractGpsDevice device ) {
        device.start();
    }
}
