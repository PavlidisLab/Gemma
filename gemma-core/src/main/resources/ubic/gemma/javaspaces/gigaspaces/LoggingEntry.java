/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.javaspaces.gigaspaces;

import com.j_spaces.core.client.MetaDataEntry;

/**
 * @author keshav
 * @version $Id$
 */
public class LoggingEntry extends MetaDataEntry {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Must be public for JavaSpaces reasons.
     */
    public String message = null;

    /**
     * 
     *
     */
    public LoggingEntry() {

    }

    /**
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     */
    public void setMessage( String message ) {
        this.message = message;
    }

    /**
     * Implemented to programmatically allow for indexing of attributes. This indexing speeds up read and take
     * operations.
     * 
     * @return
     */
    public static String[] __getSpaceIndexedFields() {
        String[] indexedFields = { "message" };
        return indexedFields;
    }
}
