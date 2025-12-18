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
package ubic.gemma.core.loader.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.persister.Persister;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities to be used by parsers and loaders.
 *
 * @author keshav
 */
@SuppressWarnings("unused") // Possible external use
public class ParserAndLoaderTools {
    protected static final Log log = LogFactory.getLog( ParserAndLoaderTools.class );

    /**
     * User the loader to persist the collection.
     *
     * @param loader loader
     * @param col    collection
     */
    public static void loadDatabase( Persister loader, Collection<? extends Identifiable> col ) {
        assert ( loader != null );
        assert ( col != null );
        if ( col.size() == 0 )
            return;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        loader.persist( col );

        stopWatch.stop();

        long time = stopWatch.getTime();
        if ( time < 1000 )
            log.info( "Time taken: " + time + " ms." );
        else {
            log.info( "Time taken: " + time / 1000 + " s." );
        }
    }

    public static void objectsPersistedUpdate( int count, int modulus, String objectName ) {
        if ( count % modulus == 0 )
            log.info( count + " " + objectName + " persisted" );

    }

    public static Method findParseLineMethod( Object obj, String fileName ) throws NoSuchMethodException {
        String[] f = StringUtils.split( fileName, System.getProperty( "file.separator" ) );
        String suffixOfFilename = f[f.length - 1];
        assert obj != null;
        Method[] methods = obj.getClass().getMethods();
        for ( Method m : methods ) {
            if ( m.getName().toLowerCase().contains( ( suffixOfFilename ).toLowerCase() ) && m.getName()
                    .startsWith( "mapFrom" ) ) {
                return m;
            }
        }
        throw new NoSuchMethodException();
    }

    /**
     * Print content of map if debug is set to true.
     *
     * @param map map
     */
    public static void debugMap( Map<?, ?> map ) {
        if ( log.isDebugEnabled() ) {
            log.info( "Map contains: " );

            for ( Object obj : map.keySet() ) {
                log.info( obj );
            }

            log.info( "map size: " + map.keySet().size() );
        }
    }

}
