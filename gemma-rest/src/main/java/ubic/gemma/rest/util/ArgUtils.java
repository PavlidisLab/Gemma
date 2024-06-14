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
package ubic.gemma.rest.util;

import org.apache.commons.io.IOUtils;
import ubic.gemma.rest.util.args.Arg;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Utilities for working with {@link Arg}.
 *
 * @author poirigui
 */
public class ArgUtils {

    /**
     * A base64-encoded gzip header to detect compressed filters.
     */
    private static final String BASE64_ENCODED_GZIP_MAGIC = "H4s";

    /**
     * Decode a base64-encoded gzip-compressed argument.
     * <p>
     * This intended to be used in the {@code valueOf} methods of subclasses.
     */
    public static String decodeCompressedArg( @Nullable String s ) {
        byte[] decodedS;
        if ( s != null && s.startsWith( BASE64_ENCODED_GZIP_MAGIC ) && ( decodedS = tryDecodeBase64( s ) ) != null ) {
            try {
                return IOUtils.toString( new GZIPInputStream( new ByteArrayInputStream( decodedS ) ), StandardCharsets.UTF_8 );
            } catch ( IOException e ) {
                throw new MalformedArgException( "Invalid base64-encoded filter, make sure that your filter is first gzipped and then base64-encoded.", e );
            }
        } else {
            return s;
        }
    }

    private static byte[] tryDecodeBase64( String s ) {
        try {
            return Base64.getDecoder().decode( s );
        } catch ( IllegalArgumentException e ) {
            // invalid base-64 encoded buffer, this might be a regular string
            return null;
        }
    }
}
