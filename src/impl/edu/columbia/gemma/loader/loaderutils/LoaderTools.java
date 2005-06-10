package edu.columbia.gemma.loader.loaderutils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utilities to be used by parsers and loaders.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class LoaderTools {
    protected static final Log log = LogFactory.getLog( LoaderTools.class );

    /**
     * Display time to be used with org.apache.commons.lang.time.StopWatch
     * 
     * @param loader
     * @param col
     */
    public static final void loadDatabase( Object loader, Collection col ) {
        assert ( loader != null );
        assert ( col != null );

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            loader.getClass().getMethod( "create", new Class[] { Collection.class } ).invoke( loader,
                    new Object[] { col } );
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( SecurityException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( NoSuchMethodException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stopWatch.stop();

        long time = stopWatch.getTime();
        if ( time < 1000 )
            log.info( "Time taken: " + time + " ms." );
        else {
            log.info( "Time taken: " + time / 1000 + " s." );
        }
    }

    /**
     * @param count
     * @param modulus
     * @param objectName
     */
    public static void objectsPersistedUpdate( int count, int modulus, String objectName ) {
        if ( count % modulus == 0 ) log.info( count + " " + objectName + " persisted" );

    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static InputStream retrieveByHTTP( String path ) throws IOException {

        if ( path == null ) throw new IllegalArgumentException();

        URL urlPattern = new URL( path );

        try {
            return urlPattern.openStream();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Method findParseLineMethod( Object obj, String fileName ) throws NoSuchMethodException {
        String[] f = StringUtils.split( fileName, System.getProperty( "file.separator" ) );
        String suffixOfFilename = f[f.length - 1];
        assert obj != null;
        Method[] methods = obj.getClass().getMethods();

        for ( Method m : methods ) {
            if ( m.getName().toLowerCase().contains( ( suffixOfFilename ).toLowerCase() ) ) {
                return m;
            }
        }
        throw new NoSuchMethodException();
    }

    /**
     * Print content of map if debug is set to true.
     * 
     * @param debug
     */
    public static void debugMap( Map map ) {
        if ( log.isDebugEnabled() ) {
            log.info( "Map contains: " );

            for ( Object obj : map.keySet() ) {
                log.info( obj );
            }

            log.info( "map size: " + map.keySet().size() );
        }
    }
}
