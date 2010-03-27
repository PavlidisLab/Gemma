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
package ubic.gemma.job.grid.util;

import ubic.gemma.job.grid.worker.SpacesRegistrationEntry;

import com.j_spaces.core.client.MetaDataEntry;

/**
 * This class is handy for testing the javaspaces notifications mechanism. It wraps messages on the server side and
 * sends them back to the client.
 * <p>
 * This class can be extended and used by workers (in a master-worker pattern) to "register" themselves with a space.
 * Workers are actually automatically registered with a space when the worker is started, but extending this class for
 * registraton purposes allows clients to detect both which workers and how many are registered with a given space. See
 * {@link SpacesRegistrationEntry}.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesGenericEntry extends MetaDataEntry {

    private static final long serialVersionUID = 1L;

    /**
     * Implemented to programmatically allow for indexing of attributes. This indexing speeds up read and take
     * operations.
     * 
     * @return String[]
     */
    public static String[] __getSpaceIndexedFields() {
        String[] indexedFields = { "message" };
        return indexedFields;
    }

    /**
     * Must be public for JavaSpaces reasons.
     */
    public String host = null;
    /**
     * Must be public for JavaSpaces reasons.
     */
    public String message = null;

    /**
     * Must be public for JavaSpaces reasons.
     */
    public String registrationId = null;

    /**
     * Must be public for JavaSpaces reasons.
     */
    public String taskId;

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @return String
     */
    public String getHost() {
        return host;
    }

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @return String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @return Long
     */
    public String getRegistrationId() {
        return registrationId;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @param host
     */
    public void setHost( String host ) {
        this.host = host;
    }

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @param message
     */
    public void setMessage( String message ) {
        this.message = message;
    }

    /**
     * Added for conventional reasons. This is not needed since the field is public (required by JavaSpaces).
     * 
     * @param registrationId
     */
    public void setRegistrationId( String registrationId ) {
        this.registrationId = registrationId;
    }

    /**
     * @param taskId the taskId to set
     */
    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }
}
