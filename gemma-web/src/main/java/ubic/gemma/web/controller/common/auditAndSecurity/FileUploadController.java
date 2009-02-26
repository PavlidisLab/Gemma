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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.web.util.upload.FileUploadUtil;
import ubic.gemma.web.view.JSONView;

/**
 * Controller class to upload Files.
 * 
 * @author paul
 * @author keshav
 * @author Traces of Matt Raible
 * @version $Id$
 * @spring.bean id="fileUploadController"
 */
public class FileUploadController extends AbstractController {

    private static Log log = LogFactory.getLog( FileUploadController.class.getName() );

    /**
     * Ajax. DWR can handle this.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public String upload( InputStream is ) throws FileNotFoundException, IOException {
        File copiedFile = FileUploadUtil.copyUploadedInputStream( is );
        log.info( "Uploaded file!" );
        return copiedFile.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest ,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( !( request instanceof MultipartHttpServletRequest ) ) {
            return super.handleRequest( request, response );
        }

        MultipartHttpServletRequest mrequest = ( MultipartHttpServletRequest ) request;
        Map<String, MultipartFile> fileMap = mrequest.getFileMap();

        Map<String, Object> model = new HashMap<String, Object>();

        try {

            if ( fileMap.size() > 1 ) {
                log.error( "Attempted to upload multiple files, returning error" );
                model.put( "success", false );
                model.put( "error", "Sorry, can't upload more than one file at a time yet" );
            }

            for ( String key : fileMap.keySet() ) {
                MultipartFile multipartFile = fileMap.get( key );
                File copiedFile = null;
                try {
                    copiedFile = FileUploadUtil.copyUploadedFile( multipartFile, request );
                    log.info( "Uploaded file: " + copiedFile );
                    model.put( "success", true );
                    model.put( "localFile", StringEscapeUtils.escapeJava( copiedFile.getAbsolutePath() ) );
                    model.put( "originalFile", multipartFile.getOriginalFilename() );
                    model.put( "size", multipartFile.getSize() );

                } catch ( Exception e ) {
                    log.error( "Error in upload: " + e.getMessage() );
                    model.put( "success", false );
                    model.put( "error", e.getMessage() );
                }

                if ( copiedFile == null ) {
                    log.error( "Error in upload: unknown problem getting file" );
                    model.put( "success", false );
                    model.put( "error", "unknown problem getting file" );
                }

            }

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        
        return new ModelAndView( new JSONView( "text/html; charset=utf-8" ), model );
    }
}
