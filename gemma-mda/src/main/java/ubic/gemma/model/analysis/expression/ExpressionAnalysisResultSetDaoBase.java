/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet
 */
public abstract class ExpressionAnalysisResultSetDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao {

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends ExpressionAnalysisResultSet> create(
            final java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    
    public Collection<? extends ExpressionAnalysisResultSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionAnalysisResultSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    public ExpressionAnalysisResultSet create(
            final ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.create - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionAnalysisResultSet );
        return expressionAnalysisResultSet;

    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#load(int, java.lang.Long)
     */
    public ExpressionAnalysisResultSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetImpl.class, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#loadAll(int)
     */
    public java.util.Collection<? extends ExpressionAnalysisResultSet> loadAll() {
        return this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetImpl.class );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#remove(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    public void remove( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.remove - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionAnalysisResultSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    public void thaw( final ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet ) {
        try {
            this.handleThaw( resultSet );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao.thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionAnalysisResultSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao#update(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    public void update( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.update - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionAnalysisResultSet );
    }

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)}
     */
    protected abstract void handleThaw( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet )
            throws java.lang.Exception;

}