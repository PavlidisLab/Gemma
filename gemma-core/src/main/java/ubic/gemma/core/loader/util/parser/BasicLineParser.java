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
 * A simple LineParser implementation that doesn't do anything. Subclass this and implement the "parseOneLine" method.
 *
 * @author pavlidis
 */
public abstract class BasicLineParser<T> implements LineParser<T> {

    private static final String COMMENTMARK = "#";

    protected final Log log = LogFactory.getLog( this.getClass() );
    private BufferedReader br;

    @Override
    public abstract Collection<T> getResults();

    @Override
    public void parse( File file ) throws IOException {
        log.debug( "Parsing: " + file );
        if ( file == null ) {
            log.error( "File is null" );
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            log.error( "Could not read from file " + file.getPath() );
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        try ( InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() ) ) {
            this.parse( stream );
        }
    }

    @SuppressWarnings("Duplicates") // not effective to extract
    @Override
    public void parse( InputStream is ) throws IOException {
        log.debug( "Parsing input stream" );
        int linesParsed = 0;
        int nullLines = 0;

        if ( br == null ) {
            if ( is == null ) {
                log.error( "Inputstream null" );
                throw new IllegalArgumentException( "Inputstream null" );
            }

            if ( is.available() == 0 ) {
                log.error( "No bytes available" );
                throw new IOException( "No bytes available to read from inputStream" );
            }
            br = new BufferedReader( new InputStreamReader( is ) );
        }

        String line;

        StopWatch timer = new StopWatch();
        timer.start();
        while ( ( line = br.readLine() ) != null ) {
            log.debug( "Parsing line: " + line );
            if ( line.startsWith( BasicLineParser.COMMENTMARK ) ) {
                continue;
            }

            T newItem = this.parseOneLine( line );

            if ( newItem != null ) {
                this.addResult( newItem );
            } else {
                nullLines++;
            }

            if ( ++linesParsed % Parser.PARSE_ALERT_FREQUENCY == 0
                    && timer.getTime() > LineParser.PARSE_ALERT_TIME_FREQUENCY_MS ) {
                String message = "Parsed " + linesParsed + " lines...";
                log.info( message );
                timer.reset();
                timer.start();
            }

        }

        if ( linesParsed > LineParser.MIN_PARSED_LINES_FOR_UPDATE && this.getResults() != null ) {
            log.info( "Parsed " + linesParsed + " lines, " + this.getResults().size() + " items" );
        }
        if ( nullLines > 0 )
            log.info( nullLines + " yielded no parse result (they may have been filtered)." );

        log.debug( "Done parsing" );
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

    protected abstract void addResult( T obj );

}
