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
package ubic.gemma.web.util.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import ubic.basecode.util.FileTools;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * Utility methods for uploading files.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class FileUploadUtil {

    private static Log log = LogFactory.getLog( FileUploadUtil.class.getName() );
    private static final int BUF_SIZE = 32768;

    /**
     * @param request
     * @param fileUpload
     * @param key
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static File copyUploadedFile( HttpServletRequest request, FileUpload fileUpload, String key )
            throws IOException, FileNotFoundException {
        MultipartHttpServletRequest multipartRequest = ( MultipartHttpServletRequest ) request;
        CommonsMultipartFile file = ( CommonsMultipartFile ) multipartRequest.getFile( key );

        if ( file == null ) {
            throw new IllegalArgumentException( "File with key " + key + " was not in the request" );
        }

        File copiedFile = getLocalUploadLocation( request, fileUpload, file );

        copyFile( file, copiedFile );

        if ( !copiedFile.canRead() || copiedFile.length() == 0 ) {
            throw new IllegalArgumentException( "Uploaded file is not readable or of size zero" );
        }

        String link = getContextUploadPath( request ); // FIXME - this will not yield a valid url.
        request.setAttribute( "link", link + file.getOriginalFilename() );

        // place the data into the request for retrieval on next page
        request.setAttribute( "friendlyName", fileUpload.getName() );
        request.setAttribute( "fileName", file.getOriginalFilename() );
        request.setAttribute( "contentType", file.getContentType() );
        request.setAttribute( "size", file.getSize() + " bytes" );
        request.setAttribute( "location", fileUpload.getLocalPath() );
        return copiedFile;
    }

    /**
     * @param request
     * @param fileUpload
     * @param file
     * @return
     */
    private static File getLocalUploadLocation( HttpServletRequest request, FileUpload fileUpload,
            CommonsMultipartFile file ) {
        // the directory to upload to - put it in a user-specific directory for now.
        String uploadDir = getUploadPath( request );

        // Create the directory if it doesn't exist
        File uploadDirFile = FileTools.createDir( uploadDir );

        File copiedFile = new File( uploadDirFile.getAbsolutePath()
                + File.separatorChar
                + ( request == null || request.getSession() == null ? RandomStringUtils.randomAlphanumeric( 20 )
                        : request.getSession().getId() ) + "__" + file.getOriginalFilename() );

        fileUpload.setLocalPath( copiedFile );
        return copiedFile;
    }

    public static File copyUploadedInputStream( InputStream is ) throws IOException, FileNotFoundException {
        // Create the directory if it doesn't exist
        String uploadDir = ConfigUtils.getDownloadPath() + "userUploads";
        File uploadDirFile = FileTools.createDir( uploadDir );

        File copiedFile = new File( uploadDirFile.getAbsolutePath() + File.separatorChar
                + RandomStringUtils.randomAlphanumeric( 50 ) );
        copy( copiedFile, is );
        return copiedFile;
    }

    /**
     * @param file
     * @param copiedFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void copyFile( CommonsMultipartFile file, File copiedFile ) throws FileNotFoundException,
            IOException {
        log.info( "Copying file " + file + " (" + file.getSize() + " bytes)" );
        // write the file to the file specified
        InputStream stream = file.getInputStream();

        copy( copiedFile, stream );
        log.info( "Done copying to " + copiedFile );
    }

    /**
     * @param copiedFile
     * @param stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void copy( File copiedFile, InputStream stream ) throws FileNotFoundException, IOException {
        OutputStream bos = new FileOutputStream( copiedFile );
        int bytesRead = 0;
        byte[] buffer = new byte[BUF_SIZE];
        while ( ( bytesRead = stream.read( buffer, 0, BUF_SIZE ) ) != -1 ) {
            bos.write( buffer, 0, bytesRead );
        }

        bos.close();
        stream.close();
    }

    /**
     * @param request
     * @return
     */
    @SuppressWarnings("unused")
    public static String getUploadPath( HttpServletRequest request ) {
        return ConfigUtils.getDownloadPath() + "userUploads";
    }

    /**
     * @param request
     * @return
     */
    public static String getContextUploadPath( HttpServletRequest request ) {
        return getUploadPath( request ).replace( File.separatorChar, '/' );
    }

}
