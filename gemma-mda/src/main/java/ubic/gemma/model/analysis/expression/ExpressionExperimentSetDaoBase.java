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
 * <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSet
 */
public abstract class ExpressionExperimentSetDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao {

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#create(int, java.util.Collection)
     */
    public java.util.Collection<? extends ExpressionExperimentSet> create( final int transform,
            final java.util.Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionExperimentSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    public Collection<? extends ExpressionExperimentSet > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public ExpressionExperimentSet create( final int transform,
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.create - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSet );
        return this.transformEntity( transform, expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#create(java.util.Collection)
     */
    public java.util.Collection<? extends ExpressionExperimentSet> create(
            final java.util.Collection<? extends ExpressionExperimentSet> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public ExpressionExperimentSet create(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        return this.create( TRANSFORM_NONE, expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#findByName(java.lang.String)
     */
    public java.util.Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public java.util.Collection<ExpressionAnalysis> getAnalyses(
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleGetAnalyses( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao.getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#load(int, java.lang.Long)
     */

    public ExpressionExperimentSet load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.analysis.expression.ExpressionExperimentSet ) entity );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#load(java.lang.Long)
     */

    public ExpressionExperimentSet load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAll()
     */

    public java.util.Collection<? extends ExpressionExperimentSet> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAll(int)
     */

    
    public java.util.Collection<? extends ExpressionExperimentSet> loadAll( final int transform ) {
        final java.util.Collection<? extends ExpressionExperimentSet> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.ExpressionExperimentSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#remove(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public void remove( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.remove - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExpressionExperimentSet> entityIterator = entities
                                .iterator(); entityIterator.hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public void update( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.update - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSet );
    }

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract java.util.Collection<ExpressionAnalysis> handleGetAnalyses(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.analysis.expression.ExpressionExperimentSet)} method. This method
     * does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */

    protected void transformEntities( final int transform,
            final java.util.Collection<? extends ExpressionExperimentSet> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected ExpressionExperimentSet transformEntity( final int transform,
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet entity ) {
        ExpressionExperimentSet target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}