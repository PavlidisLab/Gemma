/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Controller tests must extend this to make use of the XmlWebApplicationContext.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author raible
 * @version $Id$
 */
public class BaseControllerTestCase extends TestCase {
    protected final static BeanFactory ctx = SpringContextUtil.getXmlWebApplicationContext();

    /* authentication */
    ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
            .getBean( "manualAuthenticationProcessing" );

    // ~ Constructors ===========================================================

    public BaseControllerTestCase() {

        /* set user */
        String username = null;
        String password = null;
        try {
            Configuration conf = new PropertiesConfiguration( "Gemma.properties" );
            username = conf.getString( "acegi.authorization.username" );
            password = conf.getString( "acegi.authorization.password" );
        } catch ( ConfigurationException e ) {
            e.printStackTrace();
        }

        manAuthentication.validateRequest( username, password );
    }
}
