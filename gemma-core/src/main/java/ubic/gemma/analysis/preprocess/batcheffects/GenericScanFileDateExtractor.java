/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ubic.basecode.util.FileTools;

/**
 * Looks through text file looking for a date near the top of the file in a reasonable format. Warning: don't use this
 * if the file format is actually known.
 * 
 * @author paul
 * @version $Id$
 */
public class GenericScanFileDateExtractor extends BaseScanDateExtractor {

    /**
     * How many lines to read before we give up.
     */
    protected static final int MAX_HEADER_LINES = 100;

    @Override
    public Date extract( InputStream is ) throws IOException {
        Date date = null;
        try (@SuppressWarnings("resource")
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) )) {
            String line = null;
            int count = 0;
            while ( ( line = reader.readLine() ) != null ) {

                if ( line.matches( GENEPIX_DATETIME_HEADER_REGEXP ) ) {
                    date = parseGenePixDateTime( line );
                }
                if ( date == null ) {
                    Pattern regex = Pattern.compile( ".+?([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}).+" );

                    Matcher matcher = regex.matcher( line );
                    if ( matcher.matches() ) {
                        String tok = matcher.group( 1 );

                        date = parseISO8601( tok );
                    }
                }
                if ( date == null ) date = parseStandardFormat( line );

                if ( date == null ) date = parseLongFormat( line );

                if ( date != null || ++count > MAX_HEADER_LINES ) {

                    if ( date != null && date.after( new Date() ) ) {
                        throw new RuntimeException( "Did not get a valid date (Line was:" + line
                                + ", extracted date was " + date + ")" );
                    }

                    break;
                }
            }
        }

        return date;
    }

    @Override
    public Date extract( String fileName ) {
        try {
            return extract( FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

}
