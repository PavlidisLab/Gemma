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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.loader.loaderutils.Loader;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MageLoaderImpl implements Loader {
    private static Log log = LogFactory.getLog( MageLoaderImpl.class.getName() );
    private static BeanFactory ctx;

    static {
        log.debug( "Loading context" );
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
        log.debug( "Entering MageLoaderImpl.create()" );
        try {

            for ( Object entity : col ) {

                // // only persist it if its parent is outside edu.columbia.gemma.
                // if ( entity.getClass().getSuperclass().getName().startsWith( "edu.columbia" ) ) continue;

                String className = entity.getClass().getName();

                // check if className is on short list of classes to be persisted.
                // ArrayDesign (we won't usually use this)
                // ExpressionExperiment (most interested in this)
                // 
                if ( className.lastIndexOf( "ExpressionExperiment" ) < 0 ) continue; // FIXME

                // figure out the class of the entity,
                String dao = ReflectionUtil.constructDaoName( entity );
                Object daoObj = ctx.getBean( dao );

                log.debug( "Persisting: " + entity.getClass().getName() + " with " + daoObj.getClass().getName() );

                // get create method
                Method createMethod = daoObj.getClass().getMethod( "create",
                        new Class[] { ReflectionUtil.getBaseForImpl( entity ) } );

                // run create, but check exists first TODO. (need special cases)
                createMethod.invoke( daoObj, new Object[] { entity } );
            }
        } catch ( Exception e ) {
            log.error( e, e );
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
