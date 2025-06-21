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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.text.StringEscapeUtils;
import org.directwebremoting.WebContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import ubic.gemma.web.controller.util.upload.FailedMultipartHttpServletRequest;
import ubic.gemma.web.controller.util.upload.FileUploadUtil;
import ubic.gemma.web.controller.util.upload.UploadInfo;
import ubic.gemma.web.controller.util.view.JSONView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller class to upload Files.
 *
 * @author paul
 * @author keshav
 * @author Traces of Matt Raible
 */
@CommonsLog
public class FileUploadController extends AbstractController {

    @Value("${gemma.download.path}/userUploads")
    private Path uploadDir;

    public UploadInfo getUploadStatus() {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        if ( req.getSession().getAttribute( "uploadInfo" ) != null )
            return ( UploadInfo ) req.getSession().getAttribute( "uploadInfo" );

        return new UploadInfo();
    }

    /**
     * Ajax. DWR can handle this.
     *
     */
    public String upload( InputStream is ) throws IOException {
        Path copiedFile = FileUploadUtil.copyUploadedInputStream( is, uploadDir );
        FileUploadController.log.info( "DWR Uploaded file!" );
        return copiedFile.toString();
    }

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response ) {

        /*
         * At this point, the DispatcherServlet has already dealt with the multipart file via the
         * CommonsMultipartMonitoredResolver.
         */

        if ( !( request instanceof MultipartHttpServletRequest ) ) {
            return null;
        }

        Map<String, Object> model = new HashMap<>();

        if ( request instanceof FailedMultipartHttpServletRequest ) {
            String errorMessage = ( ( FailedMultipartHttpServletRequest ) request ).getErrorMessage();
            model.put( "success", false );
            model.put( "error", errorMessage );
        } else {

            MultipartHttpServletRequest mrequest = ( MultipartHttpServletRequest ) request;
            Map<String, MultipartFile> fileMap = mrequest.getFileMap();

            if ( fileMap.size() > 1 ) {
                FileUploadController.log.error( "Attempted to upload multiple files, returning error" );
                model.put( "success", false );
                model.put( "error", "Sorry, can't upload more than one file at a time yet" );
            }

            for ( String key : fileMap.keySet() ) {
                MultipartFile multipartFile = fileMap.get( key );
                Path copiedFile = null;
                try {
                    copiedFile = FileUploadUtil.copyUploadedFile( multipartFile, request, uploadDir );
                    FileUploadController.log.info( "Uploaded file: " + copiedFile );
                    model.put( "success", true );
                    model.put( "localFile", StringEscapeUtils.escapeJava( copiedFile.toString() ) );
                    model.put( "originalFile", multipartFile.getOriginalFilename() );
                    model.put( "size", multipartFile.getSize() );

                } catch ( Exception e ) {
                    FileUploadController.log.error( "Error in upload: " + e.getMessage(), e );
                    model.put( "success", false );
                    model.put( "error", e.getMessage() );
                }

                if ( copiedFile == null ) {
                    FileUploadController.log.error( "Error in upload: unknown problem getting file" );
                    model.put( "success", false );
                    model.put( "error", "unknown problem getting file" );
                }

            }
        }

        return new ModelAndView( new JSONView( "text/html; charset=utf-8" ), model );
    }
}
