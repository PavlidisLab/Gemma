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
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;

/**
 * Extract the scan date from Affymetrix CEL files. Handles both version 3 (ASCII) and 4 (binary) files.
 * <p />
 * {@link http://www.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/cel.html} and {@link http
 * ://www.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/generic.html}
 * <p/>
 * Note that the Affymetrix documentation does not mention a date, explicitly, but it's in the "DatHeader"
 * 
 * @author paul
 * @version $Id$
 */
public class AffyScanDateExtractor extends BaseScanDateExtractor {

    private static Log log = LogFactory.getLog( AffyScanDateExtractor.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.batcheffects.ScanDateExtractor#extract(java.io.InputStream)
     */
    public Date extract( InputStream is ) {

        DataInputStream str = new DataInputStream( is );
        BufferedReader reader = null;
        Date date = null;

        try {
            int magic = readByteLittleEndian( str );
            if ( magic == 64 ) {

                int version = readIntLittleEndian( str );

                if ( version != 4 ) {
                    // it's always supposed to be.
                    throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                }

                log.debug( readShortLittleEndian( str ) ); // numrows
                log.debug( readShortLittleEndian( str ) ); // numcols
                log.debug( readIntLittleEndian( str ) ); // numcells

                int headerLen = readShortLittleEndian( str );

                if ( headerLen == 0 ) {
                    // throw new IllegalStateException( "Zero header length read" );
                    headerLen = 800;
                }

                log.debug( headerLen );

                StringBuilder buf = new StringBuilder();

                for ( int i = 0; i < headerLen; i++ ) {
                    buf.append( new String( new byte[] { str.readByte() }, "US-ASCII" ) );
                }

                String[] headerLines = StringUtils.split( buf.toString(), "\n" );

                for ( String string : headerLines ) {
                    if ( string.startsWith( "DatHeader" ) ) {
                        date = parseStandardFormat( string );
                        break;
                    }
                }
            } else if ( magic == 59 ) {

                // Command Console format
                int version = readUnsignedByteLittleEndian( str );
                if ( version != 1 ) {
                    throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                }
                log.debug( readIntLittleEndian( str ) ); // number of data groups
                log.debug( readIntLittleEndian( str ) ); // file position of first group
                String datatypeIdentifier = readGCOSString( str );

                log.debug( datatypeIdentifier );

                String guid = readGCOSString( str );

                log.debug( guid );

                reader = new BufferedReader( new InputStreamReader( is, "UTF-16BE" ) );
                String line = null;
                int count = 0;
                while ( ( line = reader.readLine() ) != null ) {
                    log.debug( line );
                    if ( line.contains( "affymetrix-scan-date" ) ) {
                        date = parseISO8601( line );
                    }
                    if ( date != null || ++count > 100 ) {
                        reader.close();
                        break;
                    }
                }

                log.debug( date );

            } else {

                /*
                 * assume version 3 plain text.
                 */
                reader = new BufferedReader( new InputStreamReader( is ) );
                String line = null;
                int count = 0;
                while ( ( line = reader.readLine() ) != null ) {
                    // log.info( line );
                    if ( line.startsWith( "DatHeader" ) ) {
                        date = parseStandardFormat( line );
                    }
                    if ( date != null || ++count > 100 ) {
                        reader.close();
                        break;
                    }
                }
            }

            if ( date == null ) {
                throw new IllegalStateException( "Failed to find date" );
            }
            log.debug( date );

            return date;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } finally {
            try {
                str.close();
                if ( reader != null ) {
                    reader.close();
                }
            } catch ( IOException e ) {
                log.error( "Failed to close open file handle: " + e.getMessage() );
            }
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

    private String readGCOSString( DataInputStream str ) throws IOException {
        int fieldLength = readIntLittleEndian( str );
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < fieldLength; i++ ) {
            if ( str.available() == 0 ) throw new IOException( "Reached end of file without string end" );
            buf.append( new String( new byte[] { str.readByte() }, "US-ASCII" ) );
        }
        String field = buf.toString();
        return field;
    }

    /**
     * @param dis
     * @return
     * @throws IOException
     */
    private int readIntLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readInt();
    }

    private int readByteLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readByte();
    }

    private int readUnsignedByteLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readUnsignedByte();
    }

    private int readShortLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readShort();
    }

}
