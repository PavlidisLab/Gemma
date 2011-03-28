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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic bean to hold number of sessions.
 * 
 * @author paul
 * @version $Id$
 */
public class UserTracker {

    private static AtomicInteger activeSessions = new AtomicInteger( 0 );

    public static int decrementSessions() {
        
        if (activeSessions.get() < 1){
            activeSessions.set( 0 );
            return 0;
        }
        
        return activeSessions.decrementAndGet();
    }

    public static int incrementSessions() {
        return activeSessions.incrementAndGet();
    }

    public static int getActiveSessions() {
        return activeSessions.get();
    }

}
