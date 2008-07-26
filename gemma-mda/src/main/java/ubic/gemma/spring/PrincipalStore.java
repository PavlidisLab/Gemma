/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.spring;

/**
 * Stores the currently logged in Principal. The principal is passed from another tier of the application (i.e. the web
 * application).
 */
public final class PrincipalStore {
    private static final ThreadLocal<java.security.Principal> store = new ThreadLocal<java.security.Principal>();

    /**
     * Get the user <code>principal</code> for the currently executing thread.
     * 
     * @return the current principal.
     */
    public static java.security.Principal get() {
        return store.get();
    }

    /**
     * Set the <code>principal</code> for the currently executing thread.
     * 
     * @param name the user principal
     */
    public static void set( final java.security.Principal principal ) {
        store.set( principal );
    }
}
