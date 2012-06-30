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
package ubic.gemma;

/**
 * Constant values used throughout the application.
 * <p>
 * Originally from Appfuse
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Constants {
    // ~ Static fields/initializers =============================================

    public static final String APP_NAME = "Gemma";

    /** The name of the ResourceBundle used in this application */
    public static final String BUNDLE_KEY = "messages";

    /** File separator from System properties */
    public static final String FILE_SEP = System.getProperty( "file.separator" );

    /** User home from System properties */
    public static final String USER_HOME = System.getProperty( "user.home" ) + FILE_SEP;

    /**
     * The session scope attribute under which the User object for the currently logged in user is stored.
     */
    public static final String USER_KEY = "currentUserForm";

    /**
     * The request scope attribute under which an editable user form is stored
     */
    public static final String USER_EDIT_KEY = "userForm";

    /**
     * The request scope attribute that holds the user list
     */
    public static final String USER_LIST = "userList";

    /**
     * The name of the user's role list, a request-scoped attribute when adding/editing a user.
     */
    public static final String USER_ROLES = "userRoles";

    /**
     * The name of the available roles list, a request-scoped attribute when adding/editing a user.
     */
    public static final String AVAILABLE_ROLES = "availableRoles";

    /**
     * The name of the configuration hashmap stored in application scope. Variables placed here (in the StartupListener)
     * are available in jsps using $appConfig['key'] synax.
     */
    public static final String CONFIG = "appConfig";
}
