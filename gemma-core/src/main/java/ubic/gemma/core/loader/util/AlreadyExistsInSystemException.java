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
package ubic.gemma.core.loader.util;

/**
 * Can be thrown when an attempt is made to load data into the system that already exists. Intended to be used when
 * simply returning the existing data would be confusing. It can hold a reference to the data that was existing.
 * 
 * @author pavlidis
 *
 */
public class AlreadyExistsInSystemException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 5677999264920938691L;
    Object data;

    /**
     * @return the data
     */
    public Object getData() {
        return this.data;
    }

    /**
     * 
     */
    public AlreadyExistsInSystemException() {
        super();
    }

    /**
     * @param message
     */
    public AlreadyExistsInSystemException( String message ) {
        super( message );
    }

    /**
     * @param data The data that already existed.
     */
    public AlreadyExistsInSystemException( Object data ) {
        super();
        this.data = data;
    }

    /**
     * @param message
     * @param data The data that already existed.
     */
    public AlreadyExistsInSystemException( String message, Object data ) {
        super( message );
        this.data = data;
    }

}