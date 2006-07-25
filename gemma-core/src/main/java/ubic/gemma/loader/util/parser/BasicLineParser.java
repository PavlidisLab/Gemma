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
package ubic.gemma.loader.util.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple LineParser implementation that doesn't do anything. Subclass this and implement the "parseOneLine" method.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BasicLineParser implements LineParser {

    protected static final Log log = LogFactory.getLog( BasicLineParser.class );

    private Collection<Object> results;

    protected int linesParsed = 0;

    public BasicLineParser() {
        results = new HashSet<Object>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.InputStream)
     */
    public void parse( InputStream is ) throws IOException {

        if ( is == null || is.available() == 0 ) {
            log.fatal( "Inputstream null or empty" );
            throw new IllegalArgumentException( "Inputstream null or empty" );
        }

        linesParsed = 0;
        int nullLines = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;

        while ( ( line = br.readLine() ) != null ) {
            Object newItem = parseOneLine( line );

            if ( newItem != null ) {
                results.add( newItem );
            } else {
                log.debug( "Got null parse from " + line );
                nullLines++;
            }
            linesParsed++;
            if ( linesParsed % PARSE_ALERT_FREQUENCY == 0 ) log.debug( "Parsed " + linesParsed + " lines..." );

        }
        log.info( "Parsed " + linesParsed + " lines; " + nullLines + " yielded no parse result." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.File)
     */
    public void parse( File file ) throws IOException {
        if ( file == null ) {
            log.fatal( "File cannot be null" );
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        FileInputStream stream = new FileInputStream( file );
        parse( stream );
        stream.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#pasre(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        parse( infile );
    }

    /**
     * Add an object to the results collection.
     * 
     * @param obj
     */
    protected void addResult( Object obj ) {
        this.results.add( obj );
    }

    /**
     * 
     */
    public Collection<Object> getResults() {
        return results;
    }

}
