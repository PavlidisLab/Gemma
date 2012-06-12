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

import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.hql.ast.ASTQueryTranslatorFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Convenience methods for doing native SQL queries, modeled after the Spring hibernateTemplate methods of the same
 * names.
 * 
 * @author Paul
 * @version $Id$
 * @see org.springframework.orm.hibernate3.HibernateTemplate
 */
public class NativeQueryUtils {

    /**
     * Queries are read-only (no updates).
     * 
     * @param hibernatetemplate
     * @param queryString with no parameters
     * @return a single object
     */
    public static List<?> find( HibernateTemplate hibernateTemplate, final String queryString ) {
        return hibernateTemplate.execute( new HibernateCallback<List<?>>() {
            @Override
            public List<?> doInHibernate( Session session ) throws HibernateException {
                SQLQuery queryObject = session.createSQLQuery( queryString );
                queryObject.setReadOnly( true );
                return queryObject.list();
            }
        } );

    }

    /**
     * Queries are read-only (no updates).
     * 
     * @param hibernateTemplate
     * @param queryString
     * @param paramName
     * @param param
     * @return
     */
    public static List<?> findByNamedParam( HibernateTemplate hibernateTemplate, final String queryString,
            final String paramName, final Object param ) {
        return hibernateTemplate.execute( new HibernateCallback<List<?>>() {
            @Override
            public List<?> doInHibernate( Session session ) throws HibernateException {
                SQLQuery queryObject = session.createSQLQuery( queryString );
                queryObject.setReadOnly( true );
                queryObject.setParameter( paramName, param );
                return queryObject.list();
            }
        } );
    }

    /**
     * Queries are read-only (no updates).
     * 
     * @param hibernateTemplate
     * @param queryString
     * @param paramNames
     * @param params
     * @return
     */
    public static List<?> findByNamedParams( HibernateTemplate hibernateTemplate, final String queryString,
            final String[] paramNames, final Object[] params ) {
        return hibernateTemplate.execute( new HibernateCallback<List<?>>() {
            @Override
            public List<?> doInHibernate( Session session ) throws HibernateException {
                SQLQuery queryObject = session.createSQLQuery( queryString );
                queryObject.setReadOnly( true );
                for ( int i = 0; i < paramNames.length; i++ ) {
                    queryObject.setParameter( paramNames[i], params[i] );
                }
                return queryObject.list();
            }
        } );
    }

    /**http://narcanti.keyboardsamurais.de/hibernate-hql-to-sql-translation.html
     * @param hibernateTemplate
     * @param hqlQueryText
     * @return
     */
    public static String toSql( HibernateTemplate hibernateTemplate, String hqlQueryText ) {
        final QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();
        final SessionFactoryImplementor factory = ( SessionFactoryImplementor ) hibernateTemplate.getSessionFactory();
        final QueryTranslator translator = translatorFactory.createQueryTranslator( hqlQueryText, hqlQueryText,
                Collections.EMPTY_MAP, factory );
        translator.compile( Collections.EMPTY_MAP, false );
        return translator.getSQLString();

    }

}
