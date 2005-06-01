package edu.columbia.gemma.loader.loaderutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple LineParser implementation that doesn't do anything. Subclass this and implement the "parseOneLine" method.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BasicLineParser implements LineParser {

    protected static final Log log = LogFactory.getLog( BasicLineParser.class );

    private Collection results;

    /**
     * How often we tell the user what is happening.
     */
    protected static final int ALERT_FREQUENCY = 10000;

    public BasicLineParser() {
        results = new HashSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.InputStream)
     */
    public void parse( InputStream is ) throws IOException {
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int count = 0;
        while ( ( line = br.readLine() ) != null ) {
            Object newItem = parseOneLine( line );

            if ( newItem != null ) {
                results.add( newItem );
                count++;
            }
            if ( count % ALERT_FREQUENCY == 0 ) log.debug( "Read in " + count + " items..." );

        }
        log.info( "Read in " + count + " items..." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.File)
     */
    public void parse( File file ) throws IOException {
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        FileInputStream stream = new FileInputStream( file );
        parse( stream );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#pasre(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        File infile = new File( filename );
        parse( infile );

    }

    public Iterator<?> iterator() {
        return results.iterator();
    }

}
