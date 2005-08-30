/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.util;

import java.io.IOException;
import java.security.MessageDigest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * String Utility Class This is used to encode passwords programmatically
 * <p>
 * <a h ref="StringUtil.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 */
public class StringUtil {
    // ~ Static fields/initializers =============================================

    private final static Log log = LogFactory.getLog( StringUtil.class );

    // ~ Methods ================================================================

    /**
     * Encode a string using algorithm specified in web.xml and return the resulting encrypted password. If exception,
     * the plain credentials string is returned
     * 
     * @param password Password or other credentials to use in authenticating this username
     * @param algorithm Algorithm used to do the digest
     * @return encypted password based on the algorithm.
     */
    public static String encodePassword( String password, String algorithm ) {
        byte[] unencodedPassword = password.getBytes();

        MessageDigest md = null;

        try {
            // first create an instance, given the provider
            md = MessageDigest.getInstance( algorithm );
        } catch ( Exception e ) {
            log.error( "Exception: " + e );

            return password;
        }

        md.reset();

        // call the update method one or more times
        // (useful when you don't know the size of your data, eg. stream)
        md.update( unencodedPassword );

        // now calculate the hash
        byte[] encodedPassword = md.digest();

        StringBuffer buf = new StringBuffer();

        for ( int i = 0; i < encodedPassword.length; i++ ) {
            if ( ( encodedPassword[i] & 0xff ) < 0x10 ) {
                buf.append( "0" );
            }

            buf.append( Long.toString( encodedPassword[i] & 0xff, 16 ) );
        }

        return buf.toString();
    }

    /**
     * Encode a string using Base64 encoding. Used when storing passwords as cookies. This is weak encoding in that
     * anyone can use the decodeString routine to reverse the encoding.
     * 
     * @param str
     * @return String
     */
    public static String encodeString( String str ) {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        return encoder.encodeBuffer( str.getBytes() ).trim();
    }

    /**
     * Decode a string using Base64 encoding.
     * 
     * @param str
     * @return String
     */
    public static String decodeString( String str ) {
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        try {
            return new String( dec.decodeBuffer( str ) );
        } catch ( IOException io ) {
            throw new RuntimeException( io.getMessage(), io.getCause() );
        }
    }

    /**
     * @param str
     * @return
     */
    public static int relaxedParseInt( String str ) {
        str = StringUtils.strip( str );
        return Integer.parseInt( str );
    }

}
