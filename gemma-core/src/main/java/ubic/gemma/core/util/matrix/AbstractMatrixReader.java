/*
 * The baseCode project
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
package ubic.gemma.core.util.matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class representing an object that can read in a {@link Matrix2D} from a file.
 * <p>
 * Ported in-tree from {@code ubic.basecode.io.reader.AbstractMatrixReader} as part of the baseCode in-tree
 * migration; previously consumed via the {@code MatrixUtil.fromBaseCode} shim.
 *
 * @author Paul Pavlidis
 */
public abstract class AbstractMatrixReader<M extends Matrix2D<String, String, V>, V> {

    protected static Logger log = LoggerFactory.getLogger( AbstractMatrixReader.class );

    public abstract M read( InputStream stream ) throws IOException;

    public abstract M read( String filename ) throws IOException;

    public abstract M read( String filename, int maxRows ) throws IOException;

    /**
     * @param dis input reader positioned at the start of the header line
     * @param skipColumns how many data columns should be ignored
     * @return list of column header names (excluding the corner cell)
     * @throws IOException on I/O error or malformed header
     */
    protected List<String> readHeader( BufferedReader dis, int skipColumns ) throws IOException {
        List<String> headerVec = new Vector<String>();
        String header = null;

        /*
         * Read past comments.
         */
        while ( ( header = dis.readLine() ) != null ) {
            if ( header.startsWith( "#" ) || header.startsWith( "!" ) || StringUtils.isBlank( header ) ) {
                continue;
            }
            break;
        }

        if ( header == null ) return headerVec;

        if ( header.startsWith( "\t" ) ) header = "c" + header;

        String[] tokens = StringUtils.splitPreserveAllTokens( header, "\t" );
        // delims.

        String previousToken = "";
        int columnNumber = 0;

        for ( int i = 0; i < tokens.length; i++ ) {
            String s = StringUtils.strip( tokens[i], " " );
            boolean missing = false;

            if ( s.compareTo( "\t" ) == 0 ) {

                if ( previousToken.compareTo( "\t" ) == 0 ) { /* two tabs in a row */
                    missing = true;
                } else if ( i == tokens.length - 1 ) { // at end of line.
                    missing = true;
                } else {
                    previousToken = s;
                    continue;
                }
            } else if ( StringUtils.isBlank( s ) ) {
                missing = true;
            }

            if ( missing ) {
                throw new IOException( "Missing values are not allowed in the header (column " + columnNumber + " at '"
                        + header + "')" );
            }
            if ( columnNumber > 0 ) {

                if ( skipColumns > 0 && columnNumber <= skipColumns ) {

                    // ignore, but count it.
                } else {
                    headerVec.add( s );
                }
            } else {
                // corner string.
            }
            columnNumber++;

            previousToken = s;
        }

        return headerVec;

    }
}
