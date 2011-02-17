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
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;

/**
 * Extract the scan date from Affymetrix CEL files. Handles both version 3 (ASCII) and 4 (binary) files.
 * <p />
 * {@link http://www.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/cel.html}
 * <p/>
 * Note that the Affymetrix documentation does not mention a date, explicitly, but it's in the "DatHeader"
 * 
 * @author paul
 * @version $Id$
 */
public class AffyScanDateExtractor implements ScanDateExtractor {

    private static Log log = LogFactory.getLog( AffyScanDateExtractor.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.batcheffects.ScanDateExtractor#extract(java.io.InputStream)
     */
    public Date extract( InputStream is ) {

        DataInputStream str = new DataInputStream( is );

        Date date = null;

        try {
            int magic = readIntLittleEndian( str );
            if ( magic == 64 ) {

                int version = readIntLittleEndian( str );

                if ( version != 4 ) {
                    // it's always supposed to be.
                    throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                }

                log.debug( readIntLittleEndian( str ) ); // numrows
                log.debug( readIntLittleEndian( str ) ); // numcols
                log.debug( readIntLittleEndian( str ) ); // numcells

                int headerLen = readIntLittleEndian( str );

                if ( headerLen == 0 ) {
                    throw new IllegalStateException( "Zero header length read" );
                }

                log.debug( headerLen );

                StringBuilder buf = new StringBuilder();

                for ( int i = 0; i < headerLen; i++ ) {
                    buf.append( ( char ) str.readByte() );
                }

                String[] headerLines = StringUtils.split( buf.toString(), "\n" );

                for ( String string : headerLines ) {
                    if ( string.startsWith( "DatHeader" ) ) {
                        date = parseDatHeader( string );
                    }
                }
            } else {

                /*
                 * Must not be a version 4 file, assume version 3 plain text.
                 */
                BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
                String line = null;
                int count = 0;
                while ( ( line = reader.readLine() ) != null ) {
                    // log.info( line );
                    if ( line.startsWith( "DatHeader" ) ) {
                        date = parseDatHeader( line );
                    }
                    if ( ++count > 100 ) {
                        // give up.
                        reader.close();
                        break;
                    }
                }
                reader.close();
            }

            if ( date == null ) {
                throw new IllegalStateException( "Failed to find date" );
            }
            log.debug( date );

            return date;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( ParseException e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.batcheffects.ScanDateExtractor#extract(java.lang.String)
     */
    public Date extract( String fileName ) {
        try {
            return extract( FileTools.getInputStreamFromPlainOrCompressedFile( fileName ) );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Parse the "DatHeader" line from a CEL file and extract the date found there.
     * 
     * @param string
     * @return
     * @throws ParseException
     */
    private Date parseDatHeader( String string ) throws ParseException {

        DateFormat f = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" );
        f.setLenient( true );

        Pattern regex = Pattern.compile( ".+?([0-9]{2}\\/[0-9]{2}\\/[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}).+" );

        Matcher matcher = regex.matcher( string );
        if ( matcher.matches() ) {
            String tok = matcher.group( 1 );
            log.debug( tok );
            return f.parse( tok );
        }

        return null;
    }

    /**
     * @param dis
     * @return
     * @throws IOException
     */
    private int readIntLittleEndian( DataInputStream dis ) throws IOException {
        return Integer.reverseBytes( dis.readInt() );
    }
}
