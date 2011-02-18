/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.analysis.preprocess.batcheffects;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author paul
 * @version $Id$
 */
public abstract class BaseScanDateExtractor implements ScanDateExtractor {

    // TODO set up regexes statically.

    protected static final String GENEPIX_DATETIME_HEADER_REGEXP = "\"?DateTime=.*";

    /**
     * @param string
     * @return
     */
    protected Date parseISO8601( String string ) {

        // 2008-08-15T14:15:36
        try {
            DateFormat f = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );

            Pattern regex = Pattern.compile( ".+?([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}).+" );

            Matcher matcher = regex.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                return f.parse( tok );
            }

            return null;
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * E.g. "Mon Jun 17 21:26:34 CST 2002", but line has to have Date at start (possibly white-space padded) Shows up in
     * Imagene files.
     * 
     * @param string
     * @return
     */
    protected Date parseLongFormat( String string ) {
        // 
        try {
            DateFormat f = new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy" );

            Pattern regex = Pattern.compile( "\\s*Date\\s*(.+)" );

            Matcher matcher = regex.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                return f.parse( tok );
            }

            return null;
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Parse a common format, "MM[/-]dd[/-]yy hh:mm:ss", found for example in the "DatHeader" line from a CEL file and
     * extract the date found there.
     * 
     * @param string
     * @return
     * @throws ParseException
     */
    protected Date parseStandardFormat( String string ) {

        try {
            DateFormat f = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" );

            Pattern regex = Pattern
                    .compile( ".+?([0-9]{2}[\\/-][0-9]{2}[\\/-][0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}).+" );

            Matcher matcher = regex.matcher( string );
            if ( matcher.matches() ) {
                String tok = matcher.group( 1 );
                return f.parse( tok );
            }
            return null;
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * This method should be generic for GenePix/GPR/ATR file formats. Has DateType at the top formatted with quotes:
     * "DateTime=2005/11/09 11:36:27". Example GSE15739
     * <p>
     * For more information see {@link http ://mdc.custhelp.com/app/answers/detail/a_id/18886}.
     * <p>
     * 
     * @param reader
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected Date extractGenePix( BufferedReader reader ) throws IOException, ParseException {
        String line;
        // GPR/ATF file. Read a few lines to find the datetime (the header tells us how long the header is, but
        // this is probably okay)
        Date d = null;
        while ( ( line = reader.readLine() ) != null ) {

            if ( line.matches( GENEPIX_DATETIME_HEADER_REGEXP ) ) {
                d = parseGenePixDateTime( line );
                break;
            }
        }

        if ( d == null ) {
            throw new IllegalStateException( "Failed to find the 'DateTime' line" );
        }
        reader.close();
        return d;
    }

    /**
     * @param line like "DateTime=2005/11/09 11:36:27" (with the quotes) possibly with trailing whitespace.
     * @return
     */
    protected Date parseGenePixDateTime( String line ) {
        try {
            String dateString = line.trim().replaceAll( "\"", "" ).replaceFirst( "DateTime=", "" );
            DateFormat f = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss" ); // 2005/11/09 11:36:27, 2006/04/07 14:18:18
            return f.parse( dateString );
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }

}
