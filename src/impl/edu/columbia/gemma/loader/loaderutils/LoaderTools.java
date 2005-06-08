package edu.columbia.gemma.loader.loaderutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
     * @param stopwatch
     * @see org.apache.commons.lang.time.StopWatch
     */
    public static final void displayTime( StopWatch stopwatch ) {
        long time = stopwatch.getTime();
        if ( time < 1000 )
            log.info( "Time taken: " + time + " ms." );
        else {
            log.info( "Time taken: " + time / 1000 + " s." );
        }
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
}
