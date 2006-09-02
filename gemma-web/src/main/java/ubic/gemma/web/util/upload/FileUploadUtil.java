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

    private static final int BUF_SIZE = 8192;

    /**
     * @param request
     * @param fileUpload
     * @param key
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static CommonsMultipartFile uploadFile( HttpServletRequest request, FileUpload fileUpload, String key )
            throws IOException, FileNotFoundException {
        MultipartHttpServletRequest multipartRequest = ( MultipartHttpServletRequest ) request;
        CommonsMultipartFile file = ( CommonsMultipartFile ) multipartRequest.getFile( key );

        if ( file == null ) {
            throw new IllegalArgumentException( "File with key " + key + " was not in the request" );
        }

        // the directory to upload to - put it in a user-specific directory for now.
        String uploadDir = getUploadPath( request );

        // Create the directory if it doesn't exist
        File dirPath = FileTools.createDir( uploadDir );

        // retrieve the file data
        InputStream stream = file.getInputStream();

        // write the file to the file specified
        OutputStream bos = new FileOutputStream( uploadDir + file.getOriginalFilename() );
        int bytesRead = 0;
        byte[] buffer = new byte[BUF_SIZE];

        while ( ( bytesRead = stream.read( buffer, 0, 8192 ) ) != -1 ) {
            bos.write( buffer, 0, bytesRead );
        }

        bos.close();

        // close the stream
        stream.close();

        String link = getContextUploadPath( request ); // FIXME - this will not yield a valid url.
        request.setAttribute( "link", link + file.getOriginalFilename() );

        // place the data into the request for retrieval on next page
        request.setAttribute( "friendlyName", fileUpload.getName() );
        request.setAttribute( "fileName", file.getOriginalFilename() );
        request.setAttribute( "contentType", file.getContentType() );
        request.setAttribute( "size", file.getSize() + " bytes" );
        request.setAttribute( "location", dirPath.getAbsolutePath() + File.separatorChar + file.getOriginalFilename() );
        return file;
    }

    /**
     * @param request
     * @return
     */
    public static String getUploadPath( HttpServletRequest request ) {
        return ConfigUtils.getDownloadPath() + request.getRemoteUser();
    }

    /**
     * @param request
     * @return
     */
    public static String getContextUploadPath( HttpServletRequest request ) {
        return getUploadPath( request ).replace( File.separatorChar, '/' );
    }

}
