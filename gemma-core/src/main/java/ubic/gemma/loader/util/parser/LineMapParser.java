/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.time.StopWatch;

/**
 * The difference between this class and BasicLineMapParser is more flexibility in how keys are provided. The
 * parseOneLine method that is implemented must handle adding the data to the Map.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class LineMapParser<K, T> extends BasicLineMapParser<K, T> {

    @Override
    public final K getKey( T o ) {
        throw new UnsupportedOperationException( "The subclass must handle adding to the map" );
    }

    @Override
    protected final void put( K key, T value ) {
        throw new UnsupportedOperationException( "The subclass must handle adding to the map" );
    }

    private int linesParsed = 0;

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.InputStream)
     */
    @Override
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) {
            throw new IllegalArgumentException( "Inputstream null" );
        }

        if ( is.available() == 0 ) {
            throw new IOException( "No bytes available to read from inputStream" );
        }

        linesParsed = 0;
        int nullLines = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        StopWatch timer = new StopWatch();
        timer.start();
        String line = null;

        while ( ( line = br.readLine() ) != null ) {

            if ( line.startsWith( COMMENTMARK ) ) {
                continue;
            }

            // The returned object is just used to check if the was data in the line. Storing the data has to be taken
            // care of by the subclass.
            Object newItem = parseOneLine( line );

            if ( newItem == null ) {
                nullLines++;
            }

            if ( ++linesParsed % PARSE_ALERT_FREQUENCY == 0 && timer.getTime() > PARSE_ALERT_TIME_FREQUENCY_MS ) {
                String message = "Parsed " + linesParsed + " lines...";
                log.info( message );
                timer.reset();
                timer.start();
            }

        }

        if ( linesParsed > MIN_PARSED_LINES_FOR_UPDATE ) {
            log.info( "Parsed " + linesParsed + " lines, " + this.getResults().size() + " items" );
        }
        if ( nullLines > 0 ) log.info( nullLines + " yielded no parse result (they may have been filtered)." );
    }

    /**
     * @return the linesParsed
     */
    public int getLinesParsed() {
        return linesParsed;
    }

}
