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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
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
    @Override
    public Date extract( InputStream is ) {

        try (DataInputStream str = new DataInputStream( is )) {

            BufferedReader reader = null;
            Date date = null;

            int magic = readByte( str );
            if ( magic == 64 ) {

                int version = readIntLittleEndian( str );

                if ( version != 4 ) {
                    // it's always supposed to be.
                    throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                }

                log.debug( readShort( str ) ); // numrows
                log.debug( readShort( str ) ); // numcols
                log.debug( readIntLittleEndian( str ) ); // numcells

                int headerLen = readShort( str );

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
                int version = readUnsignedByte( str );
                if ( version != 1 ) {
                    // this is fixed to 1 according to affy docs.
                    throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                }
                @SuppressWarnings("unused")
                int numDataGroups = readIntBigEndian( str ); // number of data groups, usually = 1. Each data group
                                                             // contains another header, with different name/value/type
                                                             // triples.
                @SuppressWarnings("unused")
                int filePosOfFirstGroup = readIntBigEndian( str ); // file position of first data group.

                date = parseGenericCCHeader( str );

                // need to find number of parent file headers and then read array of generic file headers

                if ( date == null ) {
                    /*
                     * Look in the parentheader
                     */

                    date = parseGenericCCHeader( str );
                }

                // $dataHeader$parents[[1]]$parameters$`affymetrix-scan-date`

                // also $dataHeader$parents[[1]]$parameters$`affymetrix-Fluidics-HybDate`

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
                    if ( date != null || ++count > 100 ) { // don't read too much.
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
        }

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

    /**
     * @param str
     * @param date
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public Date parseGenericCCHeader( DataInputStream str ) throws IOException, UnsupportedEncodingException {
        /*
         * FIXME store values in an object. All I need right now is the date, though.
         */

        /*
         * acquisition data, intensity data etc. Usually "intensity data" for the first header.
         */
        String datatypeIdentifier = readString( str );

        log.debug( datatypeIdentifier );

        String guid = readString( str );

        log.debug( guid );

        @SuppressWarnings("unused")
        String createDate = readUnicodeString( str ); // blank?
        @SuppressWarnings("unused")
        String locale = readUnicodeString( str ); // e.g. en-US
        int numKeyValuePairs = readIntBigEndian( str ); // e.g. 55
        Date result = null;
        for ( int i = 0; i < numKeyValuePairs; i++ ) {
            String name = readUnicodeString( str );
            byte[] value = readBytes( str );
            String type = readUnicodeString( str );

            Object v = null;

            // log.info( ">>>>" + type + "<<<<<" );

            if ( type.equals( "text/x-calvin-float" ) ) {
                FloatBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asFloatBuffer();
                float[] array = new float[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );
            } else if ( type.equals( "text/plain" ) || type.equals( "text/ascii" ) ) {
                // text/ascii is undocumented, but needed.
                v = new String( value, "US-ASCII" );
                // System.err.println( name + " " + v + " " + type );

                if ( name.equals( "affymetrix-scan-date" ) ) {
                    result = parseISO8601( new String( ( ( String ) v ).getBytes(), "UTF-16" ) );
                    log.info( "Scan date = " + v );
                }

                if ( name.equals( "affymetrix-Hyb-Start-Time" ) ) {
                    // We don't use this but I'm curious to start looking at it.
                    log.info( "Hyb start date = " + v );
                }

            } else if ( type.equals( "text-x-calvin-unsigned-integer-8" ) ) {
                ShortBuffer intBuf = ByteBuffer.wrap( value ).asShortBuffer();
                short[] array = new short[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );

            } else if ( type.equals( "text/x-calvin-integer-16" ) ) {
                IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer(); // wrong?
                int[] array = new int[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );
            } else if ( type.equals( "text/x-calvin-integer-32" ) ) {
                IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();
                int[] array = new int[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );

            } else if ( type.equals( "text/x-calvin-unsigned-integer-8" ) ) {
                ShortBuffer intBuf = ByteBuffer.wrap( value ).asShortBuffer();
                short[] array = new short[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );

            } else if ( type.equals( "text/x-calvin-unsigned-integer-16" ) ) {
                IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();// wrong?
                int[] array = new int[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " " + array[0] + " " + type );

            } else if ( type.equals( "text/x-calvin-unsigned-integer-32" ) ) {
                IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();
                int[] array = new int[intBuf.remaining()];
                intBuf.get( array );
                v = array;
                // System.err.println( name + " = " + array[0] + " " + type );

            } else {
                throw new IOException( "Unknown mime type:" + type );
            }

        }

        @SuppressWarnings("unused")
        int numParentHeaders = this.readIntBigEndian( str );
        // log.info( numParentHeaders + " parent headers" );

        return result;

    }

    /**
     * 8 bit signed integral number
     * 
     * @param dis
     * @return
     * @throws IOException
     */
    private int readByte( DataInputStream dis ) throws IOException {
        return dis.readByte();
    }

    /**
     * The data is stored as a int, then the array of bytes.
     * 
     * @param str
     * @return
     * @throws IOException
     */
    private byte[] readBytes( DataInputStream str ) throws IOException {
        int fieldLength = readIntBigEndian( str );

        byte[] result = new byte[fieldLength];
        for ( int i = 0; i < fieldLength; i++ ) {
            if ( str.available() == 0 ) throw new IOException( "Reached end of file without string end" );
            result[i] = str.readByte();
        }

        return result;
    }

    private int readIntBigEndian( DataInputStream dis ) throws IOException {
        byte[] buf = new byte[4];

        for ( int i = 0; i < buf.length; i++ ) {
            buf[i] = dis.readByte();
        }

        return ByteBuffer.wrap( buf ).order( ByteOrder.BIG_ENDIAN ).getInt();
    }

    /**
     * 32 bit signed integral number
     * 
     * @param dis
     * @return
     * @throws IOException
     */
    private int readIntLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readInt();
    }

    private int readShort( DataInputStream dis ) throws IOException {
        return dis.readShort();
    }

    /**
     * A 1 byte character string. A string object is stored as an INT (to store the string length) followed by the CHAR
     * array (to store the string contents).
     * 
     * @param str
     * @return
     * @throws IOException
     */
    private String readString( DataInputStream str ) throws IOException {
        int fieldLength = readIntBigEndian( str );
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < fieldLength; i++ ) {
            if ( str.available() == 0 ) throw new IOException( "Reached end of file without string end" );
            buf.append( new String( new byte[] { str.readByte() } ) );
        }
        String field = buf.toString();
        // log.info( "String length=" + fieldLength + " val=" + field );
        return field;
    }

    /**
     * A UNICODE string. A string object is stored as an INT (to store the string length) followed by the WCHAR array
     * (to store the string contents).
     * (http://media.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/generic.html)
     * 
     * @param str
     * @return
     * @throws IOException
     */
    private String readUnicodeString( DataInputStream str ) throws IOException {
        int fieldLength = readIntBigEndian( str );

        // log.info( fieldLength );

        byte[] buf = new byte[fieldLength * 2];

        // StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < fieldLength * 2; i++ ) {
            buf[i] = str.readByte();
        }

        String field = new String( buf, "UTF-16" );
        return field;
    }

    private int readUnsignedByte( DataInputStream dis ) throws IOException {
        return dis.readUnsignedByte();
    }

}
