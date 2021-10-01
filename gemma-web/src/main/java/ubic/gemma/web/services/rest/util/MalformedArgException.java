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
package ubic.gemma.web.services.rest.util;

import javax.ws.rs.BadRequestException;

/**
 * Specialized error for malformed {@link ubic.gemma.web.services.rest.util.args.Arg}
 *
 * The recommended HTTP status for this exception is 400 Bad Request.
 *
 * @author poirigui
 */
public class MalformedArgException extends BadRequestException {

    public MalformedArgException( String message, Throwable cause ) {
        super( message, cause );
    }
}
