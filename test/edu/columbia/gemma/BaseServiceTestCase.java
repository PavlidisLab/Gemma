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
package edu.columbia.gemma;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.util.SpringContextUtil;

/**
 * This adds to the DAO base in that it grants a 'test' permissions to the application.
 * <p>
 * This partly code from AppFuse. Provides utilities to read in objects from resources (properties files).
 * 
 * @author pavlidis
 * @author raible
 * @version $Id$
 */
public class BaseServiceTestCase extends BaseDAOTestCase {

    protected final Log log = LogFactory.getLog( getClass() );
    protected ResourceBundle rb;

    public BaseServiceTestCase() {

        SpringContextUtil.grantAuthorityForTests();

        // Since a ResourceBundle is not required for each class, just
        // do a simple check to see if one exists
        String className = this.getClass().getName();

        try {
            rb = ResourceBundle.getBundle( className );

        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
        }
    }

}