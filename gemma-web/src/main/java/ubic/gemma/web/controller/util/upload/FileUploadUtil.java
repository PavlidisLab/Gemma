/*
 * The Gemma project
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
package ubic.gemma.web.controller.util.upload;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for uploading files.
 *
 * @author pavlidis
 */
public class FileUploadUtil {

    private static final Log log = LogFactory.getLog( FileUploadUtil.class.getName() );

    /**
     * Obtain the path for a previously uploaded file.
     */
    public static Path getUploadedFile( String filename, Path uploadDir ) {
        filename = FilenameUtils.getName( filename );
        if ( filename.equals( "." ) || filename.equals( ".." ) ) {
            throw new IllegalArgumentException( "A valid filename must be provided." );
        }
        return uploadDir.resolve( filename );
    }

    public static Path copyUploadedFile( HttpServletRequest request, String key, Path uploadDir ) throws IOException {
        MultipartHttpServletRequest multipartRequest = ( MultipartHttpServletRequest ) request;
        MultipartFile file = multipartRequest.getFile( key );

        if ( file == null ) {
            throw new IllegalArgumentException( "File with key " + key + " was not in the request" );
        }
        return copyUploadedFile( file, request, uploadDir );
    }

    public static Path copyUploadedFile( MultipartFile multipartFile, @Nullable HttpServletRequest request, Path uploadDir ) throws IOException {
        log.info( "Copying the file:" + multipartFile );
        Path copiedFile = getLocalUploadLocation( request, multipartFile, uploadDir );

        log.info( "Copying file " + multipartFile + " (" + multipartFile.getSize() + " bytes)" );
        // write the file to the file specified
        multipartFile.transferTo( copiedFile.toFile() );
        log.info( "Done copying to " + copiedFile );

        if ( !Files.isReadable( copiedFile ) || Files.size( copiedFile ) == 0 ) {
            throw new IllegalArgumentException( "Uploaded file is not readable or of size zero" );
        }

        if ( request != null ) {
            String link = uploadDir.toString(); // FIXME - this will not yield a valid url.
            request.setAttribute( "link", link + multipartFile.getOriginalFilename() );

            // place the data into the request for retrieval on next page
            request.setAttribute( "fileName", multipartFile.getOriginalFilename() );
            request.setAttribute( "contentType", multipartFile.getContentType() );
            request.setAttribute( "size", multipartFile.getSize() + " bytes" );
            request.setAttribute( "location", copiedFile.toString() );
        }
        return copiedFile;

    }

    public static Path copyUploadedInputStream( InputStream is, Path uploadDir ) throws IOException {
        // Create the directory if it doesn't exist
        try {
            Files.createDirectories( uploadDir );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        Path copiedFile = uploadDir.resolve( RandomStringUtils.randomAlphanumeric( 51 ) );
        try ( OutputStream fos = Files.newOutputStream( copiedFile ) ) {
            IOUtils.copy( is, fos );
        }
        return copiedFile;
    }

    private static Path getLocalUploadLocation( @Nullable HttpServletRequest request, MultipartFile file, Path uploadDir ) {
        // the directory to upload to - put it in a user-specific directory for now.

        // Create the directory if it doesn't exist
        try {
            Files.createDirectories( uploadDir );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        String filename;
        if ( file.getOriginalFilename() == null ) {
            filename = RandomStringUtils.randomAlphanumeric( 50 );
        } else {
            filename = FilenameUtils.getName( file.getOriginalFilename() );
        }

        return uploadDir.resolve(
                ( request == null || request.getSession() == null ?
                        RandomStringUtils.randomAlphanumeric( 20 ) :
                        request.getSession().getId() ) + "__" + filename );
    }
}
