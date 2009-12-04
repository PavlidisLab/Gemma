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
import java.util.Iterator;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ExpressionExperimentSet</code>.
 * </p>
 * 
 * @see ExpressionExperimentSet
 */
public abstract class ExpressionExperimentSetDaoBase extends HibernateDaoSupport implements ExpressionExperimentSetDao {

    /**
     * @see ExpressionExperimentSetDao#create(int, Collection)
     */
    public Collection<? extends ExpressionExperimentSet> create( final int transform,
            final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ExpressionExperimentSet> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ExpressionExperimentSetDao#create(int transform, ExpressionExperimentSet)
     */
    public ExpressionExperimentSet create( final int transform, final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.create - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSet );
        return this.transformEntity( transform, expressionExperimentSet );
    }

    /**
     * @see ExpressionExperimentSetDao#create(Collection)
     */
    public Collection<? extends ExpressionExperimentSet> create(
            final Collection<? extends ExpressionExperimentSet> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ExpressionExperimentSetDao#create(ExpressionExperimentSet)
     */
    public ExpressionExperimentSet create( ExpressionExperimentSet expressionExperimentSet ) {
        return this.create( TRANSFORM_NONE, expressionExperimentSet );
    }

    /**
     * @see ExpressionExperimentSetDao#findByName(java.lang.String)
     */
    public Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.findByName(java.lang.String name)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentSetDao#getAnalyses(ExpressionExperimentSet)
     */
    public Collection<ExpressionAnalysis> getAnalyses( final ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleGetAnalyses( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.getAnalyses(ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends ExpressionExperimentSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ExpressionExperimentSetDao#load(int, java.lang.Long)
     */

    public ExpressionExperimentSet load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ExpressionExperimentSetImpl.class, id );
        return transformEntity( transform, ( ExpressionExperimentSet ) entity );
    }

    /**
     * @see ExpressionExperimentSetDao#load(java.lang.Long)
     */

    public ExpressionExperimentSet load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ExpressionExperimentSetDao#loadAll()
     */

    public Collection<? extends ExpressionExperimentSet> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ExpressionExperimentSetDao#loadAll(int)
     */

    public Collection<? extends ExpressionExperimentSet> loadAll( final int transform ) {
        final Collection<? extends ExpressionExperimentSet> results = this.getHibernateTemplate().loadAll(
                ExpressionExperimentSetImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ExpressionExperimentSetDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'id' can not be null" );
        }
        ExpressionExperimentSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    public void remove( Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionExperimentSetDao#remove(ExpressionExperimentSet)
     */
    public void remove( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.remove - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    public void update( final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ExpressionExperimentSet> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ExpressionExperimentSetDao#update(ExpressionExperimentSet)
     */
    public void update( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.update - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSet );
    }

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<ExpressionExperimentSet> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAnalyses(ExpressionExperimentSet)}
     */
    protected abstract Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,ExpressionExperimentSet)} method. This
     * method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ExpressionExperimentSetDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ExpressionExperimentSet)
     */

    protected void transformEntities( final int transform, final Collection<? extends ExpressionExperimentSet> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in <code>ExpressionExperimentSetDao</code>,
     * please note that the {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be
     * returned. If the integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ExpressionExperimentSetDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,Collection)
     */
    protected ExpressionExperimentSet transformEntity( final int transform, final ExpressionExperimentSet entity ) {
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