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
package edu.columbia.gemma.loader.mage;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.loader.loaderutils.Loader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageLoaderImpl implements Loader {
    private static BeanFactory ctx;

    static {
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );

        // CAREFUL, these paths are dependent on the classpath.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml",
                servletContext + "-servlet.xml" };
        ctx = new ClassPathXmlApplicationContext( paths );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public void create( Collection col ) {
        for ( Iterator iter = col.iterator(); iter.hasNext(); ) {
            Object entity = iter.next();
            
            // figure out the class of the entity, 
            
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(edu.columbia.gemma.genome.Gene)
     */
    public void create( Object Obj ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#removeAll()
     */
    public void removeAll() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#removeAll(java.util.Collection)
     */
    public void removeAll( Collection collection ) {
        // TODO Auto-generated method stub

    }
}
