/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Used to allow downstream processing to figure out multipart resolution failed without throwing an exception. This
 * lets us return JSON that our client can understand.
 *
 * @author paul
 */
public class FailedMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

    private String errorMessage;

    public FailedMultipartHttpServletRequest( HttpServletRequest request, String message ) {
        this( request );
        this.errorMessage = message;
    }

    protected FailedMultipartHttpServletRequest( HttpServletRequest request ) {
        super( request );
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public HttpHeaders getMultipartHeaders( String arg0 ) {
        return null;
    }

    @Override
    public String getMultipartContentType( String arg0 ) {
        return null;
    }

}
