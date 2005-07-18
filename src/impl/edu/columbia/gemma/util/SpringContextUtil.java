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
package edu.columbia.gemma.util;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );
    private static BeanFactory ctx = null;

    /**
     * @return
     */
    public static BeanFactory getApplicationContext() {
        // try {
        // BeanFactoryLocator bfl = SingletonBeanFactoryLocator.getInstance();
        // BeanFactoryReference bfr = bfl.useBeanFactory( "beanRefFactory" );
        // BeanFactory bf = bfr.getFactory();
        // if ( bf != null ) {
        // log.debug( "Got factory with SingletonBeanFactoryLocator" );
        // return bf;
        // }
        // } catch ( FatalBeanException e ) {
        // log.debug( "No factory found using SingletonBeanFactoryLocator, getting from classpath" );
        // }

        if ( ctx == null ) {
            String[] paths = getConfigLocations();
            ctx = new ClassPathXmlApplicationContext( paths );
        }
        return ctx;
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @return
     */
    public static String[] getConfigLocations() {
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-localDataSource.xml", "applicationContext-" + daoType + ".xml",
                servletContext + "-servlet.xml"};
        return paths;
    }

}
