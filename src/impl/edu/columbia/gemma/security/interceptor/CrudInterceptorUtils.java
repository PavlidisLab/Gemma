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
package edu.columbia.gemma.security.interceptor;

import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.orm.hibernate3.HibernateInterceptor;

/**
 * Convenience methods needed to monitor CRUD operations on entities.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class CrudInterceptorUtils {

    private static Map metaData;

    /**
     * Determine if cascading an association is required.
     * 
     * @param m
     * @param cs
     * @return
     */
    public static boolean needCascade( Method m, CascadeStyle cs ) {

        if ( methodIsDelete( m ) ) {
            return cs.doCascade( CascadingAction.DELETE );
        }
        return cs.doCascade( CascadingAction.PERSIST ) || cs.doCascade( CascadingAction.SAVE_UPDATE )
                || cs.doCascade( CascadingAction.SAVE_UPDATE_COPY );

    }

    /**
     * @param object
     * @return
     */
    public static EntityPersister getEntityPersister( Object object ) {
        return ( ( EntityPersister ) metaData.get( object.getClass().getName() ) );
    }

    /**
     * 
     *
     */
    public static void initMetaData( HibernateInterceptor hibernateInterceptor ) {
        metaData = hibernateInterceptor.getSessionFactory().getAllClassMetadata();
    }

    /**
     * Test whether a method is a CRUD method.
     * 
     * @param m
     * @return
     */
    public static boolean methodIsCrud( Method m ) {
        return m.getName().equals( "update" ) || m.getName().equals( "create" ) || m.getName().equals( "save" )
                || m.getName().equals( "remove" ) || m.getName().equals( "read" ) || m.getName().startsWith( "find" )
                || m.getName().equals( "delete" ) || m.getName().equals( "load" );
    }

    /**
     * Test whether a method creates objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsCreate( Method m ) {
        return m.getName().equals( "create" ) || m.getName().equals( "save" ) || m.getName().equals( "findOrCreate" );
    }

    /**
     * Test whether a method deletes objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsDelete( Method m ) {
        return m.getName().equals( "remove" ) || m.getName().equals( "delete" );
    }

    /**
     * Test whether a method updates objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsUpdate( Method m ) {
        return m.getName().equals( "update" );
    }

    /**
     * Test whether a method loads objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsLoad( Method m ) {
        return m.getName().equals( "read" ) || m.getName().startsWith( "find" ) || m.getName().equals( "delete" )
                || m.getName().equals( "load" );
    }

}
