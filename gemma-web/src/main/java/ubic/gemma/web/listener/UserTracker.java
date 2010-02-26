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
package ubic.gemma.web.listener;

/**
 * Basic bean to hold number of authenticated users and sessions.
 * 
 * @author paul
 * @version $Id$
 */
public class UserTracker {

    private static int activeSessions = 0;

    private static int authenticatedUsers = 0;

    public static int decrementSessions() {
        return --activeSessions;
    }

    public static int incrementSessions() {
        return ++activeSessions;
    }

    public static int incrementAuthenticatedUsers() {
        return ++authenticatedUsers;
    }

    public static int decrementAuthenticatedUsers() {
        return --authenticatedUsers;
    }

    public static int getActiveSessions() {
        return activeSessions;
    }

    public static int getAuthenticatedUsers() {
        return authenticatedUsers;
    }

}
