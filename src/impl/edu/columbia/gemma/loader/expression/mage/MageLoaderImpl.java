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
package edu.columbia.gemma.loader.expression.mage;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.loaderutils.Loader;
import edu.columbia.gemma.util.ReflectionUtil;
import edu.columbia.gemma.util.SpringContextUtil;

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

    private Person defaultOwner = null;

    /**
     * This class needs direct access to the context because it uses reflection to find daos.
     */
    private static BeanFactory ctx;

    /**
     * 
     *
     */
    public MageLoaderImpl() {
        ctx = SpringContextUtil.getApplicationContext();
        initializeDefaultOwner();
    }

    /**
     * Fetch the fallback owner to use for newly-imported data.
     */
    private void initializeDefaultOwner() {
        PersonDao personDao = ( PersonDao ) ctx.getBean( "personDao" );
        Collection<Person> matchingPersons = personDao.findByFullName( "nobody", "nobody", "nobody" );

        assert matchingPersons.size() == 1;

        defaultOwner = matchingPersons.iterator().next();

        if ( defaultOwner == null ) throw new NullPointerException( "Default Person 'nobody' not found in database." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Loader#create(java.util.Collection)
     */
    public void create( Collection col ) {
        log.debug( "Entering MageLoaderImpl.create()" );
        assert defaultOwner != null;
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

                if ( ( ( ExpressionExperiment ) entity ).getOwner() == null ) {
                    ( ( ExpressionExperiment ) entity ).setOwner( defaultOwner );
                }

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
