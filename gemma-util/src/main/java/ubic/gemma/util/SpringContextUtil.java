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
package ubic.gemma.util;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Methods to create Spring contexts. For tests see ubic.gemma.testing.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );
    private static BeanFactory ctx = null;

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @return BeanFactory
     */
    public static BeanFactory getApplicationContext( boolean testing ) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations( testing );
            ctx = new ClassPathXmlApplicationContext( paths );
            if ( ctx != null ) {
                log.info( "Got context" );
            } else {
                log.error( "Failed to load context" );
            }
        }
        return ctx;
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @return
     */
    public static String[] getConfigLocations() {
        return getConfigLocations( false );
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @param testing - if true, it will use the test configuration.
     * @return
     */
    public static String[] getConfigLocations( boolean testing ) {
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        if ( testing ) {
            log.warn( "************** Using test configuration ***************" );
            return new String[] { "classpath*:/ubic/gemma/localTestDataSource.xml",
                    "classpath*:/ubic/gemma/applicationContext-" + daoType + ".xml",
                    "classpath*:/ubic/gemma/applicationContext-security.xml",
                 //   "classpath*:" + servletContext + "-servlet.xml",
                    "classpath*:/ubic/gemma/applicationContext-validation.xml",
                    "classpath*:/ubic/gemma/applicationContext-serviceBeans.xml" };
        }
        return new String[] { "classpath*:/ubic/gemma/applicationContext-resources.xml",
                "classpath*:/ubic/gemma/localDataSource.xml",
                "classpath*:/ubic/gemma/applicationContext-" + daoType + ".xml",
                "classpath*:/ubic/gemma/applicationContext-security.xml",
                "classpath*:" + servletContext + "-servlet.xml",
                "classpath*:/ubic/gemma/applicationContext-validation.xml",
                "classpath*:/ubic/gemma/applicationContext-serviceBeans.xml" };

    }

}
