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
package ubic.gemma.core.loader.util.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.FileTools;

import java.io.*;
import java.util.Collection;

/**
 * A line parser that produces a Map instead of a Collection. Subclasses must provide a method to generate keys, which
 * is generated from the values. A typical use case of this is when the keys are also known to another class that needs
 * the data provided by the parser; to locate the data it uses the key.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public abstract class BasicLineMapParser<K, T> implements LineParser<T> {

    /**
     * Lines starting with this will be ignored.
     */
    protected static final String COMMENT_MARK = "#";

    protected final Log log = LogFactory.getLog( this.getClass() );

    public abstract boolean containsKey( K key );

    public abstract T get( K key );

    public abstract Collection<K> getKeySet();

    @Override
    public abstract Collection<T> getResults();

    @Override
    public void parse( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        log.info( "Parsing " + file.getAbsolutePath() );
        try (InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() )) {
            this.parse( stream );
        }
    }

    @Override
    public void parse( InputStream is ) throws IOException {

        if ( is == null )
            throw new IllegalArgumentException( "InputStream was null" );
        try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

            StopWatch timer = new StopWatch();
            timer.start();

            int nullLines = 0;
            String line;
            int linesParsed = 0;
            while ( ( line = br.readLine() ) != null ) {

                if ( line.startsWith( BasicLineMapParser.COMMENT_MARK ) ) {
                    continue;
                }
                T newItem = this.parseOneLine( line );

                if ( newItem == null ) {
                    nullLines++;
                    continue;
                }

                K key = this.getKey( newItem );
                if ( key == null ) {
                    throw new IllegalStateException( "Got null key for item " + linesParsed );
                }
                this.put( key, newItem );

                if ( ++linesParsed % Parser.PARSE_ALERT_FREQUENCY == 0
                        && timer.getTime() > LineParser.PARSE_ALERT_TIME_FREQUENCY_MS ) {
                    String message = "Parsed " + linesParsed + " lines, last had key " + key;
                    log.info( message );
                    timer.reset();
                    timer.start();
                }

            }
            log.info( "Parsed " + linesParsed + " lines. " + ( nullLines > 0 ?
                    nullLines + " yielded no parse result (they may have been filtered)." :
                    "" ) );

        }
    }

    @Override
    public void parse( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        this.parse( infile );
    }

    @Override
    public abstract T parseOneLine( String line );

    protected abstract K getKey( T newItem );

    protected abstract void put( K key, T value );

}
