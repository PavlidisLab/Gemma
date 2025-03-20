package ubic.gemma.web.util;

/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

import java.util.Locale;

/**
 * Provides methods for putting messages to the user in the session.
 * <p>
 * Messages accumulate in a list until they are viewed in messages.jsp - at which point they are removed from the
 * session.
 * @author paul
 */
public interface MessageUtil {

    /**
     * Put a message into the session.
     *
     * @param parameter      parameter to be filled into the message.
     * @param defaultMessage default message
     */
    void saveMessage( String key, Object parameter, String defaultMessage );

    /**
     * Put a message into the session.
     *
     * @param parameters     array of parameters to be filled into the message.
     * @param defaultMessage default message
     */
    void saveMessage( String key, Object[] parameters, String defaultMessage );

    /**
     * Put a message into the session.
     *
     * @param key            key
     * @param defaultMessage default message
     */
    void saveMessage( String key, String defaultMessage );

    /**
     * Put a message into the session.
     */
    void saveMessage( String msg );
}