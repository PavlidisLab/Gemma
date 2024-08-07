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
package ubic.gemma.web.util;

/**
 * Signals that an entity was not found in the system.
 * <p>
 * This is translated into a 404 error by the exception resolver.
 * @author pavlidis
 */
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -4361183252269974819L;

    public EntityNotFoundException( String message ) {
        super( message );
    }

    public EntityNotFoundException( String message, Throwable cause ) {
        super( message, cause );
    }

}
