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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet
 */
public abstract class ExpressionAnalysisResultSetDaoBase extends HibernateDaoSupport implements
        ExpressionAnalysisResultSetDao {

    /**
     * @see ExpressionAnalysisResultSetDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends ExpressionAnalysisResultSet> create(
            final java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionAnalysisResultSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ExpressionAnalysisResultSetDao#create(int transform, ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet create( final ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.create - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionAnalysisResultSet );
        return expressionAnalysisResultSet;

    }

    @Override
    public Collection<? extends ExpressionAnalysisResultSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionAnalysisResultSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#load(int, java.lang.Long)
     */
    @Override
    public ExpressionAnalysisResultSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ExpressionAnalysisResultSetImpl.class, id );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends ExpressionAnalysisResultSet> loadAll() {
        return this.getHibernateTemplate().loadAll( ExpressionAnalysisResultSetImpl.class );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.remove - 'id' can not be null" );
        }
        ExpressionAnalysisResultSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#remove(ExpressionAnalysisResultSet)
     */
    @Override
    public void remove( ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.remove - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionAnalysisResultSet );
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet thaw( final ExpressionAnalysisResultSet resultSet ) {
        return this.handleThaw( resultSet );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends ExpressionAnalysisResultSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionAnalysisResultSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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
     * @see ExpressionAnalysisResultSetDao#update(ExpressionAnalysisResultSet)
     */
    @Override
    public void update( ExpressionAnalysisResultSet expressionAnalysisResultSet ) {
        if ( expressionAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionAnalysisResultSet.update - 'expressionAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionAnalysisResultSet );
    }

    /**
     * Performs the core logic for {@link #thaw(ExpressionAnalysisResultSet)}
     * 
     * @return
     */
    protected abstract ExpressionAnalysisResultSet handleThaw( ExpressionAnalysisResultSet resultSet );

}