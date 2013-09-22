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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;

/**
 * A line parser that produces a Map instead of a Collection. Subclasses must provide a method to generate keys, which
 * is generated from the values. A typical use case of this is when the keys are also known to another class that needs
 * the data provided by the parser; to locate the data it uses the key.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BasicLineMapParser<K, T> implements LineParser<T> {

    /**
     * Lines starting with this will be ignored.
     */
    protected static final String COMMENTMARK = "#";

    protected Log log = LogFactory.getLog( getClass() );

    /**
     * @param key
     * @return
     * @see Map
     */
    public abstract boolean containsKey( K key );

    /**
     * @param key
     * @return value
     */
    public abstract T get( K key );

    public abstract Collection<K> getKeySet();

    /**
     * 
     */
    @Override
    public abstract Collection<T> getResults();

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.File)
     */
    @Override
    public void parse( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        log.info( "Parsing " + file.getAbsolutePath() );
        InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );
        parse( stream );
        stream.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.InputStream)
     */
    @Override
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) throw new IllegalArgumentException( "InputStream was null" );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        StopWatch timer = new StopWatch();
        timer.start();

        int nullLines = 0;
        String line = null;
        int linesParsed = 0;
        while ( ( line = br.readLine() ) != null ) {

            if ( line.startsWith( COMMENTMARK ) ) {
                continue;
            }
            T newItem = parseOneLine( line );

            if ( newItem == null ) {
                nullLines++;
                continue;
            }

            K key = getKey( newItem );
            if ( key == null ) {
                throw new IllegalStateException( "Got null key for item " + linesParsed );
            }
            put( key, newItem );

            if ( ++linesParsed % PARSE_ALERT_FREQUENCY == 0 && timer.getTime() > PARSE_ALERT_TIME_FREQUENCY_MS ) {
                String message = "Parsed " + linesParsed + " lines, last had key " + key;
                log.info( message );
                timer.reset();
                timer.start();
            }

        }
        log.info( "Parsed " + linesParsed + " lines. "
                + ( nullLines > 0 ? nullLines + " yielded no parse result (they may have been filtered)." : "" ) );

        br.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#pasre(java.lang.String)
     */
    @Override
    public void parse( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        parse( infile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineParser#parseOneLine(java.lang.String)
     */
    @Override
    public abstract T parseOneLine( String line );

    /**
     * @param newItem
     * @return
     */
    protected abstract K getKey( T newItem );

    /**
     * @param key
     * @param value
     */
    protected abstract void put( K key, T value );

}
