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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;

/**
 * Because agilent makes slides that work with any scanner, the formats are not that predictable. I've seen these so
 * far:
 * <ul>
 * <li>GPR format (GenePixResults ?) version 3: has DateType at the top formatted with quotes:
 * "DateTime=2005/11/09 11:36:27". Example GSE15739
 * <p>
 * For more information see http://mdc.custhelp.com/app/answers/detail/a_id/18886. GPR files are ATF aka Axon text
 * format.
 * <li>Agilent scanner files. These start with "TYPE" Example: GSE14466. The second line is "FEPARAMS", the fourth
 * column is "Scan_date".
 * </ul>
 * 
 * @author paul
 * @version $Id$
 */
public class AgilentScanDateExtractor implements ScanDateExtractor {

    private static Log log = LogFactory.getLog( AgilentScanDateExtractor.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.batcheffects.ScanDateExtractor#extract(java.io.InputStream)
     */
    @Override
    public Date extract( InputStream is ) {
        try {
            /*
             * Read the first three characters. IF they are ATF, it's a Axon file. If it's TYPE then it's probably an
             * agilent file.
             */
            BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );

            String line = reader.readLine();

            if ( line.startsWith( "ATF" ) ) {
                // GPR/ATF file. Read a few lines to find the datetime (the header tells us how long the header is, but
                // this is probably okay)
                Date d = null;
                while ( ( line = reader.readLine() ) != null ) {

                    if ( line.startsWith( "\"DateTime" ) ) {
                        String dateString = line.replaceAll( "\"", "" ).replaceFirst( "DateTime=", "" );
                        DateFormat f = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" ); // 2005/11/09 11:36:27
                        f.setLenient( true );
                        d = f.parse( dateString );
                        break;
                    }
                }

                if ( d == null ) {
                    throw new IllegalStateException( "Failed to find the 'DateTime' line" );
                }

                return d;

            } else if ( line.startsWith( "TYPE" ) ) {
                line = reader.readLine();
                if ( line.startsWith( "FEPARAMS" ) ) {
                    int dateField = -1;

                    // Agilent.
                    String[] fields = StringUtils.split( line, '\t' );
                    for ( int i = 0; i < fields.length; i++ ) {
                        if ( fields[i].equalsIgnoreCase( "Scan_date" ) ) {
                            dateField = i;
                        }
                    }

                    if ( dateField < 0 ) {
                        throw new IllegalStateException( "Could not recognize the scan_date field" );
                    }

                    line = reader.readLine();

                    if ( !line.startsWith( "DATA" ) ) {
                        throw new IllegalStateException( "Could not understand Agilent scanner format" );
                    }

                    fields = StringUtils.split( line, '\t' );
                    String date = fields[dateField];

                    DateFormat f = new SimpleDateFormat( "MM-dd-yyyy hh:mm:ss" ); // 10-18-2005 13:02:36
                    f.setLenient( true );
                    Date d = f.parse( date );
                    is.close();
                    return d;
                }
            } else {
                throw new UnsupportedOperationException( "Cannot recognize this format" );
            }

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.batcheffects.ScanDateExtractor#extract(java.lang.String)
     */
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
