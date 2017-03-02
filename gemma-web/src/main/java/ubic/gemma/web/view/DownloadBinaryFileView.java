/*
 * The Gemma-1.0 project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.view;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Send a file to the browser. The full path to the file must be passed into the model.
 * 
 * @author paul
 * @version $Id$
 */
public class DownloadBinaryFileView extends AbstractView {

    public static final String PATH_PARAM = "filePath";

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.view.AbstractView#renderMergedOutputModel(java.util.Map,
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void renderMergedOutputModel( Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response ) throws Exception {

        String filePath = ( String ) model.get( PATH_PARAM );

        if ( StringUtils.isBlank( filePath ) ) {
            throw new IllegalArgumentException( PATH_PARAM + " was empty" );
        }

        File f = new File( filePath );

        if ( !f.canRead() ) {
            throw new IOException( "Cannot read from " + filePath );
        }

        // response.setContentType( "application/octet-stream" ); // see Bug4206
        response.setContentLength( ( int ) f.length() );
        response.addHeader( "Content-disposition", "attachment; filename=\"" + f.getName() + "\"" );

        InputStream reader = new BufferedInputStream( new FileInputStream( f ) );
        FileCopyUtils.copy( reader, response.getOutputStream() );
        response.flushBuffer();
        reader.close();
    }

}
