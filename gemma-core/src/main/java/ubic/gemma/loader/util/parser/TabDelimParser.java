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

import org.apache.commons.lang.StringUtils;

/**
 * A simple tab delim file parser
 * 
 * @author keshav
 * @version $Id$
 */
public class TabDelimParser extends BasicLineParser {

    private String[] header = null;

    public void setHeader( String header ) {
        this.header = ( String[] ) parseOneLine( header );
    }

    public String[] getHeader() {
        return this.header;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        log.debug( "Got " + fields.length + " fields from line '" + line.substring( 0, Math.min( line.length(), 100 ) )
                + "' ..." );
        return fields;
    }
}
