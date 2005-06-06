package edu.columbia.gemma.loader.loaderutils;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.loader.genome.gene.Parser;

/**
 * Utilities to be used by parsers and loaders.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class Utilities {
    protected static final Log log = LogFactory.getLog( Parser.class );

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
}
