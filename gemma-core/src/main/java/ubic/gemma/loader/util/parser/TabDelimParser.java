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

import ubic.basecode.util.StringUtil;

/**
 * A simple tab delim file parser
 * 
 * @author keshav
 * @version $Id$
 */
public class TabDelimParser extends BasicLineParser {

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
