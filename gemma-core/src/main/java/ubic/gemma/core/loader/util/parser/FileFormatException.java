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
package ubic.gemma.core.loader.util.parser;

/**
 * Use to indicate a file format error.
 * 
 * @author pavlidis
 *
 */
public class FileFormatException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -1984918923089214361L;

    /**
     * 
     */
    public FileFormatException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public FileFormatException( String message, Throwable cause ) {
        super( message, cause );
    }

    /**
     * @param message
     */
    public FileFormatException( String message ) {
        super( message );
    }

    /**
     * @param cause
     */
    public FileFormatException( Throwable cause ) {
        super( cause );
    }

}
