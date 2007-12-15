/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * Convenience methods for doing queries
 * 
 * @author Paul
 * @version $Id$
 * @deprecated Use hibernateTemplate methods instead.
 */
public class QueryUtils {

    private static Log log = LogFactory.getLog( QueryUtils.class.getName() );

    /**
     * Run a read-only query
     * 
     * @param queryString with no parameters
     * @return a single object
     * @throws InvalidDataAccessResourceUsageException if more than one object is returned
     * @throws DataAccessException on other errors
     * @deprecated
     */
    public static Object query( Session session, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance was found when executing query --> '" + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * @deprecated
     * @param session
     * @param queryString
     * @return
     */
    public static Collection queryForCollection( Session session, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a read-only query
     * 
     * @param id
     * @param queryString with parameter "id"
     * @return a single Object, even if the query actually returns more.
     * @deprecated
     */
    public static Object queryById( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            queryObject.setParameter( "id", id );
            // queryObject.setMaxResults( 1 );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null && results.size() > 0 ) {
                return results.iterator().next();
            }
            log.warn( "Nothing found for id=" + id );
            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a read-only query
     * 
     * @param id with parameter "id"
     * @param queryString
     * @return
     * @deprecated
     */
    public static Collection queryByIdReturnCollection( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            queryObject.setParameter( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a read-only query
     * 
     * @param id with parameter "id"
     * @param queryString
     * @param limit how many records to return; set to 0 or a negative to not use a limit.
     * @return
     * @deprecated
     */
    public static Collection queryByIdReturnCollection( Session session, Long id, final String queryString, int limit ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            queryObject.setParameter( "id", id );
            if ( limit >= 0 ) {
                queryObject.setMaxResults( limit );
            }
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a read-only query
     * 
     * @param ids
     * @param queryString with parameter "ids"
     * @return a single Object
     * @deprecated
     */
    public static Object queryByIds( Session session, Collection<Long> ids, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString ).setReadOnly( true );
            queryObject.setParameterList( "ids", ids );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance was found when executing query --> '" + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a native read-only SQL query with no parameters.
     * 
     * @param queryString
     * @return Collection of records (Object[])
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> nativeQuery( Session session, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createSQLQuery( queryString ).setReadOnly( true );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }

    }

    /**
     * Run a native read-only SQL query with a single 'id' parameter
     * 
     * @param id
     * @param queryString
     * @return Collection of records (Object[])
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> nativeQueryById( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createSQLQuery( queryString ).setReadOnly( true );
            queryObject.setLong( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

}
