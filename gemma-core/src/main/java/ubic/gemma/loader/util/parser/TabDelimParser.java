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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple tab delim file parser
 * 
 * @author keshav
 * @version $Id$
 */
public class TabDelimParser extends BasicLineParser {
    private Log log = LogFactory.getLog( this.getClass() );
    private Collection<Object> results;
    private String[] header = null;

    public TabDelimParser() {
        results = new HashSet<Object>();
    }

    /**
     * Extracts header information.
     * 
     * @param is
     * @param header
     * @throws IOException
     */
    public void parse( InputStream is, boolean header ) throws IOException {// FIXME to i really want to override this?

        linesParsed = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;

        if ( header ) setHeader( line = br.readLine() );

        while ( ( line = br.readLine() ) != null ) {
            Object newItem = parseOneLine( line );

            if ( newItem != null ) {
                results.add( newItem );
                linesParsed++;
            }
            if ( linesParsed % PARSE_ALERT_FREQUENCY == 0 ) log.debug( "Parsed " + linesParsed + " lines..." );

        }
        log.info( "Parsed " + linesParsed + " lines." );
    }

    public void setHeader( String header ) {
        this.header = ( String[] ) parseOneLine( header );
    }

    public String[] getHeader() {
        return this.header;
    }

    public Collection getResults() {
        return this.results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        // String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' ); TODO test which is more efficient
        String[] fields = line.split( "\t" );
        return fields;
    }

}
