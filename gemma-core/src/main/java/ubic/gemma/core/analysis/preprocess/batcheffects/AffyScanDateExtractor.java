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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.*;
import java.util.Date;

/**
 * Extract the scan date from Affymetrix CEL files. Handles both version 3 (ASCII) and 4 (binary) files.
 * <p>
 * See <a href='http://www.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/cel.html'>CEL</a> and
 * <a href='http://www.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/generic.html'>GENERIC</a>
 * </p>
 * Note that the Affymetrix documentation does not mention a date, explicitly, but it's in the "DatHeader"
 *
 * @author paul
 */
public class AffyScanDateExtractor extends BaseScanDateExtractor {

    private static final Log log = LogFactory.getLog( AffyScanDateExtractor.class );

    @Override
    public Date extract( InputStream is ) {

        try (DataInputStream str = new DataInputStream( is )) {

            BufferedReader reader;
            Date date = null;

            int magic = this.readByte( str );
            switch ( magic ) {
                case 64: {

                    int version = this.readIntLittleEndian( str );

                    if ( version != 4 ) {
                        // it's always supposed to be.
                        throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                    }

                    AffyScanDateExtractor.log.debug( this.readShort( str ) ); // numrows

                    AffyScanDateExtractor.log.debug( this.readShort( str ) ); // numcols

                    AffyScanDateExtractor.log.debug( this.readIntLittleEndian( str ) ); // numcells

                    int headerLen = this.readShort( str );

                    if ( headerLen == 0 ) {
                        // throw new IllegalStateException( "Zero header length read" );
                        headerLen = 800;
                    }

                    AffyScanDateExtractor.log.debug( headerLen );

                    StringBuilder buf = new StringBuilder();

                    for ( int i = 0; i < headerLen; i++ ) {
                        buf.append( new String( new byte[] { str.readByte() }, "US-ASCII" ) );
                    }

                    String[] headerLines = StringUtils.split( buf.toString(), "\n" );

                    for ( String string : headerLines ) {
                        if ( string.startsWith( "DatHeader" ) ) {
                            date = this.parseStandardFormat( string );
                            break;
                        }
                    }
                    break;
                }
                case 59: {

                    // Command Console format
                    int version = this.readUnsignedByte( str );
                    if ( version != 1 ) {
                        // this is fixed to 1 according to affy docs.
                        throw new IllegalStateException( "Affymetrix CEL format not recognized: " + version );
                    }
                    @SuppressWarnings("unused") int numDataGroups = this
                            .readIntBigEndian( str ); // number of data groups, usually = 1. Each data group

                    // contains another header, with different name/value/type
                    // triples.
                    @SuppressWarnings("unused") int filePosOfFirstGroup = this
                            .readIntBigEndian( str ); // file position of first data group.

                    date = this.parseGenericCCHeader( str );

                    // need to find number of parent file headers and then read array of generic file headers

                    if ( date == null ) {
                        /*
                         * Look in the parentheader
                         */

                        date = this.parseGenericCCHeader( str );
                    }

                    // $dataHeader$parents[[1]]$parameters$`affymetrix-scan-date`

                    // also $dataHeader$parents[[1]]$parameters$`affymetrix-Fluidics-HybDate`

                    break;
                }
                default:

                    /*
                     * assume version 3 plain text.
                     */
                    reader = new BufferedReader( new InputStreamReader( is ) );
                    String line;
                    int count = 0;
                    while ( ( line = reader.readLine() ) != null ) {
                        // log.info( line );
                        if ( line.startsWith( "DatHeader" ) ) {
                            date = this.parseStandardFormat( line );
                        }
                        if ( date != null || ++count > 100 ) { // don't read too much.
                            reader.close();
                            break;
                        }
                    }
                    break;
            }

            if ( date == null ) {
                throw new IllegalStateException( "Failed to find date" );
            }
            AffyScanDateExtractor.log.debug( date );

            return date;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public Date parseGenericCCHeader( DataInputStream str ) throws IOException {

        /*
         * acquisition data, intensity data etc. Usually "intensity data" for the first header.
         */
        String datatypeIdentifier = this.readString( str );

        AffyScanDateExtractor.log.debug( datatypeIdentifier );

        String guid = this.readString( str );

        AffyScanDateExtractor.log.debug( guid );

        @SuppressWarnings("unused") String createDate = this.readUnicodeString( str ); // blank?
        @SuppressWarnings("unused") String locale = this.readUnicodeString( str ); // e.g. en-US
        int numKeyValuePairs = this.readIntBigEndian( str ); // e.g. 55
        Date result = null;
        for ( int i = 0; i < numKeyValuePairs; i++ ) {
            String name = this.readUnicodeString( str );
            byte[] value = this.readBytes( str );
            String type = this.readUnicodeString( str );
            Object v;

            switch ( type ) {
                case "text/x-calvin-float": {
                    FloatBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asFloatBuffer();
                    float[] array = new float[intBuf.remaining()];
                    intBuf.get( array );
                    break;
                }
                case "text/plain":
                case "text/ascii":
                    // text/ascii is undocumented, but needed.
                    v = new String( value, "US-ASCII" );

                    if ( name.equals( "affymetrix-scan-date" ) ) {
                        result = this.parseISO8601( new String( ( ( String ) v ).getBytes(), "UTF-16" ) );
                        AffyScanDateExtractor.log.info( "Scan date = " + result );
                    }

                    if ( name.equals( "affymetrix-Hyb-Start-Time" ) ) {
                        // We don't use this but I'm curious to start looking at it.
                        AffyScanDateExtractor.log
                                .info( "Hyb start date = " + new String( ( ( String ) v ).getBytes(), "UTF-16" ) );
                    }

                    break;
                case "text-x-calvin-unsigned-integer-8": {
                    ShortBuffer intBuf = ByteBuffer.wrap( value ).asShortBuffer();
                    short[] array = new short[intBuf.remaining()];
                    intBuf.get( array );

                    break;
                }
                case "text/x-calvin-integer-16": {
                    IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer(); // wrong?

                    int[] array = new int[intBuf.remaining()];
                    intBuf.get( array );
                    break;
                }
                case "text/x-calvin-integer-32": {
                    IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();
                    int[] array = new int[intBuf.remaining()];
                    intBuf.get( array );

                    break;
                }
                case "text/x-calvin-unsigned-integer-8": {
                    ShortBuffer intBuf = ByteBuffer.wrap( value ).asShortBuffer();
                    short[] array = new short[intBuf.remaining()];
                    intBuf.get( array );

                    break;
                }
                case "text/x-calvin-unsigned-integer-16": {
                    IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();// wrong?

                    int[] array = new int[intBuf.remaining()];
                    intBuf.get( array );

                    break;
                }
                case "text/x-calvin-unsigned-integer-32": {
                    IntBuffer intBuf = ByteBuffer.wrap( value ).order( ByteOrder.BIG_ENDIAN ).asIntBuffer();
                    int[] array = new int[intBuf.remaining()];
                    intBuf.get( array );

                    break;
                }
                default:
                    throw new IOException( "Unknown mime type:" + type );
            }

        }

        @SuppressWarnings("unused") int numParentHeaders = this.readIntBigEndian( str );
        return result;
    }

    /**
     * @return 8 bit signed integral number
     */
    private int readByte( DataInputStream dis ) throws IOException {
        return dis.readByte();
    }

    /**
     * The data is stored as a int, then the array of bytes.
     */
    private byte[] readBytes( DataInputStream str ) throws IOException {
        int fieldLength = this.readIntBigEndian( str );

        byte[] result = new byte[fieldLength];
        for ( int i = 0; i < fieldLength; i++ ) {
            if ( str.available() == 0 )
                throw new IOException( "Reached end of file without string end" );
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
     * @return 32 bit signed integral number
     */
    private int readIntLittleEndian( DataInputStream dis ) throws IOException {
        return dis.readInt();
    }

    private int readShort( DataInputStream dis ) throws IOException {
        return dis.readShort();
    }

    /**
     * @return A 1 byte character string. A string object is stored as an INT (to store the string length) followed by the CHAR
     * array (to store the string contents).
     */
    private String readString( DataInputStream str ) throws IOException {
        int fieldLength = this.readIntBigEndian( str );
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < fieldLength; i++ ) {
            if ( str.available() == 0 )
                throw new IOException( "Reached end of file without string end" );
            buf.append( new String( new byte[] { str.readByte() } ) );
        }
        return buf.toString();
    }

    /**
     * @return A UNICODE string. A string object is stored as an INT (to store the string length) followed by the WCHAR array
     * (to store the string contents).
     * (http://media.affymetrix.com/support/developer/powertools/changelog/gcos-agcc/generic.html)
     */
    private String readUnicodeString( DataInputStream str ) throws IOException {
        int fieldLength = this.readIntBigEndian( str );

        // log.info( fieldLength );

        byte[] buf = new byte[fieldLength * 2];

        // StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < fieldLength * 2; i++ ) {
            buf[i] = str.readByte();
        }

        return new String( buf, "UTF-16" );
    }

    private int readUnsignedByte( DataInputStream dis ) throws IOException {
        return dis.readUnsignedByte();
    }

}
