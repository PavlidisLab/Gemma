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

    /**
     * The name of the configuration hashmap stored in application scope. Variables placed here (in the StartupListener)
     * are available in jsps using $appConfig['key'] synax.
     */
    public static final String CONFIG = "appConfig";
}
