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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adaptation of the standard Spring CommonsMultipartResolver that uses a MonitoredOutputStream. This allows
 * asynchronous client-side monitoring of the upload process.
 *
 * @author pavlidis
 */
public class CommonsMultipartMonitoredResolver implements MultipartResolver, ServletContextAware {

    private final Log logger = LogFactory.getLog( getClass() );
    private File uploadTempDir;
    /*
     * This is set in gemma-servlet.xml where this bean is configured.
     */
    private long sizeMax = 4194304L;

    @Override
    public void cleanupMultipart( MultipartHttpServletRequest request ) {

        if ( request instanceof FailedMultipartHttpServletRequest )
            return;

        Map<String, MultipartFile> multipartFiles = request.getFileMap();
        for ( MultipartFile multipartFile : multipartFiles.values() ) {
            CommonsMultipartFile file = ( CommonsMultipartFile ) multipartFile;
            if ( logger.isDebugEnabled() ) {
                logger.debug( "Cleaning up multipart file [" + file.getName() + "] with original filename [" + file
                        .getOriginalFilename() + "], stored " + file.getStorageDescription() );
            }
            file.getFileItem().delete();
        }
    }

    @Override
    public boolean isMultipart( HttpServletRequest request ) {
        return ServletFileUpload.isMultipartContent( request );
    }

    @Override
    public MultipartHttpServletRequest resolveMultipart( HttpServletRequest request ) throws MultipartException {
        String enc = determineEncoding( request );

        ServletFileUpload fileUpload = this.newFileUpload( request );
        DiskFileItemFactory newFactory = ( DiskFileItemFactory ) fileUpload.getFileItemFactory();
        fileUpload.setSizeMax( sizeMax );
        newFactory.setRepository( this.uploadTempDir );
        fileUpload.setHeaderEncoding( enc );

        try {
            MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();
            Map<String, String[]> multipartParams = new HashMap<>();

            // Extract multipart files and multipart parameters.
            List<?> fileItems = fileUpload.parseRequest( request );
            for ( Object fileItem1 : fileItems ) {
                FileItem fileItem = ( FileItem ) fileItem1;
                if ( fileItem.isFormField() ) {
                    String value;
                    String fieldName = fileItem.getFieldName();

                    try {
                        value = fileItem.getString( enc );
                    } catch ( UnsupportedEncodingException ex ) {
                        logger.warn( "Could not decode multipart item '" + fieldName + "' with encoding '" + enc
                                + "': using platform default" );
                        value = fileItem.getString();
                    }

                    String[] curParam = multipartParams.get( fieldName );
                    if ( curParam == null ) {
                        // simple form field
                        multipartParams.put( fieldName, new String[] { value } );
                    } else {
                        // array of simple form fields
                        String[] newParam = StringUtils.addStringToArray( curParam, value );
                        multipartParams.put( fieldName, newParam );
                    }
                } else {
                    // multipart file field
                    MultipartFile file = new CommonsMultipartFile( fileItem );
                    multipartFiles.set( file.getName(), file );
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Found multipart file [" + file.getName() + "] of size " + file.getSize()
                                + " bytes with original filename [" + file.getOriginalFilename() + "]" );
                    }
                }
            }
            return new DefaultMultipartHttpServletRequest( request, multipartFiles, multipartParams, null );
        } catch ( FileUploadException ex ) {
            return new FailedMultipartHttpServletRequest( request, ex.getMessage() );
        }
    }

    /**
     * Set the maximum allowed size (in bytes) before uploads are refused. -1 indicates no limit (the default).
     *
     * @param maxUploadSize the maximum upload size allowed
     * @see org.apache.commons.fileupload.FileUploadBase#setSizeMax
     */
    public void setMaxUploadSize( long maxUploadSize ) {
        this.sizeMax = maxUploadSize;
    }

    @Override
    public void setServletContext( ServletContext servletContext ) {
        if ( this.uploadTempDir == null ) {
            this.uploadTempDir = WebUtils.getTempDir( servletContext );
        }
    }

    /**
     * Determine the encoding for the given request. Can be overridden in subclasses.
     * The default implementation checks the request encoding, falling back to the default encoding specified for this
     * resolver.
     *
     * @param request current HTTP request
     * @return the encoding for the request (never <code>null</code>)
     * @see javax.servlet.ServletRequest#getCharacterEncoding
     */
    protected String determineEncoding( HttpServletRequest request ) {
        String enc = request.getCharacterEncoding();
        if ( enc == null ) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        return enc;
    }

    /**
     * Create a factory for disk-based file items with a listener we can check for progress.
     *
     * @param request request
     * @return the new FileUpload instance
     */
    protected ServletFileUpload newFileUpload( HttpServletRequest request ) {

        UploadListener listener = new UploadListener( request );
        DiskFileItemFactory factory = new MonitoredDiskFileItemFactory( listener );
        ServletFileUpload upload = new ServletFileUpload( factory );
        factory.setRepository( uploadTempDir );
        upload.setSizeMax( sizeMax );
        return upload;
    }

}
